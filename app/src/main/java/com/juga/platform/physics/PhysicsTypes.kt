package com.juga.platform.physics

import kotlin.math.sqrt

data class Vec2(var x: Float, var y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(v: Float) = Vec2(x * v, y * v)
    fun addIn(other: Vec2) {
        x += other.x
        y += other.y
    }

    fun len(): Float = sqrt(x * x + y * y)
    fun normalize(): Vec2 {
        val l = len().coerceAtLeast(1e-4f)
        return Vec2(x / l, y / l)
    }
}

data class Body(var pos: Vec2, var vel: Vec2, var radius: Float, var mass: Float)

data class BikeSnapshot(
    val t: Float,
    val frameX: Float,
    val frameY: Float,
    val frameA: Float,
    val backX: Float,
    val backY: Float,
    val frontX: Float,
    val frontY: Float
)
