package com.juga.platform.physics

import com.juga.platform.data.LevelData
import com.juga.platform.data.TrackSegment
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PhysicsWorld(private val level: LevelData) {
    private val gravity = 12f
    private val linearDamping = 0.995f
    private val angularDamping = 0.98f
    private val solverIterations = 10
    private val motorForce = 1.2f
    private val brakeForce = 2.2f
    private val leanGround = 18f
    private val leanAir = 26f

    val rearWheel = RigidBody2D(Vec2(level.start.x - 0.7f, level.start.y), Vec2(0f, 0f), mass = 1f, inertia = 0.2f, radius = 0.35f)
    val frontWheel = RigidBody2D(Vec2(level.start.x + 0.7f, level.start.y), Vec2(0f, 0f), mass = 1f, inertia = 0.2f, radius = 0.35f)
    val chassis = RigidBody2D(Vec2(level.start.x, level.start.y - 0.6f), Vec2(0f, 0f), mass = 3f, inertia = 1.2f, radius = 0.22f)

    private val constraints = listOf(
        DistanceConstraint(chassis, rearWheel, 0.95f),
        DistanceConstraint(chassis, frontWheel, 0.95f),
        DistanceConstraint(rearWheel, frontWheel, 1.4f, stiffness = 0.24f)
    )

    var crashed = false
    var finished = false

    fun step(dt: Float, inputUp: Boolean, inputDown: Boolean, inputLeft: Boolean, inputRight: Boolean) {
        if (crashed || finished) return

        val oldRear = rearWheel.position.copy()
        val oldFront = frontWheel.position.copy()

        chassis.integrate(dt, gravity, linearDamping, angularDamping)
        rearWheel.integrate(dt, gravity, linearDamping, angularDamping)
        frontWheel.integrate(dt, gravity, linearDamping, angularDamping)

        val rearContact = Collision.solveCircleTrack(
            body = rearWheel,
            oldPos = oldRear,
            segments = level.segments,
            frictionMu = 1.2f,
            brake = inputDown,
            motorForce = if (inputUp) motorForce else if (inputDown) -brakeForce else 0f
        )
        val frontContact = Collision.solveCircleTrack(
            body = frontWheel,
            oldPos = oldFront,
            segments = level.segments,
            frictionMu = if (inputDown) 1.8f else 1.2f,
            brake = inputDown,
            motorForce = 0f
        )

        val grounded = rearContact != null || frontContact != null
        val torque = when {
            inputLeft -> -1f
            inputRight -> 1f
            else -> 0f
        }
        val leanTorque = if (grounded) leanGround else leanAir
        chassis.angularVelocity += torque * leanTorque * dt * 0.11f
        chassis.angularVelocity = chassis.angularVelocity.coerceIn(-8f, 8f)

        repeat(solverIterations) {
            constraints.forEach { it.solve(dt) }
            Collision.solveCircleTrack(rearWheel, rearWheel.position.copy(), level.segments, 1.2f, inputDown, if (inputUp) motorForce else 0f)
            Collision.solveCircleTrack(frontWheel, frontWheel.position.copy(), level.segments, if (inputDown) 1.8f else 1.2f, inputDown, 0f)
        }

        val axleAngle = kotlin.math.atan2(frontWheel.position.y - rearWheel.position.y, frontWheel.position.x - rearWheel.position.x)
        chassis.angle = chassis.angle * 0.85f + axleAngle * 0.15f

        if (headHit(level.segments)) crashed = true
        if (chassis.position.x >= level.finishX) finished = true
    }

    private fun headHit(segments: List<TrackSegment>): Boolean {
        val hx = chassis.position.x + cos(chassis.angle) * 0.42f
        val hy = chassis.position.y + sin(chassis.angle) * 0.42f - 0.14f
        val speed = chassis.velocity.len()
        val minD = minDistanceToSegments(Vec2(hx, hy), segments)
        if (minD < 0.08f && speed > 2.4f) return true
        if (abs(chassis.angularVelocity) > 7.6f && minD < 0.12f) return true
        return false
    }

    private fun minDistanceToSegments(p: Vec2, segments: List<TrackSegment>): Float {
        var d = Float.MAX_VALUE
        for (s in segments) {
            val q = nearest(p, s)
            val dx = p.x - q.x
            val dy = p.y - q.y
            val c = kotlin.math.sqrt(dx * dx + dy * dy)
            if (c < d) d = c
        }
        return d
    }

    private fun nearest(p: Vec2, s: TrackSegment): Vec2 {
        val abx = s.x2 - s.x1
        val aby = s.y2 - s.y1
        val apx = p.x - s.x1
        val apy = p.y - s.y1
        val den = abx * abx + aby * aby
        val t = if (den <= 1e-5f) 0f else (apx * abx + apy * aby) / den
        val clamped = t.coerceIn(0f, 1f)
        return Vec2(s.x1 + abx * clamped, s.y1 + aby * clamped)
    }
}
