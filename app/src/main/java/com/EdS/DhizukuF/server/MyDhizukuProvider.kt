package com.EdS.DhizukuF.server

import com.EdS.DhizukuF.aidl.IDhizukuClient
import com.EdS.DhizukuF.server_api.DhizukuProvider
import com.EdS.DhizukuF.server_api.DhizukuService

import org.koin.core.component.KoinComponent

class MyDhizukuProvider : DhizukuProvider(), KoinComponent {
    override fun onCreateService(client: IDhizukuClient): DhizukuService {
        return MyDhizukuService(context!!, DhizukuState.admin, client)
    }
}