package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VeloraData
import com.example.model.*
import com.example.server.VeloraBackendServer
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
    val toastMessage: String? = null
)

class VeloraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VeloraUiState())
    val uiState: StateFlow<VeloraUiState> = _uiState.asStateFlow()

    fun navigateTo(screen: Screen) {
        if (screen == Screen.ADMIN_PANEL && !_uiState.value.isAdminAuthenticated) {
            // Backend/Route Guard: Unauthorized access directly to /admin is intercepted and routed to /admin/login
            _uiState.update {
                it.copy(
                    previousScreen = it.currentScreen,
                    currentScreen = Screen.ADMIN_LOGIN,
                    adminLoginError = "401 Unauthorized: Valid administrator session required to access /admin."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                previousScreen = it.currentScreen,
                currentScreen = screen
            )
        }
    }

    fun handleRoute(route: String) {
        if (route == "/admin" || route.startsWith("/admin")) {
            val token = _uiState.value.adminSessionToken
            if (_uiState.value.isAdminAuthenticated && token != null) {
                // Server-verified role check before rendering /admin
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
                    adminLoginError = null
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

            // Step 3: Server verified! Load server data
            val usersResponse = VeloraBackendServer.getAdminUsers(authHeader)
            val reportsResponse = VeloraBackendServer.getAdminReports(authHeader)

            _uiState.update {
                it.copy(
                    isAdminAuthenticated = true,
                    adminSessionToken = token,
                    adminLoginError = null,
                    profiles = usersResponse.data ?: it.profiles,
                    reports = reportsResponse.data ?: it.reports,
                    currentScreen = Screen.ADMIN_PANEL
                )
            }
            showToast("Server Verified Administrator Access (HTTP 200). Welcome to /admin.")
        }
    }

    fun logoutAdmin() {
        _uiState.update {
            it.copy(
                isAdminAuthenticated = false,
                adminSessionToken = null,
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
                Screen.AGE_VERIFICATION -> Screen.WELCOME
                Screen.WELCOME -> Screen.SPLASH
                else -> it.previousScreen
            }
            it.copy(currentScreen = target)
        }
    }

    fun setLanguage(langCode: String) {
        _uiState.update { it.copy(selectedLanguage = langCode) }
    }

    fun verifyAge(is18Plus: Boolean) {
        if (is18Plus) {
            _uiState.update {
                it.copy(
                    isAgeVerified = true,
                    isLoggedIn = true,
                    currentScreen = Screen.MAIN_DISCOVER
                )
            }
        } else {
            showToast("You must be 18 or older to use Velora.")
        }
    }

    fun quickLogin() {
        _uiState.update {
            it.copy(
                isLoggedIn = true,
                isAgeVerified = true,
                currentScreen = Screen.MAIN_DISCOVER
            )
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

    fun startVideoCall() {
        _uiState.update { it.copy(isCallingActive = true, currentScreen = Screen.VIDEO_CALL) }
    }

    fun endVideoCall() {
        _uiState.update { it.copy(isCallingActive = false, currentScreen = Screen.CHAT_ROOM) }
    }

    fun toggleCallMute() {
        _uiState.update { it.copy(isCallMuted = !it.isCallMuted) }
    }

    fun applyFilters(newFilters: DiscoveryFilters) {
        _uiState.update { state ->
            val filtered = VeloraData.discoverProfiles.filter { user ->
                val ageOk = user.age in newFilters.minAge..newFilters.maxAge
                val distanceOk = user.distanceKm <= newFilters.maxDistanceKm
                val genderOk = newFilters.gender == "All" || user.gender.equals(newFilters.gender, ignoreCase = true)
                ageOk && distanceOk && genderOk && user.id !in state.blockedUserIds
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
        val userToken = com.example.server.ServerTokenManager.createToken(
            userId = "user_me",
            email = "alif@example.com",
            role = com.example.server.Role.USER
        )
        val response = com.example.server.VeloraBackendServer.verifyPaymentAndGrantPremium(
            authHeader = "Bearer $userToken",
            paymentRequest = com.example.server.PaymentVerificationRequest(
                planId = plan.id,
                transactionReceipt = "BKASH_TXN_${System.currentTimeMillis()}",
                amount = "৳ ${plan.priceBdt}",
                currency = "BDT"
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

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
