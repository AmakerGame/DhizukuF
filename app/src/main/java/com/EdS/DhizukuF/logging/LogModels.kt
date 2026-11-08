package com.EdS.DhizukuF.logging

/**
 * Importance of an entry, and also the setting "Log level": an entry is saved only when its level is at least
 * the chosen one (Errors only = [Error], Warnings = [Warning], Normal = [Normal], Debug = [Debug]).
 */
enum class LogLevel(val rank: Int) {
    Debug(0),
    Normal(1),
    Warning(2),
    Error(3)
}

/** The switches of Settings > Logs > Log categories. */
enum class LogCategory {
    Service,
    Permission,
    AppRequest,
    Command,
    Error,
    Debug
}

/** What happened. [english] is the stable name used in exported files; the app shows a translated title. */
enum class LogEvent(
    val english: String,
    val category: LogCategory,
    val level: LogLevel
) {
    ServiceStarted("DhizukuF service started", LogCategory.Service, LogLevel.Normal),
    ServiceStopped("DhizukuF service stopped", LogCategory.Service, LogLevel.Normal),
    OwnerStatusChanged("Device Owner status changed", LogCategory.Service, LogLevel.Normal),
    ConnectionChanged("Dhizuku service connection changed", LogCategory.Service, LogLevel.Normal),
    AppRequestedAccess("App requested Dhizuku access", LogCategory.AppRequest, LogLevel.Normal),
    AppConnected("App connected", LogCategory.AppRequest, LogLevel.Normal),
    AppDisconnected("App disconnected", LogCategory.AppRequest, LogLevel.Normal),
    PermissionGranted("Permission granted", LogCategory.Permission, LogLevel.Normal),
    PermissionDenied("Permission denied", LogCategory.Permission, LogLevel.Normal),
    AppBlocked("App blocked", LogCategory.Permission, LogLevel.Normal),
    CommandFromApp("Command from app", LogCategory.Command, LogLevel.Normal),
    CommandCompleted("Command completed", LogCategory.Command, LogLevel.Normal),
    CommandFailed("Command failed", LogCategory.Command, LogLevel.Warning),
    CommandBlocked("Command blocked", LogCategory.Command, LogLevel.Warning),
    ErrorOccurred("Error", LogCategory.Error, LogLevel.Error),
    DebugInfo("Debug information", LogCategory.Debug, LogLevel.Debug)
}

/** One saved line of the journal. Only the fields that are known are filled in. */
data class LogEntry(
    val time: Long,
    val level: LogLevel,
    val category: LogCategory,
    val event: LogEvent,
    val uid: Int = -1,
    val appLabel: String? = null,
    val packageName: String? = null,
    val message: String? = null,
    val command: String? = null,
    val result: String? = null,
    val error: String? = null,
    val details: String? = null
)

/** The settings that decide what is saved. */
data class LogConfig(
    val enabled: Boolean = false,
    val level: LogLevel = LogLevel.Normal,
    val categories: Set<LogCategory> = LogCategory.entries.toSet(),
    val maxSizeMb: Int = DEFAULT_MAX_SIZE_MB
) {
    val maxBytes: Long get() = maxSizeMb.toLong() * 1024L * 1024L

    fun accepts(category: LogCategory, level: LogLevel): Boolean =
        enabled && level.rank >= this.level.rank && category in categories

    companion object {
        const val DEFAULT_MAX_SIZE_MB = 5
        val SIZE_CHOICES_MB = listOf(1, 5, 10, 25)
    }
}
