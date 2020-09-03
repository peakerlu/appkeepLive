package com.peakerkeepliveapp.utils

import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils

object Utils {
    /**
     * 判断服务是否运行
     */
    fun isRunningService(context: Context, name: String?): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = am.getRunningServices(100)
        for (info in runningServices) {
            if (TextUtils.equals(info.service.className, name)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断进程是否存活
     */
    fun isRunningProcess(manager: ActivityManager, packageName: String): Boolean {
        val runnings: List<ActivityManager.RunningAppProcessInfo> = manager.runningAppProcesses

        for (info in runnings) {
            if (TextUtils.equals(info.processName, packageName)) {
                return true
            }
        }
        return false

    }
}