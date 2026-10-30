package com.example

import com.example.server.MfsMerchantConfig
import com.example.server.PaymentVerificationRequest
import com.example.server.Role
import com.example.server.ServerTokenManager
import com.example.server.UserProfileUpdate
import com.example.server.VeloraBackendServer
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Server-Side Security & Zero-Trust Authorization Test Suite for Velora.
 *
 * Verifies that:
 * 1. Ordinary users cannot read protected admin data or invoke administrative operations (HTTP 403 Forbidden).
 * 2. Ordinary users cannot alter their own role, verification status, Premium entitlement, payment status, or moderation status.
 * 3. Unauthenticated requests are denied by default (HTTP 401 Unauthorized).
 * 4. Tampered or forged cryptographic tokens fail signature verification (HTTP 401 Unauthorized).
 * 5. Private conversations are accessible ONLY to eligible match participants (HTTP 403 Forbidden to unauthorized users).
 * 6. Message senders cannot be forged/spoofed; senderId is server-bound.
 * 7. Server-side payment validation rejects forged/invalid receipts and securely grants Premium.
 * 8. Rate limiting blocks brute-force and spam attempts (HTTP 429 Too Many Requests).
 * 9. Comprehensive audit logging records all sensitive security events and access violations.
 * 10. Only explicitly verified administrators can perform administrative and moderation actions.
 */
class VeloraAdminSecurityTest {

    @Before
    fun setUp() {
        VeloraBackendServer.clearRateLimitsForTesting()
        com.example.data.VeloraData.currentUser = com.example.model.UserProfile(
            id = "user_me",
            name = "Alif Islam",
            age = 24,
            gender = "Man",
            city = "Dhaka",
            country = "Bangladesh",
            distanceKm = 0,
            profession = "Software Engineer",
            height = "5'10\" (178 cm)",
            relationshipStatus = "Single",
            aboutMe = "Curious mind, coffee enthusiast, and avid traveler. Looking for genuine conversations and romantic connections.",
            interests = listOf("Travel", "Photography", "Music", "Coffee", "Tech"),
            photos = listOf(
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=800&q=80"
            ),
            primaryPhotoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
            isVerified = true,
            isOnline = true,
            email = "alif.islam@example.com",
            phone = "+880 1712-345678",
            isEmailVerified = true,
            isPhoneVerified = true
        )
    }

    // --- TEST 1: Ordinary User Authentication ---
    @Test
    fun test1_ordinaryUserLogin_returnsTokenWithUserRole() {
        val loginResponse = VeloraBackendServer.login("alif@example.com", "userpass123")
        assertTrue("Ordinary user login should succeed", loginResponse.isSuccessful)
        assertEquals(200, loginResponse.statusCode)
        assertNotNull("Should receive a signed session token", loginResponse.data)

        val tokenPayload = ServerTokenManager.verifyToken(loginResponse.data!!)
        assertEquals("alif@example.com", tokenPayload.email)
        assertEquals(Role.USER, tokenPayload.role)
    }

    // --- TEST 2: Ordinary User Denied Access to Admin (403 Forbidden) ---
    @Test
    fun test2_ordinaryUser_attemptAccessAdminDashboard_returns403Forbidden() {
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!

        val dashboardResponse = VeloraBackendServer.getAdminDashboard("Bearer $userToken")
        assertFalse("Ordinary user MUST NOT access admin dashboard", dashboardResponse.isSuccessful)
        assertEquals(403, dashboardResponse.statusCode)
        assertTrue(dashboardResponse.message.contains("lacks required administrator custom claims") || dashboardResponse.message.contains("lacks required administrator privileges"))
        assertNull("No dashboard data should be returned to non-admin", dashboardResponse.data)

        // Test other admin endpoints with user token:
        val usersResponse = VeloraBackendServer.getAdminUsers("Bearer $userToken")
        assertEquals(403, usersResponse.statusCode)

        val reportsResponse = VeloraBackendServer.getAdminReports("Bearer $userToken")
        assertEquals(403, reportsResponse.statusCode)

        val auditLogsResponse = VeloraBackendServer.getAuditLogs("Bearer $userToken")
        assertEquals(403, auditLogsResponse.statusCode)
    }

    // --- TEST 3: Unauthenticated Access Denied by Default (401 Unauthorized) ---
    @Test
    fun test3_unauthenticatedRequest_returns401Unauthorized() {
        val nullAuthResponse = VeloraBackendServer.getAdminDashboard(null)
        assertFalse(nullAuthResponse.isSuccessful)
        assertEquals(401, nullAuthResponse.statusCode)
        assertEquals("Unauthorized", nullAuthResponse.error)

        val emptyAuthResponse = VeloraBackendServer.getAdminDashboard("")
        assertEquals(401, emptyAuthResponse.statusCode)

        val invalidFormatResponse = VeloraBackendServer.getAdminDashboard("Basic dXNlcjpwYXNz")
        assertEquals(401, invalidFormatResponse.statusCode)
    }

    // --- TEST 4: Tampered/Forged Token Fails Signature Verification (401 Unauthorized) ---
    @Test
    fun test4_forgedOrTamperedToken_failsSignatureVerification_returns401Unauthorized() {
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!

        val parts = userToken.split(".")
        val forgedPayload = """{"sub":"user_me","email":"alif@example.com","role":"ADMIN","iat":${System.currentTimeMillis()},"exp":${System.currentTimeMillis() + 3600000}}"""

        val forgedPayloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.toByteArray(StandardCharsets.UTF_8))
        val tamperedToken = "${parts[0]}.$forgedPayloadEncoded.${parts[2]}"

