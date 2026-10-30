package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallSignalingRepository
import com.example.data.FirebaseAuthRepository
import com.example.data.FirestoreProfileRepository
import com.example.data.SessionManager
import com.example.data.VeloraData
import com.example.media.CallMediaManager
import com.example.model.*
import com.example.server.VeloraBackendServer
import com.example.service.CloudinaryUploadService
import com.example.service.UploadResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VeloraUiState(
    val currentScreen: Screen = Screen.SPLASH,
    val previousScreen: Screen = Screen.SPLASH,
    val selectedLanguage: String = "en", // "en" or "bn"
    val isAgeVerified: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isAdminMode: Boolean = false,
    val isAdminAuthenticated: Boolean = false,
    val adminSessionToken: String? = null,
    val adminLoginError: String? = null,
    val currentAdminSection: AdminSection = AdminSection.DASHBOARD,
    val currentAdminEmail: String? = null,
    val currentAdminDisplayName: String? = null,
    val authorizedAdminsList: List<com.example.server.ServerUser> = emptyList(),

    // User Authentication & Verification States
    val pendingAuthName: String = "",
    val pendingAuthEmail: String = "",
    val pendingAuthPhone: String = "",
    val isPhoneVerified: Boolean = false,
    val isEmailVerified: Boolean = false,
    val registrationError: String? = null,
    val phoneOtpError: String? = null,
    val emailVerificationError: String? = null,
    val simulatedSmsOtp: String? = null,
    val simulatedEmailCode: String? = null,
    val authSuccessMessage: String? = null,

    // Current User Profile & Cloudinary Image Upload
    val currentUserProfile: UserProfile = VeloraData.currentUser,
    val isUploadingPhoto: Boolean = false,
    val photoUploadProgress: Float = 0f,
    val photoUploadStatus: String? = null,
    val lastUploadedCdnUrl: String? = null,

    // Discovery State
    val discoverIndex: Int = 0,
    val profiles: List<UserProfile> = VeloraData.discoverProfiles,
    val selectedProfileDetails: UserProfile? = null,
    val lastPassedProfile: UserProfile? = null,
    val showMatchDialog: Boolean = false,
    val matchedUser: UserProfile? = null,
    val filters: DiscoveryFilters = DiscoveryFilters(),
    val activeSubTab: String = "Discover", // "Discover", "Nearby", "Online"

    // Matches & Chat
    val matches: List<MatchItem> = VeloraData.initialMatches,
    val activeChatMatch: MatchItem? = null,
    val chatMessages: Map<String, List<ChatMessage>> = mapOf(
        "match_ayesha" to VeloraData.ayeshaChatHistory
    ),
    val isCallingActive: Boolean = false,
    val isCallMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isFrontCamera: Boolean = true,
    val activeCallSession: CallSession? = null,
    val incomingCallSession: CallSession? = null,
    val callDurationSeconds: Int = 0,

    // Notifications & Safety
    val notifications: List<NotificationItem> = VeloraData.initialNotifications,
    val reports: List<UserReport> = VeloraData.initialReports,
    val blockedUserIds: Set<String> = emptySet(),
    val showBlockReportDialog: Boolean = false,
    val targetBlockUser: UserProfile? = null,

    // Premium & Boosts
    val isPremiumUser: Boolean = false,
    val activePlan: SubscriptionPlan? = null,
    val superLikesRemaining: Int = 3,
    val boostMinutesRemaining: Int = 0,
    val toastMessage: String? = null,

    // Bangladeshi MFS Payments & Pricing State
    val subscriptionPlans: List<SubscriptionPlan> = VeloraData.subscriptionPlans,
    val mfsConfig: com.example.server.MfsMerchantConfig = com.example.server.MfsMerchantConfig(),
    val mfsTransactions: List<com.example.server.MfsPaymentTransaction> = emptyList(),
    val isCheckoutSheetOpen: Boolean = false,
    val selectedCheckoutPlan: SubscriptionPlan? = null,
    val selectedPaymentMethod: String = "BKASH", // "BKASH", "NAGAD", "CARD"
    val mfsSenderPhone: String = "01712-345678",
    val mfsTrxId: String = "",
    val isPaymentProcessing: Boolean = false,
    val paymentSuccessDialogVisible: Boolean = false,
    val paymentSuccessMessage: String? = null
)


class VeloraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VeloraUiState())
    val uiState: StateFlow<VeloraUiState> = _uiState.asStateFlow()

    private var mediaManager: CallMediaManager? = null
    private var callTimerJob: Job? = null
    private var activeCallObserverJob: Job? = null
    private var incomingCallObserverJob: Job? = null
    private var initiateCallJob: Job? = null

    init {
        loadMfsConfigAndPlans()
        startObservingIncomingCalls(_uiState.value.currentUserProfile.id)
    }

    private var appContext: Context? = null

    fun bindMediaManager(context: Context) {
        appContext = context.applicationContext
        SessionManager.initialize(context)
        if (mediaManager == null) {
            mediaManager = CallMediaManager(context.applicationContext)
        }
    }

    /**
     * Restore user session from persistent storage (SharedPreferences) or Firebase Auth.
     * Prevents auto-logout when the app is minimized, backgrounded, or restarted.
     */
    fun restoreSession(context: Context) {
        appContext = context.applicationContext
        SessionManager.initialize(context)
        val isPersistedLoggedIn = SessionManager.isLoggedIn(context)
        val firebaseAuthUser = try { FirebaseAuthRepository.getAuth()?.currentUser } catch (_: Throwable) { null }

        if (isPersistedLoggedIn || firebaseAuthUser != null) {
            val restoredProfile = SessionManager.restoreUserSession(context) ?: VeloraData.currentUser.let { base ->
                if (firebaseAuthUser != null) {
                    base.copy(
                        id = firebaseAuthUser.uid,
                        name = firebaseAuthUser.displayName ?: base.name,
                        email = firebaseAuthUser.email ?: base.email
                    )
                } else base
            }

            VeloraData.currentUser = restoredProfile
            val lastScreen = SessionManager.getLastScreen(context)
            val targetScreen = if (lastScreen in listOf(Screen.SPLASH, Screen.WELCOME, Screen.REGISTRATION, Screen.PHONE_OTP_VERIFICATION, Screen.EMAIL_VERIFICATION)) {
                Screen.MAIN_DISCOVER
            } else {
                lastScreen
            }

            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    isPhoneVerified = true,
                    isEmailVerified = true,
                    isAgeVerified = true,
                    currentUserProfile = restoredProfile,
                    currentScreen = targetScreen
                )
            }
        }
    }

    fun getMediaManager(): CallMediaManager? = mediaManager

    fun startObservingIncomingCalls(userId: String) {
        incomingCallObserverJob?.cancel()
        incomingCallObserverJob = viewModelScope.launch {
            CallSignalingRepository.observeIncomingCalls(userId).collect { incoming ->
                if (incoming != null && (incoming.status == CallStatus.RINGING || incoming.status == CallStatus.OFFERING)) {
                    if (!_uiState.value.isCallingActive) {
                        _uiState.update { it.copy(incomingCallSession = incoming) }
                        mediaManager?.startRinging()
                    }
                } else {
                    if (_uiState.value.incomingCallSession != null) {
                        _uiState.update { it.copy(incomingCallSession = null) }
                        mediaManager?.stopRinging()
                    }
                }
            }
        }
    }

    fun loadMfsConfigAndPlans() {
        val config = VeloraBackendServer.getMfsConfig().data ?: com.example.server.MfsMerchantConfig()
        val plans = VeloraBackendServer.getSubscriptionPlans().data ?: VeloraData.subscriptionPlans
        _uiState.update {
            it.copy(
                mfsConfig = config,
                subscriptionPlans = plans
            )
        }
    }

    fun navigateTo(screen: Screen) {
        if (screen == Screen.ADMIN_PANEL) {
            val token = _uiState.value.adminSessionToken
            if (!_uiState.value.isAdminAuthenticated || token.isNullOrBlank()) {
                // Backend/Route Guard: Unauthorized access directly to /admin is intercepted and routed to /admin/login
                _uiState.update {
                    it.copy(
                        previousScreen = it.currentScreen,
                        currentScreen = Screen.ADMIN_LOGIN,
                        adminLoginError = "401 Unauthorized: Administrator session with verified custom claims required."
                    )
                }
                return
            }

            // Cryptographic server-side custom claims verification
            val verifyResponse = VeloraBackendServer.getAdminDashboard("Bearer $token")
            if (!verifyResponse.isSuccessful) {
                _uiState.update {
                    it.copy(
                        isAdminAuthenticated = false,
                        adminSessionToken = null,
                        previousScreen = it.currentScreen,
                        currentScreen = Screen.ADMIN_LOGIN,
                        adminLoginError = "403 Forbidden: Account lacks verified administrator custom claims."
                    )
                }
                return
            }
        }
        _uiState.update {
            it.copy(
                previousScreen = it.currentScreen,
                currentScreen = screen
            )
        }
        appContext?.let { SessionManager.updateActiveScreen(it, screen) }
    }

    fun handleRoute(route: String) {
        if (route == "/admin" || route.startsWith("/admin")) {
            val token = _uiState.value.adminSessionToken
            if (_uiState.value.isAdminAuthenticated && !token.isNullOrBlank()) {
                // Server-verified custom claim check before rendering /admin
                val verifyResponse = VeloraBackendServer.getAdminDashboard("Bearer $token")
                if (verifyResponse.isSuccessful) {
                    _uiState.update { it.copy(currentScreen = Screen.ADMIN_PANEL) }
                    return
                }
            }
            // Deny by default: Redirect away to /admin/login
            _uiState.update {
                it.copy(
                    isAdminAuthenticated = false,
                    adminSessionToken = null,
                    currentScreen = Screen.ADMIN_LOGIN,
                    adminLoginError = "401 Unauthorized: Valid administrator session required to access /admin."
                )
            }
        }
    }

    fun authenticateAndLoginAdmin(email: String, pass: String) {
        viewModelScope.launch {
            // Step 1: Call server authentication endpoint
            val authResponse = VeloraBackendServer.login(email, pass)
            if (!authResponse.isSuccessful || authResponse.data == null) {
                _uiState.update {
                    it.copy(
                        isAdminAuthenticated = false,
                        adminSessionToken = null,
                        adminLoginError = "HTTP ${authResponse.statusCode} ${authResponse.error ?: "Unauthorized"}: ${authResponse.message}"
                    )
                }
                return@launch
            }

            val token = authResponse.data

            // Step 2: Test protected backend administrator endpoint with Authorization header
            val authHeader = "Bearer $token"
            val dashboardResponse = VeloraBackendServer.getAdminDashboard(authHeader)

            if (!dashboardResponse.isSuccessful) {
                // Deny access by default when server verification fails (HTTP 403 or 401)
                _uiState.update {
                    it.copy(
                        isAdminAuthenticated = false,
                        adminSessionToken = null,
                        adminLoginError = "HTTP ${dashboardResponse.statusCode} ${dashboardResponse.error ?: "Forbidden"}: ${dashboardResponse.message}"
                    )
                }
                return@launch
            }

            // Step 3: Server verified! Load server data & authorized admins
            val usersResponse = VeloraBackendServer.getAdminUsers(authHeader)
            val reportsResponse = VeloraBackendServer.getAdminReports(authHeader)
            val adminsResponse = VeloraBackendServer.getAdminAccounts(authHeader)
            val mfsTxnsResponse = VeloraBackendServer.getMfsTransactions(authHeader)
            val mfsConfigResponse = VeloraBackendServer.getMfsConfig()
            val plansResponse = VeloraBackendServer.getSubscriptionPlans()

            val loggedInAdmin = adminsResponse.data?.firstOrNull { it.email.equals(email, ignoreCase = true) }

            _uiState.update {
                it.copy(
                    isAdminAuthenticated = true,
                    adminSessionToken = token,
                    currentAdminEmail = email,
                    currentAdminDisplayName = loggedInAdmin?.displayName ?: "Administrator",
                    authorizedAdminsList = adminsResponse.data.orEmpty(),
                    mfsTransactions = mfsTxnsResponse.data.orEmpty(),
                    mfsConfig = mfsConfigResponse.data ?: it.mfsConfig,
                    subscriptionPlans = plansResponse.data ?: it.subscriptionPlans,
                    adminLoginError = null,
                    profiles = usersResponse.data ?: it.profiles,
                    reports = reportsResponse.data ?: it.reports,
                    currentScreen = Screen.ADMIN_PANEL
                )
            }

            showToast("Server Verified Admin Access (${loggedInAdmin?.displayName ?: email}). Welcome to /admin.")
        }
    }

    fun logoutAdmin() {
        _uiState.update {
            it.copy(
                isAdminAuthenticated = false,
                adminSessionToken = null,
                currentAdminEmail = null,
                currentAdminDisplayName = null,
                currentScreen = Screen.MAIN_DISCOVER
            )
        }
        showToast("Logged out of /admin session.")
    }

    fun navigateBack() {
        _uiState.update {
            val target = when (it.currentScreen) {
                Screen.PROFILE_DETAILS -> Screen.MAIN_DISCOVER
                Screen.SEARCH_FILTERS -> Screen.MAIN_DISCOVER
                Screen.CHAT_ROOM -> Screen.MAIN_CHAT
                Screen.VIDEO_CALL -> Screen.CHAT_ROOM
                Screen.PREMIUM_STORE -> Screen.MAIN_PROFILE
                Screen.BOOST_STORE -> Screen.MAIN_PROFILE
                Screen.NOTIFICATIONS -> Screen.MAIN_PROFILE
                Screen.EDIT_PROFILE -> Screen.MAIN_PROFILE
                Screen.BLOCK_REPORT -> Screen.MAIN_DISCOVER
                Screen.ADMIN_LOGIN -> Screen.MAIN_DISCOVER
                Screen.ADMIN_PANEL -> Screen.MAIN_DISCOVER
                Screen.LANGUAGE_SELECTION -> Screen.SPLASH
                Screen.REGISTRATION -> Screen.WELCOME
                Screen.PHONE_OTP_VERIFICATION -> Screen.REGISTRATION
                Screen.EMAIL_VERIFICATION -> Screen.PHONE_OTP_VERIFICATION
                Screen.AGE_VERIFICATION -> Screen.EMAIL_VERIFICATION
                Screen.WELCOME -> Screen.SPLASH
                else -> it.previousScreen
            }
            it.copy(currentScreen = target)
        }
    }

    fun setLanguage(langCode: String) {
        _uiState.update { it.copy(selectedLanguage = langCode) }
    }

    /**
     * Registers a new user. Enforces phone OTP verification and email verification.
     */
    fun registerNewUser(name: String, email: String, phone: String, pass: String) {
        viewModelScope.launch {
            val response = VeloraBackendServer.registerNewUser(name, email, phone, pass)
            if (!response.isSuccessful || response.data == null) {
                _uiState.update {
                    it.copy(
                        registrationError = response.message,
                        authSuccessMessage = null
                    )
                }
                return@launch
            }

            // Immediately create the account in Firebase Authentication & Cloud Firestore
            val syncResult = FirebaseAuthRepository.registerAndSyncUser(name, email, phone, pass)
            if (syncResult.isFailure) {
                val err = syncResult.exceptionOrNull()?.message.orEmpty()
                if (err.contains("already in use", ignoreCase = true) || err.contains("already exists", ignoreCase = true)) {
                    _uiState.update {
                        it.copy(
                            registrationError = "The email address is already in use by another account. Please log in.",
                            authSuccessMessage = null
                        )
                    }
                    showToast("This email is already in use. Please log in.")
                    return@launch
                }
            }

            val user = response.data
            val pendingPhoneOtp = VeloraBackendServer.getPendingPhoneOtp(user.phone)
            val pendingEmailCode = VeloraBackendServer.getPendingEmailCode(user.email)

            _uiState.update {
                it.copy(
                    pendingAuthName = user.displayName,
                    pendingAuthEmail = user.email,
                    pendingAuthPhone = user.phone,
                    isPhoneVerified = false,
                    isEmailVerified = false,
                    simulatedSmsOtp = pendingPhoneOtp,
                    simulatedEmailCode = pendingEmailCode,
                    registrationError = null,
                    phoneOtpError = null,
                    emailVerificationError = null,
                    authSuccessMessage = "Account created! Step 1 of 2: Verify Phone Number via OTP.",
                    currentScreen = Screen.PHONE_OTP_VERIFICATION
                )
            }
            showToast("Account created in Firebase! Please verify your phone number.")
        }
    }

    /**
     * Request/Resend phone OTP code.
     */
    fun sendPhoneOtp(targetPhone: String? = null) {
        viewModelScope.launch {
            val phone = targetPhone ?: _uiState.value.pendingAuthPhone
            if (phone.isBlank()) {
                _uiState.update { it.copy(phoneOtpError = "Phone number is missing.") }
                return@launch
            }

            val response = VeloraBackendServer.sendPhoneOtp(phone)
            if (response.isSuccessful) {
                _uiState.update {
                    it.copy(
                        simulatedSmsOtp = response.data,
                        phoneOtpError = null,
                        authSuccessMessage = "New OTP sent to $phone"
                    )
                }
                showToast("New OTP sent to $phone")
            } else {
                _uiState.update { it.copy(phoneOtpError = response.message) }
            }
        }
    }

    /**
     * Submit and verify phone OTP.
     */
    fun verifyPhoneOtp(otpCode: String) {
        viewModelScope.launch {
            val phone = _uiState.value.pendingAuthPhone
            val response = VeloraBackendServer.verifyPhoneOtp(phone, otpCode)
            if (response.isSuccessful) {
                _uiState.update {
                    it.copy(
                        isPhoneVerified = true,
                        phoneOtpError = null,
                        authSuccessMessage = "Phone verified! Step 2 of 2: Verify Email Address.",
                        currentScreen = Screen.EMAIL_VERIFICATION
                    )
                }
                showToast("Phone verified! Please verify your email address.")
            } else {
                _uiState.update {
                    it.copy(phoneOtpError = response.message)
                }
            }
        }
    }

    /**
     * Request/Resend email verification code.
     */
    fun sendEmailVerification(targetEmail: String? = null) {
        viewModelScope.launch {
            val email = targetEmail ?: _uiState.value.pendingAuthEmail
            if (email.isBlank()) {
                _uiState.update { it.copy(emailVerificationError = "Email address is missing.") }
                return@launch
            }

            val response = VeloraBackendServer.sendEmailVerification(email)
            if (response.isSuccessful) {
                _uiState.update {
                    it.copy(
                        simulatedEmailCode = response.data,
                        emailVerificationError = null,
                        authSuccessMessage = "New verification code sent to $email"
                    )
                }
                showToast("Verification code sent to $email")
            } else {
                _uiState.update { it.copy(emailVerificationError = response.message) }
            }
        }
    }

    /**
     * Submit and verify email verification code.
     */
    fun verifyEmailCode(code: String) {
        viewModelScope.launch {
            val email = _uiState.value.pendingAuthEmail
            val response = VeloraBackendServer.verifyEmailCode(email, code)
            if (response.isSuccessful) {
                _uiState.update {
                    it.copy(
                        isEmailVerified = true,
                        emailVerificationError = null,
                        authSuccessMessage = "Email verified! Please complete age verification.",
                        currentScreen = Screen.AGE_VERIFICATION
                    )
                }
                showToast("Email verified successfully!")
            } else {
                _uiState.update {
                    it.copy(emailVerificationError = response.message)
                }
            }
        }
    }

    /**
     * Authenticate standard user and verify phone/email before granting access.
     */
    fun loginUser(email: String, pass: String) {
        viewModelScope.launch {
            val response = VeloraBackendServer.login(email, pass)
            if (response.isSuccessful) {
                // Ensure the account exists and is synchronized with Firebase Auth & Cloud Firestore
                FirebaseAuthRepository.loginAndSyncUser(email, pass)

                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        isPhoneVerified = true,
                        isEmailVerified = true,
                        isAgeVerified = true,
                        pendingAuthEmail = email,
                        currentScreen = Screen.MAIN_DISCOVER
                    )
                }
                appContext?.let { SessionManager.saveUserSession(it, _uiState.value.currentUserProfile) }
                showToast("Welcome back! Connected to Firebase.")
            } else if (response.error == "PHONE_NOT_VERIFIED") {
                _uiState.update {
                    it.copy(
                        pendingAuthEmail = email,
                        isPhoneVerified = false,
                        phoneOtpError = response.message,
                        currentScreen = Screen.PHONE_OTP_VERIFICATION
                    )
                }
                showToast("Phone verification required before accessing the app.")
            } else if (response.error == "EMAIL_NOT_VERIFIED") {
                _uiState.update {
                    it.copy(
                        pendingAuthEmail = email,
                        isEmailVerified = false,
                        emailVerificationError = response.message,
                        currentScreen = Screen.EMAIL_VERIFICATION
                    )
                }
                showToast("Email verification required before accessing the app.")
            } else {
                showToast("Login failed: ${response.message}")
            }
        }
    }

    /**
     * Enforce that Phone OTP, Email, and 18+ Age verification are all complete
     * before entering MAIN_DISCOVER.
     */
    fun verifyAge(is18Plus: Boolean) {
        if (!is18Plus) {
            showToast("You must be 18 or older to use Velora.")
            return
        }

        val state = _uiState.value
        // Verify phone OTP completed
        if (!state.isPhoneVerified) {
            showToast("Phone OTP verification must be completed first.")
            _uiState.update { it.copy(currentScreen = Screen.PHONE_OTP_VERIFICATION) }
            return
        }
        // Verify email address verified
        if (!state.isEmailVerified) {
            showToast("Email address verification must be completed first.")
            _uiState.update { it.copy(currentScreen = Screen.EMAIL_VERIFICATION) }
            return
        }

        _uiState.update {
            it.copy(
                isAgeVerified = true,
                isLoggedIn = true,
                currentScreen = Screen.MAIN_DISCOVER
            )
        }
        appContext?.let { SessionManager.saveUserSession(it, _uiState.value.currentUserProfile) }
        showToast("Verification complete! Welcome to Velora.")
    }

    fun quickLogin() {
        viewModelScope.launch {
            // Synchronize with Firebase Auth & Cloud Firestore
            FirebaseAuthRepository.registerAndSyncUser(
                name = VeloraData.currentUser.name,
                email = VeloraData.currentUser.email.ifBlank { "member@velora.com" },
                phone = VeloraData.currentUser.phone.ifBlank { "01700000000" },
                pass = "VeloraPass@123"
            )
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    isAgeVerified = true,
                    isPhoneVerified = true,
                    isEmailVerified = true,
                    currentScreen = Screen.MAIN_DISCOVER
                )
            }
            appContext?.let { SessionManager.saveUserSession(it, VeloraData.currentUser) }
            showToast("Signed in! Firebase account synchronized.")
        }
    }

    fun setSubTab(tab: String) {
        _uiState.update { it.copy(activeSubTab = tab) }
    }

    fun openProfileDetails(profile: UserProfile) {
        _uiState.update {
            it.copy(
                selectedProfileDetails = profile,
                currentScreen = Screen.PROFILE_DETAILS
            )
        }
    }

    fun passProfile(profile: UserProfile) {
        _uiState.update { state ->
            val nextIndex = (state.discoverIndex + 1) % state.profiles.size
            state.copy(
                discoverIndex = nextIndex,
                lastPassedProfile = profile
            )
        }
    }

    fun undoPass() {
        val lastPassed = _uiState.value.lastPassedProfile
        if (lastPassed != null) {
            _uiState.update { state ->
                val prevIndex = if (state.discoverIndex > 0) state.discoverIndex - 1 else state.profiles.size - 1
                state.copy(
                    discoverIndex = prevIndex,
                    lastPassedProfile = null
                )
            }
            showToast("Undid pass on ${lastPassed.name}")
        } else {
            showToast("No pass to undo")
        }
    }

    fun likeProfile(profile: UserProfile) {
        _uiState.update { state ->
            val nextIndex = (state.discoverIndex + 1) % state.profiles.size
            // Check if mutual match (Ayesha, Nusrat, or Tania will trigger mutual match celebration)
            val isMutualMatch = profile.id in setOf("user_ayesha", "user_nusrat", "user_tania")
            if (isMutualMatch) {
                // Add to matches if not already present
                val existingMatch = state.matches.any { it.user.id == profile.id }
                val updatedMatches = if (!existingMatch) {
                    listOf(
                        MatchItem(
                            id = "match_${profile.id}",
                            user = profile,
                            matchedAt = "Just now",
                            lastMessage = "You both liked each other! Say hello \uD83D\uDC4B",
                            lastMessageTime = "Just now",
                            isOnline = profile.isOnline,
                            isNew = true
                        )
                    ) + state.matches
                } else state.matches

                state.copy(
                    discoverIndex = nextIndex,
                    showMatchDialog = true,
                    matchedUser = profile,
                    matches = updatedMatches
                )
            } else {
                state.copy(discoverIndex = nextIndex)
            }
        }
    }

    fun superLikeProfile(profile: UserProfile) {
        if (_uiState.value.superLikesRemaining > 0 || _uiState.value.isPremiumUser) {
            _uiState.update { state ->
                val remaining = if (state.isPremiumUser) state.superLikesRemaining else (state.superLikesRemaining - 1).coerceAtLeast(0)
                val nextIndex = (state.discoverIndex + 1) % state.profiles.size
                state.copy(
                    superLikesRemaining = remaining,
                    discoverIndex = nextIndex,
                    showMatchDialog = true,
                    matchedUser = profile
                )
            }
            showToast("Super Liked ${profile.name}! \u2B50")
        } else {
            navigateTo(Screen.BOOST_STORE)
            showToast("Get more Super Likes with Velora Premium!")
        }
    }

    fun dismissMatchDialog() {
        _uiState.update { it.copy(showMatchDialog = false, matchedUser = null) }
    }

    fun openChatWith(match: MatchItem) {
        _uiState.update {
            it.copy(
                activeChatMatch = match,
                showMatchDialog = false,
                currentScreen = Screen.CHAT_ROOM
            )
        }
    }

    fun sendMessage(text: String) {
        val activeMatch = _uiState.value.activeChatMatch ?: return
        if (text.isBlank()) return

        val newMessage = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "user_me",
            text = text.trim(),
            timestamp = "Just now",
            isFromMe = true,
            isRead = false
        )

        _uiState.update { state ->
            val history = state.chatMessages[activeMatch.id].orEmpty() + newMessage
            val updatedMessages = state.chatMessages.toMutableMap().apply {
                put(activeMatch.id, history)
            }
            // Update last message in matches list
            val updatedMatches = state.matches.map {
                if (it.id == activeMatch.id) {
                    it.copy(lastMessage = text.trim(), lastMessageTime = "Just now", unreadCount = 0)
                } else it
            }
            state.copy(
                chatMessages = updatedMessages,
                matches = updatedMatches
            )
        }

        // Simulate cute reply after 1.5 seconds if talking to Ayesha
        if (activeMatch.user.id == "user_ayesha") {
            viewModelScope.launch {
                delay(1500)
                val replies = listOf(
                    "That sounds wonderful! Tell me more ✨",
                    "Aww, that's really sweet of you! \uD83E\uDEF6",
                    "I totally agree with you! What else do you love doing?",
                    "Haha you are so funny! \uD83D\uDE04",
                    "Would love to grab a coffee sometime! ☕"
                )
                val replyText = replies.random()
                val replyMessage = ChatMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    senderId = activeMatch.user.id,
                    text = replyText,
                    timestamp = "Just now",
                    isFromMe = false,
                    isRead = true
                )
                _uiState.update { state ->
                    val history = state.chatMessages[activeMatch.id].orEmpty() + replyMessage
                    val updatedMessages = state.chatMessages.toMutableMap().apply {
                        put(activeMatch.id, history)
                    }
                    val updatedMatches = state.matches.map {
                        if (it.id == activeMatch.id) {
                            it.copy(lastMessage = replyText, lastMessageTime = "Just now")
                        } else it
                    }
                    state.copy(
                        chatMessages = updatedMessages,
                        matches = updatedMatches
                    )
                }
            }
        }
    }

    /**
     * Initiates a real-time voice call to target match or active match.
     */
    fun startVoiceCall(targetMatch: MatchItem? = null) {
        val match = targetMatch ?: _uiState.value.activeChatMatch ?: _uiState.value.matches.firstOrNull() ?: return
        initiateRealTimeCall(match, CallType.AUDIO)
    }

    /**
     * Initiates a real-time video call to target match or active match.
     */
    fun startVideoCall(targetMatch: MatchItem? = null) {
        val match = targetMatch ?: _uiState.value.activeChatMatch ?: _uiState.value.matches.firstOrNull() ?: return
        initiateRealTimeCall(match, CallType.VIDEO)
    }

    private fun initiateRealTimeCall(match: MatchItem, callType: CallType) {
        val currentUser = _uiState.value.currentUserProfile
        val initialSession = CallSession(
            id = "call_${System.currentTimeMillis()}",
            callerId = currentUser.id,
            callerName = currentUser.name,
            callerPhoto = currentUser.photos.firstOrNull() ?: "",
            receiverId = match.user.id,
            receiverName = match.user.name,
            receiverPhoto = match.user.photos.firstOrNull() ?: "",
            callType = callType,
            status = CallStatus.RINGING,
            offerSdp = "v=0...",
            isAudioMuted = false,
            isVideoEnabled = (callType == CallType.VIDEO),
            isSpeakerOn = (callType == CallType.VIDEO),
            createdAt = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                isCallingActive = true,
                activeCallSession = initialSession,
                callDurationSeconds = 0,
                isCallMuted = false,
                isSpeakerOn = (callType == CallType.VIDEO),
                isVideoEnabled = (callType == CallType.VIDEO),
                isFrontCamera = true,
                currentScreen = Screen.VIDEO_CALL
            )
        }
        mediaManager?.startCallAudio(enableSpeaker = (callType == CallType.VIDEO))

        initiateCallJob?.cancel()
        initiateCallJob = viewModelScope.launch {
            val result = CallSignalingRepository.initiateCall(currentUser, match, callType)
            val session = result.getOrNull()
            if (session != null && _uiState.value.isCallingActive) {
                _uiState.update { it.copy(activeCallSession = session) }
                observeCallSession(session.id)
            }
        }
    }

    /**
     * Observes real-time state changes on active call session.
     */
    private fun observeCallSession(callId: String) {
        activeCallObserverJob?.cancel()
        activeCallObserverJob = viewModelScope.launch {
            CallSignalingRepository.observeCall(callId).collect { session ->
                if (session != null) {
                    _uiState.update { it.copy(activeCallSession = session) }
                    when (session.status) {
                        CallStatus.CONNECTED -> {
                            mediaManager?.stopRinging()
                            startCallDurationTimer()
                        }
                        CallStatus.ENDED, CallStatus.REJECTED -> {
                            delay(1200)
                            endActiveCall()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun startCallDurationTimer() {
        if (callTimerJob?.isActive == true) return
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }
            }
        }
    }

    /**
     * Accepts an incoming call session.
     */
    fun acceptIncomingCall() {
        val incoming = _uiState.value.incomingCallSession ?: return
        viewModelScope.launch {
            mediaManager?.stopRinging()
            CallSignalingRepository.acceptCall(incoming.id)
            val connectedSession = incoming.copy(status = CallStatus.CONNECTED)
            _uiState.update {
                it.copy(
                    incomingCallSession = null,
                    isCallingActive = true,
                    activeCallSession = connectedSession,
                    callDurationSeconds = 0,
                    isCallMuted = false,
                    isSpeakerOn = (incoming.callType == CallType.VIDEO),
                    isVideoEnabled = (incoming.callType == CallType.VIDEO),
                    isFrontCamera = true,
                    currentScreen = Screen.VIDEO_CALL
                )
            }
            mediaManager?.startCallAudio(enableSpeaker = (incoming.callType == CallType.VIDEO))
            startCallDurationTimer()
            observeCallSession(incoming.id)
        }
    }

    /**
     * Declines or rejects an incoming call.
     */
    fun rejectIncomingCall() {
        val incoming = _uiState.value.incomingCallSession ?: return
        _uiState.update { it.copy(incomingCallSession = null) }
        mediaManager?.stopRinging()
        viewModelScope.launch {
            CallSignalingRepository.rejectCall(incoming.id)
        }
    }

    /**
     * Ends current active call session.
     */
    fun endActiveCall() {
        val activeCall = _uiState.value.activeCallSession
        val duration = _uiState.value.callDurationSeconds
        initiateCallJob?.cancel()
        callTimerJob?.cancel()
        activeCallObserverJob?.cancel()
        _uiState.update {
            it.copy(
                isCallingActive = false,
                activeCallSession = null,
                callDurationSeconds = 0,
                currentScreen = if (it.currentScreen == Screen.VIDEO_CALL) Screen.CHAT_ROOM else it.currentScreen
            )
        }
        mediaManager?.cleanup()
        viewModelScope.launch {
            if (activeCall != null) {
                CallSignalingRepository.endCall(activeCall.id, duration)
            }
        }
    }

    fun endVideoCall() {
        endActiveCall()
    }

    fun toggleCallMute() {
        val newMuted = !_uiState.value.isCallMuted
        _uiState.update { it.copy(isCallMuted = newMuted) }
        mediaManager?.setMicrophoneMute(newMuted)
        _uiState.value.activeCallSession?.id?.let { callId ->
            viewModelScope.launch {
                CallSignalingRepository.updateMediaState(
                    callId = callId,
                    isMuted = newMuted,
                    isVideoEnabled = _uiState.value.isVideoEnabled,
                    isSpeakerOn = _uiState.value.isSpeakerOn
                )
            }
        }
    }

    fun toggleSpeakerphone() {
        val newSpeaker = !_uiState.value.isSpeakerOn
        _uiState.update { it.copy(isSpeakerOn = newSpeaker) }
        mediaManager?.setSpeakerphoneOn(newSpeaker)
        _uiState.value.activeCallSession?.id?.let { callId ->
            viewModelScope.launch {
                CallSignalingRepository.updateMediaState(
                    callId = callId,
                    isMuted = _uiState.value.isCallMuted,
                    isVideoEnabled = _uiState.value.isVideoEnabled,
                    isSpeakerOn = newSpeaker
                )
            }
        }
    }

    fun toggleVideoEnabled() {
        val newVideo = !_uiState.value.isVideoEnabled
        _uiState.update { it.copy(isVideoEnabled = newVideo) }
        if (!newVideo) {
            mediaManager?.stopCameraPreview()
        }
        _uiState.value.activeCallSession?.id?.let { callId ->
            viewModelScope.launch {
                CallSignalingRepository.updateMediaState(
                    callId = callId,
                    isMuted = _uiState.value.isCallMuted,
                    isVideoEnabled = newVideo,
                    isSpeakerOn = _uiState.value.isSpeakerOn
                )
            }
        }
    }

    fun flipCamera() {
        val newFront = !_uiState.value.isFrontCamera
        _uiState.update { it.copy(isFrontCamera = newFront) }
        mediaManager?.flipCamera()
    }

    /**
     * For solo-device demonstration and integration testing:
     * Simulates the remote partner answering the call in WebRTC and Cloud Firestore.
     */
    fun simulatePartnerAnswer() {
        val activeCall = _uiState.value.activeCallSession ?: return
        viewModelScope.launch {
            CallSignalingRepository.simulateRemotePartnerAnswer(activeCall.id)
        }
    }

    fun applyFilters(newFilters: DiscoveryFilters) {
        _uiState.update { state ->
            val isGlobal = newFilters.country.contains("Global", ignoreCase = true)
            val filtered = VeloraData.discoverProfiles.filter { user ->
                val ageOk = user.age in newFilters.minAge..newFilters.maxAge
                val distanceOk = if (isGlobal) true else user.distanceKm <= newFilters.maxDistanceKm
                val genderOk = newFilters.gender == "All" || user.gender.equals(newFilters.gender, ignoreCase = true)
                val countryOk = when {
                    isGlobal -> true
                    newFilters.country.equals("Any", ignoreCase = true) -> true
                    newFilters.country.contains("Bangladesh", ignoreCase = true) -> user.country.equals("Bangladesh", ignoreCase = true)
                    newFilters.country.contains("India", ignoreCase = true) -> user.country.equals("India", ignoreCase = true)
                    else -> user.country.contains(newFilters.country, ignoreCase = true)
                }
                ageOk && distanceOk && genderOk && countryOk && user.id !in state.blockedUserIds
            }.let { list ->
                if (isGlobal) {
                    list.sortedByDescending {
                        it.country.equals("Global", ignoreCase = true) ||
                        it.country.equals("India", ignoreCase = true) ||
                        it.distanceKm > 100
                    }
                } else {
                    list
                }
            }
            state.copy(
                filters = newFilters,
                profiles = if (filtered.isNotEmpty()) filtered else VeloraData.discoverProfiles,
                discoverIndex = 0,
                currentScreen = Screen.MAIN_DISCOVER
            )
        }
        showToast("Filters applied successfully!")
    }

    fun purchaseSubscription(plan: SubscriptionPlan) {
        openCheckout(plan)
    }

    fun openCheckout(plan: SubscriptionPlan) {
        val defaultPhone = _uiState.value.pendingAuthPhone.ifBlank {
            _uiState.value.currentUserProfile.phone.ifBlank { "01712-345678" }
        }
        _uiState.update {
            it.copy(
                isCheckoutSheetOpen = true,
                selectedCheckoutPlan = plan,
                mfsSenderPhone = defaultPhone,
                mfsTrxId = "",
                isPaymentProcessing = false
            )
        }
    }

    fun closeCheckout() {
        _uiState.update {
            it.copy(
                isCheckoutSheetOpen = false,
                isPaymentProcessing = false
            )
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun setMfsSenderPhone(phone: String) {
        _uiState.update { it.copy(mfsSenderPhone = phone) }
    }

    fun setMfsTrxId(trxId: String) {
        _uiState.update { it.copy(mfsTrxId = trxId) }
    }

    fun dismissPaymentSuccessDialog() {
        _uiState.update {
            it.copy(
                paymentSuccessDialogVisible = false,
                paymentSuccessMessage = null
            )
        }
    }

    /**
     * Submit MFS payment (bKash / Nagad / Card) and automatically verify & activate subscription.
     */
    fun submitMfsPayment(
        method: String,
        senderPhone: String,
        trxId: String,
        plan: SubscriptionPlan
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPaymentProcessing = true) }

            val cleanPhone = senderPhone.trim()
            val cleanTrxId = trxId.trim()

            if (cleanTrxId.length < 6) {
                _uiState.update { it.copy(isPaymentProcessing = false) }
                showToast("Please enter a valid Transaction ID (TrxID) with at least 6 characters.")
                return@launch
            }

            val userToken = com.example.server.ServerTokenManager.createToken(
                userId = _uiState.value.currentUserProfile.id,
                email = _uiState.value.pendingAuthEmail.ifBlank { _uiState.value.currentUserProfile.email.ifBlank { "alif@example.com" } },
                role = com.example.server.Role.USER
            )

            val response = VeloraBackendServer.verifyPaymentAndGrantPremium(
                authHeader = "Bearer $userToken",
                paymentRequest = com.example.server.PaymentVerificationRequest(
                    planId = plan.id,
                    transactionReceipt = cleanTrxId,
                    amount = "${plan.priceBdt}",
                    currency = "BDT",
                    paymentMethod = method,
                    senderAccount = cleanPhone
                )
            )

            if (response.isSuccessful) {
                // Subscription automatically activated upon transaction verification!
                _uiState.update {
                    it.copy(
                        isPremiumUser = true,
                        activePlan = plan,
                        isCheckoutSheetOpen = false,
                        isPaymentProcessing = false,
                        paymentSuccessDialogVisible = true,
                        paymentSuccessMessage = "Subscribed to ${plan.name}! ৳${plan.priceBdt} BDT paid via $method. TrxID: $cleanTrxId.",
                        superLikesRemaining = it.superLikesRemaining + 10,
                        boostMinutesRemaining = it.boostMinutesRemaining + 30
                    )
                }
                showToast("Payment verified via $method! Premium activated! 👑")
                loadMfsConfigAndPlans()
            } else {
                _uiState.update { it.copy(isPaymentProcessing = false) }
                showToast("Payment verification failed: ${response.message}")
            }
        }
    }

    fun purchaseSubscriptionDirect(plan: SubscriptionPlan, customTrxId: String? = null, method: String = "BKASH") {
        val userToken = com.example.server.ServerTokenManager.createToken(
            userId = "user_me",
            email = "alif@example.com",
            role = com.example.server.Role.USER
        )
        val response = com.example.server.VeloraBackendServer.verifyPaymentAndGrantPremium(
            authHeader = "Bearer $userToken",
            paymentRequest = com.example.server.PaymentVerificationRequest(
                planId = plan.id,
                transactionReceipt = customTrxId ?: "BKASH_TXN_${System.currentTimeMillis()}",
                amount = "৳ ${plan.priceBdt}",
                currency = "BDT",
                paymentMethod = method
            )
        )
        if (response.isSuccessful) {
            _uiState.update {
                it.copy(
                    isPremiumUser = true,
                    activePlan = plan,
                    superLikesRemaining = it.superLikesRemaining + 10,
                    toastMessage = "Velora Premium (${plan.name}) Activated! \uD83D\uDC51"
                )
            }
        } else {
            showToast("Payment verification failed: ${response.message}")
        }
    }

    /**
     * Admin: Update bKash and Nagad credentials/merchant numbers and enable/disable states.
     */
    fun adminUpdateMfsConfig(config: com.example.server.MfsMerchantConfig) {
        viewModelScope.launch {
            val token = _uiState.value.adminSessionToken
            if (token.isNullOrBlank()) {
                showToast("Admin session token required.")
                return@launch
            }
            val response = VeloraBackendServer.updateMfsConfig("Bearer $token", config)
            if (response.isSuccessful) {
                _uiState.update { it.copy(mfsConfig = response.data ?: config) }
                showToast("MFS Merchant settings updated (bKash & Nagad).")
            } else {
                showToast("Failed to update MFS settings: ${response.message}")
            }
        }
    }

    /**
     * Admin: Set package price in BDT.
     */
    fun adminUpdatePlanPrice(planId: String, newPriceBdt: Int) {
        viewModelScope.launch {
            val token = _uiState.value.adminSessionToken
            if (token.isNullOrBlank()) {
                showToast("Admin session token required.")
                return@launch
            }
            val response = VeloraBackendServer.updateSubscriptionPlanPrice("Bearer $token", planId, newPriceBdt)
            if (response.isSuccessful) {
                val plansResponse = VeloraBackendServer.getSubscriptionPlans()
                _uiState.update {
                    it.copy(subscriptionPlans = plansResponse.data ?: it.subscriptionPlans)
                }
                showToast("Plan '$planId' price updated to ৳$newPriceBdt BDT.")
            } else {
                showToast("Failed to update plan price: ${response.message}")
            }
        }
    }

    /**
     * Admin: Reload MFS transactions history.
     */
    fun adminLoadMfsTransactions() {
        viewModelScope.launch {
            val token = _uiState.value.adminSessionToken ?: return@launch
            val response = VeloraBackendServer.getMfsTransactions("Bearer $token")
            if (response.isSuccessful) {
                _uiState.update { it.copy(mfsTransactions = response.data.orEmpty()) }
            }
        }
    }


    fun activateBoost() {
        _uiState.update {
            it.copy(
                boostMinutesRemaining = 30,
                toastMessage = "Profile Boosted for 30 minutes! ⚡ Be seen 10x more."
            )
        }
    }

    fun purchaseSuperLikes() {
        _uiState.update {
            it.copy(
                superLikesRemaining = it.superLikesRemaining + 5,
                toastMessage = "Added 5 Super Likes! \u2B50"
            )
        }
    }

    fun openBlockReport(user: UserProfile) {
        _uiState.update {
            it.copy(
                targetBlockUser = user,
                currentScreen = Screen.BLOCK_REPORT
            )
        }
    }

    fun blockUser(user: UserProfile) {
        _uiState.update { state ->
            val updatedBlocked = state.blockedUserIds + user.id
            val updatedProfiles = state.profiles.filter { it.id != user.id }
            val updatedMatches = state.matches.filter { it.user.id != user.id }
            state.copy(
                blockedUserIds = updatedBlocked,
                profiles = updatedProfiles,
                matches = updatedMatches,
                currentScreen = Screen.MAIN_DISCOVER,
                toastMessage = "${user.name} has been blocked."
            )
        }
    }

    fun reportUser(user: UserProfile, reason: String) {
        val newReport = UserReport(
            id = "rep_${System.currentTimeMillis()}",
            type = "Profile",
            reporterName = "Alif Islam",
            reportedUserId = user.id,
            reportedUserName = user.name,
            reason = reason,
            date = "Today",
            status = "Pending"
        )
        _uiState.update { state ->
            state.copy(
                reports = listOf(newReport) + state.reports,
                currentScreen = Screen.MAIN_DISCOVER,
                toastMessage = "Report submitted. Thank you for keeping Velora safe."
            )
        }
    }

    // Admin Panel Methods
    fun setAdminSection(section: AdminSection) {
        _uiState.update { it.copy(currentAdminSection = section) }
    }

    fun toggleUserSuspension(userId: String) {
        val authHeader = _uiState.value.adminSessionToken?.let { "Bearer $it" }
        val response = com.example.server.VeloraBackendServer.toggleUserSuspension(authHeader, userId)
        if (!response.isSuccessful) {
            if (response.statusCode in listOf(401, 403)) {
                logoutAdmin()
                _uiState.update {
                    it.copy(
                        currentScreen = Screen.ADMIN_LOGIN,
                        adminLoginError = "HTTP ${response.statusCode} ${response.error}: ${response.message}"
                    )
                }
            } else {
                showToast("Error: ${response.message}")
            }
            return
        }

        val updatedProfiles = com.example.server.VeloraBackendServer.getAdminUsers(authHeader).data ?: _uiState.value.profiles
        _uiState.update {
            it.copy(
                profiles = updatedProfiles,
                toastMessage = response.message
            )
        }
    }

    fun resolveReport(reportId: String, newStatus: String) {
        val authHeader = _uiState.value.adminSessionToken?.let { "Bearer $it" }
        val response = com.example.server.VeloraBackendServer.resolveReport(authHeader, reportId, newStatus)
        if (!response.isSuccessful) {
            if (response.statusCode in listOf(401, 403)) {
                logoutAdmin()
                _uiState.update {
                    it.copy(
                        currentScreen = Screen.ADMIN_LOGIN,
                        adminLoginError = "HTTP ${response.statusCode} ${response.error}: ${response.message}"
                    )
                }
            } else {
                showToast("Error: ${response.message}")
            }
            return
        }

        val updatedReports = com.example.server.VeloraBackendServer.getAdminReports(authHeader).data ?: _uiState.value.reports
        _uiState.update {
            it.copy(
                reports = updatedReports,
                toastMessage = response.message
            )
        }
    }

    fun provisionAdminUser(newEmail: String, pass: String, displayName: String) {
        val authHeader = _uiState.value.adminSessionToken?.let { "Bearer $it" } ?: return
        val response = VeloraBackendServer.createAdminUser(
            operatorAuthHeader = authHeader,
            email = newEmail,
            password = pass,
            displayName = displayName
        )
        if (response.isSuccessful) {
            val updatedAdmins = VeloraBackendServer.getAdminAccounts(authHeader).data.orEmpty()
            _uiState.update {
                it.copy(
                    authorizedAdminsList = updatedAdmins,
                    toastMessage = "New administrator '$newEmail' provisioned with custom claims."
                )
            }
        } else {
            showToast("Failed to provision admin: ${response.message}")
        }
    }

    fun revokeAdminUser(targetEmail: String) {
        val authHeader = _uiState.value.adminSessionToken?.let { "Bearer $it" } ?: return
        val response = VeloraBackendServer.revokeAdminRole(authHeader, targetEmail)
        if (response.isSuccessful) {
            val updatedAdmins = VeloraBackendServer.getAdminAccounts(authHeader).data.orEmpty()
            _uiState.update {
                it.copy(
                    authorizedAdminsList = updatedAdmins,
                    toastMessage = "Revoked administrator privileges for '$targetEmail'."
                )
            }
        } else {
            showToast("Revocation error: ${response.message}")
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    // --- Cloudinary & Cloud Firestore Profile Photo Operations ---

    /**
     * Upload a dedicated Primary Profile Photo to Cloudinary CDN & Cloud Firestore.
     */
    fun uploadPrimaryPhotoFromUri(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingPhoto = true,
                    photoUploadProgress = 0.25f,
                    photoUploadStatus = "Reading primary profile photo..."
                )
            }

            delay(200)
            _uiState.update {
                it.copy(
                    photoUploadProgress = 0.6f,
                    photoUploadStatus = "Uploading primary photo to Cloudinary CDN..."
                )
            }

            val uploadResult = CloudinaryUploadService.uploadImageFromUri(context, imageUri)
            when (uploadResult) {
                is UploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            photoUploadProgress = 0.85f,
                            photoUploadStatus = "Setting primary photo in Cloud Firestore...",
                            lastUploadedCdnUrl = uploadResult.secureUrl
                        )
                    }

                    val currentUserId = _uiState.value.currentUserProfile.id
                    val firestoreResult = FirestoreProfileRepository.savePrimaryPhotoUrl(
                        userId = currentUserId,
                        primaryUrl = uploadResult.secureUrl
                    )

                    firestoreResult.fold(
                        onSuccess = { updatedProfile ->
                            _uiState.update {
                                it.copy(
                                    currentUserProfile = updatedProfile,
                                    isUploadingPhoto = false,
                                    photoUploadProgress = 1.0f,
                                    photoUploadStatus = null,
                                    toastMessage = "Primary profile photo updated via Cloudinary CDN & Firestore! ⭐"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isUploadingPhoto = false,
                                    photoUploadStatus = null,
                                    toastMessage = "Failed to sync primary photo to Firestore: ${error.localizedMessage}"
                                )
                            }
                        }
                    )
                }
                is UploadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoUploadStatus = null,
                            toastMessage = "Cloudinary upload error: ${uploadResult.message}"
                        )
                    }
                }
            }
        }
    }

    /**
     * Upload 4 to 5 clear face photos in batch or sequence.
     */
    fun uploadMultiplePhotosFromUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingPhoto = true,
                    photoUploadProgress = 0.1f,
                    photoUploadStatus = "Preparing ${uris.size} face photos for upload..."
                )
            }

            val uploadedUrls = mutableListOf<String>()
            val maxAllowed = 5 - _uiState.value.currentUserProfile.photos.size
            val selectedUris = uris.take(if (maxAllowed > 0) maxAllowed else 5)

            selectedUris.forEachIndexed { idx, uri ->
                val progress = 0.2f + (0.6f * ((idx + 1).toFloat() / selectedUris.size))
                _uiState.update {
                    it.copy(
                        photoUploadProgress = progress,
                        photoUploadStatus = "Uploading face photo ${idx + 1} of ${selectedUris.size} to Cloudinary..."
                    )
                }
                when (val result = CloudinaryUploadService.uploadImageFromUri(context, uri)) {
                    is UploadResult.Success -> {
                        uploadedUrls.add(result.secureUrl)
                    }
                    is UploadResult.Error -> {
                        // Continue uploading other photos if one fails
                    }
                }
            }

            if (uploadedUrls.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        photoUploadProgress = 0.9f,
                        photoUploadStatus = "Saving ${uploadedUrls.size} face photos in Cloud Firestore...",
                        lastUploadedCdnUrl = uploadedUrls.lastOrNull()
                    )
                }

                val currentUserId = _uiState.value.currentUserProfile.id
                val firestoreResult = FirestoreProfileRepository.saveMultipleProfilePhotos(
                    userId = currentUserId,
                    newPhotoUrls = uploadedUrls
                )

                firestoreResult.fold(
                    onSuccess = { updatedProfile ->
                        _uiState.update {
                            it.copy(
                                currentUserProfile = updatedProfile,
                                isUploadingPhoto = false,
                                photoUploadProgress = 1.0f,
                                photoUploadStatus = null,
                                toastMessage = "Successfully uploaded ${uploadedUrls.size} face photos to your profile! 📸"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isUploadingPhoto = false,
                                photoUploadStatus = null,
                                toastMessage = "Failed to sync photos to Firestore: ${error.localizedMessage}"
                            )
                        }
                    }
                )
            } else {
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        photoUploadStatus = null,
                        toastMessage = "Could not upload photos. Please try again."
                    )
                }
            }
        }
    }

    fun uploadPhotoFromUri(context: Context, imageUri: Uri) {
        if (_uiState.value.currentUserProfile.photos.size >= 5) {
            showToast("Maximum 5 face photos reached. Remove a photo to upload a new one.")
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingPhoto = true,
                    photoUploadProgress = 0.2f,
                    photoUploadStatus = "Reading image from gallery..."
                )
            }

            delay(300)
            _uiState.update {
                it.copy(
                    photoUploadProgress = 0.5f,
                    photoUploadStatus = "Uploading to Cloudinary CDN..."
                )
            }

            val uploadResult = CloudinaryUploadService.uploadImageFromUri(context, imageUri)

            when (uploadResult) {
                is UploadResult.Success -> {
                    _uiState.update {
                        it.copy(
                            photoUploadProgress = 0.85f,
                            photoUploadStatus = "Storing Cloudinary URL in Cloud Firestore...",
                            lastUploadedCdnUrl = uploadResult.secureUrl
                        )
                    }

                    // Save URL directly into user's profile document in Cloud Firestore
                    val currentUserId = _uiState.value.currentUserProfile.id
                    val isFirstPhoto = _uiState.value.currentUserProfile.photos.isEmpty()
                    val firestoreResult = FirestoreProfileRepository.saveProfilePhotoUrl(
                        userId = currentUserId,
                        newPhotoUrl = uploadResult.secureUrl,
                        setAsPrimary = isFirstPhoto
                    )

                    firestoreResult.fold(
                        onSuccess = { updatedPhotos ->
                            val currentPrimary = _uiState.value.currentUserProfile.effectivePrimaryPhoto.ifBlank { uploadResult.secureUrl }
                            val updatedProfile = _uiState.value.currentUserProfile.copy(
                                photos = updatedPhotos.take(5),
                                primaryPhotoUrl = currentPrimary
                            )
                            _uiState.update {
                                it.copy(
                                    currentUserProfile = updatedProfile,
                                    isUploadingPhoto = false,
                                    photoUploadProgress = 1.0f,
                                    photoUploadStatus = null,
                                    toastMessage = "Face photo uploaded to Cloudinary CDN & saved to Firestore! ✨"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isUploadingPhoto = false,
                                    photoUploadStatus = null,
                                    toastMessage = "Failed to sync to Firestore: ${error.localizedMessage}"
                                )
                            }
                        }
                    )
                }
                is UploadResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoUploadStatus = null,
                            toastMessage = "Cloudinary upload error: ${uploadResult.message}"
                        )
                    }
                }
            }
        }
    }

    fun uploadSamplePhoto(sampleUrl: String, asPrimary: Boolean = false) {
        if (asPrimary) {
            viewModelScope.launch {
                val currentUserId = _uiState.value.currentUserProfile.id
                val result = FirestoreProfileRepository.savePrimaryPhotoUrl(currentUserId, sampleUrl)
                result.fold(
                    onSuccess = { updatedProfile ->
                        _uiState.update {
                            it.copy(
                                currentUserProfile = updatedProfile,
                                lastUploadedCdnUrl = sampleUrl,
                                toastMessage = "Primary profile photo updated! ⭐"
                            )
                        }
                    },
                    onFailure = { error ->
                        showToast("Error updating primary photo: ${error.localizedMessage}")
                    }
                )
            }
            return
        }

        if (_uiState.value.currentUserProfile.photos.size >= 5) {
            showToast("Maximum 5 face photos reached. Remove a photo first.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingPhoto = true,
                    photoUploadProgress = 0.6f,
                    photoUploadStatus = "Storing Cloudinary photo URL in Cloud Firestore..."
                )
            }

            val currentUserId = _uiState.value.currentUserProfile.id
            val firestoreResult = FirestoreProfileRepository.saveProfilePhotoUrl(
                userId = currentUserId,
                newPhotoUrl = sampleUrl,
                setAsPrimary = _uiState.value.currentUserProfile.photos.isEmpty()
            )

            firestoreResult.fold(
                onSuccess = { updatedPhotos ->
                    val updatedProfile = _uiState.value.currentUserProfile.copy(
                        photos = updatedPhotos.take(5),
                        primaryPhotoUrl = _uiState.value.currentUserProfile.effectivePrimaryPhoto.ifBlank { sampleUrl }
                    )
                    _uiState.update {
                        it.copy(
                            currentUserProfile = updatedProfile,
                            isUploadingPhoto = false,
                            photoUploadProgress = 1.0f,
                            photoUploadStatus = null,
                            lastUploadedCdnUrl = sampleUrl,
                            toastMessage = "Face photo updated via Cloudinary CDN & saved to Firestore! ✨"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoUploadStatus = null,
                            toastMessage = "Failed to update Firestore: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun setPrimaryPhoto(photoUrl: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserProfile.id
            val result = FirestoreProfileRepository.setPrimaryPhoto(currentUserId, photoUrl)
            result.fold(
                onSuccess = { updatedPhotos ->
                    val updatedProfile = _uiState.value.currentUserProfile.copy(
                        photos = updatedPhotos,
                        primaryPhotoUrl = photoUrl
                    )
                    _uiState.update {
                        it.copy(
                            currentUserProfile = updatedProfile,
                            toastMessage = "Primary profile photo updated in Cloud Firestore. ⭐"
                        )
                    }
                },
                onFailure = { error ->
                    showToast("Error updating photo: ${error.localizedMessage}")
                }
            )
        }
    }

    fun removePhoto(photoUrl: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserProfile.id
            val result = FirestoreProfileRepository.removeProfilePhoto(currentUserId, photoUrl)
            result.fold(
                onSuccess = { updatedPhotos ->
                    val updatedPrimary = if (_uiState.value.currentUserProfile.primaryPhotoUrl == photoUrl) {
                        updatedPhotos.firstOrNull().orEmpty()
                    } else {
                        _uiState.value.currentUserProfile.primaryPhotoUrl
                    }
                    val updatedProfile = _uiState.value.currentUserProfile.copy(
                        photos = updatedPhotos,
                        primaryPhotoUrl = updatedPrimary
                    )
                    _uiState.update {
                        it.copy(
                            currentUserProfile = updatedProfile,
                            toastMessage = "Photo removed and Firestore document updated."
                        )
                    }
                },
                onFailure = { error ->
                    showToast("Error: ${error.localizedMessage}")
                }
            )
        }
    }

    fun updateProfileDetails(aboutMe: String, profession: String, city: String) {
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserProfile.id
            val firestoreResult = FirestoreProfileRepository.saveProfileDetails(
                userId = currentUserId,
                aboutMe = aboutMe.trim(),
                profession = profession.trim(),
                city = city.trim()
            )

            firestoreResult.fold(
                onSuccess = { updated ->
                    _uiState.update {
                        it.copy(
                            currentUserProfile = updated,
                            toastMessage = "Profile details saved to Cloud Firestore."
                        )
                    }
                    appContext?.let { SessionManager.saveUserSession(it, updated) }
                },
                onFailure = { error ->
                    showToast("Update failed: ${error.localizedMessage}")
                }
            )
        }
    }

    fun logout(context: Context? = null) {
        val ctx = context ?: appContext
        ctx?.let { SessionManager.clearSession(it) }
        try {
            FirebaseAuthRepository.getAuth()?.signOut()
        } catch (_: Exception) {}
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                isPhoneVerified = false,
                isEmailVerified = false,
                isAgeVerified = false,
                currentScreen = Screen.WELCOME
            )
        }
        showToast("Logged out successfully.")
    }
}
