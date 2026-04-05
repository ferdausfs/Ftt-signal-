package com.ftt.signal

import android.content.Context
import android.os.Build
import android.webkit.JavascriptInterface

/**
 * Bridge exposed to WebView JS as `window.AndroidBridge`.
 * All @JavascriptInterface methods run on a background thread —
 * use Handler/runOnUiThread for any UI work.
 */
class JsBridge(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences(ScanService.PREFS, Context.MODE_PRIVATE)

    // ── Identity ────────────────────────────────────────────────────────────
    @JavascriptInterface fun isAndroid(): Boolean = true
    @JavascriptInterface fun getVersion(): String = BuildConfig.VERSION_NAME

    // ── Key-value storage (SharedPreferences) ───────────────────────────────
    @JavascriptInterface
    fun saveData(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    @JavascriptInterface
    fun getData(key: String, defaultValue: String): String =
        prefs.getString(key, defaultValue) ?: defaultValue

    @JavascriptInterface
    fun removeData(key: String) {
        prefs.edit().remove(key).apply()
    }

    // ── Notifications ────────────────────────────────────────────────────────
    @JavascriptInterface
    fun notify(pair: String, dir: String, conf: Int) {
        NotificationHelper.sendSignalNotif(ctx, pair, dir, conf)
    }

    @JavascriptInterface
    fun notifyWithGrade(pair: String, dir: String, conf: Int, grade: String) {
        NotificationHelper.sendSignalNotif(ctx, pair, dir, conf, grade)
    }

    @JavascriptInterface
    fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (ctx as? MainActivity)?.requestNotifPermission()
        }
    }

    @JavascriptInterface
    fun notifPermStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return "granted"
        return if (ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) "granted" else "denied"
    }

    // ── Background scanner ──────────────────────────────────────────────────
    @JavascriptInterface
    fun startScan(pairsJson: String, intervalMin: Int) {
        prefs.edit().putString("scan_pairs", pairsJson).apply()
        ScanService.start(ctx, pairsJson, maxOf(1, intervalMin))
    }

    @JavascriptInterface
    fun stopScan() {
        ScanService.stop(ctx)
    }

    // ── API base URL management ─────────────────────────────────────────────
    @JavascriptInterface
    fun getApiBase(): String =
        prefs.getString(ScanService.KEY_API_BASE, ScanService.DEFAULT_API) ?: ScanService.DEFAULT_API

    @JavascriptInterface
    fun setApiBase(url: String) {
        prefs.edit().putString(ScanService.KEY_API_BASE, url.trimEnd('/')).apply()
    }

    // ── Misc helpers ────────────────────────────────────────────────────────
    @JavascriptInterface
    fun vibrate(ms: Long) {
        try {
            val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(ms)
            }
        } catch (e: Exception) { /* ignore */ }
    }

    @JavascriptInterface
    fun keepScreenOn(on: Boolean) {
        (ctx as? MainActivity)?.runOnUiThread {
            if (on) ctx.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else    ctx.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
