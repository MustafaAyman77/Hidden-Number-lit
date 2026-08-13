package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.supabase.SupabaseClientProvider
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

data class AvatarCharacter(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val emoji: String,
    val colorStart: Color,
    val colorEnd: Color
)

val PRESET_AVATARS = listOf(
    AvatarCharacter(1, "السايبر بوت", "Cyber Bot", "🤖", Color(0xFF00F0FF), Color(0xFF7000FF)),
    AvatarCharacter(2, "محارب النيون", "Neon Warrior", "⚔️", Color(0xFFFF007A), Color(0xFF9900FF)),
    AvatarCharacter(3, "المشعوذ الرقمي", "Digital Wizard", "🧙‍♂️", Color(0xFF9D00FF), Color(0xFF00F0FF)),
    AvatarCharacter(4, "القتال الخفي", "Shadow Ninja", "🥷", Color(0xFF00FF87), Color(0xFF0080FF)),
    AvatarCharacter(5, "التنين الأسطوري", "Cyber Dragon", "🐉", Color(0xFFFF2A00), Color(0xFFFF8800)),
    AvatarCharacter(6, "الملكة الرقمية", "Neon Queen", "👑", Color(0xFFFF00D6), Color(0xFFFFB800)),
    AvatarCharacter(7, "الثعلب الماكر", "Cyber Fox", "🦊", Color(0xFFFF8800), Color(0xFFFF0055)),
    AvatarCharacter(8, "القائد الفضائي", "Astro Captain", "👨‍🚀", Color(0xFF00D2FF), Color(0xFF0038FF)),
    AvatarCharacter(9, "السايبورغ", "Cyborg Hero", "🦾", Color(0xFF00FFCC), Color(0xFF0077FF)),
    AvatarCharacter(10, "الفارس الذهبي", "Golden Knight", "🛡️", Color(0xFFFFD700), Color(0xFFFF6B00)),
    AvatarCharacter(11, "القرش النيون", "Neon Shark", "🦈", Color(0xFF00E5FF), Color(0xFF1A237E)),
    AvatarCharacter(12, "البومة الحكيمة", "Wise Owl", "🦉", Color(0xFFB388FF), Color(0xFF4A148C))
)

fun getAvatarCharacter(id: Int): AvatarCharacter {
    return PRESET_AVATARS.find { it.id == id } ?: PRESET_AVATARS[0]
}

@Composable
fun PlayerAvatarView(
    avatarId: Int,
    customUri: String? = null,
    size: Dp = 56.dp,
    showBorder: Boolean = true,
    borderColor: Color = NeonMagenta,
    modifier: Modifier = Modifier
) {
    val character = getAvatarCharacter(avatarId)
    val context = LocalContext.current
    var isImageError by remember(customUri) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (showBorder) {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(character.colorStart.copy(0.3f), character.colorEnd.copy(0.3f))
                            )
                        )
                        .border(2.dp, borderColor, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!customUri.isNullOrEmpty() && !isImageError) {
            // عرض الصورة من Supabase Storage (رابط مباشر)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(customUri)
                    .crossfade(true)
                    .diskCacheKey(customUri)
                    .build(),
                contentDescription = "Custom Profile Picture",
                contentScale = ContentScale.Crop,
                onError = { 
                    isImageError = true 
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // عرض الـ Emoji الافتراضي
            Text(
                text = character.emoji,
                fontSize = (size.value * 0.5f).sp
            )
        }
    }
}

