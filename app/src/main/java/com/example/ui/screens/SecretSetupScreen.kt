package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameMode
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberKeypad
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SecretSetupScreen(
    viewModel: MainViewModel,
    codeLength: Int,
    allowRepetition: Boolean,
    mySecret: String,
    mode: GameMode,
    languageAr: Boolean
) {
    val context = LocalContext.current
    var inputSecret by remember { mutableStateOf(mySecret) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val isSecretConfirmed = mySecret.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface, DarkBackground)
                )
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.LOBBY) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (languageAr) "إعداد الرقم السري 🔐" else "Secret Code Setup 🔐",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Security",
                tint = NeonEmerald,
                modifier = Modifier.size(24.dp)
            )
        }

        // Game Rules Summary Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (languageAr) "قواعد الرقم السري للمباراة:" else "Secret Code Rules:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (languageAr) "الطول المطلوب:" else "Required Length:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (languageAr) "$codeLength أرقام" else "$codeLength Digits",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (languageAr) "تكرار الأرقام:" else "Digit Repetition:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = if (allowRepetition) {
                            if (languageAr) "مسموح 🔄" else "Allowed 🔄"
                        } else {
                            if (languageAr) "ممنوع (أرقام فريدة) 🚫" else "Unique Only 🚫"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (allowRepetition) NeonEmerald else NeonMagenta
                    )
                }
            }
        }

        // Secret Setup Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowEffect = !isSecretConfirmed,
            borderColor = if (isSecretConfirmed) NeonEmerald else NeonMagenta
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSecretConfirmed) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (isSecretConfirmed) NeonEmerald else NeonMagenta,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isSecretConfirmed) {
                            if (languageAr) "تم تأكيد وقفل الرقم السري 🔒" else "Secret Code Locked 🔒"
                        } else {
                            if (languageAr) "أدخل رقمك السري الخفي ($codeLength أرقام):" else "Enter Secret ($codeLength Digits):"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Digits Slot Display Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceGlass)
                        .border(1.5.dp, if (isSecretConfirmed) NeonEmerald else NeonCyan, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val placeHolder = "_".repeat(codeLength)
                    Text(
                        text = if (inputSecret.isEmpty()) placeHolder else inputSecret,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = if (inputSecret.isNotEmpty()) NeonCyan else TextSecondary,
                        letterSpacing = 6.sp
                    )
                }

                validationError?.let { err ->
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isSecretConfirmed) {
                    // Cyber Keypad Input
                    CyberKeypad(
                        onDigitClick = { digit ->
                            if (inputSecret.length < codeLength) {
                                inputSecret += digit
                                validationError = null
                            }
                        },
                        onBackspaceClick = {
                            if (inputSecret.isNotEmpty()) {
                                inputSecret = inputSecret.dropLast(1)
                                validationError = null
                            }
                        },
                        onSubmitClick = {
                            val err = validateSecretInput(inputSecret, codeLength, allowRepetition, languageAr)
                            if (err != null) {
                                validationError = err
                            } else {
                                viewModel.setMySecretNumberAndStart(inputSecret)
                                Toast.makeText(
                                    context,
                                    if (languageAr) "تم قفل رقمك السري بنجاح! 🔒" else "Secret locked! 🔒",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    CyberButton(
                        text = if (languageAr) "قفل وتشفير الرقم السري 🔐" else "Lock & Confirm Secret 🔐",
                        onClick = {
                            val err = validateSecretInput(inputSecret, codeLength, allowRepetition, languageAr)
                            if (err != null) {
                                validationError = err
                            } else {
                                viewModel.setMySecretNumberAndStart(inputSecret)
                                Toast.makeText(
                                    context,
                                    if (languageAr) "تم قفل رقمك السري بنجاح! 🔒" else "Secret locked! 🔒",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = inputSecret.length == codeLength,
                        modifier = Modifier.fillMaxWidth(),
                        primaryColor = NeonEmerald
                    )
                } else {
                    // Confirmed State Banner
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonEmerald,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (languageAr) "في انتظار قفل الرقم السري للخصم للانتقال التلقائي للعب... ⏳" else "Waiting for opponent secret lock... ⏳",
                            fontSize = 13.sp,
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun validateSecretInput(secret: String, length: Int, allowRep: Boolean, languageAr: Boolean): String? {
    if (secret.length != length) {
        return if (languageAr) "يجب إدخال $length أرقام بالضبط!" else "Must enter exactly $length digits!"
    }
    if (!allowRep && secret.toSet().size != length) {
        return if (languageAr) "غير مسموح بتكرار الأرقام حسب قواعد الغرفة!" else "Duplicate digits not allowed!"
    }
    return null
}
