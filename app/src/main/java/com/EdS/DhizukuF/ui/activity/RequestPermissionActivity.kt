package com.EdS.DhizukuF.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.EdS.DhizukuF.data.settings.model.room.entity.AppEntity
import com.EdS.DhizukuF.data.settings.repo.AppRepo
import com.EdS.DhizukuF.data.settings.repo.SettingsRepo
import com.EdS.DhizukuF.dish.DishApproval
import com.EdS.DhizukuF.dish.DishDecision
import com.EdS.DhizukuF.dish.DishRequests
import com.rosan.dhizuku.shared.DhizukuVariables
import com.EdS.DhizukuF.ui.theme.DhizukuTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.cos
import kotlin.math.sin

class RequestPermissionActivity : ComponentActivity(), KoinComponent {
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
        val shouldShowDialog: Boolean = false,
        /** The checks in onCreate are finished and [shouldShowDialog] is final. */
        val ready: Boolean = false,
        /** The user (or the timeout) answered, so the stored entry must be updated. */
        val decided: Boolean = false,
        val blocked: Boolean = false
    )

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val appRepo by inject<AppRepo>()
    private val settingsRepo by inject<SettingsRepo>()
    private var state by mutableStateOf(ViewState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!registerAppEntity(intent)) {
            finish()
            return
        }

        // Check if dhizuku is enabled, the app is not blocked and the window is wanted
        coroutineScope.launch {
            val hide = state.copy(allowApi = false, timedOut = true, shouldShowDialog = false, ready = true)

            if (!settingsRepo.isDhizukuEnabled) {
                state = hide
                return@launch
            }

            val entity = appRepo.findByUID(state.uid)

            if (entity?.blocked == true) {
                state = hide
                return@launch
            }

            if (entity?.allowApi == true && entity.signature == state.signature) {
                state = state.copy(allowApi = true, timedOut = false, shouldShowDialog = false, ready = true)
                return@launch
            }

            if (settingsRepo.isWhitelistMode && (entity == null || !entity.allowApi)) {
                state = hide
                return@launch
            }

            // "Confirmation window" switched off: do not ask, the app stays in the
            // app list (switch off) and can be allowed from there.
            if (!settingsRepo.isConfirmationDialog) {
                state = hide
                return@launch
            }

            state = state.copy(shouldShowDialog = true, ready = true)
        }

        setContent {
            DhizukuTheme {
                if (state.ready && state.shouldShowDialog) {
                    LaunchedEffect(Unit) {
                        repeat(AUTO_DENY_SECONDS) { second ->
                            delay(1000)
                            state = state.copy(timeLeft = AUTO_DENY_SECONDS - second - 1)
                        }
                        if (!state.timedOut) {
                            decide(DishDecision.DENY)
                        }
                    }
                    if (!showDialog()) finish()
                } else if (state.ready) {
                    LaunchedEffect(Unit) { finish() }
                }
            }
        }
    }

    private fun decide(decision: DishDecision) {
        val uid = state.uid
        state = state.copy(
            allowApi = decision == DishDecision.ALLOW,
            blocked = decision == DishDecision.BLOCK,
            timedOut = true,
            decided = true
        )
        if (uid != UID_ERR) DishApproval.publish(uid, decision)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (registerAppEntity(intent)) return
        finish()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun finish() {
        super.finish()

        coroutineScope.launch {
            val uid = state.uid
            val packageInfo = packageManager.getPackageInfoForUid(uid) ?: return@launch
            val signature = packageInfo.signature ?: return@launch

            val entity = appRepo.findByUID(uid)
            when {
                entity == null -> appRepo.insert(
                    AppEntity(
                        uid = uid,
                        signature = signature,
                        allowApi = state.allowApi,
                        blocked = state.blocked
                    )
                )

                state.decided -> appRepo.update(
                    entity.copy(
                        uid = uid,
                        signature = signature,
                        allowApi = state.allowApi,
                        blocked = state.blocked || (entity.blocked && !state.allowApi),
                        modifiedAt = System.currentTimeMillis()
                    )
                )

                // Closed without an answer: never touch an existing approval, except
                // when the app was replaced by one with another signature.
                entity.signature != signature ->
                    appRepo.update(entity.copy(signature = signature, allowApi = false))

                else -> Unit
            }
        }.invokeOnCompletion {
            if (state.decided) DishRequests.cancel(applicationContext, state.uid)
            val result = if (state.allowApi) PackageManager.PERMISSION_GRANTED
            else PackageManager.PERMISSION_DENIED
            state.listener?.onRequestPermission(result)
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
        if (uid == -1) return false

        // The binder is optional: the dish terminal client starts this activity with
        // `am start` and gets the decision through DishApproval instead of a listener.
        val binder = bundle.getBinder(DhizukuVariables.PARAM_CLIENT_REQUEST_PERMISSION_BINDER)
        val listener = binder?.let {
            kotlin.runCatching { IDhizukuRequestPermissionListener.Stub.asInterface(it) }.getOrNull()
        }

        state = state.copy(
            uid = uid,
            signature = packageManager.getPackageInfoForUid(uid)?.signature ?: "",
            listener = listener
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
        val label = applicationInfo?.loadLabel(packageManager)
            ?: packageName

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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                @Composable
                fun MyTextButton(
                    onClick: () -> Unit,
                    @StringRes textResId: Int,
                    isPrimary: Boolean = false
                ) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth(),
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
                    decide(DishDecision.ALLOW)
                }, textResId = R.string.agree, isPrimary = true)
                MyTextButton(onClick = {
                    decide(DishDecision.DENY)
                }, textResId = R.string.refuse)
                MyTextButton(onClick = {
                    decide(DishDecision.BLOCK)
                }, textResId = R.string.block)
            }
        })
        return true
    }
}