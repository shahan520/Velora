package com.example.server

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Server-side cryptographic token manager.
 * Uses HMAC-SHA256 with a server-private secret to generate and verify tamper-proof session tokens.
 */
object ServerTokenManager {

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private val SERVER_SECRET = "velora_master_secret_key_8f3a2b1c4e5d6f7a8b9c0d1e2f3a4b5c".toByteArray(StandardCharsets.UTF_8)

    fun createToken(userId: String, email: String, role: Role, durationMillis: Long = 24 * 3600 * 1000L): String {
        val now = System.currentTimeMillis()
        val expiresAt = now + durationMillis
        val isAdmin = role == Role.ADMIN

        val headerJson = """{"alg":"HS256","typ":"JWT"}"""
        // Injects Firebase Auth custom claims: "role" and "admin" (boolean)
        val payloadJson = """{"sub":"$userId","email":"$email","role":"${role.name}","admin":$isAdmin,"iat":$now,"exp":$expiresAt}"""

        val encodedHeader = base64UrlEncode(headerJson.toByteArray(StandardCharsets.UTF_8))
        val encodedPayload = base64UrlEncode(payloadJson.toByteArray(StandardCharsets.UTF_8))
        val contentToSign = "$encodedHeader.$encodedPayload"

        val signature = computeHmacSha256(contentToSign, SERVER_SECRET)
        val encodedSignature = base64UrlEncode(signature)

        return "$encodedHeader.$encodedPayload.$encodedSignature"
    }

    fun verifyToken(token: String): AuthTokenPayload {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw SecurityException("Malformed token: expected 3 parts, got ${parts.size}")
        }

        val encodedHeader = parts[0]
        val encodedPayload = parts[1]
        val encodedSignature = parts[2]

        val contentToSign = "$encodedHeader.$encodedPayload"
        val expectedSignature = computeHmacSha256(contentToSign, SERVER_SECRET)
        val actualSignature = base64UrlDecode(encodedSignature)

        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw SecurityException("Cryptographic signature mismatch: Token has been forged or tampered with.")
        }

        val payloadString = String(base64UrlDecode(encodedPayload), StandardCharsets.UTF_8)

        val sub = extractJsonValue(payloadString, "sub")
            ?: throw SecurityException("Missing sub claim in token")
        val email = extractJsonValue(payloadString, "email")
            ?: throw SecurityException("Missing email claim in token")
        val roleStr = extractJsonValue(payloadString, "role")
            ?: throw SecurityException("Missing role claim in token")
        val adminStr = extractJsonValue(payloadString, "admin")
        val iatStr = extractJsonValue(payloadString, "iat") ?: "0"
        val expStr = extractJsonValue(payloadString, "exp") ?: "0"

        val expiresAt = expStr.toLongOrNull() ?: 0L
        val now = System.currentTimeMillis()
        if (now > expiresAt) {
            throw SecurityException("Token expired at $expiresAt (current time: $now)")
        }

        val role = try {
            Role.valueOf(roleStr)
        } catch (e: Exception) {
            Role.USER
        }

        val adminClaim = adminStr?.equals("true", ignoreCase = true) ?: (role == Role.ADMIN)

        // Strict Custom Claim Integrity: An admin claim must match Role.ADMIN
        val effectiveIsAdmin = (role == Role.ADMIN) && adminClaim

        return AuthTokenPayload(
            userId = sub,
            email = email,
            role = role,
            issuedAtMillis = iatStr.toLongOrNull() ?: now,
            expiresAtMillis = expiresAt,
            admin = effectiveIsAdmin
        )
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"?([^,\"}]+)\"?")
        return regex.find(json)?.groupValues?.get(1)?.trim()
    }

    private fun computeHmacSha256(data: String, key: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val secretKey = SecretKeySpec(key, HMAC_ALGORITHM)
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun base64UrlDecode(str: String): ByteArray {
        return Base64.getUrlDecoder().decode(str)
    }
}
