package com.juga.platform.physics

import com.juga.platform.model.Level
import com.juga.platform.model.Segment
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class BikePhysics(startX: Float, startY: Float) {
    val frame = Body(Vec2(startX, startY), Vec2(0f, 0f), 0.35f, 1.2f)
    val rear = Body(Vec2(startX - 0.8f, startY + 0.2f), Vec2(0f, 0f), 0.45f, 1.1f)
    val front = Body(Vec2(startX + 0.8f, startY + 0.2f), Vec2(0f, 0f), 0.45f, 1.1f)

    var frameAngle = 0f
    var frameAngularVel = 0f

    var crashed = false
    var finished = false

    private val wheelBase = 1.6f
    private val frameToWheel = 0.9f

    fun step(dt: Float, level: Level, input: BikeInput) {
        if (crashed || finished) return

        applyGravity(frame, dt)
        applyGravity(rear, dt)
        applyGravity(front, dt)

        if (input.throttle) {
            rear.vel.x += 7.0f * dt
            rear.vel.y -= 0.8f * dt
        }
        if (input.brake) {
            rear.vel.x *= (1f - 3f * dt).coerceIn(0.8f, 1f)
            front.vel.x *= (1f - 2.5f * dt).coerceIn(0.82f, 1f)
        }

        frameAngularVel += (if (input.leanBack) -2.6f else 0f) * dt
        frameAngularVel += (if (input.leanForward) 2.6f else 0f) * dt
        frameAngularVel *= 0.985f
        frameAngularVel = frameAngularVel.coerceIn(-4.5f, 4.5f)
        frameAngle += frameAngularVel * dt

        integrate(frame, dt)
        integrate(rear, dt)
        integrate(front, dt)

        solveConstraint(rear, front, wheelBase)
        solveConstraint(frame, rear, frameToWheel)
        solveConstraint(frame, front, frameToWheel)

        collide(frame, level.segments)
        collide(rear, level.segments)
        collide(front, level.segments)

        val pitch = atan2(front.pos.y - rear.pos.y, front.pos.x - rear.pos.x)
        frameAngle = (frameAngle * 0.9f) + (pitch * 0.1f)

        checkCrash(level)
        if (frame.pos.x >= level.finishX) finished = true
    }

    private fun applyGravity(b: Body, dt: Float) {
        b.vel.y += 12.8f * dt
        b.vel.x *= 0.998f
        b.vel.y *= 0.998f
    }

    private fun integrate(b: Body, dt: Float) {
        b.pos.x += b.vel.x * dt
        b.pos.y += b.vel.y * dt
    }

    private fun solveConstraint(a: Body, b: Body, target: Float) {
        val delta = b.pos - a.pos
        val len = delta.len().coerceAtLeast(1e-4f)
        val error = len - target
        val n = delta * (1f / len)
        val correction = n * (error * 0.45f)
        a.pos.addIn(correction)
        b.pos.addIn(correction * -1f)
    }

    private fun collide(body: Body, segments: List<Segment>) {
        for (seg in segments) {
            val p = nearestPointOnSegment(body.pos, seg)
            val delta = body.pos - p
            val dist = delta.len()
            if (dist < body.radius) {
                val n = delta.normalize()
                body.pos = p + n * body.radius
                val vn = body.vel.x * n.x + body.vel.y * n.y
                if (vn < 0f) {
                    body.vel.x -= vn * n.x * 1.55f
                    body.vel.y -= vn * n.y * 1.55f
                    body.vel.x *= 0.96f
                }
            }
        }
    }

    private fun nearestPointOnSegment(p: Vec2, s: Segment): Vec2 {
        val ax = s.x1
        val ay = s.y1
        val bx = s.x2
        val by = s.y2
        val abx = bx - ax
        val aby = by - ay
        val apx = p.x - ax
        val apy = p.y - ay
        val denom = abx * abx + aby * aby
        val t = if (denom <= 1e-5f) 0f else ((apx * abx + apy * aby) / denom).coerceIn(0f, 1f)
        return Vec2(ax + abx * t, ay + aby * t)
    }

    private fun checkCrash(level: Level) {
        if (abs(frameAngle) > 2.2f && frame.vel.len() > 7f) crashed = true
        if (frame.pos.y > 30f || rear.pos.y > 30f || front.pos.y > 30f) crashed = true
        val frontHitHard = front.vel.len() > 9f && front.pos.y > groundYAt(level, front.pos.x) - 0.2f
        if (frontHitHard) crashed = true
    }

    private fun groundYAt(level: Level, x: Float): Float {
        val seg = level.segments.find { x in minOf(it.x1, it.x2)..maxOf(it.x1, it.x2) } ?: return 100f
        val t = if (seg.x2 == seg.x1) 0f else (x - seg.x1) / (seg.x2 - seg.x1)
        return seg.y1 + (seg.y2 - seg.y1) * t
    }

    fun frameNose(): Vec2 {
        return Vec2(frame.pos.x + cos(frameAngle) * 0.9f, frame.pos.y + sin(frameAngle) * 0.9f)
    }

    fun snapshot(t: Float): BikeSnapshot = BikeSnapshot(
        t, frame.pos.x, frame.pos.y, frameAngle,
        rear.pos.x, rear.pos.y, front.pos.x, front.pos.y
    )
}

data class BikeInput(
    val throttle: Boolean = false,
    val brake: Boolean = false,
    val leanBack: Boolean = false,
    val leanForward: Boolean = false
)
