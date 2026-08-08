package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.update.UpdateUIState

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

    Dialog(
        onDismissRequest = {
            if (!isMandatory && updateState !is UpdateUIState.Downloading) {
                onDismissClick()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory && updateState !is UpdateUIState.Downloading,
            dismissOnClickOutside = !isMandatory && updateState !is UpdateUIState.Downloading
        )
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            borderColor = when (updateState) {
                is UpdateUIState.Error -> if (updateState.manifest == null) NeonCyan else NeonRed
                is UpdateUIState.ReadyToInstall -> NeonEmerald
                else -> NeonCyan
            },
            glowEffect = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when (updateState) {
                                is UpdateUIState.Error -> if (updateState.manifest == null) NeonCyan.copy(0.2f) else NeonRed.copy(0.2f)
                                is UpdateUIState.ReadyToInstall -> NeonEmerald.copy(0.2f)
                                else -> NeonCyan.copy(0.2f)
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when (updateState) {
                                is UpdateUIState.Error -> if (updateState.manifest == null) NeonCyan else NeonRed
                                is UpdateUIState.ReadyToInstall -> NeonEmerald
                                else -> NeonCyan
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (updateState) {
                            is UpdateUIState.ReadyToInstall -> Icons.Default.CheckCircle
                            is UpdateUIState.Error -> if (updateState.manifest == null) Icons.Default.CheckCircle else Icons.Default.Warning
                            is UpdateUIState.Downloading -> Icons.Default.Download
                            else -> Icons.Default.SystemUpdate
                        },
                        contentDescription = "Update Icon",
                        tint = when (updateState) {
                            is UpdateUIState.Error -> if (updateState.manifest == null) NeonCyan else NeonRed
                            is UpdateUIState.ReadyToInstall -> NeonEmerald
                            else -> NeonCyan
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (updateState) {
                        is UpdateUIState.Error -> if (updateState.manifest == null) {
                            if (languageAr) "حالة التحديثات ℹ️" else "Update Status ℹ️"
                        } else {
                            if (languageAr) "تنبيه التحديث" else "Update Notice"
                        }
                        is UpdateUIState.ReadyToInstall -> if (languageAr) "التحديث جاهز للتثبيت 🚀" else "Update Ready to Install 🚀"
                        is UpdateUIState.Downloading -> if (languageAr) "جاري تنزيل التحديث..." else "Downloading Update..."
                        else -> if (languageAr) "🆕 تحديث جديد متوفر" else "🆕 New Update Available"
                    },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (updateState) {
                    is UpdateUIState.Available -> {
                        val manifest = updateState.manifest

                        // Version badge card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (languageAr) "الإصدار الحالي: ${updateState.currentVersionName}" else "Current: ${updateState.currentVersionName}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (languageAr) "الإصدار الجديد: ${manifest.versionName}" else "New Version: ${manifest.versionName}",
                                        color = NeonEmerald,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (manifest.size.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonCyan.copy(0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = manifest.size,
                                            color = NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Wi-Fi warning if on Mobile Data
                        if (!updateState.isWifi) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonYellow.copy(0.15f))
                                    .border(1.dp, NeonYellow.copy(0.4f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SignalWifiOff,
                                        contentDescription = "Wi-Fi Warning",
                                        tint = NeonYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (languageAr) "أنت متصل ببيانات الهاتف. يُفضل الاتصال بشبكة Wi-Fi للتنزيل." else "Connected via Mobile Data. Wi-Fi is recommended.",
                                        color = NeonYellow,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Mandatory badge
                        if (manifest.mandatory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (languageAr) "⚠️ هذا التحديث مطلوب للاستمرار في استخدام اللعبة." else "⚠️ This update is required to continue playing.",
                                color = NeonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Release notes
                        if (manifest.releaseNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = if (languageAr) "ملاحظات التحديث:" else "Release Notes:",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                manifest.releaseNotes.forEach { note ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(text = "• ", color = NeonCyan, fontSize = 13.sp)
                                        Text(text = note, color = TextPrimary, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        CyberButton(
                            text = if (languageAr) "تحديث الآن 🚀" else "Update Now 🚀",
                            onClick = onUpdateClick,
                            primaryColor = NeonEmerald,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!manifest.mandatory) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CyberButton(
                                text = if (languageAr) "لاحقًا" else "Later",
                                onClick = onDismissClick,
                                primaryColor = Color.Gray,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is UpdateUIState.Downloading -> {
                        val progress = updateState.progressPercent / 100f
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceGlass)
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = NeonCyan,
                                    trackColor = Color.White.copy(0.1f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${updateState.progressPercent}%",
                                        color = NeonCyan,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${updateState.downloadedFormatted} / ${updateState.totalFormatted}",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (languageAr) "يرجى الانتظار أثناء تنزيل الملف..." else "Please wait while downloading...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    is UpdateUIState.ReadyToInstall -> {
                        Text(
                            text = if (languageAr) "تم تنزيل ملف التحديث بنجاح. اضغط أدناه لبدء التثبيت." else "Update downloaded successfully. Tap below to install.",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        CyberButton(
                            text = if (languageAr) "تثبيت الآن ⚙️" else "Install Now ⚙️",
                            onClick = { onInstallClick(updateState.apkFilePath) },
                            primaryColor = NeonEmerald,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is UpdateUIState.Error -> {
                        Text(
                            text = if (languageAr) updateState.messageAr else updateState.messageEn,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        CyberButton(
                            text = if (languageAr) "موافق" else "OK",
                            onClick = onDismissClick,
                            primaryColor = if (updateState.manifest == null) NeonCyan else NeonRed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}