@Composable
fun AvatarSelectionGrid(
    selectedAvatarId: Int,
    customUri: String?,
    languageAr: Boolean,
    onAvatarSelected: (Int) -> Unit,
    onCustomUriChanged: (String?) -> Unit,
    onUploadComplete: (String) -> Unit = {},
    onUploadError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storageService = SupabaseClientProvider.storageService
    val authService = SupabaseClientProvider.authService
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    
    // تحميل userId عند بداية الـ Composable
    LaunchedEffect(Unit) {
        currentUserId = authService.getCurrentUserId()
        if (currentUserId == null) {
            uploadError = if (languageAr) "يرجى تسجيل الدخول أولاً" else "Please login first"
        }
    }
    
    // طلب الأذونات
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        if (!hasPermission) {
            uploadError = if (languageAr) "الأذونات مطلوبة لاختيار الصور" else "Permissions required to select images"
            onUploadError(uploadError ?: "Permission denied")
        }
    }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !isUploading) {
            // التحقق من الأذونات
            if (!hasPermission) {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                permissionLauncher.launch(permissions)
                return@rememberLauncherForActivityResult
            }
            
            // التحقق من تسجيل الدخول
            if (currentUserId == null) {
                uploadError = if (languageAr) "يرجى تسجيل الدخول أولاً" else "Please login first"
                onUploadError(uploadError ?: "Not authenticated")
                return@rememberLauncherForActivityResult
            }
            
            scope.launch {
                isUploading = true
                uploadError = null
                
                try {
                    val userId = currentUserId ?: run {
                        uploadError = if (languageAr) "فشل الحصول على معرف المستخدم" else "Failed to get user ID"
                        onUploadError(uploadError ?: "User ID error")
                        isUploading = false
                        return@launch
                    }
                    
                    val result = storageService.uploadAvatar(context, uri, userId)
                    result.onSuccess { publicUrl ->
                        onUploadComplete(publicUrl)
                        onCustomUriChanged(publicUrl)
                    }.onFailure { error ->
                        val message = when {
                            error.message?.contains("permission", ignoreCase = true) == true -> 
                                if (languageAr) "ليس لديك صلاحية لرفع الصورة" else "Permission denied"
                            error.message?.contains("bucket", ignoreCase = true) == true ->
                                if (languageAr) "خطأ في المجلد" else "Bucket error"
                            error.message?.contains("network", ignoreCase = true) == true ->
                                if (languageAr) "خطأ في الاتصال بالشبكة" else "Network error"
                            else -> error.message ?: if (languageAr) "فشل رفع الصورة" else "Upload failed"
                        }
                        uploadError = message
                        onUploadError(message)
                    }
                } catch (e: Exception) {
                    val message = e.message ?: if (languageAr) "حدث خطأ غير متوقع" else "Unexpected error"
                    uploadError = message
                    onUploadError(message)
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Custom Image Upload Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceGlass)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
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
                        size = 54.dp,
                        borderColor = if (!customUri.isNullOrEmpty()) NeonEmerald else NeonCyan
                    )

                    Column {
                        Text(
                            text = if (languageAr) "صورة شخصية خاصة (اختياري)" else "Custom Profile Photo (Optional)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = when {
                                isUploading -> if (languageAr) "⏳ جاري رفع الصورة..." else "⏳ Uploading photo..."
                                uploadError != null -> "❌ $uploadError"
                                !customUri.isNullOrEmpty() -> if (languageAr) "✅ تم رفع الصورة بنجاح" else "✅ Photo uploaded successfully"
                                !hasPermission -> if (languageAr) "🔒 يرجى منح أذونات الصور" else "🔒 Please grant image permissions"
                                else -> if (languageAr) "📸 يمكنك رفع صورتك أو اختيار شخصية" else "📸 Upload photo or pick character"
                            },
                            fontSize = 11.sp,
                            color = when {
                                uploadError != null -> NeonRed
                                isUploading -> NeonYellow
                                !customUri.isNullOrEmpty() -> NeonEmerald
                                else -> TextSecondary
                            }
                        )
                    }
                }

                Row {
                    if (!customUri.isNullOrEmpty() && !isUploading) {
                        IconButton(onClick = { 
                            onCustomUriChanged(null)
                            uploadError = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove photo",
                                tint = NeonRed
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { 
                            if (!hasPermission) {
                                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO
                                    )
                                } else {
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                }
                                permissionLauncher.launch(permissions)
                            } else {
                                photoPickerLauncher.launch("image/*")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Photo",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (isUploading) {
                                if (languageAr) "جاري..." else "Uploading..."
                            } else {
                                if (languageAr) "اختر صورة" else "Pick Photo"
                            },
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // Preset Character Avatars Header
        Text(
            text = if (languageAr) "أو اختر من مجموعة الشخصيات الأسطورية:" else "Or pick from legendary characters:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )

        // Preset Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(240.dp)
        ) {
            items(PRESET_AVATARS) { character ->
                val isSelected = (selectedAvatarId == character.id && customUri.isNullOrEmpty())

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) character.colorStart.copy(0.25f) else DarkSurfaceGlass
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) character.colorStart else GlassBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isUploading) {
                            onAvatarSelected(character.id)
                            onCustomUriChanged(null) // clear custom photo to use character
                            uploadError = null
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            PlayerAvatarView(
                                avatarId = character.id,
                                size = 44.dp,
                                showBorder = isSelected,
                                borderColor = character.colorStart
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (languageAr) character.nameAr else character.nameEn,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}