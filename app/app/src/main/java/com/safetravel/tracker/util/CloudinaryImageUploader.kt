package com.safetravel.tracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import com.safetravel.tracker.supabase.SupabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Dedicated Modular Utility for Uploading User Images to Cloudinary.
 * Features in-memory downscaling and JPEG compression to prevent OOM
 * and drastically conserve Cloudinary storage credits and user bandwidth.
 * Kept completely decoupled and separate from audio pipelines.
 */
object CloudinaryImageUploader {
    private const val TAG = "CloudinaryImgUploader"

    // Cloudinary Configurations dynamically populated from Supabase app_remote_configs
    var cloudName: String = "yo6dndbb"
        get() {
            val remote = SupabaseManager.getCachedConfig("cloudinary_cloud_name")
            return if (remote.isNotBlank()) remote else field
        }

    var uploadPreset: String = "safetravel_images"
        get() {
            val imagePreset = SupabaseManager.getCachedConfig("cloudinary_image_preset")
            if (imagePreset.isNotBlank()) return imagePreset
            val genericPreset = SupabaseManager.getCachedConfig("cloudinary_upload_preset")
            return if (genericPreset.isNotBlank()) genericPreset else field
        }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class UploadResult(
        val secureUrl: String,
        val publicId: String?,
        val fileSizeBytes: Long
    )

    /**
     * Memory-safe in-memory downscaling and compression of any content Uri.
     * Prevents high-resolution camera images (5MB-20MB) from crashing the VM with OOM.
     */
    fun compressUriToJpegBytes(
        context: Context,
        imageUri: Uri,
        maxDimension: Int = 1280,
        quality: Int = 80
    ): ByteArray? {
        return try {
            // 1. Decode bounds only
            val boundsStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(boundsStream, null, options)
            boundsStream.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // 2. Compute sample size
            var inSampleSize = 1
            var halfWidth = options.outWidth / 2
            var halfHeight = options.outHeight / 2
            while (halfWidth / inSampleSize >= maxDimension && halfHeight / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }

            // 3. Decode sampled bitmap with RGB_565 to cut memory consumption in half
            val decodeStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            var bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
            decodeStream.close()
            if (bitmap == null) return null

            // 4. Handle EXIF rotation safely
            try {
                val exifStream = context.contentResolver.openInputStream(imageUri)
                if (exifStream != null) {
                    val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ExifInterface(exifStream)
                    } else null
                    val orientation = exif?.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    ) ?: ExifInterface.ORIENTATION_NORMAL
                    exifStream.close()

                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (!matrix.isIdentity) {
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        if (rotated != bitmap) {
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exif rotation adjustment bypassed: ${e.message}")
            }

            // 5. Downscale if dimensions still exceed maxDimension
            val width = bitmap.width
            val height = bitmap.height
            if (width > maxDimension || height > maxDimension) {
                val ratio = maxDimension.toFloat() / Math.max(width, height)
                val targetW = (width * ratio).toInt().coerceAtLeast(1)
                val targetH = (height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }

            // 6. Compress to JPEG byte array
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            bitmap.recycle()
            baos.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image Uri: ${e.message}", e)
            null
        }
    }

    /**
     * Uploads user profile avatar to Cloudinary under:
     * safetravel/images/[userId]/profile/avatar_[timestamp]
     */
    suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String? = null
    ): UploadResult? = withContext(Dispatchers.IO) {
        val resolvedUid = if (!userId.isNullOrBlank()) {
            userId
        } else {
            SupabaseManager.currentUserId ?: "anonymous"
        }
        val shortUid = resolvedUid.take(8)
        val folderPath = "safetravel/images/$resolvedUid/profile"
        val publicId = "avatar_${System.currentTimeMillis()}"

        // Compress profile avatar to max 500x500 at 80% JPEG quality (~60KB - 100KB)
        val jpegBytes = compressUriToJpegBytes(context, imageUri, maxDimension = 500, quality = 80)
        if (jpegBytes == null || jpegBytes.isEmpty()) {
            Log.w(TAG, "Failed to compress profile image for upload")
            return@withContext null
        }

        val tags = "safetravel,image,profile,user-$shortUid"
        val contextMap = mapOf(
            "user_id" to shortUid,
            "category" to "profile_avatar"
        )

        executeUpload(
            jpegBytes = jpegBytes,
            folderPath = folderPath,
            publicId = publicId,
            tags = tags,
            contextMap = contextMap
        )
    }

    /**
     * Uploads vehicle or transit photo when starting a trip under:
     * safetravel/images/[userId]/trips/[tripId]/vehicle_[timestamp]
     */
    suspend fun uploadTripVehiclePhoto(
        context: Context,
        imageUri: Uri,
        userId: String? = null,
        tripId: String? = null
    ): UploadResult? = withContext(Dispatchers.IO) {
        val resolvedUid = if (!userId.isNullOrBlank()) {
            userId
        } else {
            SupabaseManager.currentUserId ?: "anonymous"
        }
        val shortUid = resolvedUid.take(8)
        val folderPath = if (!tripId.isNullOrBlank()) {
            "safetravel/images/$resolvedUid/trips/$tripId"
        } else {
            "safetravel/images/$resolvedUid/trips"
        }
        val publicId = "vehicle_${System.currentTimeMillis()}"

        // Compress vehicle / license plate photo to max 1280x1280 at 80% JPEG quality (~150KB - 250KB)
        val jpegBytes = compressUriToJpegBytes(context, imageUri, maxDimension = 1280, quality = 80)
        if (jpegBytes == null || jpegBytes.isEmpty()) {
            Log.w(TAG, "Failed to compress vehicle photo for upload")
            return@withContext null
        }

        val tags = "safetravel,image,vehicle_photo,user-$shortUid"
        val contextMap = mapOf(
            "user_id" to shortUid,
            "category" to "vehicle_plate_photo",
            "trip_id" to (tripId ?: "pre_trip")
        )

        executeUpload(
            jpegBytes = jpegBytes,
            folderPath = folderPath,
            publicId = publicId,
            tags = tags,
            contextMap = contextMap
        )
    }

    /**
     * Executes the multipart HTTP request directly to Cloudinary's image upload endpoint.
     */
    private fun executeUpload(
        jpegBytes: ByteArray,
        folderPath: String,
        publicId: String,
        tags: String,
        contextMap: Map<String, String>
    ): UploadResult? {
        try {
            val contextString = contextMap.entries.joinToString("|") { (k, v) ->
                "${sanitizeMeta(k)}=${sanitizeMeta(v)}"
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folderPath)
                .addFormDataPart("public_id", publicId)
                .addFormDataPart("tags", tags)
                .addFormDataPart("context", contextString)
                .addFormDataPart(
                    "file",
                    "$publicId.jpg",
                    jpegBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url", "")
                val respPublicId = json.optString("public_id", publicId)
                val bytesCount = json.optLong("bytes", jpegBytes.size.toLong())

                if (secureUrl.isNotBlank()) {
                    Log.d(TAG, "Cloudinary image upload succeeded: $secureUrl")
                    return UploadResult(
                        secureUrl = secureUrl,
                        publicId = respPublicId,
                        fileSizeBytes = bytesCount
                    )
                }
            }

            Log.w(TAG, "Cloudinary image upload failed with code ${response.code}: $responseBody")
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary image upload exception: ${e.message}", e)
        }
        return null
    }

    private fun sanitizeMeta(input: String): String {
        return input.replace("|", "-")
            .replace("=", "-")
            .replace("\n", " ")
            .replace("\r", " ")
            .filter { it.code in 32..126 } // Strictly ASCII printable (Rule 9)
            .take(60)
    }
}
