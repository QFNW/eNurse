package com.enurse.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.enurse.app.presentation.navigation.TokenDataStoreEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 开机/应用升级后重排闹钟（系统重启会清空所有 AlarmManager 闹钟）。
 * 注意：force-stop 过的应用收不到 BOOT_COMPLETED（平台限制），需用户手动打开一次 App。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ep = EntryPointAccessors.fromApplication(
                    context.applicationContext, TokenDataStoreEntryPoint::class.java
                )
                if (ep.tokenDataStore().accessToken.first() != null) {
                    Log.i("BootReceiver", "Rescheduling alarms after ${intent.action}")
                    ReminderScheduler.refresh(context.applicationContext)
                }
            } catch (e: Exception) {
                Log.w("BootReceiver", "reschedule failed: ${e.message}")
            }
        }
    }
}
