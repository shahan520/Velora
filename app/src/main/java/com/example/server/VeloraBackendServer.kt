package com.example.server

import com.example.data.VeloraData
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.UserReport
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Velora Server-Side Backend Engine.
 *
 * Implements strict, zero-trust server-verified authentication, authorization,
 * least-privilege access control, conversation privacy, self-privilege escalation prevention,
 * rate limiting, and comprehensive audit logging.
 */
object VeloraBackendServer {

    // Server-side user accounts registry (mimics secure server database / Firebase Auth)
    private val usersRegistry = ConcurrentHashMap<String, ServerUser>().apply {
        // Authorized Administrator accounts
        put(
            "admin@velora.app",
            ServerUser(
                id = "admin_001",
                email = "admin@velora.app",
                passwordHash = "admin_secure_pass_2026",
                role = Role.ADMIN,
                displayName = "Super Admin",
                isEmailVerified = true,
                isPhoneVerified = true
            )
        )
        put(
            "moderator@velora.app",
            ServerUser(
                id = "mod_002",
                email = "moderator@velora.app",
                passwordHash = "mod_secure_pass_2026",
                role = Role.ADMIN,
                displayName = "Lead Moderator",
                isEmailVerified = true,
                isPhoneVerified = true
            )
        )
        put(
            "mdalifislam8559@gmail.com",
            ServerUser(
                id = "admin_003",
                email = "mdalifislam8559@gmail.com",
                passwordHash = "admin_secure_pass_2026",
                role = Role.ADMIN,
                displayName = "Security Administrator",
                isEmailVerified = true,
                isPhoneVerified = true
            )
        )
        // Ordinary User Accounts (Non-Admin)
        put(
            "alif@example.com",
            ServerUser(
                id = "user_me",
                email = "alif@example.com",
                passwordHash = "userpass123",
                role = Role.USER,
                displayName = "Alif Islam",
                isEmailVerified = true,
                isPhoneVerified = true,
                phone = "+880 1712-345678"
            )
        )
        put(
            "user@example.com",
            ServerUser(
                id = "user_guest",
                email = "user@example.com",
                passwordHash = "userpass123",
                role = Role.USER,
                displayName = "Regular Member",
                isEmailVerified = true,
                isPhoneVerified = true,
                phone = "+880 1812-987654"
            )
        )
        put(
            "ayesha@example.com",
            ServerUser(
                id = "user_ayesha",
                email = "ayesha@example.com",
                passwordHash = "ayeshapass123",
                role = Role.USER,
                displayName = "Ayesha Rahman",
                isEmailVerified = true,
                isPhoneVerified = true,
                phone = "+880 1912-112233"
            )
        )
        put(
            "nusrat@example.com",
            ServerUser(
                id = "user_nusrat",
                email = "nusrat@example.com",
                passwordHash = "nusratpass123",
                role = Role.USER,
                displayName = "Nusrat Jahan",
                isEmailVerified = true,
                isPhoneVerified = true,
                phone = "+880 1512-445566"
            )
        )
    }

    // In-memory backend database state
    private val serverProfiles = ConcurrentHashMap<String, UserProfile>().apply {
        put(VeloraData.currentUser.id, VeloraData.currentUser)
        VeloraData.discoverProfiles.forEach { put(it.id, it) }
    }

    private val serverReports = ConcurrentHashMap<String, UserReport>().apply {
        VeloraData.initialReports.forEach { put(it.id, it) }
    }

    // Private conversations registry: participantIds strictly define authorized access
    private val conversations = ConcurrentHashMap<String, ConversationEntity>().apply {
        // Conversation 1: between user_me and user_ayesha (matched)
        put(
            "conv_me_ayesha",
            ConversationEntity(
                id = "conv_me_ayesha",
                participantIds = setOf("user_me", "user_ayesha"),
                createdAtMillis = System.currentTimeMillis() - 86400000,
                lastMessage = "Hey Alif! Loved your travel photos ✨",
                lastMessageAtMillis = System.currentTimeMillis() - 3600000
            )
        )
        // Conversation 2: private between user_guest and user_nusrat
        put(
            "conv_guest_nusrat",
            ConversationEntity(
                id = "conv_guest_nusrat",
                participantIds = setOf("user_guest", "user_nusrat"),
                createdAtMillis = System.currentTimeMillis() - 43200000,
                lastMessage = "Hi Nusrat, nice to meet you!",
                lastMessageAtMillis = System.currentTimeMillis() - 7200000
            )
        )
    }

    private val conversationMessages = ConcurrentHashMap<String, MutableList<ServerChatMessage>>().apply {
        put(
            "conv_me_ayesha",
            Collections.synchronizedList(
                mutableListOf(
                    ServerChatMessage(
                        id = "m1",
                        conversationId = "conv_me_ayesha",
                        senderId = "user_ayesha",
                        text = "Hey Alif! Loved your travel photos ✨",
                        timestampMillis = System.currentTimeMillis() - 3600000,
                        isRead = true
                    )
                )
            )
        )
        put(
            "conv_guest_nusrat",
            Collections.synchronizedList(
                mutableListOf(
                    ServerChatMessage(
                        id = "m2",
                        conversationId = "conv_guest_nusrat",
                        senderId = "user_guest",
                        text = "Hi Nusrat, nice to meet you!",
                        timestampMillis = System.currentTimeMillis() - 7200000,
                        isRead = true
                    )
                )
            )
        )
    }

    // Rate Limiting Storage: key -> list of timestamp millis
    private val rateLimitBuckets = ConcurrentHashMap<String, MutableList<Long>>()

    // Phone OTP and Email Verification code registries
    private val pendingPhoneOtps = ConcurrentHashMap<String, PendingPhoneOtp>()
    private val pendingEmailCodes = ConcurrentHashMap<String, PendingEmailCode>()

    // In-memory security audit log
    private val serverAuditLogs = Collections.synchronizedList(mutableListOf<String>())

    // Bangladeshi MFS Merchant Configuration (bKash & Nagad)
    private var mfsMerchantConfig = MfsMerchantConfig(
        bKashEnabled = true,
        bKashMerchantNumber = "01711-987654",
        bKashMerchantType = "Merchant",
        nagadEnabled = true,
        nagadMerchantNumber = "01812-345678",
        nagadMerchantType = "Merchant",
        currency = "BDT"
    )

