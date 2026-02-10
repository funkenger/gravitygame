package com.juga.platform.game

data class InputState(
    var up: Boolean = false,
    var down: Boolean = false,
    var left: Boolean = false,
    var right: Boolean = false
) {
    fun any(): Boolean = up || down || left || right
}
