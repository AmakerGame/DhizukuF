package com.EdS.DhizukuF.data.common.util

import com.EdS.DhizukuF.data.common.model.exception.ShizukuNotWorkException

fun Throwable.help(): String? {
    return when (this) {
        is ShizukuNotWorkException -> "请激活 Shizuku 并同意权限请求"
        else -> null
    }
}
