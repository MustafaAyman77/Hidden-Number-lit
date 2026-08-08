package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerProfile
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.AvatarSelectionGrid
import com.example.ui.components.CyberButton
import com.example.ui.components.GlassCard
import com.example.ui.components.PlayerAvatarView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileRegistrationScreen(
    viewModel: MainViewModel,
    profile: PlayerProfile,
    languageAr: Boolean
) {
    var username by remember(profile.username) { mutableStateOf(profile.username) }
    var selectedAvatarId by remember(profile.avatarId) { mutableStateOf(profile.avatarId) }
    var customUri by remember(profile.avatarCustomUri) { mutableStateOf(profile.avatarCustomUri) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF0F0826), DarkBackground)
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val maxContainerWidth = if (maxWidth > 600.dp) 560.dp else maxWidth

            Column(
                modifier = Modifier
                    .widthIn(max = maxContainerWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header with smooth back button and navigation title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (languageAr) "تسجيل وإدارة الحساب 👤" else "Account & Profile Registration 👤",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = if (languageAr) "خصص اسمك المستعار وصورتك لتمييز حسابك في اللعبة" else "Customize your username and avatar for the game",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Profile Level & Progress Hero Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowEffect = true,
                    borderColor = NeonCyan
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PlayerAvatarView(
                                    avatarId = selectedAvatarId,
                                    customUri = customUri,
                                    size = 58.dp,
                                    borderColor = NeonMagenta
                                )

                                Column {
                                    Text(
                                        text = username.ifEmpty { if (languageAr) "لاعب جديد" else "New Player" },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Level",
                                            tint = NeonYellow,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (languageAr) "المستوى ${profile.level}" else "Level ${profile.level}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonYellow
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonEmerald.copy(0.18f))
                                    .border(1.dp, NeonEmerald.copy(0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (languageAr) "محفوظ محلياً 🔒" else "Saved 🔒",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }
                        }

                        // XP Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (languageAr) "نقاط الخبرة (XP)" else "Experience (XP)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${profile.xp} / ${(profile.level * 200)} XP",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }

                            val progress = (profile.xp % 200).toFloat() / 200f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NeonCyan,
                                trackColor = DarkSurfaceGlass
                            )
                        }

                        // Stats Grid (Wins / Losses / Rate)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceGlass)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatCard(
                                title = if (languageAr) "الانتصارات" else "Wins",
                                value = "${profile.wins}",
                                color = NeonEmerald
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(GlassBorder)
                            )
                            ProfileStatCard(
                                title = if (languageAr) "الخسائر" else "Losses",
                                value = "${profile.losses}",
                                color = NeonYellow
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(30.dp)
                                    .background(GlassBorder)
                            )
                            ProfileStatCard(
                                title = if (languageAr) "نسبة الفوز" else "Win Rate",
                                value = "${profile.winRate}%",
                                color = NeonMagenta
                            )
                        }
                    }
                }

                // Registration Username Section
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (languageAr) "الاسم المستعار / الحساب:" else "Username / Display Name:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "${username.length}/20",
                                fontSize = 10.sp,
                                color = if (username.length > 20) NeonMagenta else TextSecondary
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { if (it.length <= 20) username = it },
                            placeholder = {
                                Text(
                                    text = if (languageAr) "أدخل اسمك المستعار..." else "Enter username...",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceGlass,
                                unfocusedContainerColor = DarkSurfaceGlass
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // Avatar Selection Grid (Custom Photo + Preset Characters)
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (languageAr) "اختر صورتك الشخصية 📸 أو الشخصية 🤖:" else "Select Profile Photo 📸 or Avatar 🤖:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )

                        AvatarSelectionGrid(
                            selectedAvatarId = selectedAvatarId,
                            customUri = customUri,
                            languageAr = languageAr,
                            onAvatarSelected = { selectedAvatarId = it },
                            onCustomUriChanged = { customUri = it }
                        )
                    }
                }

                // Save Success Message
                AnimatedVisibility(
                    visible = showSavedMessage,
                    enter = fadeIn() + slideInVertically { -20 },
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonEmerald.copy(0.2f))
                            .border(1.dp, NeonEmerald, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Saved",
                            tint = NeonEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (languageAr) "تم حفظ بيانات التسجيل والصورة بنجاح! 💾" else "Registration data saved successfully! 💾",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Save & Register Button
                CyberButton(
                    text = if (languageAr) "حفظ الحساب وتأكيد التسجيل 💾" else "Save & Confirm Registration 💾",
                    onClick = {
                        viewModel.updateProfileFull(
                            newUsername = username.ifBlank { if (languageAr) "لاعب جديد" else "New Player" },
                            newAvatarId = selectedAvatarId,
                            newCustomUri = customUri
                        )
                        showSavedMessage = true
                        coroutineScope.launch {
                            delay(1000)
                            showSavedMessage = false
                            viewModel.navigateTo(AppScreen.HOME)
                        }
                    },
                    primaryColor = NeonEmerald,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