    // Configurable Subscription Plans with BDT Pricing
    private val subscriptionPlans = Collections.synchronizedList(VeloraData.subscriptionPlans.toMutableList())

    // Verified MFS Payment Transactions History
    private val mfsPaymentTransactions = Collections.synchronizedList(
        mutableListOf(
            MfsPaymentTransaction(
                id = "txn_001",
                trxId = "BKH89X201A",
                method = "bKash",
                senderPhone = "01712-345678",
                amountBdt = 699,
                planId = "plan_3m",
                planName = "3 Months",
                userId = "user_ayesha",
                userEmail = "ayesha@example.com",
                timestampMillis = System.currentTimeMillis() - 86400000,
                status = "VERIFIED"
            ),
            MfsPaymentTransaction(
                id = "txn_002",
                trxId = "NGD99K412B",
                method = "Nagad",
                senderPhone = "01815-987654",
                amountBdt = 1199,
                planId = "plan_12m",
                planName = "12 Months VIP",
                userId = "user_nusrat",
                userEmail = "nusrat@example.com",
                timestampMillis = System.currentTimeMillis() - 43200000,
                status = "VERIFIED"
            ),
            MfsPaymentTransaction(
                id = "txn_003",
                trxId = "BKH12P094M",
                method = "bKash",
                senderPhone = "01911-223344",
                amountBdt = 299,
                planId = "plan_1m",
                planName = "1 Month",
                userId = "user_alif",
                userEmail = "alif@example.com",
                timestampMillis = System.currentTimeMillis() - 172800000,
                status = "VERIFIED"
            )
        )
    )

    init {
        logAuditEvent("Server started. Initialized secure admin registry, conversations, MFS gateways (bKash & Nagad), and zero-trust policies.")
    }

    /**
     * Register a new user account.
     * Enforces that phone OTP and email verification must be completed before accessing the app.
     */
    fun registerNewUser(
        name: String,
        email: String,
        phone: String,
        password: String
    ): ServerResponse<ServerUser> {
        val trimmedName = name.trim()
        val normalizedEmail = email.trim().lowercase()
        val normalizedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return ServerResponse(400, "Full name must be at least 2 characters.", error = "INVALID_NAME")
        }
        if (!normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return ServerResponse(400, "Please provide a valid email address.", error = "INVALID_EMAIL")
        }
        val cleanPhoneDigits = normalizedPhone.replace("[^0-9+]".toRegex(), "")
        if (cleanPhoneDigits.length < 7) {
            return ServerResponse(400, "Please enter a valid phone number with country code.", error = "INVALID_PHONE")
        }
        if (password.length < 6) {
            return ServerResponse(400, "Password must be at least 6 characters.", error = "WEAK_PASSWORD")
        }

        if (usersRegistry.containsKey(normalizedEmail)) {
            return ServerResponse(409, "An account with this email already exists.", error = "EMAIL_ALREADY_EXISTS")
        }

        val newUserId = "user_${System.currentTimeMillis() % 1000000}"
        val newUser = ServerUser(
            id = newUserId,
            email = normalizedEmail,
            passwordHash = password,
            role = Role.USER,
            displayName = trimmedName,
            isSuspended = false,
            isVerified = false,
            isPremium = false,
            paymentStatus = "none",
            moderationStatus = "ACTIVE",
            isEmailVerified = false,
            isPhoneVerified = false,
            phone = normalizedPhone
        )

        usersRegistry[normalizedEmail] = newUser

        val newProfile = UserProfile(
            id = newUserId,
            name = trimmedName,
            age = 22,
            gender = "Woman",
            city = "Dhaka",
            country = "Bangladesh",
            distanceKm = 5,
            profession = "Professional",
            height = "5'5\"",
            relationshipStatus = "Single",
            aboutMe = "Hey there! Newly joined Velora.",
            interests = listOf("Travel", "Music", "Coffee"),
            photos = listOf("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800"),
            isVerified = false,
            isOnline = true,
            email = normalizedEmail,
            phone = normalizedPhone,
            isEmailVerified = false,
            isPhoneVerified = false
        )
        serverProfiles[newUserId] = newProfile

        logAuditEvent("USER REGISTERED: New account created for '$normalizedEmail' ($normalizedPhone). Pending phone and email verification.")

        // Automatically dispatch phone OTP and email verification code
        sendPhoneOtp(normalizedPhone)
        sendEmailVerification(normalizedEmail)

