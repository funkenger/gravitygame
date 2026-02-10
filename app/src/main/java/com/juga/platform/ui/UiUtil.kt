package com.juga.platform.ui

import java.util.Locale

fun formatTime(v: Float?): String {
    return if (v == null) "--" else String.format(Locale.US, "%.2fs", v)
}
