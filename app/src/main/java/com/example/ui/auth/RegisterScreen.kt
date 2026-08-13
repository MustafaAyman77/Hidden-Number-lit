package com.example.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun RegisterScreen(
    viewModel: MainViewModel,
    languageAr: Boolean = true
) {
    val authLoading by viewModel.authLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var displayNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

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
            // Top back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.handleBackPress() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }
            }

            // Title Header
            Text(
                text = if (languageAr) "إنشاء حساب جديد" else "Create New Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (languageAr) "ابدأ رحلتك الأسطورية في اللعبة 🎮" else "Start your epic journey in the game 🎮",
                fontSize = 15.sp,
                color = NeonEmerald,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.trim()
                            usernameError = null
                        },
                        label = { Text(if (languageAr) "اسم المستخدم (Username)" else "Username", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = NeonEmerald) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (usernameError != null) NeonMagenta else NeonEmerald,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (usernameError != null) {
                        Text(usernameError!!, color = NeonMagenta, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Display Name Field
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            displayNameError = null
                        },
                        label = { Text(if (languageAr) "الاسم الظاهر (Display Name)" else "Display Name", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NeonEmerald) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (displayNameError != null) NeonMagenta else NeonEmerald,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (displayNameError != null) {
                        Text(displayNameError!!, color = NeonMagenta, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it.trim()
                            emailError = null
                        },
                        label = { Text(if (languageAr) "البريد الإلكتروني" else "Email", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeonEmerald) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailError != null) NeonMagenta else NeonEmerald,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (emailError != null) {
                        Text(emailError!!, color = NeonMagenta, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text(if (languageAr) "كلمة المرور" else "Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonEmerald) },
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
                            focusedBorderColor = if (passwordError != null) NeonMagenta else NeonEmerald,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) {
                        Text(passwordError!!, color = NeonMagenta, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = null
                        },
                        label = { Text(if (languageAr) "تأكيد كلمة المرور" else "Confirm Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonEmerald) },
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (confirmPasswordError != null) NeonMagenta else NeonEmerald,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (confirmPasswordError != null) {
                        Text(confirmPasswordError!!, color = NeonMagenta, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }

                    // Backend Auth Error Message
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    if (authLoading) {
                        CircularProgressIndicator(color = NeonEmerald, modifier = Modifier.size(36.dp))
                    } else {
                        CyberButton(
                            text = if (languageAr) "إنشاء الحساب" else "Create Account",
                            onClick = {
                                // Validation logic
                                var hasError = false

                                // Username validation
                                val usernamePattern = Regex("^[a-zA-Z0-9_]{3,20}$")
                                if (username.isBlank()) {
                                    usernameError = if (languageAr) "اسم المستخدم مطلوب" else "Username is required"
                                    hasError = true
                                } else if (!usernamePattern.matches(username)) {
                                    usernameError = if (languageAr)
                                        "اسم المستخدم يجب أن يكون بين 3 و 20 حرفاً وبدون مسافات (حروف، أرقام، _)"
                                    else
                                        "Username must be 3-20 characters (alphanumeric and _ only)"
                                    hasError = true
                                }

                                // Display name validation
                                if (displayName.isBlank()) {
                                    displayNameError = if (languageAr) "الاسم الظاهر مطلوب" else "Display name is required"
                                    hasError = true
                                }

                                // Email validation
                                val emailPattern = Regex("^[A-Za-z0-9+_.-]+@(.+)\$")
                                if (email.isBlank()) {
                                    emailError = if (languageAr) "البريد الإلكتروني مطلوب" else "Email is required"
                                    hasError = true
                                } else if (!emailPattern.matches(email)) {
                                    emailError = if (languageAr) "صيغة البريد الإلكتروني غير صحيحة" else "Invalid email format"
                                    hasError = true
                                }

                                // Password validation
                                if (password.length < 8) {
                                    passwordError = if (languageAr) "كلمة المرور يجب أن تحتوي على 8 أحرف على الأقل" else "Password must be at least 8 characters"
                                    hasError = true
                                }

                                // Confirm Password validation
                                if (confirmPassword != password) {
                                    confirmPasswordError = if (languageAr) "كلمتا المرور غير متطابقتين" else "Passwords do not match"
                                    hasError = true
                                }

                                if (!hasError) {
                                    coroutineScope.launch {
                                        // Check username uniqueness
                                        val isTaken = viewModel.isUsernameTaken(username)
                                        if (isTaken) {
                                            usernameError = if (languageAr) "اسم المستخدم مستخدم بالفعل. اختر اسماً آخر." else "Username already taken"
                                        } else {
                                            viewModel.registerWithSupabase(
                                                email = email.trim(),
                                                password = password.trim(),
                                                username = username.trim(),
                                                displayName = displayName.trim()
                                            )
                                        }
                                    }
                                }
                            },
                            primaryColor = NeonEmerald,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Already have account
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (languageAr) "لديك حساب بالفعل؟ " else "Already have an account? ",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (languageAr) "تسجيل الدخول" else "Login",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                viewModel.clearAuthError()
                                viewModel.navigateTo(AppScreen.LOGIN)
                            }
                        )
                    }
                }
            }
        }
    }
}
