package com.EdS.DhizukuF.ui.page.settings.user_manager

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.account.entity.UserEntity
import com.EdS.DhizukuF.data.account.repo.UserService
import com.EdS.DhizukuF.data.common.util.replace
import com.EdS.DhizukuF.data.common.util.requireShizukuPermissionGranted
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UserManagerViewModel : ViewModel(), KoinComponent {
    private val jobs = mutableMapOf<String, Job>()

    private val context by inject<Context>()
    private val userService by inject<UserService>()

    var state by mutableStateOf(UserManagerViewState())
        private set

    fun dispatch(action: UserManagerViewAction) {
        when (action) {
            UserManagerViewAction.Load -> load()
            is UserManagerViewAction.Remove -> remove(action.user)
        }
    }

    private fun load() {
        jobs.replace("load") {
            it?.cancel()

            viewModelScope.launch(Dispatchers.IO) {
                state = state.copy(loading = true)
                while (true) {
                    val result = try {
                        Result.success(userService.getUsers().sortedBy { u -> u.id })
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Result.failure(e)
                    }
                    val users = result.getOrNull()
                    if (users != null) {
                        // Only touch state when something really changed (no needless recomposition).
                        if (users != state.users || state.cause != null || state.loading) {
                            state = state.copy(users = users, cause = null, loading = false)
                        }
                    } else {
                        val e = result.exceptionOrNull()
                        Log.w("UserManager", "load failed", e)
                        state = state.copy(cause = e, loading = false)
                        // Do not retry in a tight loop (it re-asked for Shizuku permission every
                        // 1.5 s and froze the UI). The user retries via the button / pull-to-refresh.
                        return@launch
                    }
                    delay(5.seconds)
                }
            }
        }
    }

    private fun remove(user: UserEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = try {
                requireShizukuPermissionGranted(context) { userService.removeUser(user) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w("UserManager", "remove failed", e)
                false
            }
            if (success) {
                load()
            } else {
                state = state.copy(
                    cause = RuntimeException(context.getString(R.string.error_remove_user, user.name))
                )
            }
        }
    }
}
