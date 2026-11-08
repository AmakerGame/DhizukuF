package com.EdS.DhizukuF.ui.activity

import com.EdS.DhizukuF.logging.LogManager

import com.EdS.DhizukuF.logging.LogEvent

import com.EdS.DhizukuF.ui.widget.tvFocusBorder
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.EdS.DhizukuF.security.DeviceAuth
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.EdS.DhizukuF.R
import com.rosan.dhizuku.aidl.IDhizukuRequestPermissionListener
import com.EdS.DhizukuF.data.common.util.getPackageInfoForUid
import com.EdS.DhizukuF.data.common.util.signature
import com.EdS.DhizukuF.dish.DishApproval
import com.EdS.DhizukuF.dish.DishRegistry
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.rosan.dhizuku.shared.DhizukuVariables
import com.EdS.DhizukuF.ui.theme.DhizukuTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.cos
import kotlin.math.sin
import java.util.concurrent.atomic.AtomicBoolean

class RequestPermissionActivity : FragmentActivity(), KoinComponent {
    companion object {
        const val UID_ERR = -1
        const val AUTO_DENY_SECONDS = 15
    }

    data class ViewState(
        val uid: Int = UID_ERR,
        val allowApi: Boolean = false,
        val signature: String = "",
        val listener: IDhizukuRequestPermissionListener? = null,
        val timeLeft: Int = AUTO_DENY_SECONDS,
        val timedOut: Boolean = false,
        val shouldShowDialog: Boolean = true,
        val block: Boolean = false
    )

