package com.peakerkeepliveapp.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.peakerkeepliveapp.service.*

/**
 * 一像素保活法
 */
fun Context.openOnePixeActivityPlan() {
    //启动本地服务
    val localIntent = Intent(this, LocalService::class.java)
    startService(localIntent)
}

/**
 * 前台服务
 */
fun Context.useFrontService() {
    //启动服务
    val localIntent = Intent(this, ForegroundService::class.java)
    startService(localIntent)

}

/**
 * 服务拉活
 */
fun Context.usestickyService() {
    //启动服务
    val localIntent = Intent(this, StickyService::class.java)
    startService(localIntent)

}

/**
 * 双服务加job 保活  拉活 加前台服务
 */
fun Context.useDoubleService() {
    startService(Intent(this, OneLocalService::class.java))
    startService(Intent(this, RemoteService::class.java))
    MyJobService.startJob(this)

}



