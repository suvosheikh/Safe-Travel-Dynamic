package com.safetravel.tracker.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream

/**
 * Modular Image Upload Manager.
 * Designed to be easily swappable with other hosting services (e.g., self-hosted, AWS S3, etc.)
 */
object ImageUploadManager {
    private val IMGBB_API_KEY: String
        get() = try { com.safetravel.tracker.BuildConfig.IMGBB_API_KEY } catch (e: Exception) { "" }
    private val client = OkHttpClient()

    /**
     * Uploads an image prioritizing Cloudinary, with ImgBB as secondary fallback.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        // 1. Primary: Cloudinary User-Scoped Upload
        try {
            val cloudinaryResult = CloudinaryImageUploader.uploadProfileImage(context, imageUri)
            if (cloudinaryResult != null && cloudinaryResult.secureUrl.isNotBlank()) {
                return@withContext cloudinaryResult.secureUrl
            }
        } catch (e: Exception) {
            android.util.Log.w("ImageUpload", "Cloudinary primary upload failed, attempting fallback: ${e.message}")
        }

        // 2. Secondary Fallback: ImgBB
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes() ?: return@withContext null
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", IMGBB_API_KEY)
                .addFormDataPart("image", "profile_image.jpg", bytes.toRequestBody())
                .build()

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext null
                val json = JSONObject(responseBody)
                val data = json.getJSONObject("data")
                return@withContext data.getString("display_url")
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUpload", "ImgBB fallback upload failed", e)
        }
        return@withContext null
    }
}
