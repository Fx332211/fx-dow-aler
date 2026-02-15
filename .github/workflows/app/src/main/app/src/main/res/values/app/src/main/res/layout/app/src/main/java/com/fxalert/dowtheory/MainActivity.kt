package com.fxalert.dowtheory

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fxalert.dowtheory.worker.FxAnalysisWorker

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        requestNotificationPermission()
        initViews()
        setupTradingView()
        setupButtons()
    }
    
    private fun initViews() {
        webView = findViewById(R.id.webView)
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
    }
    
    private fun setupTradingView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.webViewClient = WebViewClient()
        
        val tradingViewUrl = "https://www.tradingview.com/chart/?symbol=FX_IDC:USDJPY&interval=1"
        webView.loadUrl(tradingViewUrl)
    }
    
    private fun setupButtons() {
        startButton.setOnClickListener {
            startMonitoring()
        }
        
        stopButton.setOnClickListener {
            stopMonitoring()
        }
    }
    
    private fun startMonitoring() {
        FxAnalysisWorker.schedule(this)
        statusText.text = "監視中... (3分ごとにチェック)"
        startButton.isEnabled = false
        stopButton.isEnabled = true
    }
    
    private fun stopMonitoring() {
        FxAnalysisWorker.cancel(this)
        statusText.text = "停止中"
        startButton.isEnabled = true
        stopButton.isEnabled = false
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 権限が許可された
                } else {
                    statusText.text = "通知権限が必要です"
                }
            }
        }
    }
}
