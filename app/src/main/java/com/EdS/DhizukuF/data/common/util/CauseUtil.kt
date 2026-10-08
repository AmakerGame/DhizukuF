package com.EdS.DhizukuF.data.common.util

import android.content.Context
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.model.exception.ShizukuNotWorkException

fun Throwable.help(context: Context): String? {
    return when (this) {
        is ShizukuNotWorkException -> context.getString(R.string.error_shizuku_not_work)
        else -> null
    }
}
