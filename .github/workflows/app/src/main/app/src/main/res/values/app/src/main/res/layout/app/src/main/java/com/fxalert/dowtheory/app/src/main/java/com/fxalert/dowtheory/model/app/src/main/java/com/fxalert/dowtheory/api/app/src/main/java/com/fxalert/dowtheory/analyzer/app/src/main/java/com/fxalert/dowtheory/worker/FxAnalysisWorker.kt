package com.fxalert.dowtheory.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.fxalert.dowtheory.analyzer.DowTheoryAnalyzer
import com.fxalert.dowtheory.api.AlphaVantageApi
import com.fxalert.dowtheory.model.CandleStick
import com.fxalert.dowtheory.model.Direction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FxAnalysisWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val api = AlphaVantageApi.create()
    private val analyzer = DowTheoryAnalyzer()
    
    companion object {
        private const val CHANNEL_ID = "fx_alert_channel"
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "fx_analysis_work"
        private const val API_KEY = "78EC5MGNUUNBXHW3"
        
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<FxAnalysisWorker>(
                3, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    1, TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val response = api.getForexIntraday(
                apiKey = API_KEY,
                outputSize = "compact"
            )
            
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.retry()
            }
            
            val data = response.body()!!
            val timeSeries = data.timeSeries ?: return@withContext Result.failure()
            
            val candles = timeSeries.map { (timestamp, values) ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                CandleStick(
                    timestamp = dateFormat.parse(timestamp)?.time ?: 0L,
                    open = values.open.toDouble(),
                    high = values.high.toDouble(),
                    low = values.low.toDouble(),
                    close = values.close.toDouble()
                )
            }.sortedBy { it.timestamp }
            
            val signals = analyzer.analyzeCandles(candles)
            
            signals.forEach { signal ->
                sendNotification(signal)
            }
            
            Result.success()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
    
    private fun sendNotification(signal: com.fxalert.dowtheory.model.DowTheorySignal) {
        createNotificationChannel()
        
        val direction = if (signal.direction == Direction.UP) "上昇" else "下降"
        val trend = if (signal.direction == Direction.UP) "↑" else "↓"
        
        val alignment = when {
            signal.tf5m.trend.name.contains("UP") && 
            signal.tf15m.trend.name.contains("UP") && 
            signal.tf1h.trend.name.contains("
