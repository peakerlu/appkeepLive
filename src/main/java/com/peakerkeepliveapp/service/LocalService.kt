package com.peakerkeepliveapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.peakerkeepliveapp.receiver.OnePixelReceiver

/**
 * 本地服务
 */
class LocalService : Service() {

    var mOnepxReceiver: OnePixelReceiver? = null
    override fun onCreate() {
        super.onCreate()
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

        var interactive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            false
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //像素保活
        if (mOnepxReceiver == null) {
            mOnepxReceiver = OnePixelReceiver()
        }
        //注册监听屏幕开闭的广播
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.SCREEN_OFF")
        intentFilter.addAction("android.intent.action.SCREEN_ON")
        registerReceiver(mOnepxReceiver, intentFilter)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //解注册
        if (mOnepxReceiver != null) {
            unregisterReceiver(mOnepxReceiver)
        }

    }
}