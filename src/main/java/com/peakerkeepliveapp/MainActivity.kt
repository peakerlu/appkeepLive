package com.peakerkeepliveapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.peakerkeepliveapp.account.AccountHelper
import com.peakerkeepliveapp.activity.TextActivity
import com.peakerkeepliveapp.service.*
import com.peakerkeepliveapp.utils.useDoubleService
import com.peakerkeepliveapp.utils.useFrontService
import com.peakerkeepliveapp.utils.usestickyService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //开启一像素保活
//        openOnePixeActivityPlan()
        //使用前台服务
//        useFrontService()
        //使用粘性服务拉活
//        usestickyService()
        //监听 进程是否活着
//   startService(Intent(this, ListenProcessService::class.java))

        // 系统账号同步 系统拉活 但是时间各大厂商不一样  不能确保时间
        /*  AccountHelper.addAccount(this)
          AccountHelper.autoSync()*/
        tv.setOnClickListener {
            startActivity(Intent(this, TextActivity::class.java))
        }
        // jobScheduler 定时拉活  但是弃用
//        KeepJobService.startJob(this)

        //双进程服务保活
        useDoubleService()

    }

    override fun onDestroy() {
        super.onDestroy()

    }
}
