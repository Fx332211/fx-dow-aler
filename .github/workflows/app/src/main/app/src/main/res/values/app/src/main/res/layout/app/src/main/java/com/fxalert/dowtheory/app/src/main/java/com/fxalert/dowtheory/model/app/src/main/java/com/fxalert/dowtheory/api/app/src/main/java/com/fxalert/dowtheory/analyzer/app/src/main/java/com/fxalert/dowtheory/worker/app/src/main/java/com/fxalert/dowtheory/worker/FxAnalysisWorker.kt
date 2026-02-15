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
            signal.tf1h.trend.name.contains("UP") && 
            signal.direction == Direction.UP -> "順張り ★★★"
            
            signal.tf5m.trend.name.contains("DOWN") && 
            signal.tf15m.trend.name.contains("DOWN") && 
            signal.tf1h.trend.name.contains("DOWN") && 
            signal.direction == Direction.DOWN -> "順張り ★★★"
            
            else -> "逆張り"
        }
        
        val title = "[$trend] ダウ理論更新 & 帯形成"
        val message = buildString {
            appendLine("方向: ${direction}トレンド")
            appendLine("価格: ${String.format("%.3f", signal.currentPrice)}")
            appendLine()
            appendLine("【帯情報】")
            appendLine("レンジ上限: ${String.format("%.3f", signal.band.rangeHigh)}")
            appendLine("レンジ下限: ${String.format("%.3f", signal.band.rangeLow)}")
            appendLine("切り返し: ${String.format("%.3f", signal.band.reversalRate)}")
            appendLine("前回安値: ${String.format("%.3f", signal.band.previousLow)}")
            appendLine("pips差: ${String.format("%.1f", signal.band.pipsDiff)} pips")
            appendLine()
            appendLine("【上位足】")
            appendLine("5分足: ${getTrendSymbol(signal.tf5m.trend)}")
            appendLine("15分足: ${getTrendSymbol(signal.tf15m.trend)}")
            appendLine("1時間足: ${getTrendSymbol(signal.tf1h.trend)}")
            appendLine()
            appendLine("エントリー: $alignment")
        }
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("${direction}トレンド - ${alignment}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
            as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + signal.timestamp.toInt(), notification)
    }
    
    private fun getTrendSymbol(trend: com.fxalert.dowtheory.model.Trend): String {
        return when (trend) {
            com.fxalert.dowtheory.model.Trend.UPTREND -> "上昇 ↑"
            com.fxalert.dowtheory.model.Trend.DOWNTREND -> "下降 ↓"
            com.fxalert.dowtheory.model.Trend.RANGING -> "レンジ ―"
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FX アラート"
            val descriptionText = "ダウ理論シグナル通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
