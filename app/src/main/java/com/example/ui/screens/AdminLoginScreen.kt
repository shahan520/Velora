package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VeloraBrandLogo
import com.example.ui.theme.*

@Composable
fun AdminLoginScreen(
    onAuthenticate: (email: String, password: String) -> Unit,
    onBackToApp: () -> Unit,
    errorMessage: String?,
    isAuthenticating: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayError = errorMessage ?: localError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VeloraPlumDark,
                        Color(0xFF150827),
                        Color(0xFF0C0318)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Top Nav & Route Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToApp,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VeloraPlumSurface)
                        .testTag("admin_login_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Dating App",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VeloraPlumSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraViolet)
                ) {
                    Text(
                        text = "ROUTE: /admin",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = VeloraPinkLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Branding & Logo
            VeloraBrandLogo(size = 72.dp, isDark = true)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Velora Admin Portal",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Staff & Moderator Authentication",
                fontSize = 13.sp,
                color = VeloraLavender
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Strict Security Notice
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VeloraPlumSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraPlumMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = VeloraGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Backend Authorization Required",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VeloraGold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "This dashboard is completely segregated from the user dating interface. Only verified accounts with role SUPER_ADMIN or MODERATOR may access administrative APIs.",
                            fontSize = 11.sp,
                            color = VeloraLavender.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Input Fields
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("Staff Email / Admin ID", color = VeloraLavender) },
                placeholder = { Text("admin@velora.app", color = VeloraLavender.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = VeloraPink)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_email_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VeloraPink,
                    unfocusedBorderColor = VeloraPlumMedium,
                    focusedContainerColor = VeloraPlumSurface,
                    unfocusedContainerColor = VeloraPlumSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Admin Password", color = VeloraLavender) },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = VeloraPink)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            tint = VeloraLavender
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_password_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VeloraPink,
                    unfocusedBorderColor = VeloraPlumMedium,
                    focusedContainerColor = VeloraPlumSurface,
                    unfocusedContainerColor = VeloraPlumSurface
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (email.isBlank() || password.isBlank()) {
                            localError = "Please enter both administrator email and password."
                        } else {
                            localError = null
                            onAuthenticate(email.trim(), password)
                        }
                    }
                )
            )

            // Error display
            if (!displayError.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VeloraErrorRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraErrorRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = VeloraErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayError,
                            color = Color(0xFFFFB4AB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Fill Test Buttons
            Text(
                text = "QUICK TEST CREDENTIALS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraLavender.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        email = "admin@velora.app"
                        password = "admin_secure_pass_2026"
                        localError = null
                    },
                    label = { Text("Staff Admin", fontSize = 11.sp, color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = VeloraOnlineGreen, modifier = Modifier.size(14.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = VeloraPlumSurface),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fill_admin_creds")
                )

                AssistChip(
                    onClick = {
                        email = "user@example.com"
                        password = "userpass123"
                        localError = null
                    },
                    label = { Text("Unauthorized User", fontSize = 11.sp, color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = VeloraErrorRed, modifier = Modifier.size(14.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = VeloraPlumSurface),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fill_unauth_creds")
                )
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        localError = "Please enter both administrator email and password."
                    } else {
                        localError = null
                        onAuthenticate(email.trim(), password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("admin_login_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                shape = RoundedCornerShape(26.dp),
                enabled = !isAuthenticating
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify & Access /admin",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onBackToApp,
                modifier = Modifier.testTag("admin_return_app_link")
            ) {
                Text(
                    text = "Return to Velora Dating App",
                    fontSize = 13.sp,
                    color = VeloraLavender,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
