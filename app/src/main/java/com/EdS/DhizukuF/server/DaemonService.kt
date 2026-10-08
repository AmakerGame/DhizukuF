package com.EdS.DhizukuF.server

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
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

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val uid = intent.getIntExtra(Intent.EXTRA_UID, -1)
            if (uid < 0) return
            scope.launch {
                val entity = repo.findByUID(uid) ?: return@launch
                if (verify(entity)) return@launch
                repo.delete(entity)
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
        registerPackageReceiver()
        foreground(DhizukuState.state.isOwner)
        syncDish()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground(DhizukuState.state.isOwner)
        syncDish()
        return START_STICKY
    }

    override fun onDestroy() {
        foreground(false)
        com.EdS.DhizukuF.dish.DishServer.stop()
        try {
            unregisterReceiver(packageReceiver)
        } catch (_: IllegalArgumentException) {
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun syncDish() {
        if (DhizukuState.state.isOwner) com.EdS.DhizukuF.dish.DishServer.start()
        else com.EdS.DhizukuF.dish.DishServer.stop()
    }

    private fun registerPackageReceiver() {
        scope.launch {
            repo.all().filter { !verify(it) }.forEach { repo.delete(it) }
        }

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        filter.addDataScheme("package")
        registerReceiver(packageReceiver, filter)
    }

    private fun verify(entity: AppEntity): Boolean {
        val packageInfo = packageManager.getPackageInfoForUid(entity.uid) ?: return false

        // Only forget apps that are gone or were replaced by a different signature.
        // Pending (not approved yet) and blocked apps must stay in the list.
        if (entity.signature.isNotEmpty() && entity.signature != packageInfo.signature) return false

        return true
    }

    private fun foreground(enabled: Boolean) {
        if (!enabled) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }
        val manager = NotificationManagerCompat.from(this)


        val channel = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_LOW
        ).setName(getString(R.string.daemon_service_notification_title))
            .setDescription(getString(R.string.daemon_service_notification_text)).build()
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.daemon_service_notification_title))
            .setContentText(getString(R.string.daemon_service_notification_text))
            .setSmallIcon(R.drawable.round_hourglass_empty_black_24).setSilent(true).setSound(null)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
        startForeground(NOTIFICATION_ID, notification)
    }
}