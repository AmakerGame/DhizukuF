package com.EdS.DhizukuF.dish

import com.EdS.DhizukuF.BuildConfig

/** Wire protocol between the `dish` terminal client and the Dhizuku app (abstract unix socket). */
object DishProtocol {
    const val MAGIC = 0x44495348 // "DISH"
    const val VERSION = 1

    /** Abstract socket name, unique per applicationId. */
    val SOCKET_NAME: String = BuildConfig.APPLICATION_ID + ".dish"

    // Handshake status (server -> client)
    const val ST_OK = 0
    const val ST_NEED_PERMISSION = 1
    const val ST_DENIED = 2

    // Output frames (server -> client)
    const val FR_END = 0
    const val FR_OUT = 1
    const val FR_ERR = 2

    const val ENV_SERVER_UID = "DISH_SERVER_UID"
    const val ENV_TTY = "DISH_TTY"

    const val MAX_CHUNK = 16000
    const val MAX_ARGS = 256
    const val APPROVAL_TIMEOUT_MS = 60_000L
}
