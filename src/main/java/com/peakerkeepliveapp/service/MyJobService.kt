package com.peakerkeepliveapp.service

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.peakerkeepliveapp.utils.Utils

@SuppressLint("NewApi")
class MyJobService : JobService() {

    //
    override fun onStopJob(params: JobParameters?): Boolean = false

    override fun onStartJob(params: JobParameters?): Boolean {

        // 如果7.0以上 轮询
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startJob(this)
        }
        val isLocal: Boolean = Utils.isRunningService(this, OneLocalService::class.java.name)
        val isRemote: Boolean = Utils.isRunningService(this, RemoteService::class.java.name)
        if (!isLocal || !isRemote) {
            startService(Intent(this, OneLocalService::class.java))
            startService(Intent(this, RemoteService::class.java))
        }
        return false
    }

    companion object {
        fun startJob(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            //setPersisted 在设备重启依然执行
            val builder = JobInfo.Builder(8, ComponentName(context.packageName,
                    MyJobService::class.java.name)).setPersisted(true)

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