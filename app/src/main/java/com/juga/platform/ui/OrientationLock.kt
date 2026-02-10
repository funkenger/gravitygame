package com.juga.platform.ui

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.lockPortrait() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}
