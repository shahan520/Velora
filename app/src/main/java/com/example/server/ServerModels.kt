package com.example.server

import com.example.model.UserProfile
import com.example.model.UserReport

enum class Role {
    USER,
    ADMIN
}

data class ServerUser(
    val id: String,
    val email: String,
    val passwordHash: String,
    val role: Role,
    val displayName: String,
    val isSuspended: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val paymentStatus: String = "none",
    val moderationStatus: String = "ACTIVE"
)

data class ServerResponse<T>(
    val statusCode: Int,
    val message: String,
    val data: T? = null,
    val error: String? = null
) {
    val isSuccessful: Boolean get() = statusCode in 200..299
}

data class AuthTokenPayload(
    val userId: String,
    val email: String,
    val role: Role,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long
)

data class AdminDashboardData(
    val totalUsers: Int,
    val activeUsers: Int,
    val totalMatches: Int,
    val openReports: Int,
    val verifiedRevenue: String,
    val userLocations: Map<String, Int>
)

data class ConversationEntity(
    val id: String,
    val participantIds: Set<String>,
    val createdAtMillis: Long,
    val lastMessage: String = "",
    val lastMessageAtMillis: Long = 0L,
    val isArchived: Boolean = false
)

data class ServerChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val timestampMillis: Long,
    val isRead: Boolean = false
)

data class UserProfileUpdate(
    val aboutMe: String? = null,
    val bio: String? = null,
    val profession: String? = null,
    val city: String? = null,
    val interests: List<String>? = null,
    // Privileged fields (attempted update must be blocked for ordinary users)
    val role: Role? = null,
    val isVerified: Boolean? = null,
    val isPremium: Boolean? = null,
    val paymentStatus: String? = null,
    val isSuspended: Boolean? = null,
    val moderationStatus: String? = null
)

data class PaymentVerificationRequest(
    val planId: String,
    val transactionReceipt: String,
    val amount: String,
    val currency: String = "BDT"
)
