package com.EdS.DhizukuF.di.init

import com.EdS.DhizukuF.di.dataModule
import com.EdS.DhizukuF.di.reflectModule
import com.EdS.DhizukuF.di.viewModelModule

val appModules = listOf(
    dataModule,
    viewModelModule,
    reflectModule
)