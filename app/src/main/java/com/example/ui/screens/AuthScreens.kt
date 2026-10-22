package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.VeloraData
import com.example.model.Screen
import com.example.ui.components.VeloraBrandLogo
import com.example.ui.theme.*

@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    onLoginClick: () -> Unit,
    onLanguageClick: () -> Unit,
    currentLanguage: String,
    onAdminRouteClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VeloraPlumDark,
                        VeloraPlumMedium,
                        Color(0xFF0D0517)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Language Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onLanguageClick,
                    modifier = Modifier.testTag("splash_language_button")
                ) {
                    Text(
                        text = if (currentLanguage == "bn") "বাংলা 🇧🇩" else "English 🌐",
                        color = VeloraLavender,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Center Branding & Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(VeloraPlumSurface)
                        .border(2.dp, Brush.linearGradient(listOf(VeloraPink, VeloraViolet)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_velora_logo),
                        contentDescription = "Velora Logo",
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Velora",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (currentLanguage == "bn") "আসল মানুষ • আসল সম্পর্ক" else "Real People \u2764\uFE0F Real Connections",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = VeloraPinkLight
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VeloraPlumSurface.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraPlumMedium)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (currentLanguage == "bn") "আপনার মনের মানুষকে খুঁজে নিন" else "Find Your Person",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentLanguage == "bn")
                                "শুধু একটি ডেটিং অ্যাপ নয়, যেখানে আসল হৃদয়ের মেলবন্ধন ঘটে।"
                            else
                                "More than just a dating app.\nIt's a place where real hearts connect.",
                            fontSize = 13.sp,
                            color = VeloraLavender,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("get_started_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (currentLanguage == "bn") "শুরু করুন" else "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentLanguage == "bn") "ইতিমধ্যে একাউন্ট আছে? " else "Already have an account? ",
                        color = VeloraLavender,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (currentLanguage == "bn") "লগ ইন" else "Log In",
                        color = VeloraPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable(onClick = onLoginClick)
                            .testTag("login_link")
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onContinueGoogle: () -> Unit,
    onContinuePhone: () -> Unit,
    onContinueEmail: () -> Unit,
    onSelectLanguage: () -> Unit,
    currentLanguage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(VeloraPlumDark),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_velora_logo),
                    contentDescription = "Velora",
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Velora",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = VeloraTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentLanguage == "bn") "ভেলোরায় স্বাগতম" else "Welcome to Velora",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraPink
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentLanguage == "bn")
                    "আপনার সঙ্গী খুঁজুন, নতুন বন্ধু বানান এবং সুন্দর মুহূর্ত তৈরি করুন।"
                else
                    "Find your match, make new friends, explore the world — all in one place.",
                fontSize = 14.sp,
                color = VeloraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Social / Auth Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = onContinueGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_login_button"),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Google",
                    tint = VeloraPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (currentLanguage == "bn") "গুগল দিয়ে প্রবেশ করুন" else "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VeloraTextPrimary
                )
            }

            Button(
                onClick = onContinuePhone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("phone_login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VeloraRose),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (currentLanguage == "bn") "ফোন নম্বর দিয়ে প্রবেশ করুন" else "Continue with Phone",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onContinueEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("email_login_button"),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = VeloraViolet,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (currentLanguage == "bn") "ইমেইল দিয়ে প্রবেশ করুন" else "Continue with Email",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VeloraTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language & Disclaimer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onSelectLanguage,
                modifier = Modifier.testTag("select_language_link")
            ) {
                Text(
                    text = if (currentLanguage == "bn") "🌐 বাংলা | English" else "🌐 English | বাংলা",
                    fontSize = 13.sp,
                    color = VeloraTextSecondary
                )
            }

            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy. Must be 18+.",
                fontSize = 11.sp,
                color = VeloraTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun AgeVerificationScreen(
    onConfirmAge: (Boolean) -> Unit
) {
    var birthYear by remember { mutableStateOf("2002") }
    var calculatedAge by remember { mutableIntStateOf(24) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(VeloraPinkSoft)
                    .border(2.dp, VeloraPink, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Age Verification",
                    tint = VeloraPink,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Are you 18 or older?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This app is only for adults 18+. Velora strictly protects minor safety and enforces real age confirmation.",
                fontSize = 14.sp,
                color = VeloraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 18+ Badge
            Surface(
                shape = CircleShape,
                color = VeloraPinkSoft,
                border = androidx.compose.foundation.BorderStroke(2.dp, VeloraPink),
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "18+",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = VeloraPink
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Calculated Age: $calculatedAge years old",
                fontWeight = FontWeight.SemiBold,
                color = VeloraViolet,
                fontSize = 15.sp
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onConfirmAge(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_18_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = "Yes, I am 18+",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onConfirmAge(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("deny_18_button"),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder)
            ) {
                Text(
                    text = "No",
                    fontSize = 15.sp,
                    color = VeloraTextSecondary
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionScreen(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    var selectedLang by remember { mutableStateOf(currentLanguage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Select Language",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose your preferred language for the Velora experience",
                fontSize = 14.sp,
                color = VeloraTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            VeloraData.languages.forEach { lang ->
                val isSelected = selectedLang == lang.code
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) VeloraPinkSoft else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) VeloraPink else VeloraCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            selectedLang = lang.code
                            onLanguageSelected(lang.code)
                        }
                        .testTag("lang_${lang.code}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = lang.flag, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = lang.localName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VeloraTextPrimary
                                )
                                Text(
                                    text = lang.name,
                                    fontSize = 12.sp,
                                    color = VeloraTextSecondary
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedLang = lang.code
                                onLanguageSelected(lang.code)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = VeloraPink)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("language_continue_button"),
            colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun RegistrationScreen(
    onRegister: (name: String, email: String, phone: String, pass: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    errorMessage: String? = null,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+880 ") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VeloraTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Account",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security requirement banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VeloraPinkSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = VeloraPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Phone OTP verification & email address verification are strictly required for all new accounts.",
                        fontSize = 12.sp,
                        color = VeloraPink,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2C1015),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Full Name Input
            Text(
                text = "Full Name",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VeloraTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Alif Islam") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Name", tint = VeloraPink)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email Input
            Text(
                text = "Email Address",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VeloraTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("e.g. alif@example.com") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = VeloraPink)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Number Input
            Text(
                text = "Phone Number (SMS OTP Required)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VeloraTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("+880 1812-345678") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone", tint = VeloraPink)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Input
            Text(
                text = "Password (min 6 characters)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = VeloraTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = VeloraPink)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = VeloraTextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_input"),
                shape = RoundedCornerShape(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { onRegister(name, email, phone, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("register_button"),
                colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    text = "Continue to Phone Verification",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Already have an account? ",
                    color = VeloraTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Log In",
                    color = VeloraPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToLogin)
                        .testTag("login_link")
                )
            }
        }
    }
}

@Composable
fun PhoneOtpVerificationScreen(
    phoneNumber: String,
    simulatedOtp: String?,
    onVerifyOtp: (otp: String) -> Unit,
    onResendOtp: () -> Unit,
    errorMessage: String? = null,
    onBack: () -> Unit
) {
    var otpInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VeloraTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phone Verification",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(VeloraPinkSoft)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Phone OTP",
                    tint = VeloraPink,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enter 6-Digit OTP",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraTextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a 6-digit verification code to:\n$phoneNumber",
                fontSize = 14.sp,
                color = VeloraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Simulated SMS Delivery card for instant testing and emulator convenience
            if (!simulatedOtp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VeloraPlumDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = "SMS",
                                    tint = VeloraViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SMS Dispatch (Simulation)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VeloraViolet
                                )
                            }
                            TextButton(
                                onClick = { otpInput = simulatedOtp },
                                modifier = Modifier.testTag("autofill_otp_button")
                            ) {
                                Text("Fill OTP", fontSize = 12.sp, color = VeloraPink)
                            }
                        }
                        Text(
                            text = "Your Velora verification code is $simulatedOtp. Valid for 10 minutes.",
                            fontSize = 13.sp,
                            color = VeloraLavender,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2C1015),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OTP Input Field
            OutlinedTextField(
                value = otpInput,
                onValueChange = { if (it.length <= 6) otpInput = it },
                placeholder = { Text("Enter 6-digit OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onResendOtp,
                    modifier = Modifier.testTag("resend_otp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resend",
                        tint = VeloraPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resend Code",
                        color = VeloraPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Button(
            onClick = { onVerifyOtp(otpInput) },
            enabled = otpInput.trim().length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("verify_otp_button"),
            colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text(
                text = "Verify Phone & Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun EmailVerificationScreen(
    email: String,
    simulatedCode: String?,
    onVerifyEmail: (code: String) -> Unit,
    onResendEmail: () -> Unit,
    errorMessage: String? = null,
    onBack: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VeloraTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Email Verification",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(VeloraPinkSoft)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = "Email Verification",
                    tint = VeloraPink,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify Email Address",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraTextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a 6-digit confirmation code to:\n$email\n\nYou must verify your email before accessing Velora.",
                fontSize = 14.sp,
                color = VeloraTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Simulated Email Inbox card for testing convenience
            if (!simulatedCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VeloraPlumDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Inbox",
                                    tint = VeloraViolet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Velora Inbox (Simulation)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VeloraViolet
                                )
                            }
                            TextButton(
                                onClick = { codeInput = simulatedCode },
                                modifier = Modifier.testTag("autofill_email_button")
                            ) {
                                Text("Fill Code", fontSize = 12.sp, color = VeloraPink)
                            }
                        }
                        Text(
                            text = "Subject: Confirm your Velora email address\nCode: $simulatedCode",
                            fontSize = 13.sp,
                            color = VeloraLavender,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2C1015),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Code Input Field
            OutlinedTextField(
                value = codeInput,
                onValueChange = { if (it.length <= 6) codeInput = it },
                placeholder = { Text("Enter 6-digit code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_code_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onResendEmail,
                    modifier = Modifier.testTag("resend_email_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resend",
                        tint = VeloraPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resend Email Code",
                        color = VeloraPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Button(
            onClick = { onVerifyEmail(codeInput) },
            enabled = codeInput.trim().length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("verify_email_button"),
            colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text(
                text = "Verify Email & Complete Registration",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
