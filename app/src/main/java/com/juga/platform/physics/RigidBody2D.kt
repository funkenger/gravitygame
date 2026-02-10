package com.juga.platform.physics

class RigidBody2D(
    var position: Vec2,
    var velocity: Vec2,
    var angle: Float = 0f,
    var angularVelocity: Float = 0f,
    val mass: Float = 1f,
    val inertia: Float = 1f,
    val radius: Float = 0.2f
) {
    val invMass: Float = if (mass <= 0f) 0f else 1f / mass
    val invInertia: Float = if (inertia <= 0f) 0f else 1f / inertia

    fun integrate(dt: Float, gravity: Float, linearDamping: Float, angularDamping: Float) {
        velocity.y += gravity * dt
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        angle += angularVelocity * dt
        velocity.x *= linearDamping
        velocity.y *= linearDamping
        angularVelocity *= angularDamping
    }
}
