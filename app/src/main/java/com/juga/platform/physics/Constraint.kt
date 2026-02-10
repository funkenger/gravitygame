package com.juga.platform.physics

import kotlin.math.sqrt

interface Constraint {
    fun solve(dt: Float)
}

class DistanceConstraint(
    private val a: RigidBody2D,
    private val b: RigidBody2D,
    private val targetDistance: Float,
    private val stiffness: Float = 0.42f,
    private val damping: Float = 0.06f
) : Constraint {
    override fun solve(dt: Float) {
        val dx = b.position.x - a.position.x
        val dy = b.position.y - a.position.y
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-4f)
        val nx = dx / dist
        val ny = dy / dist
        val err = dist - targetDistance

        val corr = err * stiffness
        a.position.x += nx * corr
        a.position.y += ny * corr
        b.position.x -= nx * corr
        b.position.y -= ny * corr

        val rvx = b.velocity.x - a.velocity.x
        val rvy = b.velocity.y - a.velocity.y
        val rel = rvx * nx + rvy * ny
        val impulse = rel * damping
        a.velocity.x += nx * impulse
        a.velocity.y += ny * impulse
        b.velocity.x -= nx * impulse
        b.velocity.y -= ny * impulse
    }
}
