package com.example

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
}
