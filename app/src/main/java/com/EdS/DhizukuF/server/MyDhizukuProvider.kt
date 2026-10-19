package com.EdS.DhizukuF.server

import com.EdS.DhizukuF.logging.LogManager

import com.EdS.DhizukuF.logging.LogEvent

import com.EdS.DhizukuF.logging.LogCategory

import android.os.Binder

import com.rosan.dhizuku.aidl.IDhizukuClient
import com.rosan.dhizuku.server_api.DhizukuProvider
import com.rosan.dhizuku.server_api.DhizukuService

import org.koin.core.component.KoinComponent

class MyDhizukuProvider : DhizukuProvider(), KoinComponent {
    override fun onCreateService(client: IDhizukuClient): DhizukuService {
        // Settings > Logs: an app got the binder of the service ("connected"); its death is "disconnected".
        if (LogManager.isActive(LogCategory.AppRequest)) {
            val uid = Binder.getCallingUid()
            LogManager.log(LogEvent.AppConnected, uid = uid)
            try {
                client.asBinder().linkToDeath({ LogManager.log(LogEvent.AppDisconnected, uid = uid) }, 0)
            } catch (_: Throwable) {
                // the client is gone already
                LogManager.log(LogEvent.AppDisconnected, uid = uid)
            }
        }
        return MyDhizukuService(context!!, DhizukuState.admin, client)
    }
}