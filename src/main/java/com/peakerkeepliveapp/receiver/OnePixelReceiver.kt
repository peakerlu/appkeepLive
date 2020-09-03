package com.peakerkeepliveapp.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.peakerkeepliveapp.activity.OnePixelActivity

/**
 * 一像素
 */
class OnePixelReceiver : BroadcastReceiver() {
    var mHander: Handler? = null
    init {
         mHander = Handler(Looper.getMainLooper())
    }

    var screenOn = true
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(Intent.ACTION_SCREEN_OFF)) {//屏幕关闭的时候
            screenOn = false
            mHander?.postDelayed({
                if (!screenOn) {//打开界面
                    val intent2 = Intent(context, OnePixelActivity::class.java)
                    intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)//打开app界面 需要时这个
                    val pendingIntent = PendingIntent.getActivity(context, 0, intent2, 0)
                    try {
                        pendingIntent.send()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 1000)
            //这里可以使用播放无声音乐的方法保活


        } else if (intent?.action.equals(Intent.ACTION_SCREEN_ON)) {//屏幕打开的时候
            screenOn = true
            //关闭一像素界面
        }
    }
}