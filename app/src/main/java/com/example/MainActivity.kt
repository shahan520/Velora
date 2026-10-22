package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Screen
import com.example.ui.components.VeloraBottomBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VeloraPink
import com.example.ui.theme.VeloraPlumDark
import com.example.viewmodel.VeloraViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: VeloraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
                VeloraAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null) {
            val path = data.path
            val host = data.host
            if (path == "/admin" || host == "admin" || path?.startsWith("/admin") == true) {
                viewModel.handleRoute("/admin")
            }
        }
    }
}

@Composable
fun VeloraAppRoot(viewModel: VeloraViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle Hardware & System Back Gesture
    val isRootScreen = uiState.currentScreen in listOf(
        Screen.SPLASH,
        Screen.MAIN_DISCOVER
    )
    BackHandler(enabled = !isRootScreen) {
        viewModel.navigateBack()
    }

    // Auto-dismiss in-app toast
    LaunchedEffect(uiState.toastMessage) {
        if (uiState.toastMessage != null) {
            delay(2800)
            viewModel.clearToast()
        }
    }

    val showBottomBar = uiState.currentScreen in listOf(
        Screen.MAIN_DISCOVER,
        Screen.MAIN_MATCHES,
        Screen.MAIN_CHAT,
        Screen.MAIN_PROFILE
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                VeloraBottomBar(
                    currentScreen = uiState.currentScreen,
                    onTabSelected = { target -> viewModel.navigateTo(target) },
                    unreadMessages = uiState.matches.sumOf { it.unreadCount }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            when (uiState.currentScreen) {
                Screen.SPLASH -> {
                    SplashScreen(
                        onGetStarted = { viewModel.navigateTo(Screen.REGISTRATION) },
                        onLoginClick = { viewModel.navigateTo(Screen.WELCOME) },
                        onLanguageClick = { viewModel.navigateTo(Screen.LANGUAGE_SELECTION) },
                        currentLanguage = uiState.selectedLanguage
                    )
                }

                Screen.WELCOME -> {
                    WelcomeScreen(
                        onContinueGoogle = { viewModel.quickLogin() },
                        onContinuePhone = { viewModel.navigateTo(Screen.REGISTRATION) },
                        onContinueEmail = { viewModel.navigateTo(Screen.REGISTRATION) },
                        onSelectLanguage = { viewModel.navigateTo(Screen.LANGUAGE_SELECTION) },
                        currentLanguage = uiState.selectedLanguage
                    )
                }

                Screen.REGISTRATION -> {
                    RegistrationScreen(
                        onRegister = { name, email, phone, pass ->
                            viewModel.registerNewUser(name, email, phone, pass)
                        },
                        onNavigateToLogin = { viewModel.navigateTo(Screen.WELCOME) },
                        errorMessage = uiState.registrationError,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.PHONE_OTP_VERIFICATION -> {
                    PhoneOtpVerificationScreen(
                        phoneNumber = uiState.pendingAuthPhone.ifBlank { "+880 1812-345678" },
                        simulatedOtp = uiState.simulatedSmsOtp,
                        onVerifyOtp = { otp -> viewModel.verifyPhoneOtp(otp) },
                        onResendOtp = { viewModel.sendPhoneOtp() },
                        errorMessage = uiState.phoneOtpError,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.EMAIL_VERIFICATION -> {
                    EmailVerificationScreen(
                        email = uiState.pendingAuthEmail.ifBlank { "user@example.com" },
                        simulatedCode = uiState.simulatedEmailCode,
                        onVerifyEmail = { code -> viewModel.verifyEmailCode(code) },
                        onResendEmail = { viewModel.sendEmailVerification() },
                        errorMessage = uiState.emailVerificationError,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.AGE_VERIFICATION -> {
                    AgeVerificationScreen(
                        onConfirmAge = { is18Plus -> viewModel.verifyAge(is18Plus) }
                    )
                }

                Screen.LANGUAGE_SELECTION -> {
                    LanguageSelectionScreen(
                        currentLanguage = uiState.selectedLanguage,
                        onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                        onContinue = { viewModel.navigateBack() }
                    )
                }

                Screen.MAIN_DISCOVER -> {
                    DiscoverScreen(
                        profiles = uiState.profiles,
                        currentIndex = uiState.discoverIndex,
                        activeSubTab = uiState.activeSubTab,
                        onSubTabSelected = { tab -> viewModel.setSubTab(tab) },
                        onPass = { profile -> viewModel.passProfile(profile) },
                        onLike = { profile -> viewModel.likeProfile(profile) },
                        onSuperLike = { profile -> viewModel.superLikeProfile(profile) },
                        onUndoPass = { viewModel.undoPass() },
                        onCardClick = { profile -> viewModel.openProfileDetails(profile) },
                        onFilterClick = { viewModel.navigateTo(Screen.SEARCH_FILTERS) },
                        onNotificationsClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) },
                        showMatchDialog = uiState.showMatchDialog,
                        matchedUser = uiState.matchedUser,
                        onDismissMatch = { viewModel.dismissMatchDialog() },
                        onStartChatWithMatch = { user ->
                            val match = uiState.matches.firstOrNull { it.user.id == user.id }
                                ?: uiState.matches.first()
                            viewModel.openChatWith(match)
                        }
                    )
                }

                Screen.MAIN_MATCHES, Screen.MAIN_CHAT -> {
                    MatchesScreen(
                        matches = uiState.matches,
                        onMatchClick = { match -> viewModel.openChatWith(match) },
                        onFilterClick = { viewModel.navigateTo(Screen.SEARCH_FILTERS) },
                        onNotificationsClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) }
                    )
                }

                Screen.MAIN_PROFILE -> {
                    ProfileScreen(
                        user = uiState.currentUserProfile,
                        isPremium = uiState.isPremiumUser,
                        onEditProfile = { viewModel.navigateTo(Screen.EDIT_PROFILE) },
                        onPremiumClick = { viewModel.navigateTo(Screen.PREMIUM_STORE) },
                        onBoostClick = { viewModel.navigateTo(Screen.BOOST_STORE) },
                        onNotificationsClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) },
                        onSafetyClick = {
                            val target = uiState.profiles.firstOrNull()
                            if (target != null) viewModel.openBlockReport(target)
                        },
                        onLogout = { viewModel.navigateTo(Screen.SPLASH) }
                    )
                }

                Screen.EDIT_PROFILE -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    EditProfileScreen(
                        user = uiState.currentUserProfile,
                        isUploading = uiState.isUploadingPhoto,
                        uploadProgress = uiState.photoUploadProgress,
                        uploadStatus = uiState.photoUploadStatus,
                        lastUploadedUrl = uiState.lastUploadedCdnUrl,
                        onUploadPhotoUri = { uri -> viewModel.uploadPhotoFromUri(context, uri) },
                        onUploadSamplePhoto = { sampleUrl -> viewModel.uploadSamplePhoto(sampleUrl) },
                        onSetPrimaryPhoto = { url -> viewModel.setPrimaryPhoto(url) },
                        onRemovePhoto = { url -> viewModel.removePhoto(url) },
                        onSaveProfile = { aboutMe, profession, city ->
                            viewModel.updateProfileDetails(aboutMe, profession, city)
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.CHAT_ROOM -> {
                    val activeMatch = uiState.activeChatMatch ?: uiState.matches.first()
                    val messages = uiState.chatMessages[activeMatch.id].orEmpty()
                    ChatRoomScreen(
                        match = activeMatch,
                        messages = messages,
                        onSendMessage = { text -> viewModel.sendMessage(text) },
                        onStartVideoCall = { viewModel.startVideoCall() },
                        onBack = { viewModel.navigateBack() },
                        onReportBlock = { viewModel.openBlockReport(activeMatch.user) }
                    )
                }

                Screen.VIDEO_CALL -> {
                    val activeMatch = uiState.activeChatMatch ?: uiState.matches.first()
                    VideoCallScreen(
                        match = activeMatch,
                        isMuted = uiState.isCallMuted,
                        onToggleMute = { viewModel.toggleCallMute() },
                        onEndCall = { viewModel.endVideoCall() }
                    )
                }

                Screen.PROFILE_DETAILS -> {
                    val profile = uiState.selectedProfileDetails ?: uiState.profiles.first()
                    ProfileDetailsScreen(
                        profile = profile,
                        onBack = { viewModel.navigateBack() },
                        onLike = {
                            viewModel.likeProfile(profile)
                            viewModel.navigateBack()
                        },
                        onPass = {
                            viewModel.passProfile(profile)
                            viewModel.navigateBack()
                        },
                        onSuperLike = {
                            viewModel.superLikeProfile(profile)
                            viewModel.navigateBack()
                        },
                        onReportBlock = { viewModel.openBlockReport(profile) }
                    )
                }

                Screen.SEARCH_FILTERS -> {
                    SearchFiltersScreen(
                        currentFilters = uiState.filters,
                        onApply = { newFilters -> viewModel.applyFilters(newFilters) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.PREMIUM_STORE -> {
                    PremiumSubscriptionScreen(
                        plans = uiState.subscriptionPlans,
                        isAlreadyPremium = uiState.isPremiumUser,
                        onSubscribe = { plan -> viewModel.purchaseSubscription(plan) },
                        onBack = { viewModel.navigateBack() },
                        mfsConfig = uiState.mfsConfig,
                        onSubmitMfsPayment = { method, phone, trxId, plan ->
                            viewModel.submitMfsPayment(method, phone, trxId, plan)
                        },
                        isPaymentProcessing = uiState.isPaymentProcessing,
                        paymentSuccessDialogVisible = uiState.paymentSuccessDialogVisible,
                        paymentSuccessMessage = uiState.paymentSuccessMessage,
                        onDismissPaymentSuccess = { viewModel.dismissPaymentSuccessDialog() }
                    )
                }


                Screen.BOOST_STORE -> {
                    BoostStoreScreen(
                        boostMinutes = uiState.boostMinutesRemaining,
                        superLikes = uiState.superLikesRemaining,
                        onActivateBoost = { viewModel.activateBoost() },
                        onBuySuperLikes = { viewModel.purchaseSuperLikes() },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        notifications = uiState.notifications,
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.BLOCK_REPORT -> {
                    val targetUser = uiState.targetBlockUser ?: uiState.profiles.first()
                    BlockReportScreen(
                        user = targetUser,
                        onBlockUser = { u -> viewModel.blockUser(u) },
                        onReportUser = { u, r -> viewModel.reportUser(u, r) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                Screen.ADMIN_LOGIN -> {
                    AdminLoginScreen(
                        onAuthenticate = { email, password -> viewModel.authenticateAndLoginAdmin(email, password) },
                        onBackToApp = { viewModel.navigateTo(Screen.MAIN_DISCOVER) },
                        errorMessage = uiState.adminLoginError
                    )
                }

                Screen.ADMIN_PANEL -> {
                    if (!uiState.isAdminAuthenticated || uiState.adminSessionToken.isNullOrBlank()) {
                        LaunchedEffect(Unit) {
                            viewModel.logoutAdmin()
                            viewModel.navigateTo(Screen.ADMIN_LOGIN)
                        }
                    } else {
                        AdminPanelScreen(
                            currentSection = uiState.currentAdminSection,
                            onSectionChange = { sec -> viewModel.setAdminSection(sec) },
                            profiles = uiState.profiles,
                            reports = uiState.reports,
                            onToggleSuspendUser = { userId -> viewModel.toggleUserSuspension(userId) },
                            onResolveReport = { repId, status -> viewModel.resolveReport(repId, status) },
                            onExitAdmin = { viewModel.logoutAdmin() },
                            mfsConfig = uiState.mfsConfig,
                            onUpdateMfsConfig = { cfg -> viewModel.adminUpdateMfsConfig(cfg) },
                            subscriptionPlans = uiState.subscriptionPlans,
                            onUpdatePlanPrice = { planId, price -> viewModel.adminUpdatePlanPrice(planId, price) },
                            mfsTransactions = uiState.mfsTransactions
                        )

                    }
                }

                else -> {
                    SplashScreen(
                        onGetStarted = { viewModel.navigateTo(Screen.WELCOME) },
                        onLoginClick = { viewModel.navigateTo(Screen.WELCOME) },
                        onLanguageClick = { viewModel.navigateTo(Screen.LANGUAGE_SELECTION) },
                        currentLanguage = uiState.selectedLanguage
                    )
                }
            }

            // In-app Toast Banner
            AnimatedVisibility(
                visible = uiState.toastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = VeloraPlumDark.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraPink),
                    modifier = Modifier.testTag("in_app_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.toastMessage.orEmpty(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
