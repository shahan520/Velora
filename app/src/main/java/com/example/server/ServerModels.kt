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
    val moderationStatus: String = "ACTIVE",
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val phone: String = ""
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
    val expiresAtMillis: Long,
    val admin: Boolean = (role == Role.ADMIN)
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
    val photos: List<String>? = null,
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
    val currency: String = "BDT",
    val paymentMethod: String = "BKASH",
    val senderAccount: String = ""
)

data class MfsMerchantConfig(
    val bKashEnabled: Boolean = true,
    val bKashMerchantNumber: String = "01711-987654",
    val bKashMerchantType: String = "Merchant",
    val nagadEnabled: Boolean = true,
    val nagadMerchantNumber: String = "01812-345678",
    val nagadMerchantType: String = "Merchant",
    val currency: String = "BDT",
    val merchantCounter: String = "1"
)

data class MfsPaymentTransaction(
    val id: String,
    val trxId: String,
    val method: String,
    val senderPhone: String,
    val amountBdt: Int,
    val currency: String = "BDT",
    val planId: String,
    val planName: String,
    val userId: String,
    val userEmail: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "VERIFIED"
)

data class PendingPhoneOtp(
    val phone: String,
    val otp: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val attempts: Int = 0
)

data class PendingEmailCode(
    val email: String,
    val code: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val attempts: Int = 0
)


