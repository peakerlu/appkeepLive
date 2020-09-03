package com.peakerkeepliveapp.service

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

@SuppressLint("NewApi")
class KeepJobService : JobService() {

    private val TAG = "KeepJobService"
    override fun onStopJob(params: JobParameters?): Boolean = false

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.e(TAG, "onStartJob")

        // 如果7.0以上 轮询
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startJob(this)
        }

        return false
    }

    companion object {
        fun startJob(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            //setPersisted 在设备重启依然执行
            // 需要增加权限 RECEIVE_BOOT_COMPLETED
            val builder = JobInfo.Builder(8, ComponentName(context.packageName,
                    KeepJobService::class.java.name)).setPersisted(true)

            // 小于7.0
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // 每隔 1s 执行一次 job
                // 版本23 开始 进行了改进，最小周期为 5s
                builder.setPeriodic(1000)
            } else {
                // 延迟执行任务
                builder.setMinimumLatency(1000)
            }
            jobScheduler.schedule(builder.build())
        }
    }

}