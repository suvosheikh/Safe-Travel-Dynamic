package com.safetravel.tracker.notification

import android.util.Base64
import android.util.Log
import com.safetravel.tracker.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.TimeZone
import java.util.Calendar

object FcmNotificationSender {
    private const val TAG = "FcmNotificationSender"

    // Hardcoded Service Account parameters securely matching the user's provided config
    private const val CLIENT_EMAIL = "firebase-adminsdk-fbsvc@safe-travel-1984b.iam.gserviceaccount.com"
    private const val PROJECT_ID = "safe-travel-1984b"
    private const val PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCsQVdXoReqcDGk\nhMbP4opxQS8kOtcO00b3mrdYC/CKSvcf7S5LAfiYegnh6eQ2Zf5JK17S132mnRYO\nd2eX764w77hUs3oeaDZ8xfFFepdGBqaAN7PSoV5JiojWeH1Kbb/YAblJVHR2JvLQ\nJ7IUfw8bfkxfT9jTdsCYgo70ZlkG5Y1y52a7hrJ+p4sWK16qKHd4KisKClJzui06\nGiJ29Y9DaieTjSN8GHQLuE1GjLq74m0Ucrx1+13Dk7bS2+OXodzTNMr1A6AI7ZkJ\nUkX7WdybZ8ZNWiVNV1FWSSjnb4e5cCQP8sTFRqcxEVYh3U9/6GHH8rXCR1cFle9n\n6iGbkAJLAgMBAAECggEABRcb46UJCVUDbTSB7jFoR0HWiMrhrA3Q8mQ0uCSyLlla\nGtbBdXSmH1YL5gx+sp9/ojM75sh4wsGwSIEoIzScAH+KV5vDfL6Lov2edvUmdJVM\nZPbrGMsCpEhfFCdqBxcIgkjwFsX+Iw0HmTXIbL6hzagvzZPoeRHwoxEd9FmfQTWJ\nob0+y6M0zPP4FZKrX0cX5uHqnu7GYT8LzaIscQWPbGeCkKZi2+KavR/4gk3zMK7i\nPE6CBWHcB7RP1vwQh03agVmZMGwdJi46B4cZfeC00nzHskVZ8UeoEQFrrJRYkSWI\nLOrWtVx/N1al2ixa5ffJvRcOP+klSKnQOjzqyqgTwQKBgQDXAVsw913V9mEqfnxi\n8TA/UvrfsgtPv13sQMRab906udlySLRoVeoWpPU7dNrVX73exUuwa3DiNid4yqVt\nzPXizexIjmasClWsx8n6IMHIeIPJPpH424te2tV8wB1ovUp5yhXRrXGJVcVDNnIK\ndaxn4lMrWjuiUpjPWdFLtHrfGwKBgQDNGU7OGpvktPCr/78SutjHhqx+9dFdeCjT\nstbI7VzU8EhRg9M+gYxmEsYzzG9XTwCn+N56THkzLBa790461QUYWjiTZ/Bf/toq\nEDXEA4umFoSGqBEzPBgVpPAaC0tyxQ59clHZ84rXBtkt3XfRYPIUN9Nx3kzCeMUj\nC2Pegc0skQKBgAkHRY0I1ifl8K6YISEGf8Ao+9/o2hC/OHJzRcuGZwPQShFCfFDl\nSq8oviOBBK9xFcgFAOinmlXlcELiP5AcyzZ67zEcybLvvClFOnYGahzA8sfAmOCa\nV3/kMJLMfm+ngzkaA1CKt/nMYDwyTRejWVoDZvqrD1YBoekTHHbRXVeBAoGBAJ0N\nLBMWvRk8lK6gf6fP+/+NWLk/crqoMFsrLt1dJSDJfhctYq0SdngTGELB4OByVoZJ\nQ9NSi9xrfl0g2n6ib+xd6cS2apKXjGd5UjJupJjh1UlnmQQqmCa6zkCjNXsE+juF\nNrg2jbJqRBOi6wUhvz1MIlWnbA/eetaC+k6ABqSBAoGAQRZ4amm7WZYWRxI/qpdf\n1UdjXIqhtPS8gfYf+qAJ3rf4Arfg+4DKbCoy6/PoEr/wMgdgjnkGKdy4zYTowWb6\nNbtMN1HZZsBe3SsarqyRwUnYyzyToIIA4w9kyqTdJHd+04fOx53DNo56wK4Ou7Vf\nm5+w7SGllgviBgwxH6gD+08=\n-----END PRIVATE KEY-----"

    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0

