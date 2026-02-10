package com.juga.platform.data

data class LevelData(
    val id: Int,
    val name: String,
    val start: VecPoint,
    val finishX: Float,
    val medals: MedalThresholds,
    val segments: List<TrackSegment>,
    val checkpoints: List<VecPoint>
)

data class VecPoint(val x: Float, val y: Float)

data class TrackSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

data class MedalThresholds(val gold: Float, val silver: Float, val bronze: Float)

enum class Medal { NONE, BRONZE, SILVER, GOLD }
