package com.ftt.signal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    lateinit var webView: WebView

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        webView.post {
            webView.evaluateJavascript(
                "if(typeof window.onNotifPermResult==='function') window.onNotifPermResult($granted);",
                null
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge / full screen
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            }
        }

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        setupWebView()
        setupBackPress()
        requestNotifIfNeeded()

        // Bridge is injected by app.html itself — no duplicate injection here
        webView.loadUrl("file:///android_asset/app.html")
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled          = true
            domStorageEnabled          = true
            databaseEnabled            = true
            allowFileAccess            = true
            allowContentAccess         = true
            mixedContentMode           = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode                  = WebSettings.LOAD_DEFAULT
            useWideViewPort            = true
            loadWithOverviewMode       = true
            setSupportZoom(false)
            builtInZoomControls        = false
            displayZoomControls        = false
        }

        webView.addJavascriptInterface(JsBridge(this), "AndroidBridge")
        webView.setBackgroundColor(0xFF000000.toInt())

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean = false  // Keep navigation inside the WebView
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                // Uncomment for debugging:
                // android.util.Log.d("FTT-JS", "${msg?.message()} (${msg?.sourceId()}:${msg?.lineNumber()})")
                return true
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestNotifIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume()  { super.onResume();  webView.onResume() }
    override fun onPause()   { webView.onPause(); super.onPause() }
    override fun onDestroy() { webView.destroy();  super.onDestroy() }
}
