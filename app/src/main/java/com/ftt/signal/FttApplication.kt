package com.ftt.signal

import android.app.Application

class FttApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
