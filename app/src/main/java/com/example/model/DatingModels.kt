package com.example.model

enum class Screen {
    SPLASH,
    WELCOME,
    AGE_VERIFICATION,
    LANGUAGE_SELECTION,
    MAIN_DISCOVER,
    MAIN_MATCHES,
    MAIN_CHAT,
    MAIN_PROFILE,
    CHAT_ROOM,
    VIDEO_CALL,
    PROFILE_DETAILS,
    PREMIUM_STORE,
    BOOST_STORE,
    NOTIFICATIONS,
    SEARCH_FILTERS,
    EDIT_PROFILE,
    BLOCK_REPORT,
    ADMIN_LOGIN,
    ADMIN_PANEL
}

enum class AdminSection {
    DASHBOARD,
    USERS,
    REPORTS,
    MESSAGES,
    ANALYTICS,
    SUBSCRIPTIONS,
    SETTINGS
}

data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val gender: String, // "Woman", "Man", "Other"
    val city: String,
    val country: String,
    val distanceKm: Int,
    val profession: String,
    val height: String,
    val relationshipStatus: String, // "Single", "Dating", "Looking for relationship"
    val aboutMe: String,
    val interests: List<String>,
    val photos: List<String>,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val lastActive: String = "Just now",
    val isSuspended: Boolean = false,
    val isPremium: Boolean = false,
    val joinedDate: String = "10 Oct 2026",
    val email: String = "",
    val phone: String = ""
)

data class MatchItem(
    val id: String,
    val user: UserProfile,
    val matchedAt: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isNew: Boolean = false
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val isRead: Boolean = true,
    val mediaUrl: String? = null
)

data class UserReport(
    val id: String,
    val type: String, // "Profile", "Message", "Content"
    val reporterName: String,
    val reportedUserId: String,
    val reportedUserName: String,
    val reason: String,
    val date: String,
    var status: String = "Pending" // "Pending", "Under Review", "Resolved", "Dismissed"
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val durationMonths: Int,
    val priceBdt: Int,
    val originalPriceBdt: Int? = null,
    val savingsPercent: Int = 0,
    val features: List<String>,
    val isPopular: Boolean = false
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val type: String, // "MATCH", "MESSAGE", "VIEW", "LIKE", "SYSTEM"
    val avatarUrl: String? = null,
    val isRead: Boolean = false
)

data class DiscoveryFilters(
    val minAge: Int = 18,
    val maxAge: Int = 35,
    val maxDistanceKm: Int = 50,
    val gender: String = "All",
    val country: String = "Any",
    val language: String = "Any"
)

data class LanguageOption(
    val code: String,
    val name: String,
    val localName: String,
    val flag: String
)
