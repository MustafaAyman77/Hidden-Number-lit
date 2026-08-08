package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.runtime.collectAsState
import com.example.ui.theme.NeonEmerald
import com.example.update.UpdateUIState

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    languageAr: Boolean
) {
    var soundEnabled by remember { mutableStateOf(viewModel.soundManager.soundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(viewModel.soundManager.hapticsEnabled) }
    val updateState by viewModel.updateState.collectAsState()
    val isChecking = updateState is UpdateUIState.Checking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, com.example.ui.theme.DarkSurface, DarkBackground)
                )
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = if (languageAr) "الإعدادات" else "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Audio & Haptics Settings
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = if (languageAr) "المؤثرات الصوتية (SFX)" else "Sound Effects (SFX)",
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        viewModel.soundManager.soundEnabled = it
                    }
                )

                SettingRow(
                    icon = Icons.Default.Notifications,
                    title = if (languageAr) "اهتزاز النقر (Haptics)" else "Haptic Vibration",
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        hapticsEnabled = it
                        viewModel.soundManager.hapticsEnabled = it
                    }
                )

                SettingRow(
                    icon = Icons.Default.Language,
                    title = if (languageAr) "اللغة (العربية)" else "Language (Arabic)",
                    checked = languageAr,
                    onCheckedChange = { viewModel.toggleLanguage() }
                )
            }
        }

        // App Information
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = NeonCyan)
                    Text(
                        text = if (languageAr) "عن التطبيق" else "About App",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = if (languageAr) "لعبة الرقم المخفي v1.0 - تم تطويرها بأحدث تقنيات أندرويد (Kotlin, Jetpack Compose, Material 3, WebSockets, Audio Track Real-Time Voice)."
                    else "Hidden Number Game v1.0 - Built with modern Android technologies (Kotlin, Jetpack Compose, Material 3, WebSockets, Real-Time Audio).",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))

                com.example.ui.components.CyberButton(
                    text = when (updateState) {
                        is UpdateUIState.Checking -> if (languageAr) "⏳ جاري التحقق..." else "⏳ Checking..."
                        is UpdateUIState.Available -> if (languageAr) "🚀 تحديث جديد متوفر!" else "🚀 New Update Available!"
                        is UpdateUIState.ReadyToInstall -> if (languageAr) "⚙️ تثبيت التحديث" else "⚙️ Install Update"
                        else -> if (languageAr) "🔄 التحقق من وجود تحديثات" else "🔄 Check for Updates"
                    },
                    onClick = {
                        if (!isChecking) {
                            if (updateState is UpdateUIState.Available) {
                                viewModel.downloadAndInstallUpdate((updateState as UpdateUIState.Available).manifest)
                            } else if (updateState is UpdateUIState.ReadyToInstall) {
                                viewModel.installApk((updateState as UpdateUIState.ReadyToInstall).apkFilePath)
                            } else {
                                viewModel.checkUpdatesManually()
                            }
                        }
                    },
                    primaryColor = when (updateState) {
                        is UpdateUIState.Available, is UpdateUIState.ReadyToInstall -> NeonEmerald
                        else -> NeonCyan
                    },
                    enabled = !isChecking,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = NeonCyan)
            Text(text = title, fontSize = 15.sp, color = TextPrimary)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(0.3f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = com.example.ui.theme.DarkSurfaceGlass
            )
        )
    }
}
