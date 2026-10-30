package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Screen
import com.example.model.UserProfile
import org.json.JSONArray

/**
 * SessionManager handles persistent authentication and profile state across app lifecycles.
 *
 * Ensures that when the app is minimized, sent to background, or killed and reopened,
 * the user stays logged in and is never forced to re-verify OTP or re-login.
 */
object SessionManager {

    private const val PREFS_NAME = "velora_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_PRIMARY_PHOTO = "primary_photo_url"
    private const val KEY_PHOTOS_JSON = "photos_json"
    private const val KEY_ABOUT_ME = "about_me"
    private const val KEY_PROFESSION = "profession"
    private const val KEY_CITY = "city"
    private const val KEY_IS_PHONE_VERIFIED = "is_phone_verified"
    private const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
    private const val KEY_IS_AGE_VERIFIED = "is_age_verified"
    private const val KEY_LAST_SCREEN = "last_active_screen"

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(context: Context? = appContext): SharedPreferences? {
        val ctx = context ?: appContext ?: return null
        return try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (_: Throwable) {
            null
        }
    }

    fun isLoggedIn(context: Context? = appContext): Boolean {
        return getPrefs(context)?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
    }

    fun saveUserSession(
        context: Context? = appContext,
        profile: UserProfile,
        isPhoneVerified: Boolean = true,
        isEmailVerified: Boolean = true,
        isAgeVerified: Boolean = true,
        lastScreen: Screen = Screen.MAIN_DISCOVER
    ) {
        val prefs = getPrefs(context) ?: return
        try {
            val photosArray = JSONArray()
            profile.photos.forEach { photosArray.put(it) }

            prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, profile.id)
                .putString(KEY_USER_NAME, profile.name)
                .putString(KEY_USER_EMAIL, profile.email)
                .putString(KEY_USER_PHONE, profile.phone)
                .putString(KEY_PRIMARY_PHOTO, profile.effectivePrimaryPhoto)
                .putString(KEY_PHOTOS_JSON, photosArray.toString())
                .putString(KEY_ABOUT_ME, profile.aboutMe)
                .putString(KEY_PROFESSION, profile.profession)
                .putString(KEY_CITY, profile.city)
                .putBoolean(KEY_IS_PHONE_VERIFIED, isPhoneVerified)
                .putBoolean(KEY_IS_EMAIL_VERIFIED, isEmailVerified)
                .putBoolean(KEY_IS_AGE_VERIFIED, isAgeVerified)
                .putString(KEY_LAST_SCREEN, lastScreen.name)
                .apply()
        } catch (_: Exception) {}
    }

    fun updateActiveScreen(context: Context? = appContext, screen: Screen) {
        if (screen in listOf(
                Screen.MAIN_DISCOVER,
                Screen.MAIN_MATCHES,
                Screen.MAIN_CHAT,
                Screen.MAIN_PROFILE,
                Screen.EDIT_PROFILE,
                Screen.SEARCH_FILTERS,
                Screen.PREMIUM_STORE,
                Screen.ADMIN_PANEL
            )
        ) {
            getPrefs(context)?.edit()?.putString(KEY_LAST_SCREEN, screen.name)?.apply()
        }
    }

    fun getLastScreen(context: Context? = appContext): Screen {
        val name = getPrefs(context)?.getString(KEY_LAST_SCREEN, Screen.MAIN_DISCOVER.name)
        return try {
            if (!name.isNullOrBlank()) Screen.valueOf(name) else Screen.MAIN_DISCOVER
        } catch (_: Exception) {
            Screen.MAIN_DISCOVER
        }
    }

    fun restoreUserSession(context: Context? = appContext): UserProfile? {
        val prefs = getPrefs(context) ?: return null
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return null

        val id = prefs.getString(KEY_USER_ID, VeloraData.currentUser.id) ?: VeloraData.currentUser.id
        val name = prefs.getString(KEY_USER_NAME, VeloraData.currentUser.name) ?: VeloraData.currentUser.name
        val email = prefs.getString(KEY_USER_EMAIL, VeloraData.currentUser.email) ?: VeloraData.currentUser.email
        val phone = prefs.getString(KEY_USER_PHONE, VeloraData.currentUser.phone) ?: VeloraData.currentUser.phone
        val primaryPhoto = prefs.getString(KEY_PRIMARY_PHOTO, VeloraData.currentUser.primaryPhotoUrl) ?: ""
        val aboutMe = prefs.getString(KEY_ABOUT_ME, VeloraData.currentUser.aboutMe) ?: VeloraData.currentUser.aboutMe
        val profession = prefs.getString(KEY_PROFESSION, VeloraData.currentUser.profession) ?: VeloraData.currentUser.profession
        val city = prefs.getString(KEY_CITY, VeloraData.currentUser.city) ?: VeloraData.currentUser.city

        val photos = mutableListOf<String>()
        val photosJsonStr = prefs.getString(KEY_PHOTOS_JSON, null)
        if (!photosJsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(photosJsonStr)
                for (i in 0 until array.length()) {
                    photos.add(array.getString(i))
                }
            } catch (_: Exception) {}
        }
        if (photos.isEmpty()) {
            photos.addAll(VeloraData.currentUser.photos)
        }

        return VeloraData.currentUser.copy(
            id = id,
            name = name,
            email = email,
            phone = phone,
            primaryPhotoUrl = primaryPhoto.ifBlank { photos.firstOrNull().orEmpty() },
            photos = photos,
            aboutMe = aboutMe,
            profession = profession,
            city = city,
            isPhoneVerified = prefs.getBoolean(KEY_IS_PHONE_VERIFIED, true),
            isEmailVerified = prefs.getBoolean(KEY_IS_EMAIL_VERIFIED, true)
        )
    }

    fun clearSession(context: Context? = appContext) {
        getPrefs(context)?.edit()?.clear()?.apply()
    }
}
