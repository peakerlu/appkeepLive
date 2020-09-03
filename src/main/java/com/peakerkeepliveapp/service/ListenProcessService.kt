package com.peakerkeepliveapp.service

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.text.TextUtils
import android.util.Log
import com.peakerkeepliveapp.utils.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader


class ListenProcessService : Service() {
    private val TAG = "ListenProcessService"
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        var am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var packageName = packageName
        var currentProcess = getCurrentProcess()
        var runningProcess = Utils.isRunningProcess(am, packageName)

        Log.d(TAG, "onCreate: packageName $packageName Pid:${Process.myPid()}UID:${Process.myUid()} runningProcess $runningProcess")
        if (!runningProcess) {
            Log.d(TAG, "onCreate: 进程关闭")
        }

    }

    /**
     * 获取当前进程
     */
    private fun getCurrentProcess(): String {
        return try {
            val file = File("/proc/" + Process.myPid() + "/" + "cmdline")
            val mBufferedReader = BufferedReader(FileReader(file))
            val processName: String = mBufferedReader.readLine().trim()
            mBufferedReader.close()
            processName
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }


}