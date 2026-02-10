package com.juga.platform.game

import com.juga.platform.data.GhostSample
import com.juga.platform.data.LevelData
import com.juga.platform.data.Medal
import com.juga.platform.physics.PhysicsWorld

class GameState(val level: LevelData) {
    val world = PhysicsWorld(level)
    val input = InputState()

    var timer = 0f
    var started = false
    var checkpointPenalty = 0f
    var lastCheckpointX = level.start.x
    var finished = false
    var crashed = false
    var medal = Medal.NONE

    var touchEvents = 0
    var frameCount = 0
    var throttleResponseSeen = false
    var groundedWithin3s = false
    var selfCheckWarning = ""

    val ghostRecording = mutableListOf<GhostSample>()
    var loadedGhost: List<GhostSample> = emptyList()

    fun update(dt: Float, checkpointsEnabled: Boolean) {
        frameCount++
        if (!started && input.any()) started = true
        if (started && !finished && !crashed) timer += dt

        world.step(dt, input.up, input.down, input.left, input.right)

        if (started && (ghostRecording.isEmpty() || timer - ghostRecording.last().t >= 0.05f)) {
            ghostRecording += GhostSample(
                t = timer,
                cx = world.chassis.position.x,
                cy = world.chassis.position.y,
                ca = world.chassis.angle,
                rx = world.rearWheel.position.x,
                ry = world.rearWheel.position.y,
                fx = world.frontWheel.position.x,
                fy = world.frontWheel.position.y
            )
        }

        if (checkpointsEnabled) {
            level.checkpoints.forEach { cp -> if (world.chassis.position.x > cp.x && cp.x > lastCheckpointX) lastCheckpointX = cp.x }
        }

        if (world.crashed) crashed = true
        if (world.finished) {
            finished = true
            val total = timer + checkpointPenalty
            medal = when {
                total <= level.medals.gold -> Medal.GOLD
                total <= level.medals.silver -> Medal.SILVER
                total <= level.medals.bronze -> Medal.BRONZE
                else -> Medal.NONE
            }
        }

        if (timer <= 3f && world.rearGrounded) groundedWithin3s = true
        if (input.up && world.chassis.velocity.x > 0.6f) throttleResponseSeen = true

        selfCheckWarning = when {
            frameCount < 30 -> "WARMUP"
            touchEvents == 0 -> "WARN: no touch events"
            timer > 3f && !groundedWithin3s -> "WARN: rear wheel not grounded in 3s"
            timer > 2f && input.up && !throttleResponseSeen -> "WARN: throttle no +X response"
            else -> "OK"
        }
    }
}
