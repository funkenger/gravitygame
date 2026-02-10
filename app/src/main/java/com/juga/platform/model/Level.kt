package com.juga.platform.model

data class Level(
    val id: Int,
    val name: String,
    val start: Point,
    val finishX: Float,
    val medals: Medals,
    val segments: List<Segment>,
    val checkpoints: List<Point>
)

data class Point(val x: Float, val y: Float)

data class Segment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

data class Medals(val gold: Float, val silver: Float, val bronze: Float)

enum class MedalType { NONE, BRONZE, SILVER, GOLD }
