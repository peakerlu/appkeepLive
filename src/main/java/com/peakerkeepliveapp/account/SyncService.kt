package com.peakerkeepliveapp.account

import android.accounts.Account
import android.app.Service
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log

class SyncService : Service() {
    lateinit var syncAdapter:SyncAdapter
    companion object{
        private  val TAG = "SyncService"
    }

    override fun onCreate() {
        super.onCreate()
        syncAdapter= SyncAdapter(applicationContext,true)
    }
    override fun onBind(intent: Intent?): IBinder? {
       return syncAdapter.syncAdapterBinder
    }


    class SyncAdapter(context: Context?, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {

        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            Log.e(TAG, "同步账户")
            //与互联网 或者 本地数库同据步账户
        }
    }
}