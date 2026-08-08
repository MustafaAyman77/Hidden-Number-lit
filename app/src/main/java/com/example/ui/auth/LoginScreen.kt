package com.example.ui.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    languageAr: Boolean = true
) {
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var isResetLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header / Game Title
            Text(
                text = "الرقم السري المخفي",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "تسجيل الدخول إلى حسابك 🎮",
                fontSize = 16.sp,
                color = NeonCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(if (languageAr) "البريد الإلكتروني" else "Email", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonCyan) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (languageAr) "كلمة المرور" else "Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Forgot Password link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (languageAr) "نسيت كلمة المرور؟" else "Forgot password?",
                            fontSize = 13.sp,
                            color = NeonCyan,
                            modifier = Modifier.clickable {
                                resetEmail = email
                                resetSuccessMessage = null
                                resetErrorMessage = null
                                showForgotPasswordDialog = true
                            }
                        )
                    }

                    // Display Auth Error if any
                    if (!authError.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = authError!!,
                            color = NeonMagenta,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    if (authLoading) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(36.dp))
                    } else {
                        CyberButton(
                            text = if (languageAr) "تسجيل الدخول" else "Login",
                            onClick = {
                                viewModel.loginWithSupabase(email.trim(), password.trim())
                            },
                            primaryColor = NeonCyan,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Go to Register Screen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (languageAr) "ليس لديك حساب؟ " else "Don't have an account? ",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (languageAr) "أنشئ حساباً جديداً" else "Register now",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                viewModel.clearAuthError()
                                viewModel.navigateTo(AppScreen.REGISTER)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.DarkGray)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Continue as Guest Button
                    TextButton(
                        onClick = {
                            viewModel.continueAsGuest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (languageAr) "الدخول بدون حساب (ضيف)" else "Continue as Guest",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (languageAr) "استعادة كلمة المرور" else "Reset Password",
                            color = Color.White
                        )
                    }
                },
                text = {
                    Column {
                        Text(
                            text = if (languageAr)
                                "أدخل بريدك الإلكتروني لإرسال رابط إعادة تعيين كلمة المرور:"
                            else
                                "Enter your email address to receive a password reset link:",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text(if (languageAr) "البريد الإلكتروني" else "Email", color = Color.Gray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!resetSuccessMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(resetSuccessMessage!!, color = NeonEmerald, fontSize = 13.sp)
                        }

                        if (!resetErrorMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(resetErrorMessage!!, color = NeonMagenta, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    if (isResetLoading) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(
                            onClick = {
                                if (resetEmail.trim().isEmpty()) {
                                    resetErrorMessage = if (languageAr) "يرجى إدخال البريد الإلكتروني" else "Please enter email"
                                    return@TextButton
                                }
                                isResetLoading = true
                                resetErrorMessage = null
                                resetSuccessMessage = null
                                coroutineScope.launch {
                                    val result = viewModel.sendPasswordReset(resetEmail.trim())
                                    isResetLoading = false
                                    if (result is com.example.data.supabase.AuthResult.Success) {
                                        resetSuccessMessage = if (languageAr)
                                            "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني!"
                                        else
                                            "Reset link sent to your email!"
                                    } else if (result is com.example.data.supabase.AuthResult.Error) {
                                        resetErrorMessage = result.messageAr
                                    }
                                }
                            }
                        ) {
                            Text(if (languageAr) "إرسال" else "Send", color = NeonCyan)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text(if (languageAr) "إلغاء" else "Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E2A)
            )
        }
    }
}
