package com.fxalert.dowtheory.model

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 0
)

data class Band(
    val rangeHigh: Double,
    val rangeLow: Double,
    val reversalRate: Double,
    val previousLow: Double,
    val previousHigh: Double,
    val pipsDiff: Double
)

data class DowTheorySignal(
    val timestamp: Long,
    val direction: Direction,
    val currentPrice: Double,
    val band: Band,
    val tf1m: TrendInfo,
    val tf5m: TrendInfo,
    val tf15m: TrendInfo,
    val tf1h: TrendInfo
)

data class TrendInfo(
    val timeframe: String,
    val trend: Trend,
    val isAligned: Boolean
)

enum class Direction {
    UP, DOWN
}

enum class Trend {
    UPTREND,
    DOWNTREND,
    RANGING
}

data class SwingPoint(
    val timestamp: Long,
    val price: Double,
    val type: SwingType
)

enum class SwingType {
    HIGH,
    LOW
}
