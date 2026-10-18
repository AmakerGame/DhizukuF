package com.EdS.DhizukuF.ui.activity

import com.EdS.DhizukuF.ui.widget.TvTextButton
import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.security.DeviceAuth
import com.EdS.DhizukuF.security.EntryLock
import com.EdS.DhizukuF.ui.widget.TvButton
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import com.EdS.DhizukuF.R
import com.EdS.DhizukuF.data.common.util.asFlow
import com.EdS.DhizukuF.ui.page.settings.SettingsPage
import com.EdS.DhizukuF.ui.theme.DhizukuTheme
import org.koin.core.component.KoinComponent
import androidx.core.net.toUri

class SettingsActivity : FragmentActivity(), KoinComponent {
    private val appPrefs by lazy {
        getSharedPreferences("app", MODE_PRIVATE)
    }

    private val agreementFlow by lazy {
        appPrefs.asFlow("agreement", false)
    }

    private lateinit var stringLauncher: ActivityResultLauncher<String>

    private val settingsRepo by inject<SettingsRepo>()

    // "Require authentication > when opening the app": the page stays hidden until the user passes the prompt.
    private var locked by mutableStateOf(false)
    private var started = false

    private fun entryAuthNeeded(): Boolean =
        settingsRepo.authMode.onEntry && DeviceAuth.canAuthenticate(this) && !EntryLock.isUnlocked()

    private fun unlock() {
        DeviceAuth.authenticate(
            this,
            getString(R.string.auth_prompt_entry_title),
            getString(R.string.auth_prompt_entry_subtitle)
        ) { ok ->
            if (ok) {
                locked = false
                startUp()
            }
        }
    }

    /** The system requests that used to run at once, now after the unlock. */
    private fun startUp() {
        if (started) return
        started = true
        requestPostNotificationPermission()
        requestIgnoreBatteryOptimization()
    }

    override fun onStart() {
        super.onStart()
        EntryLock.onReturned()
        if (!locked && entryAuthNeeded()) {
            locked = true
            unlock()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) EntryLock.onLeft()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        locked = entryAuthNeeded()
        setContent {
            DhizukuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (locked) {
                        LockScreen()
                    } else {
                        AgreementDialog()
                        SettingsPage(windowInsets = WindowInsets.safeDrawing)
                    }
                }
            }
        }

        stringLauncher = registerForActivityResult(
            RequestPermission()
        ) { _: Boolean -> }
        if (locked) unlock() else startUp()
    }

    @Composable
    private fun LockScreen() {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(400)
            runCatching { focus.requestFocus() }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.TwoTone.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.locked_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            TvButton(onClick = { unlock() }, modifier = Modifier.focusRequester(focus)) {
                Text(stringResource(R.string.locked_unlock))
            }
            TvTextButton(onClick = { finish() }) {
                Text(stringResource(R.string.locked_close))
            }
        }
    }

    fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                stringLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        ("package:$packageName").toUri()
                    )
                )
            } catch (_: ActivityNotFoundException) {
                // Some TV and embedded system images omit this optional settings activity.
                return
            }
        }
    }

    @Composable
    private fun AgreementDialog() {
        val agreement by agreementFlow.collectAsState(false)
        if (agreement) return

        AlertDialog(onDismissRequest = { }, title = {
            Text(text = stringResource(id = R.string.agreement_title))
        }, text = {
            val textColor = AlertDialogDefaults.textContentColor.toArgb()
            AndroidView(factory = {
                TextView(it).apply {
                    setTextColor(textColor)
                    movementMethod = LinkMovementMethod.getInstance()
                    text = HtmlCompat.fromHtml(
                        context.getString(R.string.agreement_text),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                }
            })
        }, dismissButton = {
            TvTextButton(onClick = {
                this@SettingsActivity.finish()
            }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }, confirmButton = {
            TvTextButton(onClick = {
                appPrefs.edit(true) {
                    putBoolean("agreement", true)
                }
            }) {
                Text(text = stringResource(id = R.string.agree_text))
            }
        })
    }
}