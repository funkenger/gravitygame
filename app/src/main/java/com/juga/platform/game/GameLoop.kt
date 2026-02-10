package com.juga.platform.game

class GameLoop(
    private val tick: (Float) -> Unit,
    private val render: () -> Unit
) {
    @Volatile var running = false
    @Volatile var paused = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread(::loop, "juga-loop").also { it.start() }
    }

    fun stop() {
        running = false
        thread?.join(700)
    }

    private fun loop() {
        val dt = 1f / 60f
        var prev = System.nanoTime()
        var acc = 0f
        while (running) {
            val now = System.nanoTime()
            acc += (now - prev) / 1_000_000_000f
            prev = now
            while (acc >= dt) {
                if (!paused) tick(dt)
                acc -= dt
            }
            render()
            Thread.sleep(2)
        }
    }
}