    /**
     * Obtains a valid OAuth2 Access Token from Google OAuth servers using RSA-256 JWT signing.
     */
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        if (cachedAccessToken != null && nowMs < tokenExpiryTime) {
            return@withContext cachedAccessToken
        }

        try {
            Log.d(TAG, "Generating new Google OAuth2 access token...")
            // Parse PKCS8 private key
            val privateKeyPemClean = PRIVATE_KEY_PEM
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
            
            val keyBytes = Base64.decode(privateKeyPemClean, Base64.DEFAULT)
            val spec = PKCS8EncodedKeySpec(keyBytes)
            val kf = KeyFactory.getInstance("RSA")
            val privateKey = kf.generatePrivate(spec)

            // Create JWT Header & Claimset
            val header = Base64.encodeToString(
                "{\"alg\":\"RS256\",\"typ\":\"JWT\"}".toByteArray(),
                Base64.NO_WRAP or Base64.URL_SAFE
            )

            val nowSeconds = nowMs / 1000
            val expirySeconds = nowSeconds + 3600
            val claimSet = "{\"iss\":\"$CLIENT_EMAIL\",\"scope\":\"https://www.googleapis.com/auth/firebase.messaging\",\"aud\":\"https://oauth2.googleapis.com/token\",\"exp\":$expirySeconds,\"iat\":$nowSeconds}"
            
            val payload = Base64.encodeToString(
                claimSet.toByteArray(),
                Base64.NO_WRAP or Base64.URL_SAFE
            )

            val signatureInput = "$header.$payload"
            val signatureInstance = Signature.getInstance("SHA256withRSA")
            signatureInstance.initSign(privateKey)
            signatureInstance.update(signatureInput.toByteArray())
            
            val signatureBytes = signatureInstance.sign()
            val signature = Base64.encodeToString(
                signatureBytes,
                Base64.NO_WRAP or Base64.URL_SAFE
            )

            val assertionJwt = "$signatureInput.$signature"

            // Call Google OAuth Token Endpoint
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .add("assertion", assertionJwt)
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "OAuth failed with code: ${response.code} - ${response.body?.string()}")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                val jsonResponse = JSONObject(responseBodyStr)
                val token = jsonResponse.getString("access_token")
                val expiresIn = jsonResponse.getLong("expires_in")

                cachedAccessToken = token
                tokenExpiryTime = System.currentTimeMillis() + ((expiresIn - 60) * 1000) // cache with 1-min buffer
                Log.d(TAG, "Successfully cached Google OAuth2 token.")
                return@withContext token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Google OAuth2 access token: ", e)
            return@withContext null
        }
    }

    /**
     * Sends a push notification to a specific guardian's device using Google FCM v1 API.
     */
    suspend fun sendNotificationToGuardian(
        guardianFcmToken: String,
        title: String,
        body: String,
        tripId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false

        try {
            val client = OkHttpClient()
            
            // Build FCM v1 JSON Payload
            val messageJson = JSONObject().apply {
                put("token", guardianFcmToken)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
                put("data", JSONObject().apply {
                    put("trip_id", tripId)
                    put("type", "trip_share")
                })
            }
            
            val payload = JSONObject().apply {
                put("message", messageJson)
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Push notification sent successfully to token $guardianFcmToken")
                    return@withContext true
                } else {
                    Log.e(TAG, "FCM send failed: ${response.code} - ${response.body?.string()}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending push notification via FCM: ", e)
            return@withContext false
        }
    }
}
