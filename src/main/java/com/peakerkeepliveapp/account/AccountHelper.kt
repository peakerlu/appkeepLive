package com.peakerkeepliveapp.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.peakerkeepliveapp.R

object AccountHelper {
    private val TAG = "AccountHelper"

    private val ACCOUNT_TYPE = "com.peakerkeepliveapp.account"

    /**
     * 添加账号
     */
    fun addAccount(context: Context) {
        var accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        //获取此类型的账户
        //需要增加权限 GET_ACCOUNTS
        var accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
        if (accounts.isNotEmpty()) {
            //账户已存在
            Log.d(TAG, "addAccount: 账户已存在")
            return
        }
        val account = Account("keeplive", ACCOUNT_TYPE)
        // 给这个账户类型添加一个账户
        // 需要增加权限  AUTHENTICATE_ACCOUNTS
        accountManager.addAccountExplicitly(account, "password", Bundle())
    }

    /**
     * 设置沾账户自动同步
     */
    fun autoSync() {
        val account = Account("keeplive", ACCOUNT_TYPE)
        // 下面三个都需要同一个权限  WRITE_SYNC_SETTINGS
        // 设置同步
        ContentResolver.setIsSyncable(account, "com.peakerkeepliveapp.provider", 1)
        // 自动同步
        ContentResolver.setSyncAutomatically(account, "com.peakerkeepliveapp.provider", true)
        // 设置同步周期
        ContentResolver.addPeriodicSync(account, "com.peakerkeepliveapp.provider", Bundle(), 1)
    }
}