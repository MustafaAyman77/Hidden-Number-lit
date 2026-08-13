package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.update.UpdateManifest
import com.example.update.UpdateUIState

/**
 * واجهة التحديث المحسنة مع تأثيرات بصرية
 */
@Composable
fun UpdateDialog(
    updateState: UpdateUIState,
    languageAr: Boolean,
    onUpdateClick: () -> Unit,
    onInstallClick: (String) -> Unit,
    onDismissClick: () -> Unit
) {
    if (updateState is UpdateUIState.Idle) return

    val isMandatory = when (updateState) {
        is UpdateUIState.Available -> updateState.manifest.mandatory
        is UpdateUIState.Downloading -> updateState.manifest.mandatory
        is UpdateUIState.ReadyToInstall -> updateState.manifest.mandatory
        is UpdateUIState.Error -> updateState.manifest?.mandatory ?: false
        else -> false
    }

    val canDismiss = !isMandatory && updateState !is UpdateUIState.Downloading

    Dialog(
        onDismissRequest = {
            if (canDismiss) {
                onDismissClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = DarkSurfaceGlass,
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UpdateDialogHeader(
                        updateState = updateState,
                        languageAr = languageAr,
                        canDismiss = canDismiss,
                        onDismiss = onDismissClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (updateState) {
                        is UpdateUIState.Available -> {
                            UpdateAvailableContent(
                                state = updateState,
                                languageAr = languageAr,
                                onUpdateClick = onUpdateClick,
                                onDismiss = onDismissClick
                            )
                        }
                        is UpdateUIState.Downloading -> {
                            UpdateDownloadingContent(
                                state = updateState,
                                languageAr = languageAr
                            )
                        }
                        is UpdateUIState.ReadyToInstall -> {
                            UpdateReadyContent(
                                manifest = updateState.manifest,
                                languageAr = languageAr,
                                onInstallClick = { onInstallClick(updateState.apkFilePath) },
                                onDismiss = onDismissClick
                            )
                        }
                        is UpdateUIState.Checking -> {
                            UpdateCheckingContent(languageAr = languageAr)
                        }
                        is UpdateUIState.Error -> {
                            UpdateErrorContent(
                                state = updateState,
                                languageAr = languageAr,
                                onDismiss = onDismissClick
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

/**
 * Header مع أيقونة وحالة التحديث
 */
@Composable
private fun UpdateDialogHeader(
    updateState: UpdateUIState,
    languageAr: Boolean,
    canDismiss: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    when (updateState) {
                        is UpdateUIState.Error -> if (updateState.manifest == null) 
                            NeonCyan.copy(0.15f) else NeonRed.copy(0.15f)
                        is UpdateUIState.ReadyToInstall -> NeonEmerald.copy(0.15f)
                        is UpdateUIState.Downloading -> NeonCyan.copy(0.15f)
                        else -> NeonCyan.copy(0.15f)
                    }
                )
                .border(
                    width = 2.dp,
                    color = when (updateState) {
                        is UpdateUIState.Error -> if (updateState.manifest == null) 
                            NeonCyan else NeonRed
                        is UpdateUIState.ReadyToInstall -> NeonEmerald
                        is UpdateUIState.Downloading -> NeonCyan
                        else -> NeonCyan
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (updateState) {
                is UpdateUIState.Downloading -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloading",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(rotation)
                    )
                }
                is UpdateUIState.ReadyToInstall -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Ready",
                        tint = NeonEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                }
                is UpdateUIState.Error -> {
                    Icon(
                        imageVector = if (updateState.manifest == null) Icons.Default.Info else Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = if (updateState.manifest == null) NeonCyan else NeonRed,
                        modifier = Modifier.size(28.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Text(
            text = when (updateState) {
                is UpdateUIState.Checking -> if (languageAr) "جاري التحقق..." else "Checking..."
                is UpdateUIState.Error -> if (updateState.manifest == null) {
                    if (languageAr) "معلومات التحديث" else "Update Info"
                } else {
                    if (languageAr) "خطأ في التحديث" else "Update Error"
                }
                is UpdateUIState.ReadyToInstall -> if (languageAr) "جاهز للتثبيت" else "Ready to Install"
                is UpdateUIState.Downloading -> if (languageAr) "جاري التحميل" else "Downloading"
                else -> if (languageAr) "تحديث متاح" else "Update Available"
            },
            color = when (updateState) {
                is UpdateUIState.Error -> if (updateState.manifest == null) NeonCyan else NeonRed
                is UpdateUIState.ReadyToInstall -> NeonEmerald
                else -> NeonCyan
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        if (canDismiss) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }
    }
}

/**
 * محتوى حالة "تحديث متاح"
 */
@Composable
private fun UpdateAvailableContent(
    state: UpdateUIState.Available,
    languageAr: Boolean,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val manifest = state.manifest

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (languageAr) "الإصدار الحالي" else "Current Version",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = state.currentVersionName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (languageAr) "الإصدار الجديد" else "New Version",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = manifest.versionName,
                        color = NeonEmerald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (manifest.size.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📦 ${manifest.size}",
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (!state.isWifi) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NeonYellow.copy(0.1f))
                .border(1.dp, NeonYellow.copy(0.3f), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = "Wi-Fi Warning",
                tint = NeonYellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (languageAr) 
                    "⚠️ يُفضل الاتصال بـ Wi-Fi للتنزيل" 
                else 
                    "⚠️ Wi-Fi recommended for download",
                color = NeonYellow,
                fontSize = 12.sp
            )
        }
    }

    if (manifest.mandatory) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NeonRed.copy(0.1f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Mandatory",
                tint = NeonRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (languageAr) 
                    "⚠️ هذا التحديث إجباري" 
                else 
                    "⚠️ This update is mandatory",
                color = NeonRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (manifest.releaseNotes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (languageAr) "📝 ما الجديد:" else "📝 What's New:",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            manifest.releaseNotes.take(4).forEach { note ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        color = NeonCyan,
                        fontSize = 13.sp
                    )
                    Text(
                        text = note,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    CyberButton(
        text = if (languageAr) "🚀 تحديث الآن" else "🚀 Update Now",
        onClick = onUpdateClick,
        primaryColor = NeonEmerald,
        modifier = Modifier.fillMaxWidth()
    )

    if (!manifest.mandatory) {
        Spacer(modifier = Modifier.height(8.dp))
        CyberButton(
            text = if (languageAr) "تذكر لاحقاً" else "Remind Later",
            onClick = onDismiss,
            primaryColor = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * محتوى حالة "جاري التحميل"
 */
@Composable
private fun UpdateDownloadingContent(
    state: UpdateUIState.Downloading,
    languageAr: Boolean
) {
    val progress = state.progressPercent / 100f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(NeonCyan, NeonEmerald)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${state.progressPercent}%",
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${state.downloadedFormatted} / ${state.totalFormatted}",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (languageAr) 
                "⏳ جاري تنزيل التحديث... يرجى الانتظار" 
            else 
                "⏳ Downloading update... Please wait",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * محتوى حالة "جاهز للتثبيت"
 */
@Composable
private fun UpdateReadyContent(
    manifest: UpdateManifest,
    languageAr: Boolean,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (languageAr) 
                "✅ تم تنزيل التحديث بنجاح!" 
            else 
                "✅ Update downloaded successfully!",
            color = NeonEmerald,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (languageAr) 
                "الإصدار ${manifest.versionName} جاهز للتثبيت" 
            else 
                "Version ${manifest.versionName} is ready to install",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        CyberButton(
            text = if (languageAr) "⚙️ تثبيت الآن" else "⚙️ Install Now",
            onClick = onInstallClick,
            primaryColor = NeonEmerald,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        CyberButton(
            text = if (languageAr) "تذكر لاحقاً" else "Remind Later",
            onClick = onDismiss,
            primaryColor = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * محتوى حالة "جاري التحقق"
 */
@Composable
private fun UpdateCheckingContent(
    languageAr: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = NeonCyan,
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (languageAr) 
                "🔍 جاري التحقق من وجود تحديثات..." 
            else 
                "🔍 Checking for updates...",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * محتوى حالة "خطأ"
 */
@Composable
private fun UpdateErrorContent(
    state: UpdateUIState.Error,
    languageAr: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (state.manifest == null) Icons.Default.Info else Icons.Default.Error,
            contentDescription = "Error",
            tint = if (state.manifest == null) NeonCyan else NeonRed,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (languageAr) state.messageAr else state.messageEn,
            color = TextPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        CyberButton(
            text = if (languageAr) "موافق" else "OK",
            onClick = onDismiss,
            primaryColor = if (state.manifest == null) NeonCyan else NeonRed,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