    private val coroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, t -> Log.w("RequestPermission", "background task failed", t) }
    )

    // The answer of one request is saved and reported exactly once, however often finish() runs
    // (a button, onPause, the timer and a recomposition can all call it).
    private val answered = AtomicBoolean(false)

    // True while the system asks for the screen lock / fingerprint / face. The PIN screen is an activity of its
    // own, so this window pauses: that must not count as "the user left" (it would refuse the request), and the
    // countdown waits.
    @Volatile
    private var authenticating = false

    /**
     * "Allow" gives an app Device Owner power, so with *Require authentication > on confirmation* it needs the
     * screen lock / fingerprint / face first. Without a screen lock (or with the option off) it is allowed at once.
     * Refuse and Block only take power away: they never ask, and neither do the timer and Back.
     */
    private fun allowWithConfirmation(subtitleRes: Int, decide: () -> Unit) {
        if (!settingsRepo.authMode.onConfirm || !DeviceAuth.canAuthenticate(this)) {
            decide()
            return
        }
        authenticating = true
        DeviceAuth.authenticate(
            this,
            getString(R.string.auth_prompt_title),
            getString(subtitleRes)
        ) { ok ->
            authenticating = false
            if (ok) decide()
        }
    }

    private val appRepo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()
    private var state by mutableStateOf(ViewState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!registerAppEntity(intent)) {
            finish()
            return
        }


        // DB on IO; Compose state only on Main
        coroutineScope.launch {
            val uid = state.uid
            val signature = state.signature
            val enabled = if (DishRegistry.isDish(uid))
                settingsRepo.isDishEnabled else settingsRepo.isDhizukuEnabled
            val entity = appRepo.findByUID(uid)

            val next = when {
                !enabled ->
                    state.copy(allowApi = false, timedOut = true, shouldShowDialog = false)
                settingsRepo.isWhitelistMode && (entity == null || !entity.allowApi) ->
                    state.copy(allowApi = false, timedOut = true, shouldShowDialog = false)
                entity?.blocked == true ->
                    state.copy(allowApi = false, timedOut = true, shouldShowDialog = false)
                entity?.allowApi == true && entity.signature == signature ->
                    state.copy(allowApi = true, timedOut = false, shouldShowDialog = false)
                else -> null
            }
            if (next != null) {
                withContext(Dispatchers.Main) {
                    state = next
                    finish()
                }
            }
        }

        setContent {
            DhizukuTheme {
                if (state.shouldShowDialog) {
                    LaunchedEffect(Unit) {
                        var left = AUTO_DENY_SECONDS
                        while (left > 0) {
                            delay(1000)
                            if (authenticating) continue
                            left--
                            state = state.copy(timeLeft = left)
                        }
                        if (!state.timedOut) {
                            state = state.copy(allowApi = false, timedOut = true)
                            finish()
                        }
                    }
                }

                // finish() is a side effect: never call it straight from composition
                if (state.shouldShowDialog) {
                    if (!showDialog()) LaunchedEffect("unknown-caller") { finish() }
                } else {
                    LaunchedEffect("no-dialog") { finish() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A request that is replaced by a new one must still get an answer, or its client waits forever.
        if (answered.compareAndSet(false, true)) {
            reply(state.listener, PackageManager.PERMISSION_DENIED)
        }
        if (registerAppEntity(intent)) {
            answered.set(false)
            return
        }
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations && !authenticating) finish()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        if (!answered.compareAndSet(false, true)) return

        val uid = state.uid
        val allowApi = state.allowApi
        val block = state.block
        val listener = state.listener
        // dish waits for this: every refusal (button, Back, timeout, losing focus) must reach it
        if (!allowApi && uid != UID_ERR) DishApproval.results[uid] = false
        if (uid != UID_ERR) {
            LogManager.log(
                when {
                    allowApi -> LogEvent.PermissionGranted
                    block -> LogEvent.AppBlocked
                    else -> LogEvent.PermissionDenied
                },
                uid = uid, message = "Decision in the permission dialog"
            )
        }
        coroutineScope.launch {
            val packageInfo = packageManager.getPackageInfoForUid(uid) ?: return@launch
            val signature = packageInfo.signature ?: return@launch

            val entity = appRepo.findByUID(uid)

            // A dish terminal that was refused (or not answered) is forgotten, so it does not stay in
            // App management as a disabled row. A block is kept, and so is an approved terminal.
            if (!allowApi && !block && DishRegistry.isDish(uid)) {
                if (entity == null || (!entity.allowApi && !entity.blocked)) {
                    DishRegistry.unregister(uid)
                    if (entity != null) appRepo.delete(entity)
                }
                return@launch
            }

            if (entity == null)
                appRepo.insert(
                    AppEntity(uid = uid, signature = signature, allowApi = allowApi, blocked = block)
                )
            else
                appRepo.update(
                    entity.copy(
                        uid = uid,
                        signature = signature,
                        allowApi = allowApi,
                        blocked = entity.blocked || block
                    )
                )
        }.invokeOnCompletion {
            reply(
                listener,
                if (allowApi) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            )
        }
    }

    /** The client may be gone already: a dead listener must not crash DhizukuF. */
    private fun reply(listener: IDhizukuRequestPermissionListener?, result: Int) {
        try {
            listener?.onRequestPermission(result)
        } catch (t: Throwable) {
            Log.w("RequestPermission", "cannot reach the client", t)
        }
    }

    private fun registerAppEntity(intent: Intent?): Boolean {
        if (intent == null) return false
        val bundle = listOfNotNull(
            intent.extras,
            intent.getBundleExtra("bundle")
        ).find { it.containsKey(DhizukuVariables.PARAM_CLIENT_UID) }
            ?: return false

        val uid = bundle.getInt(DhizukuVariables.PARAM_CLIENT_UID, -1)
        if (uid < 0) return false

        // Reject unknown / gone packages (no dialog for a fake uid).
        if (packageManager.getPackagesForUid(uid).isNullOrEmpty()) {
            Log.w("RequestPermission", "refuse dialog: no package for uid $uid")
            return false
        }

        // API 31+: the activity must be started by that uid, or by DhizukuF itself (dish).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val from = launchedFromUid
            val self = applicationInfo.uid
            if (from != uid && from != self && from != Process.SYSTEM_UID) {
                Log.w(
                    "RequestPermission",
                    "refuse dialog: launchedFromUid=$from but client uid=$uid"
                )
                return false
            }
        }

        // The binder is optional: DhizukuF opens this dialog for a dish terminal
        // and learns the decision through DishApproval instead of a listener.
        val binder = bundle.getBinder(DhizukuVariables.PARAM_CLIENT_REQUEST_PERMISSION_BINDER)
        val listener = binder?.let {
            kotlin.runCatching { IDhizukuRequestPermissionListener.Stub.asInterface(it) }.getOrNull()
        }

        state = state.copy(
            uid = uid,
            signature = packageManager.getPackageInfoForUid(uid)?.signature ?: "",
            listener = listener
        )
        LogManager.log(
            LogEvent.AppRequestedAccess, uid = uid,
            message = if (DishRegistry.isDish(uid)) "dish terminal asks for approval" else "Permission dialog"
        )
        return true
    }

    @Composable
    private fun CountdownPieChart(
        progress: Float,
        modifier: Modifier = Modifier
    ) {
        val color = MaterialTheme.colorScheme.outline

        Canvas(modifier = modifier) {
            val radius = size.minDimension / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 2.dp.toPx() // Made thicker
            val lineRadius = radius - strokeWidth / 2f

            // Draw the remaining arc (hollow circle outline that gets eaten away counter-clockwise)
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f, // Start from top (12 o'clock)
                    sweepAngle = 360f * progress, // Sweep clockwise for remaining time
                    useCenter = false, // This makes it hollow!
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - lineRadius,
                        center.y - lineRadius
                    ),
                    size = androidx.compose.ui.geometry.Size(lineRadius * 2, lineRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Always draw a line from center to top (12 o'clock position)
            drawLine(
                color = color,
                start = center,
                end = androidx.compose.ui.geometry.Offset(center.x, center.y - lineRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Draw current time position line (moves clockwise as arc gets eaten counter-clockwise)
            if (progress > 0f && progress < 1f) {
                val angle = -90f + (360f * progress)
                val angleRad = Math.toRadians(angle.toDouble())
                val endX = center.x + lineRadius * cos(angleRad).toFloat()
                val endY = center.y + lineRadius * sin(angleRad).toFloat()

                drawLine(
                    color = color,
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    @Composable
    private fun showDialog(): Boolean {
        val uid = state.uid
        val packageInfo = packageManager.getPackageInfoForUid(uid) ?: return false
        val packageName = packageInfo.packageName
        val applicationInfo = packageInfo.applicationInfo
        val icon = applicationInfo?.loadIcon(packageManager)
            ?: packageManager.defaultActivityIcon
        val appLabel = applicationInfo?.loadLabel(packageManager)
            ?: packageName
        // "Termux (dish)" for terminals that use dish (can be turned off in settings)
        val label: CharSequence = if (DishRegistry.isDish(uid)) {
            DishRegistry.displayName(uid, appLabel.toString())
        } else appLabel

        val titleText = androidx.compose.runtime.remember(label) {
            AnnotatedString.fromHtml(
                getString(R.string.request_permission_text, label)
            )
        }

        val progress by animateFloatAsState(
            targetValue = state.timeLeft.toFloat() / AUTO_DENY_SECONDS,
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
            label = "countdown_progress"
        )

        AlertDialog(onDismissRequest = {
            finish()
        }, icon = {
            Image(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }, title = {
            Text(
                titleText,
                textAlign = TextAlign.Center
            )
        }, text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CountdownPieChart(
                        progress = progress,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (state.timeLeft > 0) {
                            stringResource(R.string.auto_deny_in_seconds, state.timeLeft)
                        } else {
                            stringResource(R.string.denying_access)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }, confirmButton = {
            // On a TV / box without a touch screen the dialog starts with the focus on "Refuse": a key
            // that is still held down from the previous screen can then never grant access by accident.
            val noTouch = !packageManager.hasSystemFeature("android.hardware.touchscreen") ||
                packageManager.hasSystemFeature("android.software.leanback")
            val safeFocus = remember { FocusRequester() }
            LaunchedEffect(noTouch) {
                if (noTouch) {
                    delay(300)
                    runCatching { safeFocus.requestFocus() }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                @Composable
                fun MyTextButton(
                    onClick: () -> Unit,
                    @StringRes textResId: Int,
                    isPrimary: Boolean = false,
                    focus: FocusRequester? = null
                ) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusBorder(RoundedCornerShape(8.dp))
                            .then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(16.dp),
                        colors = if (isPrimary) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) else ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(
                            stringResource(textResId),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                MyTextButton(onClick = {
                    allowWithConfirmation(R.string.auth_prompt_allow) {
                        state = state.copy(allowApi = true, timedOut = true)
                        finish()
                    }
                }, textResId = R.string.agree, isPrimary = true)
                MyTextButton(onClick = {
                    if (state.uid != UID_ERR) {
                        DishApproval.results[state.uid] = false
                    }
                    state = state.copy(allowApi = false, timedOut = true)
                    finish()
                }, textResId = R.string.refuse, focus = safeFocus)
                if (settingsRepo.isAdvancedConfirmation) {
                    MyTextButton(onClick = {
                        if (state.uid != UID_ERR) {
                            DishApproval.results[state.uid] = false
                        }
                        state = state.copy(allowApi = false, timedOut = true, block = true)
                        finish()
                    }, textResId = R.string.block)
                }
            }
        })
        return true
    }
}