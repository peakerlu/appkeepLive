package com.peakerkeepliveapp.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


class OnePixelActivity : AppCompatActivity() {
    private  val TAG = "OnePixelActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_one_pixel)
        //设定一像素的activity
        val window: Window = window
        window.setGravity(Gravity.START or Gravity.TOP)
        val params: WindowManager.LayoutParams = window.attributes
        params.x = 0
        params.y = 0
        params.height = 1
        params.width = 1
        window.attributes = params
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: OnePixelActivity")
        checkScreenOn()//检测是否打开屏幕
    }

    private fun checkScreenOn() {
        try {
            var pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                if (pm.isInteractive) {
                    finish()//如果屏幕打开 就关闭一像素页面
                }
            }
        } catch (e: Exception) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }
}