package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameMode
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateJoinRoomScreen(
    viewModel: MainViewModel,
    mode: GameMode,
    roomCode: String,
    languageAr: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var inputRoomCode by remember { mutableStateOf("") }
    var joinError by remember { mutableStateOf<String?>(null) }

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
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (mode == GameMode.ONLINE_ROOM) {
                    if (languageAr) "غرفة أونلاين (Online PIN)" else "Online Remote Room"
                } else {
                    if (languageAr) "شبكة محلية (Local Wi-Fi)" else "Local Wi-Fi Room"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (mode == GameMode.ONLINE_ROOM) Icons.Default.Public else Icons.Default.Wifi,
                contentDescription = "Mode",
                tint = if (mode == GameMode.ONLINE_ROOM) NeonMagenta else NeonEmerald,
                modifier = Modifier.size(24.dp)
            )
        }

        // Card 1: Create New Room
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowEffect = true,
            borderColor = NeonCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Create",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (languageAr) "1. إنشاء غرفة جديدة (Host Room)" else "1. Create New Room (Host)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (languageAr) "أنشئ غرفة برمز سداسي فريد وشارك الرمز مع الخصم" else "Generate a unique 6-digit PIN and share with opponent",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Create Button only
                CyberButton(
                    text = if (languageAr) "إنشاء وتوليد رمز غرفة جديد ⚡" else "Generate New Room PIN ⚡",
                    onClick = {
                        viewModel.createRoom()
                        Toast.makeText(
                            context,
                            if (languageAr) "تم إنشاء الغرفة بنجاح! 🚀" else "Room created successfully! 🚀",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    primaryColor = NeonCyan
                )
            }
        }

        // Card 2: Join Opponent's Room (DIRECTLY BELOW Card 1 as requested)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonMagenta
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = "Join",
                        tint = NeonMagenta,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (languageAr) "2. الانضمام لغرفة الخصم (Join Opponent Room)" else "2. Join Opponent Room (Guest)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (mode == GameMode.LOCAL_WIFI) {
                                if (languageAr) "أدخل عنوان IP الخاص بشبكة الخصم شاملاً الأرقام والنقاط (.)" else "Enter opponent's Host IP address (including numbers & dots)"
                            } else {
                                if (languageAr) "أدخل الرمز السداسي الذي أنشأه الخصم للانضمام السريع" else "Enter the 6-digit PIN created by your opponent"
                            },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = if (mode == GameMode.LOCAL_WIFI) {
                        if (languageAr) "عنوان IP الخاص بالغرفة:" else "Opponent's IP Address Input:"
                    } else {
                        if (languageAr) "خانة إدخال رمز الغرفة للخصم:" else "Opponent's Room PIN Input:"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val maxLen = if (mode == GameMode.LOCAL_WIFI) 15 else 6

                OutlinedTextField(
                    value = inputRoomCode,
                    onValueChange = {
                        if (it.length <= maxLen) {
                            inputRoomCode = it.uppercase().trim()
                            joinError = null
                        }
                    },
                    placeholder = {
                        Text(
                            text = if (mode == GameMode.LOCAL_WIFI) {
                                if (languageAr) "أدخل عنوان IP كامل (مثال: 192.168.1.5)..." else "Enter full IP address (e.g. 192.168.1.5)..."
                            } else {
                                if (languageAr) "أدخل الرمز السداسي (مثال: 849201)..." else "Enter 6-Digit PIN (e.g. 849201)..."
                            },
                            color = TextSecondary.copy(0.6f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonMagenta,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceGlass,
                        unfocusedContainerColor = DarkSurfaceGlass
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                joinError?.let { err ->
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = com.example.ui.theme.NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                val isInputValid = if (mode == GameMode.LOCAL_WIFI) {
                    inputRoomCode.length >= 7 && inputRoomCode.contains(".")
                } else {
                    inputRoomCode.length == 6
                }

                CyberButton(
                    text = if (languageAr) "الانضمام المباشر للغرفة الآن ⚡" else "Join Room Instantly ⚡",
                    onClick = {
                        if (!isInputValid) {
                            joinError = if (mode == GameMode.LOCAL_WIFI) {
                                if (languageAr) "يرجى إدخال عنوان IP كامل مع النقاط (مثال: 192.168.1.5)!" else "Please enter a valid IP address with dots!"
                            } else {
                                if (languageAr) "يجب إدخال الرمز السداسي كاملاً (6 أرقام)!" else "Must enter full 6-digit PIN!"
                            }
                        } else {
                            viewModel.joinRoom(inputRoomCode)
                            Toast.makeText(
                                context,
                                if (languageAr) "جارٍ الاتصال السريع بالغرفة... ⚡" else "Connecting fast to room... ⚡",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = isInputValid,
                    modifier = Modifier.fillMaxWidth(),
                    primaryColor = NeonMagenta
                )
            }
        }
    }
}
