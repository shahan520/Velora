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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

                if (onAdminRouteClick != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Staff Portal (/admin)",
                        color = VeloraLavender.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .clickable(onClick = onAdminRouteClick)
                            .testTag("splash_admin_portal_link")
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
