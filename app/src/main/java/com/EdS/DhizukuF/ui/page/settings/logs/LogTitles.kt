package com.EdS.DhizukuF.ui.page.settings.logs

import androidx.annotation.StringRes
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.logging.LogCategory
import com.EdS.DhizukuF.logging.LogEvent
import com.EdS.DhizukuF.logging.LogLevel

@StringRes
fun LogEvent.titleRes(): Int = when (this) {
    LogEvent.ServiceStarted -> R.string.log_event_service_started
    LogEvent.ServiceStopped -> R.string.log_event_service_stopped
    LogEvent.OwnerStatusChanged -> R.string.log_event_owner_changed
    LogEvent.ConnectionChanged -> R.string.log_event_connection_changed
    LogEvent.AppRequestedAccess -> R.string.log_event_app_requested
    LogEvent.AppConnected -> R.string.log_event_app_connected
    LogEvent.AppDisconnected -> R.string.log_event_app_disconnected
    LogEvent.PermissionGranted -> R.string.log_event_permission_granted
    LogEvent.PermissionDenied -> R.string.log_event_permission_denied
    LogEvent.AppBlocked -> R.string.log_event_app_blocked
    LogEvent.CommandFromApp -> R.string.log_event_command_from_app
    LogEvent.CommandCompleted -> R.string.log_event_command_completed
    LogEvent.CommandFailed -> R.string.log_event_command_failed
    LogEvent.CommandBlocked -> R.string.log_event_command_blocked
    LogEvent.ErrorOccurred -> R.string.log_event_error
    LogEvent.DebugInfo -> R.string.log_event_debug
}

/** The level of one entry. */
@StringRes
fun LogLevel.entryLabelRes(): Int = when (this) {
    LogLevel.Debug -> R.string.log_entry_level_debug
    LogLevel.Normal -> R.string.log_entry_level_normal
    LogLevel.Warning -> R.string.log_entry_level_warning
    LogLevel.Error -> R.string.log_entry_level_error
}

/** The choice in Settings > Logs > Log level. */
@StringRes
fun LogLevel.settingLabelRes(): Int = when (this) {
    LogLevel.Error -> R.string.log_level_errors
    LogLevel.Warning -> R.string.log_level_warnings
    LogLevel.Normal -> R.string.log_level_normal
    LogLevel.Debug -> R.string.log_level_debug
}

@StringRes
fun LogLevel.settingDescRes(): Int = when (this) {
    LogLevel.Error -> R.string.log_level_errors_desc
    LogLevel.Warning -> R.string.log_level_warnings_desc
    LogLevel.Normal -> R.string.log_level_normal_desc
    LogLevel.Debug -> R.string.log_level_debug_desc
}

@StringRes
fun LogCategory.labelRes(): Int = when (this) {
    LogCategory.Service -> R.string.log_cat_service
    LogCategory.Permission -> R.string.log_cat_permission
    LogCategory.AppRequest -> R.string.log_cat_app_request
    LogCategory.Command -> R.string.log_cat_command
    LogCategory.Error -> R.string.log_cat_error
    LogCategory.Debug -> R.string.log_cat_debug
}

@StringRes
fun LogCategory.descRes(): Int = when (this) {
    LogCategory.Service -> R.string.log_cat_service_desc
    LogCategory.Permission -> R.string.log_cat_permission_desc
    LogCategory.AppRequest -> R.string.log_cat_app_request_desc
    LogCategory.Command -> R.string.log_cat_command_desc
    LogCategory.Error -> R.string.log_cat_error_desc
    LogCategory.Debug -> R.string.log_cat_debug_desc
}
