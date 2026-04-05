package com.ftt.signal

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ScanService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, NotificationHelper.buildScanNotif(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val pairsJson  = intent.getStringExtra(EXTRA_PAIRS) ?: "[]"
                val intervalMin = intent.getIntExtra(EXTRA_INTERVAL, 5)
                startScanning(pairsJson, intervalMin)
            }
            ACTION_STOP -> {
                stopScanning()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startScanning(pairsJson: String, intervalMin: Int) {
        scanJob?.cancel()
        scanJob = scope.launch {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            while (isActive) {
                val apiBase = prefs.getString(KEY_API_BASE, DEFAULT_API) ?: DEFAULT_API
                try {
                    val pairs = JSONArray(pairsJson)
                    val count = pairs.length()

                    // Update foreground notification with pair count
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    nm.notify(NOTIF_ID, NotificationHelper.buildScanNotif(this@ScanService, count))

                    for (i in 0 until count) {
                        if (!isActive) break
                        val pair = pairs.getString(i)
                        try {
                            val signal = fetchSignal(apiBase, pair) ?: continue
                            val label   = signal.optString("label", "")
                            val newTs   = signal.optString("timestamp", "")
                            val lastTs  = prefs.getString("last_ts_${pair}", "")

                            if (newTs.isNotEmpty() && newTs != lastTs
                                && (label == "BUY" || label == "SELL")) {
                                val conf  = signal.optInt("confidence", 0)
                                val grade = signal.optString("grade", "")
                                NotificationHelper.sendSignalNotif(
                                    this@ScanService, pair, label, conf, grade
                                )
                                prefs.edit().putString("last_ts_${pair}", newTs).apply()
                            }
                        } catch (e: Exception) { /* skip pair */ }

                        delay(400L) // 400ms between pairs to be gentle on API
                    }
                } catch (e: Exception) { /* retry next cycle */ }

                delay(intervalMin * 60 * 1000L)
            }
        }
    }

    private fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── HTTP fetch ─────────────────────────────────────────────────────────
    private suspend fun fetchSignal(apiBase: String, pair: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val isOtc   = pair.contains("-OTC")
            val apiPair = if (isOtc) pair.replace("-OTC", "otc") else pair
            val encoded = URLEncoder.encode(apiPair, "UTF-8")
            val url     = URL("$apiBase/api/signal?pair=$encoded")

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout    = 15_000

            try {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)

                when {
                    json.has("signal") -> {
                        val sig  = json.getJSONObject("signal")
                        val raw  = sig.optString("finalSignal", "")
                        val lbl  = when (raw) {
                            "BUY"      -> "BUY"
                            "SELL"     -> "SELL"
                            "NO_TRADE" -> "WAIT"
                            else       -> "HOLD"
                        }
                        val conf = sig.optString("confidence", "0")
                            .replace("%", "").toIntOrNull() ?: 0
                        val grade = if (sig.has("grade") && !sig.isNull("grade")) {
                            if (sig.get("grade") is JSONObject)
                                sig.getJSONObject("grade").optString("grade", "")
                            else sig.optString("grade", "")
                        } else ""

                        JSONObject().apply {
                            put("label",     lbl)
                            put("confidence", conf)
                            put("grade",     grade)
                            put("timestamp", json.optString("timestamp", ""))
                        }
                    }
                    json.has("label") -> json
                    else -> null
                }
            } catch (e: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        const val ACTION_START   = "com.ftt.signal.START_SCAN"
        const val ACTION_STOP    = "com.ftt.signal.STOP_SCAN"
        const val EXTRA_PAIRS    = "pairs"
        const val EXTRA_INTERVAL = "interval"
        const val NOTIF_ID       = 1001
        const val PREFS          = "ftt_prefs"
        const val KEY_API_BASE   = "api_base"
        const val DEFAULT_API    = "https://asignal.umuhammadiswa.workers.dev"

        fun start(ctx: Context, pairsJson: String, intervalMin: Int) {
            val intent = Intent(ctx, ScanService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PAIRS, pairsJson)
                putExtra(EXTRA_INTERVAL, intervalMin)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, ScanService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
