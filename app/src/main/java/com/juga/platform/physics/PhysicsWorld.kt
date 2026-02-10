package com.juga.platform.physics

import com.juga.platform.data.LevelData
import com.juga.platform.data.TrackSegment
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PhysicsWorld(private val level: LevelData) {
    private val gravity = 12f
    private val linearDamping = 0.998f
    private val angularDamping = 0.985f
    private val solverIterations = 10
    private val motorForce = 7.2f
    private val brakeForce = 5.8f
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
    var rearGrounded = false
    var frontGrounded = false
    var lastRearNormalImpulse = 0f
    var lastRearFrictionImpulse = 0f

    fun step(dt: Float, inputUp: Boolean, inputDown: Boolean, inputLeft: Boolean, inputRight: Boolean) {
        if (crashed || finished) return

        val oldRear = rearWheel.position.copy()
        val oldFront = frontWheel.position.copy()

        chassis.integrate(dt, gravity, linearDamping, angularDamping)
        rearWheel.integrate(dt, gravity, linearDamping, angularDamping)
        frontWheel.integrate(dt, gravity, linearDamping, angularDamping)

        rearGrounded = false
        frontGrounded = false
        lastRearNormalImpulse = 0f
        lastRearFrictionImpulse = 0f

        var rearContact = Collision.solveCircleTrack(
            body = rearWheel,
            oldPos = oldRear,
            segments = level.segments,
            frictionMu = if (inputDown) 1.7f else 1.25f,
            brake = inputDown,
            motorForce = 0f
        )
        var frontContact = Collision.solveCircleTrack(
            body = frontWheel,
            oldPos = oldFront,
            segments = level.segments,
            frictionMu = if (inputDown) 1.8f else 1.2f,
            brake = inputDown,
            motorForce = 0f
        )

        rearGrounded = rearContact?.normal?.y ?: 0f < -0.2f
        frontGrounded = frontContact?.normal?.y ?: 0f < -0.2f

        if (inputUp && rearContact != null) {
            rearWheel.velocity.x += rearContact.tangent.x * motorForce * dt
            rearWheel.velocity.y += rearContact.tangent.y * motorForce * dt
        }
        if (inputDown && rearContact != null) {
            rearWheel.velocity.x -= rearContact.tangent.x * brakeForce * dt
            rearWheel.velocity.y -= rearContact.tangent.y * brakeForce * dt
            rearWheel.velocity.x *= 0.985f
            rearWheel.velocity.y *= 0.985f
        }

        val grounded = rearGrounded || frontGrounded
        val leanTorque = if (grounded) leanGround else leanAir
        val leanInput = when {
            inputLeft -> -1f
            inputRight -> 1f
            else -> 0f
        }
        chassis.angularVelocity += leanInput * leanTorque * dt * 0.11f
        chassis.angularVelocity = chassis.angularVelocity.coerceIn(-8f, 8f)

        repeat(solverIterations) {
            constraints.forEach { it.solve(dt) }
            rearContact = Collision.solveCircleTrack(
                rearWheel,
                rearWheel.position.copy(),
                level.segments,
                if (inputDown) 1.7f else 1.25f,
                inputDown,
                if (inputUp && rearGrounded) motorForce * 0.15f else 0f
            )
            frontContact = Collision.solveCircleTrack(
                frontWheel,
                frontWheel.position.copy(),
                level.segments,
                if (inputDown) 1.8f else 1.2f,
                inputDown,
                0f
            )
        }

        rearGrounded = rearContact?.normal?.y ?: 0f < -0.2f
        frontGrounded = frontContact?.normal?.y ?: 0f < -0.2f
        lastRearNormalImpulse = rearContact?.normalImpulse ?: 0f
        lastRearFrictionImpulse = rearContact?.frictionImpulse ?: 0f

        val axleAngle = kotlin.math.atan2(frontWheel.position.y - rearWheel.position.y, frontWheel.position.x - rearWheel.position.x)
        chassis.angle = chassis.angle * 0.85f + axleAngle * 0.15f

        clampVel(rearWheel)
        clampVel(frontWheel)
        clampVel(chassis)

        if (headHit(level.segments)) crashed = true
        if (chassis.position.x >= level.finishX) finished = true
    }

    private fun clampVel(body: RigidBody2D) {
        body.velocity.x = body.velocity.x.coerceIn(-25f, 25f)
        body.velocity.y = body.velocity.y.coerceIn(-25f, 25f)
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
