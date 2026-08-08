package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameMode
import com.example.data.model.PlayerProfile
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
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

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    profile: PlayerProfile,
    languageAr: Boolean
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF110A2B), DarkBackground)
                )
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, NeonMagenta))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("7", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }

                Column {
                    Text(
                        text = if (languageAr) "لعبة الرقم المخفي" else "Hidden Number",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = if (languageAr) "التحدي النيون المستقبلي⚡" else "Cyber Neon Challenge⚡",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
            }

            Row {
                IconButton(onClick = { viewModel.toggleLanguage() }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = NeonCyan
                    )
                }
                IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Profile & XP Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(AppScreen.PROFILE) },
            glowEffect = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerAvatarView(
                            avatarId = profile.avatarId,
                            customUri = profile.avatarCustomUri,
                            size = 56.dp,
                            borderColor = NeonMagenta
                        )

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.username,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                IconButton(
                                    onClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonYellow.copy(0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (languageAr) "المستوى ${profile.level} ⭐" else "Level ${profile.level} ⭐",
                                    fontSize = 11.sp,
                                    color = NeonYellow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Stats summary badge
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${profile.winRate}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonEmerald
                        )
                        Text(
                            text = if (languageAr) "نسبة الفوز" else "Win Rate",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // XP Progress bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (languageAr) "نقاط الخبرة (XP)" else "Experience (XP)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${profile.xp} / ${(profile.level * 200)} XP",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (profile.xp % 200) / 200f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonCyan,
                        trackColor = DarkSurfaceGlass
                    )
                }
            }
        }

        // Section Title: Modes
        Text(
            text = if (languageAr) "اختر نمط اللعب:" else "Select Game Mode:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Game Mode Cards
        ModeCard(
            mode = GameMode.SINGLE_PLAYER,
            icon = Icons.Default.Psychology,
            accentColor = NeonCyan,
            languageAr = languageAr,
            onClick = {
                viewModel.selectGameMode(GameMode.SINGLE_PLAYER)
                viewModel.createRoom()
                viewModel.navigateTo(AppScreen.SECRET_SETUP)
            }
        )

        ModeCard(
            mode = GameMode.ONLINE_ROOM,
            icon = Icons.Default.Public,
            accentColor = NeonMagenta,
            languageAr = languageAr,
            onClick = {
                viewModel.selectGameMode(GameMode.ONLINE_ROOM)
                viewModel.navigateTo(AppScreen.CREATE_JOIN)
            }
        )

        ModeCard(
            mode = GameMode.LOCAL_WIFI,
            icon = Icons.Default.Wifi,
            accentColor = NeonEmerald,
            languageAr = languageAr,
            onClick = {
                viewModel.selectGameMode(GameMode.LOCAL_WIFI)
                viewModel.navigateTo(AppScreen.CREATE_JOIN)
            }
        )

        // Bottom Action Bar: History & Leaderboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(AppScreen.HISTORY) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = NeonCyan
                    )
                    Text(
                        text = if (languageAr) "سجل المباريات" else "Match History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileModal(
            currentUsername = profile.username,
            currentAvatarId = profile.avatarId,
            languageAr = languageAr,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, avatarId ->
                viewModel.updateProfile(name, avatarId)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun ModeCard(
    mode: GameMode,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    languageAr: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        borderColor = accentColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.5.dp, accentColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = mode.name,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (languageAr) mode.titleAr else mode.titleEn,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (languageAr) mode.descAr else mode.descEn,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun EditProfileModal(
    currentUsername: String,
    currentAvatarId: Int,
    languageAr: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentUsername) }
    var selectedAvatar by remember { mutableStateOf(currentAvatarId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF17123A),
        title = {
            Text(
                text = if (languageAr) "تعديل الملف الشخصي" else "Edit Profile",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (languageAr) "اسم اللاعب" else "Player Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Text(
                    text = if (languageAr) "اختر الصورة الرمزية (Avatar):" else "Choose Avatar:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..4) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (selectedAvatar == i) NeonCyan.copy(0.3f) else DarkSurfaceGlass)
                                .border(
                                    2.dp,
                                    if (selectedAvatar == i) NeonCyan else GlassBorder,
                                    CircleShape
                                )
                                .clickable { selectedAvatar = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = avatarEmoji(i), fontSize = 24.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nameInput, selectedAvatar) }) {
                Text(if (languageAr) "حفظ" else "Save", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (languageAr) "إلغاء" else "Cancel", color = TextSecondary)
            }
        }
    )
}

fun avatarEmoji(avatarId: Int): String {
    return when (avatarId) {
        1 -> "🥷"
        2 -> "🤖"
        3 -> "🦊"
        4 -> "👽"
        else -> "🥷"
    }
}
