package com.EdS.DhizukuF.server

import com.EdS.DhizukuF.logging.LogManager

import com.EdS.DhizukuF.logging.LogEvent

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.ui.activity.MainActivity
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.dish.DishRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DaemonService : Service(), KoinComponent {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val repo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val uid = intent.getIntExtra(Intent.EXTRA_UID, -1)
            if (uid < 0) return
            // During an update the old package is removed first; the entry is re-checked by the
            // following "changed" broadcast (signature), so do not drop it now.
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED &&
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            ) return
            scope.launch {
                try {
                    if (DishRegistry.isDish(uid) && packageManager.getPackagesForUid(uid).isNullOrEmpty()) {
                        DishRegistry.unregister(uid)
                    }
                    val entity = repo.findByUID(uid) ?: return@launch
                    if (verify(entity)) return@launch
                    repo.delete(entity)
                } catch (t: Throwable) {
                    Log.w("DaemonService", "package check failed", t)
                    LogManager.error("DaemonService: package check failed", t)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001

        private const val NOTIFICATION_CHANNEL_ID = "daemon_service"
    }

    override fun onCreate() {
        super.onCreate()
        LogManager.log(LogEvent.ServiceStarted, message = "Foreground service created")
        registerPackageReceiver()
        // A service started with startForegroundService() must call startForeground() in time even
        // when it is about to stop, otherwise Android kills the app.
        foreground(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground(true)
        if (!DhizukuState.state.isOwner) {
            foreground(false)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        foreground(false)
        try {
            unregisterReceiver(packageReceiver)
        } catch (_: IllegalArgumentException) {
        }
        scope.cancel()
        LogManager.log(LogEvent.ServiceStopped, message = "Foreground service destroyed")
        super.onDestroy()
    }

    private fun registerPackageReceiver() {
        // Register first so PACKAGE_* is not missed during the startup cleanup.
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        filter.addDataScheme("package")
        registerReceiver(packageReceiver, filter)

        scope.launch {
            try {
                repo.all().filter { !verify(it) }.forEach {
                    DishRegistry.unregister(it.uid)
                    repo.delete(it)
                }
                DishRegistry.all().keys
                    .filter { packageManager.getPackagesForUid(it).isNullOrEmpty() }
                    .forEach { DishRegistry.unregister(it) }
            } catch (t: Throwable) {
                Log.w("DaemonService", "cleanup failed", t)
                LogManager.error("DaemonService: cleanup failed", t)
            }
        }
    }

    private fun verify(entity: AppEntity): Boolean {
        // Only a missing package or a changed signature invalidates an entry. Blocked and pending
        // entries must be kept, otherwise a blocked app would be forgotten (unblocked) on every
        // service start or package change.
        val packageInfo = packageManager.getPackageInfoForUid(entity.uid) ?: return false

        // No readable signature (e.g. the package is being updated) proves nothing: keep the entry.
        val signature = packageInfo.signature ?: return true

        return entity.signature == signature
    }

    private fun foreground(enabled: Boolean) {
        if (!enabled) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        val show = settingsRepo.isDaemonNotificationEnabled
        val importance = if (show) NotificationManager.IMPORTANCE_LOW
        else NotificationManager.IMPORTANCE_MIN
        val manager = NotificationManagerCompat.from(this)
        val channel = NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, importance)
            .setName(getString(R.string.daemon_service_notification_title))
            .setDescription(getString(R.string.daemon_service_notification_text))
            .build()
        manager.createNotificationChannel(channel)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_hourglass_empty_black_24)
            .setSilent(true)
            .setSound(null)
            .setContentIntent(contentIntent)
            .setPriority(
                if (show) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN
            )
        if (show) {
            builder
                .setContentTitle(getString(R.string.daemon_service_notification_title))
                .setContentText(getString(R.string.daemon_service_notification_text))
        } else {
            // Android still requires a FGS notification; keep it as quiet as allowed.
            builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.daemon_service_notification_quiet))
                .setShowWhen(false)
        }
        startForeground(NOTIFICATION_ID, builder.build())
    }
}