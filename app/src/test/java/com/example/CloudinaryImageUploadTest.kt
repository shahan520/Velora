package com.example

import com.example.data.FirestoreProfileRepository
import com.example.data.VeloraData
import com.example.server.Role
import com.example.server.ServerTokenManager
import com.example.server.UserProfileUpdate
import com.example.server.VeloraBackendServer
import com.example.service.CloudinaryUploadService
import com.example.service.UploadResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudinaryImageUploadTest {

    @Before
    fun setUp() {
        VeloraBackendServer.clearRateLimitsForTesting()
        // Reset currentUser photos
        VeloraData.currentUser = VeloraData.currentUser.copy(
            photos = listOf(
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=800&q=80"
            )
        )
    }

    @Test
    fun test1_cloudinaryService_hasValidDefaultConfiguration() {
        assertEquals("yrqulncp", CloudinaryUploadService.getEffectiveCloudName())
        assertEquals("ml_default", CloudinaryUploadService.getEffectiveUploadPreset())
        assertTrue("Curated sample photos must not be empty", CloudinaryUploadService.sampleProfilePhotos.isNotEmpty())
    }

    @Test
    fun test2_cloudinaryUploadBytes_returnsValidCdnUrl() = runBlocking {
        val dummyImageBytes = "mock_jpeg_image_data_bytes_for_testing".toByteArray()
        val result = CloudinaryUploadService.uploadImageBytes(
            imageBytes = dummyImageBytes,
            fileName = "avatar_test.jpg",
            mimeType = "image/jpeg"
        )

        assertTrue("Upload result should be Success", result is UploadResult.Success)
        val success = result as UploadResult.Success
        assertNotNull("Secure CDN URL must not be null", success.secureUrl)
        assertTrue("URL must be non-blank", success.secureUrl.isNotBlank())
        assertNotNull("Public ID must not be null", success.publicId)
    }

    @Test
    fun test3_saveProfilePhotoUrl_updatesProfileAndFirestoreDocument() = runBlocking {
        val cloudinaryUrl = "https://res.cloudinary.com/velora-app/image/upload/c_fill,g_face,w_800,h_800,q_auto,f_auto/v1/velora_profiles/new_avatar.jpg"

        val result = FirestoreProfileRepository.saveProfilePhotoUrl(
            userId = "user_me",
            newPhotoUrl = cloudinaryUrl,
            setAsPrimary = true
        )

        assertTrue("Saving photo URL should succeed", result.isSuccess)
        val photos = result.getOrNull()
        assertNotNull(photos)
        assertEquals("New Cloudinary photo must be set as primary photo (index 0)", cloudinaryUrl, photos!![0])
        assertEquals("Active current user photos must be updated", cloudinaryUrl, VeloraData.currentUser.photos[0])
    }

    @Test
    fun test4_ordinaryUser_canUpdatePhotos_butBlockedFromTamperingPrivilegedFields() {
        val userToken = ServerTokenManager.createToken(
            userId = "user_me",
            email = "alif@example.com",
            role = Role.USER
        )

        val newPhotoList = listOf("https://res.cloudinary.com/velora-app/image/upload/v1/profile1.jpg")

        // 1. Updating photos only: MUST SUCCEED (Allowed by firestore.rules and server)
        val validUpdate = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(photos = newPhotoList)
        )
        assertTrue("Ordinary user must be allowed to update photos", validUpdate.isSuccessful)
        assertEquals(200, validUpdate.statusCode)
        assertEquals(newPhotoList, validUpdate.data?.photos)

        // 2. Tampering attempt: updating photos while attempting to sneak role = ADMIN: MUST BE FORBIDDEN (403)
        val tamperingUpdate = VeloraBackendServer.updateUserProfile(
            authHeader = "Bearer $userToken",
            targetUserId = "user_me",
            updates = UserProfileUpdate(
                photos = newPhotoList,
                role = Role.ADMIN
            )
        )
        assertFalse("Attempt to alter role during photo update MUST fail", tamperingUpdate.isSuccessful)
        assertEquals(403, tamperingUpdate.statusCode)
        assertEquals("Forbidden", tamperingUpdate.error)
    }

    @Test
    fun test5_setPrimaryPhoto_reordersPhotosCorrectly() = runBlocking {
        val photo1 = "https://res.cloudinary.com/velora-app/image/upload/photo1.jpg"
        val photo2 = "https://res.cloudinary.com/velora-app/image/upload/photo2.jpg"

        FirestoreProfileRepository.saveProfilePhotoUrl("user_me", photo1, setAsPrimary = true)
        FirestoreProfileRepository.saveProfilePhotoUrl("user_me", photo2, setAsPrimary = false)

        val reorderResult = FirestoreProfileRepository.setPrimaryPhoto("user_me", photo2)
        assertTrue(reorderResult.isSuccess)
        assertEquals("photo2 must now be primary", photo2, reorderResult.getOrNull()!![0])
    }

    @Test
    fun test6_removeProfilePhoto_removesCorrectPhoto_andPreventsEmptyList() = runBlocking {
        val photo1 = "https://res.cloudinary.com/velora-app/image/upload/photo1.jpg"
        val photo2 = "https://res.cloudinary.com/velora-app/image/upload/photo2.jpg"

        VeloraData.currentUser = VeloraData.currentUser.copy(photos = listOf(photo1, photo2))

        val removeResult = FirestoreProfileRepository.removeProfilePhoto("user_me", photo1)
        assertTrue(removeResult.isSuccess)
        assertFalse("Removed photo should no longer exist", removeResult.getOrNull()!!.contains(photo1))
        assertEquals(listOf(photo2), removeResult.getOrNull())

        // Attempting to remove the last remaining photo must fail safely
        val removeLastResult = FirestoreProfileRepository.removeProfilePhoto("user_me", photo2)
        assertFalse("Cannot remove last photo", removeLastResult.isSuccess)
    }

    @Test
    fun test7_sessionPersistence_restoresUserSessionAndPreventsAutoLogout() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.example.data.SessionManager.initialize(context)

        // 1. Save user session (simulate active login)
        val testProfile = com.example.data.VeloraData.currentUser.copy(
            id = "user_me",
            name = "Alif Islam",
            email = "alif.islam@example.com",
            aboutMe = "Active persistent member"
        )
        com.example.data.SessionManager.saveUserSession(
            context = context,
            profile = testProfile,
            isPhoneVerified = true,
            isEmailVerified = true,
            isAgeVerified = true,
            lastScreen = com.example.model.Screen.MAIN_DISCOVER
        )

        // 2. Verify SessionManager reflects logged in state
        assertTrue("SessionManager must report logged in", com.example.data.SessionManager.isLoggedIn(context))
        val restored = com.example.data.SessionManager.restoreUserSession(context)
        assertNotNull("Restored profile must not be null", restored)
        assertEquals("Alif Islam", restored!!.name)
        assertEquals("alif.islam@example.com", restored.email)

        // 3. Create a fresh ViewModel and restore session (simulating app process restart / resume from background)
        val viewModel = com.example.viewmodel.VeloraViewModel()
        assertEquals("Before restore, default screen is SPLASH", com.example.model.Screen.SPLASH, viewModel.uiState.value.currentScreen)

        viewModel.restoreSession(context)

        // User must be kept logged in without needing to re-login or re-verify OTP
        assertTrue("User must stay logged in after restore", viewModel.uiState.value.isLoggedIn)
        assertTrue("Phone verification must stay intact", viewModel.uiState.value.isPhoneVerified)
        assertTrue("Email verification must stay intact", viewModel.uiState.value.isEmailVerified)
        assertTrue("Age verification must stay intact", viewModel.uiState.value.isAgeVerified)
        assertEquals("User should land on MAIN_DISCOVER without seeing Splash or OTP screens", com.example.model.Screen.MAIN_DISCOVER, viewModel.uiState.value.currentScreen)
    }
}
