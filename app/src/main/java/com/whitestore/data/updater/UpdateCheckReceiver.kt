package com.whitestore.data.updater

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.whitestore.MainActivity
import com.whitestore.R
import com.whitestore.WhiteStoreApp
import com.whitestore.data.prefs.AppPreferences
import com.whitestore.data.prefs.AutoCheckInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdateCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? WhiteStoreApp ?: return
        AppPreferences.init(context)

        val interval = AppPreferences.autoCheckInterval.value
        if (interval == AutoCheckInterval.OFF) return

        // Reschedule alarm for next run
        scheduleNextCheck(context, interval)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pm = context.packageManager
                val ignored = AppPreferences.ignoredPackages.value
                val installed = pm.getInstalledApplications(0).filter {
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                            it.packageName != context.packageName &&
                            !ignored.contains(it.packageName)
                }

                var updatesCount = 0
                for (appInfo in installed) {
                    try {
                        val pkgName = appInfo.packageName
                        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(pkgName, 0)
                        }

                        val localCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        }

                        val storeDetail = app.apiService.getAppDetail(pkgName)
                        if (storeDetail != null && storeDetail.versionCode != null) {
                            if (storeDetail.versionCode > localCode) {
                                updatesCount++
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (updatesCount > 0 && AppPreferences.autoCheckNotify.value) {
                    showUpdateNotification(context, updatesCount)
                }
            } catch (e: Exception) {
                Log.e("UpdateCheckReceiver", "Auto-check updates failed", e)
            }
        }
    }

    private fun showUpdateNotification(context: Context, count: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "whitestore_updates_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Обновления приложений",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о доступных обновлениях"
            }
            nm.createNotificationChannel(channel)
        }

        val notifyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val text = when {
            count % 10 == 1 && count % 100 != 11 -> "Доступно $count обновление"
            count % 10 in 2..4 && (count % 100 < 10 || count % 100 >= 20) -> "Доступно $count обновления"
            else -> "Доступно $count обновлений"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("WhiteStore")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(1001, notification)
    }

    companion object {
        fun scheduleNextCheck(context: Context, interval: AutoCheckInterval) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, UpdateCheckReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                42,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            if (interval == AutoCheckInterval.OFF) {
                alarmManager.cancel(pendingIntent)
                return
            }

            val triggerAtMillis = System.currentTimeMillis() + interval.hours * 60 * 60 * 1000
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (e: Exception) {
                Log.e("UpdateCheckReceiver", "Failed to schedule alarm", e)
            }
        }
    }
}
