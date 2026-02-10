package com.juga.platform.physics

import com.juga.platform.data.TrackSegment
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class WheelContact(
    val normal: Vec2,
    val tangent: Vec2,
    val normalImpulse: Float,
    val frictionImpulse: Float
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
        val subSteps = ceil((speed / body.radius).toDouble()).toInt().coerceIn(1, 7)
        var contact: WheelContact? = null
        val start = oldPos.copy()
        val end = body.position.copy()
        for (i in 0 until subSteps) {
            val t = (i + 1f) / subSteps
            body.position = Vec2(
                start.x + (end.x - start.x) * t,
                start.y + (end.y - start.y) * t
            )
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

        body.position.x += n.x * (bestPen + 1e-3f)
        body.position.y += n.y * (bestPen + 1e-3f)

        val vn = body.velocity.x * n.x + body.velocity.y * n.y
        var normalImpulse = 0f
        if (vn < 0f) {
            normalImpulse = -vn * 1.7f
            body.velocity.x += n.x * normalImpulse
            body.velocity.y += n.y * normalImpulse
        }

        val tangent = Vec2(-n.y, n.x)
        val vt = body.velocity.x * tangent.x + body.velocity.y * tangent.y
        val maxFric = max(0.4f, frictionMu * normalImpulse + if (brake) 0.9f else 0.2f)
        val desired = (if (brake) -vt * 1.4f else 0f) + motorForce - vt * 0.2f
        val jt = desired.coerceIn(-maxFric, maxFric)
        body.velocity.x += tangent.x * jt
        body.velocity.y += tangent.y * jt

        return WheelContact(n, tangent, normalImpulse, jt)
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
