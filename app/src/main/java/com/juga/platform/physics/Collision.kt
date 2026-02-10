package com.juga.platform.physics

import com.juga.platform.data.TrackSegment
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class WheelContact(
    val normal: Vec2,
    val tangent: Vec2,
    val normalImpulse: Float
)

object Collision {
    fun solveCircleTrack(
        body: RigidBody2D,
        oldPos: Vec2,
        segments: List<TrackSegment>,
        frictionMu: Float,
        brake: Boolean,
        motorForce: Float
    ): WheelContact? {
        val speed = body.velocity.len()
        val subSteps = ceil((speed / body.radius).toDouble()).toInt().coerceIn(1, 6)
        var contact: WheelContact? = null
        for (i in 0 until subSteps) {
            val t = (i + 1f) / subSteps
            val interp = Vec2(
                oldPos.x + (body.position.x - oldPos.x) * t,
                oldPos.y + (body.position.y - oldPos.y) * t
            )
            body.position = interp
            contact = resolveCircleOnce(body, segments, frictionMu, brake, motorForce) ?: contact
        }
        return contact
    }

    private fun resolveCircleOnce(
        body: RigidBody2D,
        segments: List<TrackSegment>,
        frictionMu: Float,
        brake: Boolean,
        motorForce: Float
    ): WheelContact? {
        var bestPen = 0f
        var bestN: Vec2? = null
        for (s in segments) {
            val p = closestPoint(body.position, s)
            val dx = body.position.x - p.x
            val dy = body.position.y - p.y
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-5f)
            val pen = body.radius - dist
            if (pen > bestPen) {
                bestPen = pen
                bestN = Vec2(dx / dist, dy / dist)
            }
        }
        val n = bestN ?: return null
        if (bestPen <= 0f) return null

        body.position.x += n.x * bestPen
        body.position.y += n.y * bestPen

        val vn = body.velocity.x * n.x + body.velocity.y * n.y
        var normalImpulse = 0f
        if (vn < 0f) {
            val jn = -vn * 1.6f
            body.velocity.x += n.x * jn
            body.velocity.y += n.y * jn
            normalImpulse = jn
        }

        val t = Vec2(-n.y, n.x)
        val vt = body.velocity.x * t.x + body.velocity.y * t.y
        val maxFric = max(0.6f, frictionMu * normalImpulse + 0.2f)
        val desired = if (brake) -vt else (motorForce - vt)
        val jt = desired.coerceIn(-maxFric, maxFric)
        body.velocity.x += t.x * jt
        body.velocity.y += t.y * jt

        return WheelContact(n, t, normalImpulse)
    }

    private fun closestPoint(p: Vec2, s: TrackSegment): Vec2 {
        val abx = s.x2 - s.x1
        val aby = s.y2 - s.y1
        val apx = p.x - s.x1
        val apy = p.y - s.y1
        val den = abx * abx + aby * aby
        val t = if (den <= 1e-5f) 0f else (apx * abx + apy * aby) / den
        val clamped = min(1f, max(0f, t))
        return Vec2(s.x1 + abx * clamped, s.y1 + aby * clamped)
    }
}