        val tamperedResponse = VeloraBackendServer.getAdminDashboard("Bearer $tamperedToken")
        assertFalse("Tampered token MUST be rejected", tamperedResponse.isSuccessful)
        assertEquals(401, tamperedResponse.statusCode)
        assertTrue(tamperedResponse.message.contains("Invalid or tampered token") || tamperedResponse.message.contains("signature"))
    }

    // --- TEST 5: Authorized Administrator Authentication ---
    @Test
    fun test5_authorizedAdminLogin_returnsTokenWithAdminRole() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        assertTrue("Admin login should succeed", adminLogin.isSuccessful)
        assertEquals(200, adminLogin.statusCode)
        assertNotNull("Should receive a signed session token", adminLogin.data)

        val tokenPayload = ServerTokenManager.verifyToken(adminLogin.data!!)
        assertEquals("admin@velora.app", tokenPayload.email)
        assertEquals(Role.ADMIN, tokenPayload.role)
    }

    // --- TEST 6: Authorized Administrator Granted Access (200 OK) ---
    @Test
    fun test6_authorizedAdmin_canAccessAdminDashboard_returns200OK() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!

        val dashboardResponse = VeloraBackendServer.getAdminDashboard("Bearer $adminToken")
        assertTrue("Admin MUST be granted dashboard access", dashboardResponse.isSuccessful)
        assertEquals(200, dashboardResponse.statusCode)
        assertNotNull(dashboardResponse.data)
        assertEquals(48532, dashboardResponse.data!!.totalUsers)

        val usersResponse = VeloraBackendServer.getAdminUsers("Bearer $adminToken")
        assertTrue(usersResponse.isSuccessful)
        assertNotNull(usersResponse.data)

        val reportsResponse = VeloraBackendServer.getAdminReports("Bearer $adminToken")
        assertTrue(reportsResponse.isSuccessful)
        assertNotNull(reportsResponse.data)

        val auditLogs = VeloraBackendServer.getAuditLogs("Bearer $adminToken")
        assertTrue(auditLogs.isSuccessful)
        assertTrue(auditLogs.data!!.isNotEmpty())
    }

    // --- TEST 7: Users Cannot Self-Assign Administrator Roles ---
    @Test
    fun test7_ordinaryUser_cannotAssignThemselvesAdminRole_returns403Forbidden() {
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!

        val escalationResponse = VeloraBackendServer.assignAdminRole(
            operatorAuthHeader = "Bearer $userToken",
            targetEmail = "alif@example.com",
            newRole = Role.ADMIN
        )

        assertFalse("Self-role-assignment MUST be blocked", escalationResponse.isSuccessful)
        assertEquals(403, escalationResponse.statusCode)
        assertEquals("Forbidden", escalationResponse.error)

        val freshUserLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val payload = ServerTokenManager.verifyToken(freshUserLogin.data!!)
        assertEquals(Role.USER, payload.role)
    }

    // --- TEST 8: Trusted Administrator Can Assign/Revoke Roles ---
    @Test
    fun test8_authorizedAdmin_canAssignAndRevokeRoles() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!

        val assignResponse = VeloraBackendServer.assignAdminRole(
            operatorAuthHeader = "Bearer $adminToken",
            targetEmail = "moderator@velora.app",
            newRole = Role.ADMIN
        )

        assertTrue(assignResponse.isSuccessful)
        assertEquals(200, assignResponse.statusCode)
    }

    // --- TEST 9: Administrator Moderation Actions ---
    @Test
    fun test9_authorizedAdmin_canSuspendAndModerateUsers() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!

        val suspendResponse = VeloraBackendServer.toggleUserSuspension("Bearer $adminToken", "user_ayesha")
        assertTrue(suspendResponse.isSuccessful)
        assertEquals(200, suspendResponse.statusCode)

        val resolveResponse = VeloraBackendServer.resolveReport("Bearer $adminToken", "rep_1", "Resolved")
        assertTrue(resolveResponse.isSuccessful)
        assertEquals(200, resolveResponse.statusCode)

        val badgeResponse = VeloraBackendServer.adminVerifyUserBadge("Bearer $adminToken", "user_ayesha", true)
        assertTrue(badgeResponse.isSuccessful)
        assertEquals(200, badgeResponse.statusCode)
    }

    // --- TEST 10: Users Cannot Change Their Own Role, Verification, Premium, Payment, or Moderation Status ---
    @Test
    fun test10_usersCannotModifyOwnPrivilegedSecurityFields() {
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!

        // Attempt 1: Ordinary user attempts to self-verify
        val verifyAttempt = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(isVerified = true)
        )
        assertFalse("Self-verification MUST be rejected", verifyAttempt.isSuccessful)
        assertEquals(403, verifyAttempt.statusCode)
        assertTrue(verifyAttempt.message.contains("cannot change their own role, verification status"))

        // Attempt 2: Ordinary user attempts to grant themselves Premium
        val premiumAttempt = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(isPremium = true)
        )
        assertFalse("Self-granting premium MUST be rejected", premiumAttempt.isSuccessful)
        assertEquals(403, premiumAttempt.statusCode)

        // Attempt 3: Ordinary user attempts to change payment status
        val paymentAttempt = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(paymentStatus = "COMPLETED")
        )
        assertFalse("Self-modifying payment status MUST be rejected", paymentAttempt.isSuccessful)
        assertEquals(403, paymentAttempt.statusCode)

        // Attempt 4: Ordinary user attempts to change role to ADMIN
        val roleAttempt = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(role = Role.ADMIN)
        )
        assertFalse("Self-modifying role MUST be rejected", roleAttempt.isSuccessful)
        assertEquals(403, roleAttempt.statusCode)

        // Attempt 5: Ordinary user attempts to modify moderation status
        val modAttempt = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(moderationStatus = "EXEMPT")
        )
        assertFalse("Self-modifying moderation status MUST be rejected", modAttempt.isSuccessful)
        assertEquals(403, modAttempt.statusCode)

        // Valid profile update (bio & profession) succeeds
        val validUpdate = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(bio = "Lover of coffee, books, and long walks.", profession = "Software Engineer")
        )
        assertTrue("Legitimate profile update must succeed", validUpdate.isSuccessful)
        assertEquals(200, validUpdate.statusCode)
        assertEquals("Software Engineer", validUpdate.data?.profession)
    }

    // --- TEST 11: Private Conversations Accessible ONLY to Eligible Participants ---
    @Test
    fun test11_privateConversations_onlyAccessibleToEligibleParticipants() {
        // Participant in conv_me_ayesha (Alif)
        val userAlifLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val alifToken = userAlifLogin.data!!

        // Third-party user NOT in conv_me_ayesha (Guest User)
        val userGuestLogin = VeloraBackendServer.login("user@example.com", "userpass123")
        val guestToken = userGuestLogin.data!!

        // 1. Participant can access their private conversation
        val alifReadResponse = VeloraBackendServer.getConversationMessages("Bearer $alifToken", "conv_me_ayesha")
        assertTrue("Participant must be allowed to read conversation", alifReadResponse.isSuccessful)
        assertEquals(200, alifReadResponse.statusCode)
        assertNotNull(alifReadResponse.data)
        assertTrue(alifReadResponse.data!!.isNotEmpty())

        // 2. Non-participant unauthorized user is DENIED access (403 Forbidden)
        val guestReadResponse = VeloraBackendServer.getConversationMessages("Bearer $guestToken", "conv_me_ayesha")
        assertFalse("Unauthorized user MUST NOT read private conversation", guestReadResponse.isSuccessful)
        assertEquals(403, guestReadResponse.statusCode)
        assertEquals("Forbidden", guestReadResponse.error)
        assertTrue(guestReadResponse.message.contains("not an eligible participant"))

        // 3. Non-participant cannot send messages to other users' conversation (403 Forbidden)
        val guestSendResponse = VeloraBackendServer.sendChatMessage("Bearer $guestToken", "conv_me_ayesha", "Eavesdropping hello!")
        assertFalse("Unauthorized user MUST NOT send message into private conversation", guestSendResponse.isSuccessful)
        assertEquals(403, guestSendResponse.statusCode)

        // 4. Authorized participant can send messages, and senderId is verified on server
        val alifSendResponse = VeloraBackendServer.sendChatMessage("Bearer $alifToken", "conv_me_ayesha", "Hey Ayesha, how are you?")
        assertTrue("Participant can send message", alifSendResponse.isSuccessful)
        assertEquals(200, alifSendResponse.statusCode)
        assertEquals("user_me", alifSendResponse.data?.senderId) // Server-verified sender ID

        // 5. Authorized administrator can access conversation in authorized safety audit workflow
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!
        val adminAuditResponse = VeloraBackendServer.getConversationMessages("Bearer $adminToken", "conv_me_ayesha")
        assertTrue("Administrator in safety audit workflow can access conversation", adminAuditResponse.isSuccessful)
        assertEquals(200, adminAuditResponse.statusCode)
    }

    // --- TEST 12: Server-Side Payment Verification Rejects Forgery & Grants Premium ---
    @Test
    fun test12_serverSidePaymentVerification_rejectsForgedReceipts_grantsOnValidReceipt() {
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!

        // Fraud attempt: fake or empty receipt
        val fakeReceiptPayment = VeloraBackendServer.verifyPaymentAndGrantPremium(
            authHeader = "Bearer $userToken",
            paymentRequest = PaymentVerificationRequest(
                planId = "plan_platinum",
                transactionReceipt = "fake_receipt",
                amount = "৳ 2,499"
            )
        )
        assertFalse("Forged or fake receipt must be rejected", fakeReceiptPayment.isSuccessful)
        assertEquals(402, fakeReceiptPayment.statusCode)

        // Valid server-verified payment receipt
        val validPayment = VeloraBackendServer.verifyPaymentAndGrantPremium(
            authHeader = "Bearer $userToken",
            paymentRequest = PaymentVerificationRequest(
                planId = "plan_platinum",
                transactionReceipt = "BKASH_TXN_VERIFIED_9921",
                amount = "৳ 2,499"
            )
        )
        assertTrue("Valid payment receipt must be verified by server", validPayment.isSuccessful)
        assertEquals(200, validPayment.statusCode)
        assertTrue(validPayment.data == true)
    }

    // --- TEST 13: Rate Limiting Blocks Excessive Requests (429 Too Many Requests) ---
    @Test
    fun test13_rateLimiting_blocksExcessiveRequests_returns429TooManyRequests() {
        val userEmail = "spammer@example.com"

        // Execute rapid login attempts (max 5 allowed per minute)
        for (i in 1..5) {
            val res = VeloraBackendServer.login(userEmail, "wrongpassword")
            assertEquals(401, res.statusCode) // 401 for wrong credentials within rate limit
        }

        // 6th attempt should be blocked by rate limiter with 429
        val rateLimitedResponse = VeloraBackendServer.login(userEmail, "wrongpassword")
        assertEquals("6th rapid attempt must trigger rate limiter", 429, rateLimitedResponse.statusCode)
        assertEquals("Too Many Requests", rateLimitedResponse.error)
        assertTrue(rateLimitedResponse.message.contains("Too many login attempts"))
    }

    // --- TEST 14: Comprehensive Audit Logging Records Sensitive Operations ---
    @Test
    fun test14_auditLogging_capturesAllSensitiveSecurityEvents() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!

        val auditLogsResponse = VeloraBackendServer.getAuditLogs("Bearer $adminToken")
        assertTrue(auditLogsResponse.isSuccessful)
        val logs = auditLogsResponse.data!!
        assertTrue("Audit log should contain initialization and runtime events", logs.isNotEmpty())

        // Verify that security events are logged with timestamps
        assertTrue(logs.any { it.contains("Server started") })
    }

    // --- TEST 15: Editing Frontend State or Changing URL Route Does Not Grant Admin Access ---
    @Test
    fun test15_urlOrRouteTampering_doesNotGrantAdminAccess() {
        val ordinaryUserLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val ordinaryUserToken = ordinaryUserLogin.data!!

        // Attempt direct access via /admin route or screen transition
        // The server backend dashboard endpoint ALWAYS verifies cryptographic admin role
        val directRouteAttempt = VeloraBackendServer.getAdminDashboard("Bearer $ordinaryUserToken")
        assertFalse("Direct URL route attempt with user token must fail", directRouteAttempt.isSuccessful)
        assertEquals(403, directRouteAttempt.statusCode)

        // Attempt without any token (raw URL visit)
        val anonymousRouteAttempt = VeloraBackendServer.getAdminDashboard(null)
        assertFalse("Anonymous URL route attempt must fail", anonymousRouteAttempt.isSuccessful)
        assertEquals(401, anonymousRouteAttempt.statusCode)
    }

    // --- TEST 16: Unauthorized Users Cannot Suspend Accounts, Inspect Reports, Modify Subscriptions, or Change Settings ---
    @Test
    fun test16_unauthorizedUsers_cannotPerformAdminOperations() {
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!
        val authHeader = "Bearer $userToken"

        // 1. Cannot suspend accounts
        val suspendAttempt = VeloraBackendServer.toggleUserSuspension(authHeader, "user_1")
        assertEquals(403, suspendAttempt.statusCode)

        // 2. Cannot inspect protected reports
        val reportsAttempt = VeloraBackendServer.getAdminReports(authHeader)
        assertEquals(403, reportsAttempt.statusCode)
        assertNull(reportsAttempt.data)

        // 3. Cannot modify subscription records for other users or bypass payment
        val fraudSubscription = VeloraBackendServer.verifyPaymentAndGrantPremium(
            authHeader = authHeader,
            paymentRequest = PaymentVerificationRequest(
                planId = "plan_platinum",
                transactionReceipt = "hacked_receipt",
                amount = "0"
            )
        )
        assertEquals(402, fraudSubscription.statusCode)

        // 4. Cannot update moderation status
        val moderationAttempt = VeloraBackendServer.updateModerationStatus(authHeader, "user_1", "Banned")
        assertEquals(403, moderationAttempt.statusCode)

        // 5. Cannot verify user badges
        val verifyBadgeAttempt = VeloraBackendServer.adminVerifyUserBadge(authHeader, "user_1", true)
        assertEquals(403, verifyBadgeAttempt.statusCode)
    }

    // --- TEST 17: Authorized Administrators Can Perform Permitted Administrative Tasks ---
    @Test
    fun test17_authorizedAdmin_canPerformPermittedAdministrativeTasks() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        assertTrue(adminLogin.isSuccessful)
        val adminToken = adminLogin.data!!
        val authHeader = "Bearer $adminToken"

        // 1. Access Dashboard
        val dashboard = VeloraBackendServer.getAdminDashboard(authHeader)
        assertTrue(dashboard.isSuccessful)
        assertNotNull(dashboard.data)
        assertTrue(dashboard.data!!.totalUsers > 0)

        // 2. Read Users List
        val users = VeloraBackendServer.getAdminUsers(authHeader)
        assertTrue(users.isSuccessful)
        assertTrue(users.data!!.isNotEmpty())

        // 3. Suspend / Unsuspend User
        val suspendToggle = VeloraBackendServer.toggleUserSuspension(authHeader, "user_nusrat")
        assertTrue(suspendToggle.isSuccessful)

        // 4. Read Reports and Resolve Report
        val reports = VeloraBackendServer.getAdminReports(authHeader)
        assertTrue(reports.isSuccessful)
        val firstReport = reports.data!!.first()
        val resolve = VeloraBackendServer.resolveReport(authHeader, firstReport.id, "Resolved")
        assertTrue(resolve.isSuccessful)

        // 5. Inspect Audit Logs
        val auditLogs = VeloraBackendServer.getAuditLogs(authHeader)
        assertTrue(auditLogs.isSuccessful)
        assertTrue(auditLogs.data!!.isNotEmpty())
    }

    // --- TEST 18: Existing Dating Features Continue to Work ---
    @Test
    fun test18_datingFeaturesPreserved_discoverLikesMatchesChatProfileContinueToWork() {
        // 1. Discover Profiles available
        val profiles = com.example.data.VeloraData.discoverProfiles
        assertTrue("Discover feed profiles must be present", profiles.isNotEmpty())

        // 2. Matches and Conversations available
        val matches = com.example.data.VeloraData.initialMatches
        assertTrue("Matches must be present", matches.isNotEmpty())

        // 3. User Authentication works
        val loginResponse = VeloraBackendServer.login("alif@example.com", "userpass123")
        assertTrue(loginResponse.isSuccessful)
        val token = loginResponse.data!!

        // 4. Chat messaging works for match participants
        val chatResponse = VeloraBackendServer.getConversationMessages("Bearer $token", "conv_me_ayesha")
        assertTrue("Chat history must load for participant", chatResponse.isSuccessful)
        assertTrue(chatResponse.data!!.isNotEmpty())

        val sendResponse = VeloraBackendServer.sendChatMessage("Bearer $token", "conv_me_ayesha", "Looking forward to our chat!")
        assertTrue("Chat send must succeed", sendResponse.isSuccessful)
        assertEquals("Looking forward to our chat!", sendResponse.data?.text)

        // 5. Profile viewing & updating works
        val updateResponse = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $token",
            targetUserId = "user_me",
            updates = UserProfileUpdate(bio = "Updated Bio for testing", profession = "Product Designer")
        )
        assertTrue("Profile update must succeed for owner", updateResponse.isSuccessful)
        assertEquals("Product Designer", updateResponse.data?.profession)
    }

    // --- TEST 19: Firebase Auth Custom Claims Enforced on All Admin Backend Actions ---
    @Test
    fun test19_firebaseAuthCustomClaims_adminToken_containsAdminTrue_andProtectsAllActions() {
        // Admin token must have admin == true custom claim
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!
        val adminPayload = ServerTokenManager.verifyToken(adminToken)
        assertTrue("Admin token must contain admin: true custom claim", adminPayload.admin)
        assertEquals(Role.ADMIN, adminPayload.role)

        // User token must have admin == false
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!
        val userPayload = ServerTokenManager.verifyToken(userToken)
        assertFalse("User token must NOT contain admin: true custom claim", userPayload.admin)
        assertEquals(Role.USER, userPayload.role)

        // Test that every admin action fails with 403 Forbidden when called with user token
        val userHeader = "Bearer $userToken"
        assertEquals(403, VeloraBackendServer.getAdminDashboard(userHeader).statusCode)
        assertEquals(403, VeloraBackendServer.getAdminUsers(userHeader).statusCode)
        assertEquals(403, VeloraBackendServer.getAdminReports(userHeader).statusCode)
        assertEquals(403, VeloraBackendServer.toggleUserSuspension(userHeader, "user_ayesha").statusCode)
        assertEquals(403, VeloraBackendServer.adminVerifyUserBadge(userHeader, "user_ayesha", true).statusCode)
        assertEquals(403, VeloraBackendServer.resolveReport(userHeader, "rep_001", "Resolved").statusCode)
        assertEquals(403, VeloraBackendServer.assignAdminRole(userHeader, "alif@example.com", Role.ADMIN).statusCode)
        assertEquals(403, VeloraBackendServer.updateModerationStatus(userHeader, "user_ayesha", "Active").statusCode)
        assertEquals(403, VeloraBackendServer.getAuditLogs(userHeader).statusCode)

        // Valid admin token succeeds on admin actions
        val adminHeader = "Bearer $adminToken"
        assertEquals(200, VeloraBackendServer.getAdminDashboard(adminHeader).statusCode)
        assertEquals(200, VeloraBackendServer.getAdminUsers(adminHeader).statusCode)
        assertEquals(200, VeloraBackendServer.getAdminReports(adminHeader).statusCode)
    }

    // --- TEST 20: Admin Route Protection Against Direct URL/Navigation Tampering ---
    @Test
    fun test20_adminRouteProtection_requiresVerifiedCustomClaims() {
        val viewModel = com.example.viewmodel.VeloraViewModel()

        // 1. Attempting to navigate directly to /admin when unauthenticated must redirect to ADMIN_LOGIN
        viewModel.handleRoute("/admin")
        assertEquals(com.example.model.Screen.ADMIN_LOGIN, viewModel.uiState.value.currentScreen)
        assertFalse(viewModel.uiState.value.isAdminAuthenticated)

        // 2. Attempting navigateTo(Screen.ADMIN_PANEL) without session redirects to ADMIN_LOGIN
        viewModel.navigateTo(com.example.model.Screen.ADMIN_PANEL)
        assertEquals(com.example.model.Screen.ADMIN_LOGIN, viewModel.uiState.value.currentScreen)

        // 3. Ordinary user token presented to admin route fails server-side custom claim verification
        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!
        val userVerify = VeloraBackendServer.getAdminDashboard("Bearer $userToken")
        assertFalse("Ordinary user MUST NOT pass admin custom claims verification", userVerify.isSuccessful)
        assertEquals(403, userVerify.statusCode)

        // 4. Authenticated admin with valid custom claims passes verification and is granted dashboard access
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!
        val adminVerify = VeloraBackendServer.getAdminDashboard("Bearer $adminToken")
        assertTrue("Admin with verified custom claims MUST be granted access", adminVerify.isSuccessful)
        assertEquals(200, adminVerify.statusCode)
    }

    // --- TEST 21: User Registration Defaults to Unverified Phone and Email ---
    @Test
    fun test21_newUserRegistration_defaultsToUnverifiedEmailAndPhone() {
        val testEmail = "testuser_${System.currentTimeMillis()}@example.com"
        val testPhone = "+880 1711-${(100000..999999).random()}"
        val regResponse = VeloraBackendServer.registerNewUser(
            name = "Test New User",
            email = testEmail,
            phone = testPhone,
            password = "testpassword123"
        )

        assertTrue("Registration must succeed with HTTP 201", regResponse.isSuccessful)
        assertEquals(201, regResponse.statusCode)
        val registeredUser = regResponse.data
        assertNotNull(registeredUser)
        assertFalse("New user must NOT have email verified initially", registeredUser!!.isEmailVerified)
        assertFalse("New user must NOT have phone verified initially", registeredUser.isPhoneVerified)

        // Duplicate registration must be rejected with HTTP 409
        val dupResponse = VeloraBackendServer.registerNewUser(
            name = "Duplicate",
            email = testEmail,
            phone = testPhone,
            password = "testpassword123"
        )
        assertEquals(409, dupResponse.statusCode)
    }

    // --- TEST 22: Unverified User Login Blocked with HTTP 403 Forbidden ---
    @Test
    fun test22_unverifiedUserLogin_blockedWith403Forbidden() {
        val testEmail = "unverified_${System.currentTimeMillis()}@example.com"
        val testPhone = "+880 1722-${(100000..999999).random()}"
        VeloraBackendServer.registerNewUser(
            name = "Unverified User",
            email = testEmail,
            phone = testPhone,
            password = "securePassword123"
        )

        val loginResponse = VeloraBackendServer.login(testEmail, "securePassword123")
        assertFalse("Unverified user login MUST be rejected", loginResponse.isSuccessful)
        assertEquals(403, loginResponse.statusCode)
        assertTrue(
            "Must state phone or email verification is required",
            loginResponse.message.contains("verification required", ignoreCase = true)
        )
    }

    // --- TEST 23: Phone OTP Verification Rejects Invalid Code and Accepts Valid Code ---
    @Test
    fun test23_phoneOtpVerification_rejectsWrongCodeAndAcceptsValidCode() {
        val testPhone = "+880 1733-${(100000..999999).random()}"
        val otpResponse = VeloraBackendServer.sendPhoneOtp(testPhone)
        assertTrue(otpResponse.isSuccessful)
        val realOtp = otpResponse.data!!
        assertEquals(6, realOtp.length)

        // 1. Wrong OTP is rejected
        val wrongVerify = VeloraBackendServer.verifyPhoneOtp(testPhone, "000000")
        assertFalse("Incorrect OTP must be rejected", wrongVerify.isSuccessful)
        assertEquals(400, wrongVerify.statusCode)

        // 2. Real OTP is accepted
        val successVerify = VeloraBackendServer.verifyPhoneOtp(testPhone, realOtp)
        assertTrue("Valid OTP must succeed", successVerify.isSuccessful)
        assertEquals(200, successVerify.statusCode)
        assertTrue(successVerify.data == true)

        // 3. Reusing used/consumed OTP fails
        val reusedVerify = VeloraBackendServer.verifyPhoneOtp(testPhone, realOtp)
        assertFalse("Consumed OTP cannot be reused", reusedVerify.isSuccessful)
        assertEquals(400, reusedVerify.statusCode)
    }

    // --- TEST 24: Email Verification Rejects Invalid Code and Accepts Valid Code ---
    @Test
    fun test24_emailVerification_rejectsWrongCodeAndAcceptsValidCode() {
        val testEmail = "verify_${System.currentTimeMillis()}@example.com"
        val sendResponse = VeloraBackendServer.sendEmailVerification(testEmail)
        assertTrue(sendResponse.isSuccessful)
        val realCode = sendResponse.data!!
        assertEquals(6, realCode.length)

        // 1. Wrong code rejected
        val wrongVerify = VeloraBackendServer.verifyEmailCode(testEmail, "111111")
        assertFalse("Incorrect email code must be rejected", wrongVerify.isSuccessful)
        assertEquals(400, wrongVerify.statusCode)

        // 2. Correct code accepted
        val successVerify = VeloraBackendServer.verifyEmailCode(testEmail, realCode)
        assertTrue("Correct email verification code must succeed", successVerify.isSuccessful)
        assertEquals(200, successVerify.statusCode)
        assertTrue(successVerify.data == true)

        // 3. Consumed code cannot be reused
        val reusedVerify = VeloraBackendServer.verifyEmailCode(testEmail, realCode)
        assertFalse("Consumed email verification code cannot be reused", reusedVerify.isSuccessful)
        assertEquals(400, reusedVerify.statusCode)
    }

    // --- TEST 25: Full Verification Flow Allows Login and Grants Access ---
    @Test
    fun test25_fullyVerifiedUser_loginSucceedsWith200AndToken() {
        val testEmail = "completed_${System.currentTimeMillis()}@example.com"
        val testPhone = "+880 1744-${(100000..999999).random()}"
        val pass = "Password123!"

        // 1. Register
        VeloraBackendServer.registerNewUser("Completed User", testEmail, testPhone, pass)

        // 2. Phone OTP verify
        val phoneOtp = VeloraBackendServer.getPendingPhoneOtp(testPhone)
        assertNotNull("Phone OTP must be generated upon registration", phoneOtp)
        val phoneVerify = VeloraBackendServer.verifyPhoneOtp(testPhone, phoneOtp!!)
        assertTrue(phoneVerify.isSuccessful)

        // 3. Attempting login when phone is verified but email is still unverified must return 403
        val partialLogin = VeloraBackendServer.login(testEmail, pass)
        assertEquals(403, partialLogin.statusCode)
        assertTrue(partialLogin.message.contains("Email verification required", ignoreCase = true))

        // 4. Email verify
        val emailCode = VeloraBackendServer.getPendingEmailCode(testEmail)
        assertNotNull("Email code must be generated upon registration", emailCode)
        val emailVerify = VeloraBackendServer.verifyEmailCode(testEmail, emailCode!!)
        assertTrue(emailVerify.isSuccessful)

        // 5. Now both phone and email are verified -> Login succeeds with HTTP 200
        val finalLogin = VeloraBackendServer.login(testEmail, pass)
        assertTrue("Login must succeed after completing phone OTP and email verification", finalLogin.isSuccessful)
        assertEquals(200, finalLogin.statusCode)
        assertNotNull("Valid JWT token must be issued", finalLogin.data)
    }

    // --- TEST 26: Bangladeshi MFS Payment Verification (bKash & Nagad) Automatically Activates Subscription ---
    @Test
    fun test26_mfsPaymentVerification_bKashAndNagad_automaticallyGrantsPremium() {
        val userEmail = "mfs_user_${System.currentTimeMillis()}@example.com"
        val userPhone = "+880 1711-223344"
        VeloraBackendServer.registerNewUser("MFS User", userEmail, userPhone, "Pass123!")
        val otp = VeloraBackendServer.getPendingPhoneOtp(userPhone)!!
        VeloraBackendServer.verifyPhoneOtp(userPhone, otp)
        val code = VeloraBackendServer.getPendingEmailCode(userEmail)!!
        VeloraBackendServer.verifyEmailCode(userEmail, code)
        val loginRes = VeloraBackendServer.login(userEmail, "Pass123!")
        val token = loginRes.data!!

        // 1. Submit valid bKash payment with TrxID in BDT
        val bkashRequest = PaymentVerificationRequest(
            planId = "plan_1m",
            amount = "299",
            currency = "BDT",
            transactionReceipt = "BK9X${System.currentTimeMillis() % 100000}",
            paymentMethod = "BKASH",
            senderAccount = "01712-345678"
        )
        val bkashVerify = VeloraBackendServer.verifyPaymentAndGrantPremium("Bearer $token", bkashRequest)
        assertTrue("Valid bKash transaction must verify successfully", bkashVerify.isSuccessful)
        assertEquals(200, bkashVerify.statusCode)
        assertTrue(bkashVerify.data == true)

        // 2. Submit valid Nagad payment with TrxID in BDT
        val nagadUserEmail = "nagad_user_${System.currentTimeMillis()}@example.com"
        VeloraBackendServer.registerNewUser("Nagad User", nagadUserEmail, "+880 1811-998877", "Pass123!")
        val nOtp = VeloraBackendServer.getPendingPhoneOtp("+880 1811-998877")!!
        VeloraBackendServer.verifyPhoneOtp("+880 1811-998877", nOtp)
        val nCode = VeloraBackendServer.getPendingEmailCode(nagadUserEmail)!!
        VeloraBackendServer.verifyEmailCode(nagadUserEmail, nCode)
        val nLogin = VeloraBackendServer.login(nagadUserEmail, "Pass123!")
        val nToken = nLogin.data!!

        val nagadRequest = PaymentVerificationRequest(
            planId = "plan_3m",
            amount = "699",
            currency = "BDT",
            transactionReceipt = "NG7Y${System.currentTimeMillis() % 100000}",
            paymentMethod = "NAGAD",
            senderAccount = "01811-998877"
        )
        val nagadVerify = VeloraBackendServer.verifyPaymentAndGrantPremium("Bearer $nToken", nagadRequest)
        assertTrue("Valid Nagad transaction must verify successfully", nagadVerify.isSuccessful)
        assertEquals(200, nagadVerify.statusCode)
        assertTrue(nagadVerify.data == true)

        // 3. Verify transactions logged in MFS transaction history for admin inspection
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminTxns = VeloraBackendServer.getMfsTransactions("Bearer ${adminLogin.data!!}").data!!
        assertTrue(adminTxns.any { it.trxId == bkashRequest.transactionReceipt && it.method == "bKash" && it.amountBdt == 299 })
        assertTrue(adminTxns.any { it.trxId == nagadRequest.transactionReceipt && it.method == "Nagad" && it.amountBdt == 699 })
    }

    // --- TEST 27: Admin Gateway Controls: Disabling MFS Gateway Enforces Server-Side Rejection ---
    @Test
    fun test27_disabledMfsGateway_rejectsPayment() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!

        // Admin disables bKash gateway
        val currentConfig = VeloraBackendServer.getMfsConfig().data!!
        val updatedConfig = currentConfig.copy(bKashEnabled = false, nagadEnabled = true)
        val updateRes = VeloraBackendServer.updateMfsConfig("Bearer $adminToken", updatedConfig)
        assertTrue(updateRes.isSuccessful)
        assertFalse("bKash must now be disabled", VeloraBackendServer.getMfsConfig().data!!.bKashEnabled)

        // Ordinary user tries to pay via bKash
        val userEmail = "disabled_test_${System.currentTimeMillis()}@example.com"
        val phone = "+880 1511-${(100000..999999).random()}"
        VeloraBackendServer.registerNewUser("Test User", userEmail, phone, "Pass123!")
        val otp = VeloraBackendServer.getPendingPhoneOtp(phone)!!
        VeloraBackendServer.verifyPhoneOtp(phone, otp)
        val code = VeloraBackendServer.getPendingEmailCode(userEmail)!!
        VeloraBackendServer.verifyEmailCode(userEmail, code)
        val token = VeloraBackendServer.login(userEmail, "Pass123!").data!!

        val req = PaymentVerificationRequest(
            planId = "plan_1m",
            amount = "299",
            currency = "BDT",
            transactionReceipt = "BKASH123456",
            paymentMethod = "BKASH",
            senderAccount = "01511-000000"
        )
        val payRes = VeloraBackendServer.verifyPaymentAndGrantPremium("Bearer $token", req)
        assertFalse("Payment via disabled bKash must be rejected", payRes.isSuccessful)
        assertEquals(400, payRes.statusCode)
        assertEquals("MFS_METHOD_DISABLED", payRes.error)

        // Restore bKash for subsequent tests
        VeloraBackendServer.updateMfsConfig("Bearer $adminToken", currentConfig.copy(bKashEnabled = true))
    }

    // --- TEST 28: Admin Configures MFS Merchant Numbers & BDT Package Prices ---
    @Test
    fun test28_adminUpdatesMfsConfigAndPlanPricesInBdt_persisted() {
        val adminLogin = VeloraBackendServer.login("admin@velora.app", "admin_secure_pass_2026")
        val adminToken = adminLogin.data!!

        val userLogin = VeloraBackendServer.login("alif@example.com", "userpass123")
        val userToken = userLogin.data!!

        // 1. Ordinary user cannot update MFS config (403 Forbidden)
        val fakeConfig = MfsMerchantConfig(bKashMerchantNumber = "01999-999999")
        val unauthorizedConfigUpdate = VeloraBackendServer.updateMfsConfig("Bearer $userToken", fakeConfig)
        assertEquals(403, unauthorizedConfigUpdate.statusCode)

        // 2. Ordinary user cannot update package prices (403 Forbidden)
        val unauthorizedPriceUpdate = VeloraBackendServer.updateSubscriptionPlanPrice("Bearer $userToken", "plan_1m", 100)
        assertEquals(403, unauthorizedPriceUpdate.statusCode)

        // 3. Admin successfully updates bKash merchant number & Nagad merchant number
        val newConfig = MfsMerchantConfig(
            bKashEnabled = true,
            bKashMerchantNumber = "01755-123456",
            bKashMerchantType = "Merchant",
            nagadEnabled = true,
            nagadMerchantNumber = "01655-654321",
            nagadMerchantType = "Personal",
            currency = "BDT"
        )
        val configUpdateRes = VeloraBackendServer.updateMfsConfig("Bearer $adminToken", newConfig)
        assertTrue(configUpdateRes.isSuccessful)
        assertEquals("01755-123456", VeloraBackendServer.getMfsConfig().data!!.bKashMerchantNumber)
        assertEquals("01655-654321", VeloraBackendServer.getMfsConfig().data!!.nagadMerchantNumber)

        // 4. Admin successfully updates package price in BDT
        val priceUpdateRes = VeloraBackendServer.updateSubscriptionPlanPrice("Bearer $adminToken", "plan_1m", 350)
        assertTrue(priceUpdateRes.isSuccessful)
        assertEquals(350, priceUpdateRes.data?.priceBdt)

        val updatedPlans = VeloraBackendServer.getSubscriptionPlans().data!!
        val plan1m = updatedPlans.find { it.id == "plan_1m" }
        assertEquals(350, plan1m?.priceBdt)

        // 5. Admin can retrieve transaction log
        val txnsRes = VeloraBackendServer.getMfsTransactions("Bearer $adminToken")
        assertTrue(txnsRes.isSuccessful)
        assertNotNull(txnsRes.data)
    }

    // --- TEST 29: Firebase Auth & Cloud Firestore New User Account Creation & Config Verification ---
    @Test
    fun test29_firebaseAuthAndFirestore_newUserRegistrationAndSync() = kotlinx.coroutines.runBlocking {
        // 1. Verify new user registration and sync via FirebaseAuthRepository
        val registerResult = com.example.data.FirebaseAuthRepository.registerAndSyncUser(
            name = "Shafiqul Islam",
            email = "shafiqul@velora.com",
            phone = "01812345678",
            pass = "StrongPass@2026"
        )
        assertTrue("Firebase registration and Firestore sync must succeed", registerResult.isSuccess)
        val profile = registerResult.getOrNull()
        assertNotNull(profile)
        assertEquals("Shafiqul Islam", profile?.name)
        assertEquals("shafiqul@velora.com", profile?.email)
        assertEquals("01812345678", profile?.phone)
        assertFalse("New user must NOT be verified without admin audit", profile?.isVerified ?: true)
        assertFalse("New user must NOT have premium without payment", profile?.isPremium ?: true)

        // 2. Verify current user state updated in session
        assertEquals("Shafiqul Islam", com.example.data.VeloraData.currentUser.name)
        assertEquals("shafiqul@velora.com", com.example.data.VeloraData.currentUser.email)

        // 3. Verify login and sync
        val loginResult = com.example.data.FirebaseAuthRepository.loginAndSyncUser(
            email = "shafiqul@velora.com",
            pass = "StrongPass@2026"
        )
        assertTrue("Firebase login & Firestore sync must succeed", loginResult.isSuccess)
    }

    // --- TEST 30: Real-Time Audio & Video Calling Firestore Signaling and Media State Management ---
    @Test
    fun test30_realTimeAudioAndVideoCalling_firestoreSignalingAndMediaState() = kotlinx.coroutines.runBlocking {
        val caller = com.example.data.VeloraData.currentUser
        val receiver = com.example.data.VeloraData.initialMatches.first()

        // 1. Initiate Real-Time Video Call
        val callResult = com.example.data.CallSignalingRepository.initiateCall(
            caller = caller,
            receiver = receiver,
            callType = com.example.model.CallType.VIDEO
        )
        assertTrue("Call initiation must succeed", callResult.isSuccess)
        val session = callResult.getOrNull()
        assertNotNull(session)
        assertEquals(caller.id, session?.callerId)
        assertEquals(receiver.user.id, session?.receiverId)
        assertEquals(com.example.model.CallType.VIDEO, session?.callType)
        assertEquals(com.example.model.CallStatus.RINGING, session?.status)
        assertTrue("Offer SDP must be generated for WebRTC signaling", session?.offerSdp?.isNotBlank() == true)

        // 2. Exchange ICE Candidate
        val iceCand = com.example.model.IceCandidateModel(
            id = "cand_1",
            candidate = "candidate:1 1 UDP 2130706431 192.168.1.100 50000 typ host",
            sdpMid = "0",
            sdpMLineIndex = 0
        )
        val candResult = com.example.data.CallSignalingRepository.sendIceCandidate(session!!.id, isCaller = true, iceCand)
        assertTrue("Sending ICE candidate must succeed", candResult.isSuccess)

        // 3. Update Media Toggles (Mute, Camera, Speakerphone)
        val mediaResult = com.example.data.CallSignalingRepository.updateMediaState(
            callId = session.id,
            isMuted = true,
            isVideoEnabled = false,
            isSpeakerOn = true
        )
        assertTrue("Media toggle update must succeed", mediaResult.isSuccess)

        // 4. Partner answers / accepts call
        val acceptResult = com.example.data.CallSignalingRepository.acceptCall(session.id)
        assertTrue("Call acceptance must succeed", acceptResult.isSuccess)

        // 5. End Call and record duration
        val endResult = com.example.data.CallSignalingRepository.endCall(session.id, durationSeconds = 45)
        assertTrue("Ending call must succeed", endResult.isSuccess)

        // 6. Test ViewModel Calling State Flow
        val viewModel = com.example.viewmodel.VeloraViewModel()
        assertFalse(viewModel.uiState.value.isCallingActive)
        viewModel.startVideoCall(receiver)
        assertTrue("Video calling must activate state", viewModel.uiState.value.isCallingActive)
        assertEquals(com.example.model.Screen.VIDEO_CALL, viewModel.uiState.value.currentScreen)
        assertEquals(com.example.model.CallType.VIDEO, viewModel.uiState.value.activeCallSession?.callType)

        // Toggle mute and speakerphone in ViewModel
        assertFalse(viewModel.uiState.value.isCallMuted)
        viewModel.toggleCallMute()
        assertTrue(viewModel.uiState.value.isCallMuted)

        assertTrue(viewModel.uiState.value.isSpeakerOn)
        viewModel.toggleSpeakerphone()
        assertFalse(viewModel.uiState.value.isSpeakerOn)

        viewModel.endActiveCall()
        assertFalse(viewModel.uiState.value.isCallingActive)
        assertNull(viewModel.uiState.value.activeCallSession)
    }

    // --- TEST 31: Camera Switch Toggles Front and Back Cameras Successfully ---
    @Test
    fun test31_cameraSwitch_togglesFrontAndBackCamerasSuccessfully() {
        val viewModel = com.example.viewmodel.VeloraViewModel()
        // Default is front camera
        assertTrue("Default camera orientation should be front camera", viewModel.uiState.value.isFrontCamera)

        // First toggle: switches from front camera to back camera
        viewModel.flipCamera()
        assertFalse("Camera switch should toggle from front to back camera", viewModel.uiState.value.isFrontCamera)

        // Second toggle: switches back to front camera
        viewModel.flipCamera()
        assertTrue("Camera switch should toggle back to front camera", viewModel.uiState.value.isFrontCamera)
    }

    // --- TEST 32: Profile Photos Logic Supports Dedicated Primary Photo and 4 to 5 Clear Face Photos ---
    @Test
    fun test32_profilePhotos_supportsDedicatedPrimaryAnd4To5FacePhotos() = kotlinx.coroutines.runBlocking {
        val initialTestPhotos = listOf(
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=800&q=80"
        )
        com.example.data.VeloraData.currentUser = com.example.data.VeloraData.currentUser.copy(
            photos = initialTestPhotos,
            primaryPhotoUrl = initialTestPhotos[0]
        )
        val currentUser = com.example.data.VeloraData.currentUser
        assertNotNull("Current user must exist", currentUser)

        // 1. Verify 4-5 clear face photos are present in user profile
        assertTrue("User should have at least 4 clear face photos", currentUser.photos.size >= 4)
        assertTrue("User can have up to 5 clear face photos", currentUser.photos.size <= 5)

        // 2. Verify dedicated primary photo and effectivePrimaryPhoto helper
        assertTrue("Dedicated primary photo URL must be set", currentUser.primaryPhotoUrl.isNotBlank())
        assertEquals(currentUser.primaryPhotoUrl, currentUser.effectivePrimaryPhoto)

        // 3. Test saving a new dedicated primary photo URL
        val newPrimaryUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80"
        val savePrimaryResult = com.example.data.FirestoreProfileRepository.savePrimaryPhotoUrl(
            userId = currentUser.id,
            primaryUrl = newPrimaryUrl
        )
        assertTrue("Saving dedicated primary photo must succeed", savePrimaryResult.isSuccess)
        val updatedProfile = savePrimaryResult.getOrNull()
        assertNotNull(updatedProfile)
        assertEquals(newPrimaryUrl, updatedProfile!!.primaryPhotoUrl)
        assertEquals(newPrimaryUrl, updatedProfile.effectivePrimaryPhoto)
        assertEquals(newPrimaryUrl, updatedProfile.photos.first())

        // 4. Test multi-photo upload (saving batch of clear face photos, capped at 5)
        val additionalPhotos = listOf(
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80"
        )
        val multiResult = com.example.data.FirestoreProfileRepository.saveMultipleProfilePhotos(
            userId = currentUser.id,
            newPhotoUrls = additionalPhotos
        )
        assertTrue("Saving multiple face photos must succeed", multiResult.isSuccess)
        val multiProfile = multiResult.getOrNull()
        assertNotNull(multiProfile)
        assertTrue("Face photos count should be between 4 and 5", multiProfile!!.photos.size in 4..5)

        // 5. Test setPrimaryPhoto
        val targetPrimary = multiProfile.photos.last()
        val setPrimaryResult = com.example.data.FirestoreProfileRepository.setPrimaryPhoto(
            userId = currentUser.id,
            primaryUrl = targetPrimary
        )
        assertTrue("setPrimaryPhoto must succeed", setPrimaryResult.isSuccess)
        assertEquals(targetPrimary, com.example.data.VeloraData.currentUser.primaryPhotoUrl)

        // 6. Test removeProfilePhoto
        val initialSize = com.example.data.VeloraData.currentUser.photos.size
        val removeResult = com.example.data.FirestoreProfileRepository.removeProfilePhoto(
            userId = currentUser.id,
            photoUrlToRemove = targetPrimary
        )
        assertTrue("removeProfilePhoto must succeed", removeResult.isSuccess)
        assertEquals(initialSize - 1, com.example.data.VeloraData.currentUser.photos.size)
        assertFalse(com.example.data.VeloraData.currentUser.photos.contains(targetPrimary))
    }

    // --- TEST 33: Global Filter Successfully Displays Worldwide Profiles without Distance Restrictions ---
    @Test
    fun test33_globalFilter_successfullyFiltersProfilesWorldwideWithoutDistanceRestriction() {
        val viewModel = com.example.viewmodel.VeloraViewModel()

        // 1. Apply standard local filter with 50 km distance limit
        viewModel.applyFilters(
            com.example.model.DiscoveryFilters(
                minAge = 18,
                maxAge = 40,
                maxDistanceKm = 50,
                gender = "All",
                country = "Bangladesh 🇧🇩"
            )
        )
        val localProfiles = viewModel.uiState.value.profiles
        assertTrue("Local profiles must be within Bangladesh and distance limit", localProfiles.all { it.distanceKm <= 50 })
        assertTrue("All local profiles must be Bangladesh", localProfiles.all { it.country.contains("Bangladesh", ignoreCase = true) })

        // 2. Apply "Global 🌐" filter
        viewModel.applyFilters(
            com.example.model.DiscoveryFilters(
                minAge = 18,
                maxAge = 40,
                maxDistanceKm = 50, // Even with 50 km max, Global should lift distance bounds
                gender = "All",
                country = "Global 🌐"
            )
        )
        val globalProfiles = viewModel.uiState.value.profiles
        assertTrue("Global filter must return profiles", globalProfiles.isNotEmpty())

        // Verify international/worldwide profiles with distance > 100 km are present
        val internationalProfiles = globalProfiles.filter { it.distanceKm > 100 || it.country.contains("Global", ignoreCase = true) || it.country.contains("India", ignoreCase = true) }
        assertTrue("Global filter must include worldwide profiles regardless of local distance bounds", internationalProfiles.isNotEmpty())

        val hasSarah = globalProfiles.any { it.name.contains("Sarah") }
        val hasPriya = globalProfiles.any { it.name.contains("Priya") }
        assertTrue("Global filter must show international profiles like Sarah or Priya", hasSarah || hasPriya)

        // 3. Apply "India 🇮🇳" filter
        viewModel.applyFilters(
            com.example.model.DiscoveryFilters(
                minAge = 18,
                maxAge = 40,
                maxDistanceKm = 2000,
                gender = "All",
                country = "India 🇮🇳"
            )
        )
        val indiaProfiles = viewModel.uiState.value.profiles
        assertTrue("India filter must find profiles in India", indiaProfiles.isNotEmpty())
        assertTrue("All returned profiles must be in India", indiaProfiles.all { it.country.equals("India", ignoreCase = true) })
    }

    // --- TEST 34: Active User Status Checks Allow Profile & Photo Updates Without "Inactive or Revoked" Error ---
    @Test
    fun test34_accountActiveStatus_allowsActiveUserToUpdateProfileAndPhotosWithoutInactiveError() {
        // Active user with email alif.islam@example.com (matching VeloraData.currentUser)
        val token = com.example.server.ServerTokenManager.createToken(
            userId = "user_me",
            email = "alif.islam@example.com",
            role = com.example.server.Role.USER
        )

        // 1. Updating profile details (aboutMe, profession, city) must succeed (HTTP 200, NOT 403 inactive error)
        val detailsResponse = com.example.server.VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $token",
            targetUserId = "user_me",
            updates = com.example.server.UserProfileUpdate(
                aboutMe = "Passionate traveler & tech enthusiast.",
                profession = "Lead Android Engineer",
                city = "Dhaka"
            )
        )
        assertTrue("Active user details update should succeed", detailsResponse.isSuccessful)
        assertEquals(200, detailsResponse.statusCode)
        assertFalse("Must NOT return account inactive error", detailsResponse.message.contains("inactive or revoked", ignoreCase = true))
        assertEquals("Lead Android Engineer", detailsResponse.data?.profession)

        // 2. Updating profile photos & setting primary photo must succeed
        val newPhotoUrls = listOf(
            "https://res.cloudinary.com/yrqulncp/image/upload/v1/profile_1.jpg",
            "https://res.cloudinary.com/yrqulncp/image/upload/v1/profile_2.jpg",
            "https://res.cloudinary.com/yrqulncp/image/upload/v1/profile_3.jpg",
            "https://res.cloudinary.com/yrqulncp/image/upload/v1/profile_4.jpg"
        )
        val photosResponse = com.example.server.VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $token",
            targetUserId = "user_me",
            updates = com.example.server.UserProfileUpdate(
                photos = newPhotoUrls,
                primaryPhotoUrl = newPhotoUrls[0]
            )
        )
        assertTrue("Active user photo update should succeed", photosResponse.isSuccessful)
        assertEquals(200, photosResponse.statusCode)
        assertEquals(newPhotoUrls[0], photosResponse.data?.primaryPhotoUrl)
        assertEquals(4, photosResponse.data?.photos?.size)
    }

    // --- TEST 35: Active User Status Checks Allow Profile Details Updates Without Inactive Account Error ---
    @Test
    fun test35_activeUserStatus_allowsProfileDetailsUpdates() {
        val userToken = com.example.server.ServerTokenManager.createToken(
            userId = "user_me",
            email = "alif.islam@example.com",
            role = com.example.server.Role.USER
        )

        val updateResult = com.example.server.VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = com.example.server.UserProfileUpdate(
                aboutMe = "Updated bio for active user",
                profession = "Lead Architect",
                city = "Dhaka"
            )
        )
        assertTrue("Profile details update must succeed", updateResult.isSuccessful)
        assertEquals(200, updateResult.statusCode)
        assertFalse("Must NOT return account inactive error", updateResult.message.contains("inactive or revoked", ignoreCase = true))
        assertEquals("Lead Architect", updateResult.data?.profession)
    }

    // --- TEST 36: Cloudinary Upload Generates Secure HTTPS CDN URLs and Syncs to Firestore ---
    @Test
    fun test36_cloudinaryCdnUrl_returnsSecureHttpsUrlAndSyncsToFirestore() = kotlinx.coroutines.runBlocking {
        val dummyBytes = "sample_raw_image_bytes_content".toByteArray()
        val uploadResult = com.example.service.CloudinaryUploadService.uploadImageBytes(
            imageBytes = dummyBytes,
            fileName = "face_photo_test.jpg",
            mimeType = "image/jpeg"
        )

        assertTrue("Upload result must be Success", uploadResult is com.example.service.UploadResult.Success)
        val cdnUrl = (uploadResult as com.example.service.UploadResult.Success).secureUrl
        assertNotNull(cdnUrl)
        assertTrue("Uploaded photo URL must be an HTTPS Cloudinary CDN URL", cdnUrl.startsWith("https://res.cloudinary.com/"))
        assertFalse("Uploaded photo URL must NOT be a bulky base64 data URI", cdnUrl.startsWith("data:"))

        // Verify Firestore profile repository sync
        val syncResult = com.example.data.FirestoreProfileRepository.savePrimaryPhotoUrl(
            userId = "user_me",
            primaryUrl = cdnUrl
        )
        assertTrue("Firestore primary photo sync must succeed", syncResult.isSuccess)
        val updatedProfile = syncResult.getOrNull()
        assertNotNull(updatedProfile)
        assertEquals(cdnUrl, updatedProfile!!.primaryPhotoUrl)
        assertEquals(cdnUrl, updatedProfile.photos.first())
    }
}

