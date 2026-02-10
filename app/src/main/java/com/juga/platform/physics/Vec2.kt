package com.juga.platform.physics

import kotlin.math.sqrt

data class Vec2(var x: Float, var y: Float) {
    operator fun plus(v: Vec2) = Vec2(x + v.x, y + v.y)
    operator fun minus(v: Vec2) = Vec2(x - v.x, y - v.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun dot(v: Vec2) = x * v.x + y * v.y
    fun len() = sqrt(x * x + y * y)
    fun nrm(): Vec2 {
        val l = len().coerceAtLeast(1e-5f)
        return Vec2(x / l, y / l)
    }
}
