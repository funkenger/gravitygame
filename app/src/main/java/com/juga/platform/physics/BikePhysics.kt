package com.juga.platform.physics

import com.juga.platform.model.Level
import com.juga.platform.model.Segment
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BikePhysics(startX: Float, startY: Float) {
    val frame = Body(Vec2(startX, startY - 0.55f), Vec2(0f, 0f), 0.28f, 1.0f)
    val rear = Body(Vec2(startX - 0.78f, startY), Vec2(0f, 0f), 0.46f, 1.35f)
    val front = Body(Vec2(startX + 0.78f, startY), Vec2(0f, 0f), 0.46f, 1.30f)

    var frameAngle = 0f
    private var frameAngularVel = 0f

    var rearSpin = 0f
    var frontSpin = 0f

    var crashed = false
    var finished = false

    private val gravity = 16.2f
    private val wheelBase = 1.56f
    private val frameToWheel = 0.96f
    private val suspension = 0.22f

    private var hardNoseHit = false
    private var hardFlipHit = false

    fun step(dt: Float, level: Level, input: BikeInput) {
        if (crashed || finished) return

        applyForces(dt, input)
        integrate(frame, dt)
        integrate(rear, dt)
        integrate(front, dt)

        repeat(3) {
            solveConstraint(rear, front, wheelBase, 0.42f)
            solveConstraint(frame, rear, frameToWheel, 0.50f)
            solveConstraint(frame, front, frameToWheel + suspension, 0.50f)
        }

        hardNoseHit = false
        hardFlipHit = false
        collide(rear, level.segments, isWheel = true)
        collide(front, level.segments, isWheel = true)
        collide(frame, level.segments, isWheel = false)

        val axlePitch = atan2(front.pos.y - rear.pos.y, front.pos.x - rear.pos.x)
        frameAngle = (frameAngle * 0.88f) + (axlePitch * 0.12f)
        frameAngle += frameAngularVel * dt
        frameAngularVel *= 0.985f
        frameAngularVel = frameAngularVel.coerceIn(-5.6f, 5.6f)

        rearSpin += rear.vel.x * dt * 1.4f
        frontSpin += front.vel.x * dt * 1.4f

        val speed = (rear.vel.x + front.vel.x + frame.vel.x) / 3f
        val upsideDown = abs(axlePitch) > 2.55f
        if (hardNoseHit || (upsideDown && hardFlipHit)) crashed = true
        if (frame.pos.y > 35f || rear.pos.y > 35f || front.pos.y > 35f) crashed = true

        if (frame.pos.x >= level.finishX && speed > 0.5f) finished = true
    }

    private fun applyForces(dt: Float, input: BikeInput) {
        applyGravity(frame, dt)
        applyGravity(rear, dt)
        applyGravity(front, dt)

        val motor = when {
            input.throttle -> 18f
            input.brake -> -10f
            else -> 0f
        }
        rear.vel.x += motor * dt

        if (input.brake) {
            rear.vel.x *= 0.965f
            front.vel.x *= 0.94f
        }

        if (input.leanBack) frameAngularVel -= 4.4f * dt
        if (input.leanForward) frameAngularVel += 4.4f * dt
    }

    private fun applyGravity(b: Body, dt: Float) {
        b.vel.y += gravity * dt
        b.vel.x *= 0.9988f
        b.vel.y *= 0.999f
    }

    private fun integrate(b: Body, dt: Float) {
        b.pos.x += b.vel.x * dt
        b.pos.y += b.vel.y * dt
    }

    private fun solveConstraint(a: Body, b: Body, target: Float, stiffness: Float) {
        val dx = b.pos.x - a.pos.x
        val dy = b.pos.y - a.pos.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-4f)
        val diff = (dist - target) / dist
        val corrX = dx * diff * stiffness
        val corrY = dy * diff * stiffness
        a.pos.x += corrX
        a.pos.y += corrY
        b.pos.x -= corrX
        b.pos.y -= corrY
    }

    private fun collide(body: Body, segments: List<Segment>, isWheel: Boolean) {
        for (s in segments) {
            val nearest = nearestPoint(body.pos, s)
            val dx = body.pos.x - nearest.x
            val dy = body.pos.y - nearest.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < body.radius) {
                val inv = 1f / max(dist, 1e-4f)
                val nx = dx * inv
                val ny = dy * inv

                val penetration = body.radius - dist
                body.pos.x += nx * penetration
                body.pos.y += ny * penetration

                val vn = body.vel.x * nx + body.vel.y * ny
                if (vn < 0f) {
                    body.vel.x -= vn * nx * 1.65f
                    body.vel.y -= vn * ny * 1.65f
                }

                val tangentX = -ny
                val tangentY = nx
                val vt = body.vel.x * tangentX + body.vel.y * tangentY
                val grip = if (isWheel) 0.80f else 0.35f
                body.vel.x -= tangentX * vt * grip * 0.20f
                body.vel.y -= tangentY * vt * grip * 0.20f

                if (!isWheel && abs(vn) > 9.2f) {
                    hardNoseHit = true
                }
                if (isWheel && abs(vn) > 12f) {
                    hardFlipHit = true
                }
            }
        }
    }

    private fun nearestPoint(p: Vec2, s: Segment): Vec2 {
        val abx = s.x2 - s.x1
        val aby = s.y2 - s.y1
        val apx = p.x - s.x1
        val apy = p.y - s.y1
        val den = abx * abx + aby * aby
        val t = if (den < 1e-5f) 0f else ((apx * abx + apy * aby) / den)
        val tt = min(1f, max(0f, t))
        return Vec2(s.x1 + abx * tt, s.y1 + aby * tt)
    }

    fun snapshot(t: Float): BikeSnapshot = BikeSnapshot(
        t = t,
        frameX = frame.pos.x,
        frameY = frame.pos.y,
        frameA = frameAngle,
        backX = rear.pos.x,
        backY = rear.pos.y,
        frontX = front.pos.x,
        frontY = front.pos.y
    )
}

data class BikeInput(
    val throttle: Boolean = false,
    val brake: Boolean = false,
    val leanBack: Boolean = false,
    val leanForward: Boolean = false
)