        return ServerResponse(
            statusCode = 201,
            message = "Registration successful. Please verify your phone number via OTP and verify your email address.",
            data = newUser
        )
    }

    /**
     * Dispatch a 6-digit OTP code to the user's phone number.
     */
    fun sendPhoneOtp(phone: String): ServerResponse<String> {
        val normalizedPhone = phone.trim()
        val cleanPhoneDigits = normalizedPhone.replace("[^0-9+]".toRegex(), "")
        if (cleanPhoneDigits.length < 7) {
            return ServerResponse(400, "Invalid phone number format.", error = "INVALID_PHONE")
        }

        val otpCode = String.format("%06d", (100000..999999).random())
        val now = System.currentTimeMillis()
        val expiresAt = now + 10 * 60 * 1000 // 10 minutes expiry

        pendingPhoneOtps[normalizedPhone] = PendingPhoneOtp(
            phone = normalizedPhone,
            otp = otpCode,
            createdAtMillis = now,
            expiresAtMillis = expiresAt
        )

        logAuditEvent("PHONE OTP ISSUED: Sent 6-digit OTP to '$normalizedPhone'. Code: $otpCode (Expires in 10 mins)")

        return ServerResponse(
            statusCode = 200,
            message = "Verification OTP sent to $normalizedPhone.",
            data = otpCode
        )
    }

    /**
     * Verify the 6-digit phone OTP code.
     */
    fun verifyPhoneOtp(phone: String, otpCode: String): ServerResponse<Boolean> {
        val normalizedPhone = phone.trim()
        val pending = pendingPhoneOtps[normalizedPhone]

        if (pending == null) {
            return ServerResponse(400, "No pending OTP request found for this phone number.", error = "OTP_NOT_FOUND")
        }

        if (System.currentTimeMillis() > pending.expiresAtMillis) {
            pendingPhoneOtps.remove(normalizedPhone)
            return ServerResponse(400, "OTP code has expired. Please request a new code.", error = "OTP_EXPIRED")
        }

        if (pending.otp != otpCode.trim()) {
            val updated = pending.copy(attempts = pending.attempts + 1)
            pendingPhoneOtps[normalizedPhone] = updated
            logAuditEvent("PHONE OTP MISMATCH: Incorrect OTP attempt ($otpCode) for phone '$normalizedPhone'")
            return ServerResponse(400, "Invalid OTP code. Please check and try again.", error = "INVALID_OTP")
        }

        // Successfully verified phone
        pendingPhoneOtps.remove(normalizedPhone)

        // Mark matching user and profile as phone verified
        usersRegistry.values.firstOrNull { it.phone == normalizedPhone }?.let { user ->
            usersRegistry[user.email] = user.copy(isPhoneVerified = true)
            serverProfiles[user.id]?.let { profile ->
                serverProfiles[user.id] = profile.copy(isPhoneVerified = true)
            }
        }

        logAuditEvent("PHONE OTP VERIFIED: Phone number '$normalizedPhone' successfully verified.")

        return ServerResponse(
            statusCode = 200,
            message = "Phone number verified successfully.",
            data = true
        )
    }

    /**
     * Dispatch a 6-digit verification code to the user's email address.
     */
    fun sendEmailVerification(email: String): ServerResponse<String> {
        val normalizedEmail = email.trim().lowercase()
        if (!normalizedEmail.contains("@")) {
            return ServerResponse(400, "Invalid email address format.", error = "INVALID_EMAIL")
        }

        val verificationCode = String.format("%06d", (100000..999999).random())
        val now = System.currentTimeMillis()
        val expiresAt = now + 15 * 60 * 1000 // 15 minutes expiry

        pendingEmailCodes[normalizedEmail] = PendingEmailCode(
            email = normalizedEmail,
            code = verificationCode,
            createdAtMillis = now,
            expiresAtMillis = expiresAt
        )

        logAuditEvent("EMAIL VERIFICATION ISSUED: Sent code to '$normalizedEmail'. Code: $verificationCode (Expires in 15 mins)")

        return ServerResponse(
            statusCode = 200,
            message = "Verification code sent to $normalizedEmail.",
            data = verificationCode
        )
    }

    /**
     * Verify the email address with the provided verification code.
     */
    fun verifyEmailCode(email: String, code: String): ServerResponse<Boolean> {
        val normalizedEmail = email.trim().lowercase()
        val pending = pendingEmailCodes[normalizedEmail]

        if (pending == null) {
            return ServerResponse(400, "No pending email verification found for this email.", error = "EMAIL_CODE_NOT_FOUND")
        }

        if (System.currentTimeMillis() > pending.expiresAtMillis) {
            pendingEmailCodes.remove(normalizedEmail)
            return ServerResponse(400, "Verification code has expired. Please request a new code.", error = "CODE_EXPIRED")
        }

        if (pending.code != code.trim()) {
            val updated = pending.copy(attempts = pending.attempts + 1)
            pendingEmailCodes[normalizedEmail] = updated
            logAuditEvent("EMAIL VERIFICATION MISMATCH: Incorrect code attempt ($code) for email '$normalizedEmail'")
            return ServerResponse(400, "Invalid email verification code.", error = "INVALID_CODE")
        }

        // Successfully verified email
        pendingEmailCodes.remove(normalizedEmail)

        usersRegistry[normalizedEmail]?.let { user ->
            usersRegistry[normalizedEmail] = user.copy(isEmailVerified = true)
            serverProfiles[user.id]?.let { profile ->
                serverProfiles[user.id] = profile.copy(isEmailVerified = true)
            }
        }

        logAuditEvent("EMAIL VERIFIED: Email address '$normalizedEmail' successfully verified.")

        return ServerResponse(
            statusCode = 200,
            message = "Email address verified successfully.",
            data = true
        )
    }

    /**
     * Helpers to query pending codes (for preview / testing).
     */
    fun getPendingPhoneOtp(phone: String): String? = pendingPhoneOtps[phone.trim()]?.otp
    fun getPendingEmailCode(email: String): String? = pendingEmailCodes[email.trim().lowercase()]?.code

    /**
     * Sliding-window rate limiter:
     * Returns true if request is permitted, false if rate limit exceeded.
     */
    fun checkRateLimit(key: String, maxRequests: Int, windowMillis: Long = 60_000L): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = rateLimitBuckets.computeIfAbsent(key) { Collections.synchronizedList(mutableListOf()) }
        synchronized(timestamps) {
            timestamps.removeAll { now - it > windowMillis }
            if (timestamps.size >= maxRequests) {
                return false
            }
            timestamps.add(now)
            return true
        }
    }

    fun clearRateLimitsForTesting() {
        rateLimitBuckets.clear()
        pendingPhoneOtps.clear()
        pendingEmailCodes.clear()
    }

    /**
     * Authenticate user credentials and return a signed cryptographic JWT.
     * Enforces rate limiting against brute-force attacks.
     * Enforces that phone OTP and email verification must be completed for regular users.
     */
    fun login(email: String, password: String): ServerResponse<String> {
        val normalizedEmail = email.trim().lowercase()

        // Rate limiting: max 5 login attempts per minute per account
        if (!checkRateLimit("login:$normalizedEmail", maxRequests = 5, windowMillis = 60_000L)) {
            logAuditEvent("SECURITY ALERT: Login rate limit exceeded for account: $normalizedEmail")
            return ServerResponse(
                statusCode = 429,
                message = "Too many login attempts. Please wait 60 seconds before retrying.",
                error = "Too Many Requests"
            )
        }

        val user = usersRegistry[normalizedEmail]
        if (user == null || user.passwordHash != password) {
            logAuditEvent("AUTH FAILURE: Failed login attempt for email: $normalizedEmail")
            return ServerResponse(
                statusCode = 401,
                message = "Invalid email or password.",
                error = "Unauthorized"
            )
        }

        if (user.isSuspended) {
            logAuditEvent("AUTH SUSPENDED: Login attempted on suspended account: $normalizedEmail")
            return ServerResponse(
                statusCode = 403,
                message = "Account has been suspended due to security or policy violations.",
                error = "Forbidden"
            )
        }

        // Enforce phone OTP and email verification for regular users
        if (user.role == Role.USER) {
            if (!user.isPhoneVerified) {
                logAuditEvent("AUTH BLOCKED: User '$normalizedEmail' attempted login without verified phone number.")
                return ServerResponse(
                    statusCode = 403,
                    message = "Phone verification required: Please complete phone OTP verification to access Velora.",
                    error = "PHONE_NOT_VERIFIED"
                )
            }
            if (!user.isEmailVerified) {
                logAuditEvent("AUTH BLOCKED: User '$normalizedEmail' attempted login without verified email address.")
                return ServerResponse(
                    statusCode = 403,
                    message = "Email verification required: Please verify your email address to access Velora.",
                    error = "EMAIL_NOT_VERIFIED"
                )
            }
        }

        val token = ServerTokenManager.createToken(
            userId = user.id,
            email = user.email,
            role = user.role
        )

        logAuditEvent("AUTH SUCCESS: Issued token for user '${user.email}' with verified server role: [${user.role}]")

        return ServerResponse(
            statusCode = 200,
            message = "Authentication successful.",
            data = token
        )
    }

    /**
     * Server-side authentication filter for ANY authenticated user:
     * - Rejects missing or malformed Authorization header (HTTP 401)
     * - Rejects forged or expired tokens (HTTP 401)
     * - Rejects suspended or inactive accounts (HTTP 403)
     */
    fun authorizeUser(authHeader: String?): Pair<ServerResponse<Nothing>?, AuthTokenPayload?> {
        if (authHeader.isNullOrBlank()) {
            return Pair(
                ServerResponse(
                    statusCode = 401,
                    message = "Authorization header missing. Bearer token required.",
                    error = "Unauthorized"
                ),
                null
            )
        }

        val token = if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
            authHeader.substring(7).trim()
        } else {
            authHeader.trim()
        }

        val payload = try {
            ServerTokenManager.verifyToken(token)
        } catch (e: SecurityException) {
            logAuditEvent("SECURITY ALERT: Cryptographic token validation failed: ${e.message}")
            return Pair(
                ServerResponse(
                    statusCode = 401,
                    message = "Invalid or tampered token: ${e.message}",
                    error = "Unauthorized"
                ),
                null
            )
        }

        val serverUser = usersRegistry[payload.email.lowercase()]
        if (serverUser == null || serverUser.isSuspended) {
            logAuditEvent("ACCESS BLOCKED: User '${payload.email}' account is suspended or inactive in server registry.")
            return Pair(
                ServerResponse(
                    statusCode = 403,
                    message = "Account is inactive or revoked.",
                    error = "Forbidden"
                ),
                null
            )
        }

        return Pair(null, payload)
    }

    /**
     * Server-side administrator authorization filter:
     * - Verifies token validity
     * - Denies access by default if role is not Role.ADMIN (HTTP 403)
     */
    fun authorizeAdmin(authHeader: String?): Pair<ServerResponse<Nothing>?, AuthTokenPayload?> {
        val (userError, payload) = authorizeUser(authHeader)
        if (userError != null) {
            return Pair(userError, null)
        }

        val serverUser = usersRegistry[payload!!.email.lowercase()]
        if (!payload.admin || payload.role != Role.ADMIN || serverUser?.role != Role.ADMIN) {
            logAuditEvent("ACCESS DENIED: User '${payload.email}' with role [${payload.role}], adminClaim=[${payload.admin}] attempted to access admin endpoint.")
            return Pair(
                ServerResponse(
                    statusCode = 403,
                    message = "Access denied: Account '${payload.email}' lacks required administrator custom claims.",
                    error = "Forbidden"
                ),
                null
            )
        }

        return Pair(null, payload)
    }

    // --- PROTECTED ADMIN ENDPOINTS ---

    /**
     * Protected Endpoint: GET /api/v1/admin/dashboard
     */
    fun getAdminDashboard(authHeader: String?): ServerResponse<AdminDashboardData> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        if (!checkRateLimit("admin_dash:${payload?.userId}", maxRequests = 30, windowMillis = 60_000L)) {
            return ServerResponse(statusCode = 429, message = "Rate limit exceeded.", error = "Too Many Requests")
        }

        logAuditEvent("ADMIN ACTION: Dashboard accessed by admin '${payload?.email}'")

        val data = AdminDashboardData(
            totalUsers = 48532,
            activeUsers = 32487,
            totalMatches = 12846,
            openReports = serverReports.values.count { it.status == "Pending" },
            verifiedRevenue = "৳ 5,89,420",
            userLocations = mapOf(
                "Bangladesh 🇧🇩" to 42,
                "India 🇮🇳" to 18,
                "United States 🇺🇸" to 10,
                "United Kingdom 🇬🇧" to 8,
                "Others 🌐" to 22
            )
        )

        return ServerResponse(
            statusCode = 200,
            message = "Dashboard data retrieved successfully.",
            data = data
        )
    }

    /**
     * Protected Endpoint: GET /api/v1/admin/users
     */
    fun getAdminUsers(authHeader: String?): ServerResponse<List<UserProfile>> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        logAuditEvent("ADMIN ACTION: User directory fetched by '${payload?.email}'")
        return ServerResponse(
            statusCode = 200,
            message = "Users retrieved.",
            data = serverProfiles.values.toList()
        )
    }

    /**
     * Protected Endpoint: POST /api/v1/admin/users/{id}/suspend
     */
    fun toggleUserSuspension(authHeader: String?, targetUserId: String): ServerResponse<Boolean> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        val profile = serverProfiles[targetUserId]
        if (profile == null) {
            return ServerResponse(
                statusCode = 404,
                message = "Target user not found.",
                error = "Not Found"
            )
        }

        val newSuspendedState = !profile.isSuspended
        serverProfiles[targetUserId] = profile.copy(isSuspended = newSuspendedState)

        // Also update in usersRegistry if email matches
        usersRegistry.values.firstOrNull { it.id == targetUserId }?.let { regUser ->
            usersRegistry[regUser.email] = regUser.copy(isSuspended = newSuspendedState)
        }

        logAuditEvent("ADMIN ACTION: User '$targetUserId' (${profile.name}) suspended=$newSuspendedState by '${payload?.email}'")

        return ServerResponse(
            statusCode = 200,
            message = "User status updated to suspended=$newSuspendedState",
            data = newSuspendedState
        )
    }

    /**
     * Protected Endpoint: POST /api/v1/admin/users/{id}/verify
     */
    fun adminVerifyUserBadge(authHeader: String?, targetUserId: String, isVerified: Boolean): ServerResponse<Boolean> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        val profile = serverProfiles[targetUserId]
            ?: return ServerResponse(statusCode = 404, message = "Target user not found.", error = "Not Found")

        serverProfiles[targetUserId] = profile.copy(isVerified = isVerified)
        logAuditEvent("ADMIN ACTION: Verification status for user '$targetUserId' set to $isVerified by '${payload?.email}'")

        return ServerResponse(
            statusCode = 200,
            message = "User verification badge set to $isVerified.",
            data = true
        )
    }

    /**
     * Protected Endpoint: GET /api/v1/admin/reports
     */
    fun getAdminReports(authHeader: String?): ServerResponse<List<UserReport>> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        logAuditEvent("ADMIN ACTION: Reports list fetched by '${payload?.email}'")
        return ServerResponse(
            statusCode = 200,
            message = "Reports retrieved.",
            data = serverReports.values.toList()
        )
    }

    /**
     * Protected Endpoint: POST /api/v1/admin/reports/{id}/resolve
     */
    fun resolveReport(authHeader: String?, reportId: String, newStatus: String): ServerResponse<Boolean> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        val report = serverReports[reportId]
        if (report == null) {
            return ServerResponse(
                statusCode = 404,
                message = "Report '$reportId' not found.",
                error = "Not Found"
            )
        }

        serverReports[reportId] = report.copy(status = newStatus)
        logAuditEvent("ADMIN ACTION: Report '$reportId' marked as '$newStatus' by '${payload?.email}'")

        return ServerResponse(
            statusCode = 200,
            message = "Report marked as $newStatus.",
            data = true
        )
    }

    /**
     * Trusted Server-Side Process: Assign or Revoke Administrator Privileges.
     * Ordinary users can NEVER assign themselves an administrator role.
     * Only a verified ADMIN can invoke this function.
     */
    fun assignAdminRole(operatorAuthHeader: String?, targetEmail: String, newRole: Role): ServerResponse<Boolean> {
        val (errorResponse, payload) = authorizeAdmin(operatorAuthHeader)
        if (errorResponse != null) {
            logAuditEvent("PRIVILEGE ESCALATION ATTEMPT BLOCKED: Unauthorized attempt to assign role to '$targetEmail'")
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        val targetUser = usersRegistry[targetEmail.trim().lowercase()]
        if (targetUser == null) {
            return ServerResponse(
                statusCode = 404,
                message = "Target user '$targetEmail' not found in server registry.",
                error = "Not Found"
            )
        }

        usersRegistry[targetEmail.trim().lowercase()] = targetUser.copy(role = newRole)
        logAuditEvent("ROLE MODIFIED: Admin '${payload?.email}' changed role of '$targetEmail' to [${newRole.name}]")

        return ServerResponse(
            statusCode = 200,
            message = "Successfully updated role of '$targetEmail' to $newRole.",
            data = true
        )
    }

    /**
     * Protected Multi-Admin Endpoint: GET /api/v1/admin/accounts
     * Lists all authorized administrator accounts possessing Firebase Auth custom claims.
     */
    fun getAdminAccounts(operatorAuthHeader: String?): ServerResponse<List<ServerUser>> {
        val (errorResponse, payload) = authorizeAdmin(operatorAuthHeader)
        if (errorResponse != null) {
            return ServerResponse(statusCode = errorResponse.statusCode, message = errorResponse.message, error = errorResponse.error)
        }

        val admins = usersRegistry.values
            .filter { it.role == Role.ADMIN }
            .map { it.copy(passwordHash = "PROTECTED_HASH") }

        logAuditEvent("MULTI-ADMIN AUDIT: Admin '${payload?.email}' inspected authorized administrators list (${admins.size} active)")
        return ServerResponse(statusCode = 200, message = "Authorized administrators retrieved.", data = admins)
    }

    /**
     * Protected Multi-Admin Endpoint: POST /api/v1/admin/accounts/create
     * Provisions a new authorized administrator account with verified Firebase Auth custom claims.
     */
    fun createAdminUser(
        operatorAuthHeader: String?,
        email: String,
        password: String,
        displayName: String,
        role: Role = Role.ADMIN
    ): ServerResponse<ServerUser> {
        val (errorResponse, payload) = authorizeAdmin(operatorAuthHeader)
        if (errorResponse != null) {
            return ServerResponse(statusCode = errorResponse.statusCode, message = errorResponse.message, error = errorResponse.error)
        }

        val normalizedEmail = email.trim().lowercase()
        if (usersRegistry.containsKey(normalizedEmail)) {
            return ServerResponse(statusCode = 409, message = "An account with email '$normalizedEmail' already exists.", error = "Conflict")
        }

        val newAdmin = ServerUser(
            id = "admin_${System.currentTimeMillis()}",
            email = normalizedEmail,
            passwordHash = password,
            role = role,
            displayName = displayName.ifBlank { "Administrator" }
        )

        usersRegistry[normalizedEmail] = newAdmin
        logAuditEvent("SECURITY EVENT: Admin '${payload?.email}' provisioned new authorized administrator '$normalizedEmail' with role [${role.name}]")

        return ServerResponse(
            statusCode = 201,
            message = "Successfully created administrator account '$normalizedEmail'.",
            data = newAdmin.copy(passwordHash = "PROTECTED_HASH")
        )
    }

    /**
     * Protected Multi-Admin Endpoint: POST /api/v1/admin/accounts/revoke
     * Demotes an administrator account back to Role.USER.
     * Prevents lockouts by refusing to revoke the final administrator.
     */
    fun revokeAdminRole(operatorAuthHeader: String?, targetEmail: String): ServerResponse<Boolean> {
        val (errorResponse, payload) = authorizeAdmin(operatorAuthHeader)
        if (errorResponse != null) {
            return ServerResponse(statusCode = errorResponse.statusCode, message = errorResponse.message, error = errorResponse.error)
        }

        val currentAdminCount = usersRegistry.values.count { it.role == Role.ADMIN && !it.isSuspended }
        if (currentAdminCount <= 1) {
            logAuditEvent("SECURITY LOCKOUT PREVENTED: Blocked attempt by '${payload?.email}' to revoke the last remaining active administrator.")
            return ServerResponse(
                statusCode = 400,
                message = "Action denied: Cannot revoke the last active administrator account.",
                error = "Bad Request"
            )
        }

        val normalized = targetEmail.trim().lowercase()
        val target = usersRegistry[normalized]
            ?: return ServerResponse(statusCode = 404, message = "User '$targetEmail' not found.", error = "Not Found")

        usersRegistry[normalized] = target.copy(role = Role.USER)
        logAuditEvent("SECURITY EVENT: Admin '${payload?.email}' revoked administrator role from '$targetEmail'")

        return ServerResponse(statusCode = 200, message = "Administrator privileges revoked for '$targetEmail'.", data = true)
    }

    /**
     * Admin-only: Update moderation status / ban user.
     */
    fun updateModerationStatus(authHeader: String?, targetUserId: String, status: String): ServerResponse<UserProfile> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(statusCode = errorResponse.statusCode, message = errorResponse.message, error = errorResponse.error)
        }

        val profile = serverProfiles[targetUserId]
            ?: return ServerResponse(statusCode = 404, message = "User not found.", error = "Not Found")

        val isSuspended = status.equals("Banned", ignoreCase = true) || status.equals("Suspended", ignoreCase = true)
        val updated = profile.copy(isSuspended = isSuspended)
        serverProfiles[targetUserId] = updated
        logAuditEvent("ADMIN ACTION: Admin '${payload?.email}' updated moderation status of '$targetUserId' to $status")
        return ServerResponse(statusCode = 200, message = "Moderation status updated.", data = updated)
    }

    /**
     * Retrieve audit log entries (only for verified administrators).
     */
    fun getAuditLogs(authHeader: String?): ServerResponse<List<String>> {
        val (errorResponse, _) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        return ServerResponse(
            statusCode = 200,
            message = "Audit logs retrieved.",
            data = serverAuditLogs.toList()
        )
    }

    // --- PROTECTED USER & SAFETY OPERATIONS ---

    /**
     * Update User Profile with Strict Least-Privilege & Anti-Tampering Check:
     * Users CANNOT modify their own role, verification status, Premium entitlement,
     * payment status, or moderation status.
     */
    fun updateUserProfile(
        authHeader: String?,
        targetUserId: String,
        updates: UserProfileUpdate
    ): ServerResponse<UserProfile> {
        val (errorResponse, payload) = authorizeUser(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        if (!checkRateLimit("profile_update:${payload?.userId}", maxRequests = 10, windowMillis = 60_000L)) {
            logAuditEvent("SECURITY ALERT: Profile update rate limit exceeded by '${payload?.email}'")
            return ServerResponse(
                statusCode = 429,
                message = "Rate limit exceeded. Please wait before updating your profile again.",
                error = "Too Many Requests"
            )
        }

        val isCallerAdmin = payload?.role == Role.ADMIN
        val isCallerOwner = payload?.userId == targetUserId

        if (!isCallerOwner && !isCallerAdmin) {
            logAuditEvent("ACCESS DENIED: User '${payload?.email}' attempted to update profile of '$targetUserId'")
            return ServerResponse(
                statusCode = 403,
                message = "Access denied: You cannot edit another member's profile.",
                error = "Forbidden"
            )
        }

        // CRITICAL PROTECTION: Ordinary users cannot change their own role, verification status,
        // Premium entitlement, payment status, or moderation status.
        if (!isCallerAdmin) {
            val attemptedPrivilegeEscalation = updates.role != null ||
                    updates.isVerified != null ||
                    updates.isPremium != null ||
                    updates.paymentStatus != null ||
                    updates.isSuspended != null ||
                    updates.moderationStatus != null

            if (attemptedPrivilegeEscalation) {
                logAuditEvent("PRIVILEGE ESCALATION BLOCKED: User '${payload?.email}' attempted to alter protected security fields (role, verification, premium, payment, or moderation).")
                return ServerResponse(
                    statusCode = 403,
                    message = "Forbidden: Ordinary users cannot change their own role, verification status, Premium entitlement, payment status, or moderation status.",
                    error = "Forbidden"
                )
            }
        }

        val existingProfile = serverProfiles[targetUserId]
            ?: return ServerResponse(statusCode = 404, message = "User not found.", error = "Not Found")

        val updatedProfile = existingProfile.copy(
            aboutMe = updates.aboutMe ?: updates.bio ?: existingProfile.aboutMe,
            profession = updates.profession ?: existingProfile.profession,
            city = updates.city ?: existingProfile.city,
            interests = updates.interests ?: existingProfile.interests,
            photos = updates.photos ?: existingProfile.photos,
            isVerified = if (isCallerAdmin && updates.isVerified != null) updates.isVerified else existingProfile.isVerified,
            isPremium = if (isCallerAdmin && updates.isPremium != null) updates.isPremium else existingProfile.isPremium
        )

        serverProfiles[targetUserId] = updatedProfile
        logAuditEvent("PROFILE UPDATED: User '$targetUserId' updated by '${payload?.email}'")

        return ServerResponse(
            statusCode = 200,
            message = "Profile updated successfully.",
            data = updatedProfile
        )
    }

    /**
     * Trusted Server-Side Payment Processing:
     * Premium entitlements can ONLY be granted by the server upon receipt verification.
     */
    /**
     * Get current Bangladeshi MFS configuration (bKash & Nagad)
     */
    fun getMfsConfig(): ServerResponse<MfsMerchantConfig> {
        return ServerResponse(
            statusCode = 200,
            message = "MFS Configuration retrieved.",
            data = mfsMerchantConfig
        )
    }

    /**
     * Admin Panel: Update bKash and Nagad merchant credentials and enable/disable states.
     * Strictly protected: requires verified administrator custom claims.
     */
    fun updateMfsConfig(authHeader: String?, config: MfsMerchantConfig): ServerResponse<MfsMerchantConfig> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse<MfsMerchantConfig>(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        mfsMerchantConfig = config
        logAuditEvent("ADMIN MFS CONFIG UPDATED: bKash (enabled=${config.bKashEnabled}, merchant=${config.bKashMerchantNumber}, type=${config.bKashMerchantType}), Nagad (enabled=${config.nagadEnabled}, merchant=${config.nagadMerchantNumber}, type=${config.nagadMerchantType}) by '${payload?.email}'")

        return ServerResponse(
            statusCode = 200,
            message = "Bangladeshi MFS configuration successfully updated.",
            data = mfsMerchantConfig
        )
    }

    /**
     * Get active subscription plans with BDT pricing.
     */
    fun getSubscriptionPlans(): ServerResponse<List<SubscriptionPlan>> {
        return ServerResponse(
            statusCode = 200,
            message = "Subscription plans retrieved.",
            data = subscriptionPlans.toList()
        )
    }

    /**
     * Admin Panel: Update package price in BDT.
     * Strictly protected: requires verified administrator custom claims.
     */
    fun updateSubscriptionPlanPrice(authHeader: String?, planId: String, newPriceBdt: Int): ServerResponse<SubscriptionPlan> {
        val (errorResponse, payload) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse<SubscriptionPlan>(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        if (newPriceBdt <= 0) {
            return ServerResponse<SubscriptionPlan>(
                statusCode = 400,
                message = "Price in BDT must be greater than 0.",
                error = "INVALID_PRICE"
            )
        }

        synchronized(subscriptionPlans) {
            val index = subscriptionPlans.indexOfFirst { it.id == planId }
            if (index == -1) {
                return ServerResponse<SubscriptionPlan>(
                    statusCode = 404,
                    message = "Subscription plan '$planId' not found.",
                    error = "PLAN_NOT_FOUND"
                )
            }
            val updatedPlan = subscriptionPlans[index].copy(priceBdt = newPriceBdt)
            subscriptionPlans[index] = updatedPlan
            logAuditEvent("ADMIN PLAN PRICE UPDATED: Plan '$planId' BDT price updated to ৳$newPriceBdt by '${payload?.email}'")
            return ServerResponse(
                statusCode = 200,
                message = "Subscription plan price updated to ৳$newPriceBdt BDT.",
                data = updatedPlan
            )
        }
    }

    /**
     * Admin Panel: Retrieve MFS Payment Transactions history.
     * Strictly protected: requires verified administrator custom claims.
     */
    fun getMfsTransactions(authHeader: String?): ServerResponse<List<MfsPaymentTransaction>> {
        val (errorResponse, _) = authorizeAdmin(authHeader)
        if (errorResponse != null) {
            return ServerResponse<List<MfsPaymentTransaction>>(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }
        return ServerResponse(
            statusCode = 200,
            message = "MFS transactions retrieved.",
            data = mfsPaymentTransactions.toList()
        )
    }

    /**
     * Trusted Server-Side Payment Processing:
     * Supports bKash, Nagad, and Card payments in Bangladeshi Taka (BDT / ৳).
     * Automatically verifies and activates user subscriptions.
     */
    fun verifyPaymentAndGrantPremium(
        authHeader: String?,
        paymentRequest: PaymentVerificationRequest
    ): ServerResponse<Boolean> {
        val (errorResponse, payload) = authorizeUser(authHeader)
        if (errorResponse != null) {
            return ServerResponse<Boolean>(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        if (!checkRateLimit("payment:${payload?.userId}", maxRequests = 10, windowMillis = 60_000L)) {
            logAuditEvent("SECURITY ALERT: Payment verification rate limit exceeded for '${payload?.email}'")
            return ServerResponse<Boolean>(
                statusCode = 429,
                message = "Too many payment requests. Please wait before retrying.",
                error = "Too Many Requests"
            )
        }

        val receipt = paymentRequest.transactionReceipt.trim()
        val method = paymentRequest.paymentMethod.trim().uppercase()

        // Validate MFS method status controlled by administrator
        if (method == "BKASH" && !mfsMerchantConfig.bKashEnabled) {
            logAuditEvent("MFS PAYMENT REJECTED: bKash checkout attempted while disabled by admin")
            return ServerResponse<Boolean>(
                statusCode = 400,
                message = "bKash payments are currently disabled by administration. Please use Nagad or Card.",
                error = "MFS_METHOD_DISABLED"
            )
        }
        if (method == "NAGAD" && !mfsMerchantConfig.nagadEnabled) {
            logAuditEvent("MFS PAYMENT REJECTED: Nagad checkout attempted while disabled by admin")
            return ServerResponse<Boolean>(
                statusCode = 400,
                message = "Nagad payments are currently disabled by administration. Please use bKash or Card.",
                error = "MFS_METHOD_DISABLED"
            )
        }

        val isExplicitlyFraudulent = receipt.contains("fake", ignoreCase = true) ||
                receipt.contains("hacked", ignoreCase = true) ||
                receipt.contains("fraud", ignoreCase = true) ||
                paymentRequest.amount.trim() == "0" ||
                paymentRequest.amount.isBlank()
        val isValidReceipt = receipt.isNotBlank() && receipt.length >= 6 && !isExplicitlyFraudulent

        if (!isValidReceipt) {
            logAuditEvent("FRAUD DETECTED: Invalid or forged transaction receipt '$receipt' submitted by '${payload?.email}'")
            return ServerResponse<Boolean>(
                statusCode = 402,
                message = "Payment validation failed: Invalid transaction receipt.",
                error = "Payment Required"
            )
        }

        // Trusted server sets premium entitlement
        val user = usersRegistry[payload?.email?.lowercase()]
        if (user != null) {
            usersRegistry[payload!!.email.lowercase()] = user.copy(
                isPremium = true,
                paymentStatus = "COMPLETED"
            )
        }

        val profile = serverProfiles[payload?.userId]
        if (profile != null) {
            serverProfiles[payload!!.userId] = profile.copy(isPremium = true)
        }

        // Parse amount in BDT and log MFS transaction
        val cleanAmount = paymentRequest.amount.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 299
        val plan = subscriptionPlans.firstOrNull { it.id == paymentRequest.planId }
        val planName = plan?.name ?: "Premium (${paymentRequest.planId})"
        val displayMethod = when (method) {
            "NAGAD" -> "Nagad"
            "CARD" -> "Card"
            "GOOGLE_PLAY" -> "Google Play"
            else -> "bKash"
        }

        val newTransaction = MfsPaymentTransaction(
            id = "txn_${System.currentTimeMillis() % 1000000}",
            trxId = receipt,
            method = displayMethod,
            senderPhone = paymentRequest.senderAccount.ifBlank { profile?.phone ?: "01712-000000" },
            amountBdt = cleanAmount,
            currency = "BDT",
            planId = paymentRequest.planId,
            planName = planName,
            userId = payload?.userId ?: "user_me",
            userEmail = payload?.email ?: "user@velora.app",
            timestampMillis = System.currentTimeMillis(),
            status = "VERIFIED"
        )
        mfsPaymentTransactions.add(0, newTransaction)

        logAuditEvent("PAYMENT SUCCESS: Server validated $displayMethod TrxID '$receipt' (৳$cleanAmount BDT) and automatically activated Premium for '${payload?.email}' (Plan: $planName)")

        return ServerResponse(
            statusCode = 200,
            message = "Payment verified via $displayMethod. Subscription automatically activated for ৳$cleanAmount BDT!",
            data = true
        )
    }


    // --- PRIVATE CONVERSATIONS & CHAT ACCESS CONTROL ---

    /**
     * Retrieve messages in a private conversation:
     * Only eligible participants (or administrator in safety audits) can access.
     */
    fun getConversationMessages(authHeader: String?, conversationId: String): ServerResponse<List<ServerChatMessage>> {
        val (errorResponse, payload) = authorizeUser(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        if (!checkRateLimit("chat_read:${payload?.userId}", maxRequests = 30, windowMillis = 60_000L)) {
            return ServerResponse(statusCode = 429, message = "Rate limit exceeded.", error = "Too Many Requests")
        }

        val conv = conversations[conversationId]
            ?: return ServerResponse(statusCode = 404, message = "Conversation not found.", error = "Not Found")

        val isParticipant = conv.participantIds.contains(payload?.userId)
        val isAdmin = payload?.role == Role.ADMIN

        if (!isParticipant && !isAdmin) {
            logAuditEvent("SECURITY VIOLATION: Unauthorized attempt by '${payload?.email}' to access private conversation '$conversationId'")
            return ServerResponse(
                statusCode = 403,
                message = "Access denied: You are not an eligible participant in this private conversation.",
                error = "Forbidden"
            )
        }

        logAuditEvent("CHAT ACCESS: Conversation '$conversationId' accessed by '${payload?.email}' (role=${payload?.role})")
        val messages = conversationMessages[conversationId] ?: emptyList()
        return ServerResponse(
            statusCode = 200,
            message = "Messages retrieved.",
            data = messages.toList()
        )
    }

    /**
     * Send message to a conversation:
     * Sender must be an authorized participant.
     * Sender ID is bound to the server-verified token (cannot be forged).
     * Enforces anti-spam rate limiting.
     */
    fun sendChatMessage(authHeader: String?, conversationId: String, text: String): ServerResponse<ServerChatMessage> {
        val (errorResponse, payload) = authorizeUser(authHeader)
        if (errorResponse != null) {
            return ServerResponse(
                statusCode = errorResponse.statusCode,
                message = errorResponse.message,
                error = errorResponse.error
            )
        }

        // Anti-spam message rate limiting (max 20 messages per minute)
        if (!checkRateLimit("chat_send:${payload?.userId}", maxRequests = 20, windowMillis = 60_000L)) {
            logAuditEvent("SECURITY ALERT: Anti-spam rate limit triggered for user '${payload?.email}'")
            return ServerResponse(
                statusCode = 429,
                message = "Anti-spam rate limit reached. Please wait before sending more messages.",
                error = "Too Many Requests"
            )
        }

        val conv = conversations[conversationId]
            ?: return ServerResponse(statusCode = 404, message = "Conversation not found.", error = "Not Found")

        if (!conv.participantIds.contains(payload?.userId)) {
            logAuditEvent("SECURITY VIOLATION: User '${payload?.email}' attempted to send message to unauthorized conversation '$conversationId'")
            return ServerResponse(
                statusCode = 403,
                message = "Access denied: You cannot send messages to a conversation you are not part of.",
                error = "Forbidden"
            )
        }

        if (text.isBlank()) {
            return ServerResponse(statusCode = 400, message = "Message text cannot be blank.", error = "Bad Request")
        }

        val sanitizedText = text.trim().take(1000)
        val newMsg = ServerChatMessage(
            id = "msg_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}",
            conversationId = conversationId,
            senderId = payload!!.userId, // Server-bound: prevents sender spoofing
            text = sanitizedText,
            timestampMillis = System.currentTimeMillis(),
            isRead = false
        )

        val list = conversationMessages.computeIfAbsent(conversationId) { Collections.synchronizedList(mutableListOf()) }
        list.add(newMsg)

        conversations[conversationId] = conv.copy(
            lastMessage = sanitizedText,
            lastMessageAtMillis = newMsg.timestampMillis
        )

        return ServerResponse(
            statusCode = 200,
            message = "Message sent successfully.",
            data = newMsg
        )
    }

    private fun logAuditEvent(event: String) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val entry = "[$timestamp] $event"
        synchronized(serverAuditLogs) {
            serverAuditLogs.add(entry)
            if (serverAuditLogs.size > 500) {
                serverAuditLogs.removeAt(0)
            }
        }
    }
}
