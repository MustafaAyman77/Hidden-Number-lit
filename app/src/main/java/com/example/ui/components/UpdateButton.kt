package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.update.AppUpdateManager
import com.example.update.UpdateUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun UpdateButton(
    modifier: Modifier = Modifier,
    autoCheck: Boolean = true,
    languageAr: Boolean = true
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showReadyDialog by remember { mutableStateOf(false) }
    
    if (activity == null) {
        Text("Activity not available", color = Color.Red)
        return
    }
    
    val updateManager = remember { AppUpdateManager(context, scope) }
    val updateState by updateManager.updateState.collectAsState()
    
    // Auto-check for updates on startup
    LaunchedEffect(Unit) {
        if (autoCheck) {
            updateManager.checkForUpdates(manualTrigger = false)
        }
    }
    
    // Handle state changes
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateUIState.Available -> {
                showUpdateDialog = true
                showProgressDialog = false
                showReadyDialog = false
            }
            is UpdateUIState.Downloading -> {
                showProgressDialog = true
                showUpdateDialog = false
                showReadyDialog = false
            }
            is UpdateUIState.ReadyToInstall -> {
                showProgressDialog = false
                showReadyDialog = true
                showUpdateDialog = false
            }
            is UpdateUIState.Error -> {
                showErrorDialog = true
                showUpdateDialog = false
                showProgressDialog = false
                showReadyDialog = false
            }
            else -> {
                // Idle or Checking
            }
        }
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // زر التحقق من التحديث
        Button(
            onClick = {
                when (updateState) {
                    is UpdateUIState.Idle -> {
                        updateManager.checkForUpdates(manualTrigger = true)
                    }
                    is UpdateUIState.Available -> {
                        showUpdateDialog = true
                    }
                    else -> {
                        // Do nothing
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = updateState !is UpdateUIState.Downloading
        ) {
            when (updateState) {
                is UpdateUIState.Checking -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (languageAr) "جاري التحقق..." else "Checking...")
                }
                is UpdateUIState.Downloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val state = updateState as UpdateUIState.Downloading
                    Text(
                        if (languageAr) 
                            "جاري التحميل ${state.progressPercent}%" 
                        else 
                            "Downloading ${state.progressPercent}%"
                    )
                }
                is UpdateUIState.Available -> {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (languageAr) "تحديث متاح 🚀" else "Update Available 🚀")
                }
                else -> {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (languageAr) "التحقق من التحديث" else "Check for Update")
                }
            }
        }
        
        // عرض حالة التحديث
        when (updateState) {
            is UpdateUIState.Downloading -> {
                val state = updateState as UpdateUIState.Downloading
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    LinearProgressIndicator(
                        progress = state.progressPercent / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.downloadedFormatted} / ${state.totalFormatted}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            is UpdateUIState.Error -> {
                val state = updateState as UpdateUIState.Error
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (languageAr) state.messageAr else state.messageEn,
                    color = Color.Red,
                    fontSize = 12.sp
                )
            }
            else -> {}
        }
    }
    
    // Dialog التحديث المتاح
    if (showUpdateDialog && updateState is UpdateUIState.Available) {
        val state = updateState as UpdateUIState.Available
        UpdateAvailableDialog(
            manifest = state.manifest,
            currentVersion = state.currentVersionName,
            isWifi = state.isWifi,
            languageAr = languageAr,
            onUpdate = {
                showUpdateDialog = false
                updateManager.downloadAndInstallApk(state.manifest)
            },
            onSkip = {
                showUpdateDialog = false
                updateManager.skipVersion(state.manifest.versionCode.toLong())
            },
            onDismiss = {
                showUpdateDialog = false
            }
        )
    }
    
    // Dialog التحميل
    if (showProgressDialog && updateState is UpdateUIState.Downloading) {
        val state = updateState as UpdateUIState.Downloading
        DownloadProgressDialog(
            progress = state.progressPercent,
            downloaded = state.downloadedFormatted,
            total = state.totalFormatted,
            languageAr = languageAr,
            onCancel = {
                showProgressDialog = false
                updateManager.dismissUpdateUi()
            }
        )
    }
    
    // Dialog جاهز للتثبيت
    if (showReadyDialog && updateState is UpdateUIState.ReadyToInstall) {
        val state = updateState as UpdateUIState.ReadyToInstall
        ReadyToInstallDialog(
            manifest = state.manifest,
            languageAr = languageAr,
            onInstall = {
                showReadyDialog = false
                updateManager.installApk(state.apkFilePath)
            },
            onDismiss = {
                showReadyDialog = false
                updateManager.dismissUpdateUi()
            }
        )
    }
    
    // Dialog الخطأ
    if (showErrorDialog && updateState is UpdateUIState.Error) {
        val state = updateState as UpdateUIState.Error
        ErrorDialog(
            message = if (languageAr) state.messageAr else state.messageEn,
            languageAr = languageAr,
            onDismiss = {
                showErrorDialog = false
                updateManager.dismissUpdateUi()
            },
            onRetry = {
                showErrorDialog = false
                updateManager.checkForUpdates(manualTrigger = true)
            }
        )
    }
}

@Composable
fun UpdateAvailableDialog(
    manifest: UpdateManifest,
    currentVersion: String,
    isWifi: Boolean,
    languageAr: Boolean,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Green.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Green
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (languageAr) "🚀 تحديث متاح!" else "🚀 Update Available!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (languageAr) 
                        "الإصدار الحالي: $currentVersion → الإصدار الجديد: ${manifest.versionName}"
                    else 
                        "Current: $currentVersion → New: ${manifest.versionName}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                if (manifest.releaseNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (languageAr) "📝 ما الجديد:" else "📝 What's New:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    manifest.releaseNotes.take(3).forEach { note ->
                        Text(
                            text = "• $note",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (!isWifi) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Orange
                        )
                        Text(
                            text = if (languageAr) 
                                "⚠️ أنت تستخدم بيانات الجوال. قد تستهلك بيانات إضافية." 
                            else 
                                "⚠️ You are on mobile data. May consume extra data.",
                            fontSize = 12.sp,
                            color = Color.Orange
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!manifest.mandatory) {
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (languageAr) "تذكر لاحقاً" else "Remind Later")
                        }
                    }
                    
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier.weight(if (manifest.mandatory) 1f else 1f)
                    ) {
                        Text(if (languageAr) "تحديث الآن" else "Update Now")
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadProgressDialog(
    progress: Int,
    downloaded: String,
    total: String,
    languageAr: Boolean,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (languageAr) "⏳ جاري التحميل..." else "⏳ Downloading...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$progress%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "$downloaded / $total",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (languageAr) "إلغاء" else "Cancel")
                }
            }
        }
    }
}

@Composable
fun ReadyToInstallDialog(
    manifest: UpdateManifest,
    languageAr: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✅ ${if (languageAr) "اكتمل التحميل!" else "Download Complete!"}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (languageAr) 
                        "تم تحميل الإصدار ${manifest.versionName} بنجاح. هل تريد تثبيته الآن؟"
                    else 
                        "Version ${manifest.versionName} downloaded successfully. Install now?",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (languageAr) "تذكر لاحقاً" else "Remind Later")
                    }
                    
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (languageAr) "تثبيت الآن" else "Install Now")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    languageAr: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "❌ ${if (languageAr) "خطأ!" else "Error!"}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (languageAr) "إغلاق" else "Close")
                    }
                    
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (languageAr) "إعادة المحاولة" else "Retry")
                    }
                }
            }
        }
    }
}
