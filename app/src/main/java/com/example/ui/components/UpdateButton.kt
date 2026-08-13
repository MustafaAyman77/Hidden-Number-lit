package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.update.AppUpdateManager
import com.example.update.UpdateUIState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun UpdateButton(
    modifier: Modifier = Modifier,
    autoCheck: Boolean = true,
    languageAr: Boolean = true
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    var showDialog by remember { mutableStateOf(false) }
    
    if (activity == null) {
        return
    }
    
    val updateManager = remember { AppUpdateManager(context, scope) }
    val updateState by updateManager.updateState.collectAsState()
    
    // ✅ التحقق التلقائي عند بدء التطبيق
    LaunchedEffect(Unit) {
        if (autoCheck) {
            updateManager.checkForUpdates(manualTrigger = false)
        }
    }
    
    // ✅ إظهار الـ Dialog عند وجود تحديث
    LaunchedEffect(updateState) {
        val state = updateState
        when (state) {
            is UpdateUIState.Available -> {
                showDialog = true
            }
            is UpdateUIState.Error -> {
                if (state.manifest == null) {
                    showDialog = true
                }
            }
            else -> {}
        }
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        val currentState = updateState
        // ✅ زر التحقق من التحديث
        Button(
            onClick = {
                updateManager.checkForUpdates(manualTrigger = true)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentState !is UpdateUIState.Downloading
        ) {
            when (currentState) {
                is UpdateUIState.Checking -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (languageAr) "جاري التحقق..." else "Checking...")
                }
                is UpdateUIState.Downloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (languageAr) "جاري التحميل..." else "Downloading...")
                }
                is UpdateUIState.Available -> {
                    Text(if (languageAr) "🚀 تحديث متاح!" else "🚀 Update Available!")
                }
                else -> {
                    Text(if (languageAr) "🔄 التحقق من التحديث" else "🔄 Check Update")
                }
            }
        }
        
        // ✅ عرض الـ Dialog
        if (showDialog) {
            UpdateDialog(
                updateState = updateState,
                languageAr = languageAr,
                onUpdateClick = {
                    (updateState as? UpdateUIState.Available)?.let { availableState ->
                        updateManager.downloadAndInstallApk(availableState.manifest)
                    }
                },
                onInstallClick = { filePath ->
                    updateManager.installApk(filePath)
                },
                onDismissClick = {
                    showDialog = false
                    (updateState as? UpdateUIState.Available)?.let { availableState ->
                        updateManager.skipVersion(availableState.manifest.versionCode.toLong())
                    } ?: run {
                        updateManager.dismissUpdateUi()
                    }
                }
            )
        }
    }
}
