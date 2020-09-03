package com.peakerkeepliveapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class StickyService :Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}