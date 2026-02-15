package com.fxalert.dowtheory.analyzer

import com.fxalert.dowtheory.model.*
import kotlin.math.abs

class DowTheoryAnalyzer {
    
    private val swingHighs = mutableListOf<SwingPoint>()
    private val swingLows = mutableListOf<SwingPoint>()
    
    fun analyzeCandles(candles: List<CandleStick>): List<DowTheorySignal> {
        if (candles.size < 50) return emptyList()
        
        val signals = mutableListOf<DowTheorySignal>()
        detectSwingPoints(candles)
        
        val latest = candles.last()
        
        checkHighBreak(candles, latest)?.let { signal ->
            signals.add(signal)
        }
        
        checkLowBreak(candles, latest)?.let { signal ->
            signals.add(signal)
        }
        
        return signals
    }
    
    private fun detectSwingPoints(candles: List<CandleStick>) {
        swingHighs.clear()
        swingLows.clear()
        
        for (i in 2 until candles.size - 2) {
            val current = candles[i]
            
            if (isSwingHigh(candles, i)) {
                swingHighs.add(SwingPoint(
                    timestamp = current.timestamp,
                    price = current.high,
                    type = SwingType.HIGH
                ))
            }
            
            if (isSwingLow(candles, i)) {
                swingLows.add(SwingPoint(
                    timestamp = current.timestamp,
                    price = current.low,
                    type = SwingType.LOW
                ))
            }
        }
    }
    
    private fun isSwingHigh(candles: List<CandleStick>, index: Int): Boolean {
        val current = candles[index].high
        return current > candles[index - 1].high &&
               current > candles[index - 2].high &&
               current > candles[index + 1].high &&
               current > candles[index + 2].high
    }
    
    private fun isSwingLow(candles: List<CandleStick>, index: Int): Boolean {
        val current = candles[index].low
        return current < candles[index - 1].low &&
               current < candles[index - 2].low &&
               current < candles[index + 1].low &&
               current < candles[index + 2].low
    }
    
    private fun checkHighBreak(candles: List<CandleStick>, latest: CandleStick): DowTheorySignal? {
        if (swingHighs.isEmpty()) return null
        
        val recentHighs = swingHighs.takeLast(3)
        if (recentHighs.size < 2) return null
        
        val previousHigh = recentHighs[recentHighs.size - 2].price
        
        if (latest.close > previousHigh && 
            latest.close == latest.high &&
            latest.close > latest.open) {
            
            val band = calculateBand(candles, Direction.UP)
            
            return DowTheorySignal(
                timestamp = latest.timestamp,
                direction = Direction.UP,
                currentPrice = latest.close,
                band = band,
                tf1m = TrendInfo("1m", Trend.UPTREND, true),
                tf5m = TrendInfo("5m", Trend.UPTREND, true),
                tf15m = TrendInfo("15m", Trend.UPTREND, true),
                tf1h = TrendInfo("1h", Trend.UPTREND, true)
            )
        }
        
        return null
    }
    
    private fun checkLowBreak(candles: List<CandleStick>, latest: CandleStick): DowTheorySignal? {
        if (swingLows.isEmpty()) return null
        
        val recentLows = swingLows.takeLast(3)
        if (recentLows.size < 2) return null
        
        val previousLow = recentLows[recentLows.size - 2].price
        
        if (latest.close < previousLow &&
            latest.close == latest.low &&
            latest.close < latest.open) {
            
            val band = calculateBand(candles, Direction.DOWN)
            
            return DowTheorySignal(
                timestamp = latest.timestamp,
                direction = Direction.DOWN,
                currentPrice = latest.close,
                band = band,
                tf1m = TrendInfo("1m", Trend.DOWNTREND, true),
                tf5m = TrendInfo("5m", Trend.DOWNTREND,
