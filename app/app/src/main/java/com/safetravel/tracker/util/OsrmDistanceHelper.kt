package com.safetravel.tracker.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OsrmDistanceHelper {
    private const val TAG = "OsrmDistanceHelper"

    /**
     * OSRM Table API ব্যবহার করে ইউজারের লোকেশন থেকে একাধিক গন্তব্যের বাস্তব ড্রাইভিং দূরত্ব (কিলোমিটারে) বের করে।
     * @param userLat ইউজারের অক্ষাংশ (Latitude)
     * @param userLng ইউজারের দ্রাঘিমাংশ (Longitude)
     * @param destinations গন্তব্যের কোঅর্ডিনেটগুলোর লিস্ট (Pair of Lat, Lng)
     * @return প্রতিটি গন্তব্যের বাস্তব দূরত্ব কিলোমিটারে (Double?)। কোনো গন্তব্যের দূরত্ব না পাওয়া গেলে বা এরর হলে null রিটার্ন করবে।
     */
    suspend fun fetchRealRoadDistances(
        userLat: Double,
        userLng: Double,
        destinations: List<Pair<Double, Double>>
    ): List<Double?> = withContext(Dispatchers.IO) {
        if (destinations.isEmpty()) return@withContext emptyList<Double?>()

        try {
            // ওএসআরএম ফরম্যাট: Lng,Lat;Lng,Lat;Lng,Lat...
            // প্রথম কোঅর্ডিনেটটি হবে আমাদের সোর্স (ইউজারের পজিশন)
            val coordinatesString = buildString {
                append("$userLng,$userLat")
                for (dest in destinations) {
                    append(";${dest.second},${dest.first}")
                }
            }

            // sources=0 অর্থ প্রথম লোকেশন (ইউজার) থেকে বাকি সবার দূরত্ব মাপবে
            // annotations=distance নির্দেশ করে আমাদের শুধু কিলোমিটার দূরত্ব লাগবে
            val urlStr = "https://router.project-osrm.org/table/v1/driving/$coordinatesString?sources=0&annotations=distance"
            Log.d(TAG, "Requesting OSRM Table API with url: $urlStr")

            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SafeTravelTracker/2.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                
                if (json.optString("code") == "Ok") {
                    val distancesArray = json.optJSONArray("distances")
                    if (distancesArray != null && distancesArray.length() > 0) {
                        // প্রথম রো (row) টি হলো সোর্স ০ (ইউজার) থেকে অন্য সবার দূরত্বের অ্যারে
                        val userDistances = distancesArray.optJSONArray(0)
                        if (userDistances != null) {
                            val results = mutableListOf<Double?>()
                            // index ০ হলো ইউজারের নিজের থেকে নিজের দূরত্ব (যা সবসময় ০ থাকে), তাই আমরা ১ থেকে শুরু করে বাকি গন্তব্যগুলোর দূরত্ব রিড করব
                            for (i in 1 until userDistances.length()) {
                                if (userDistances.isNull(i)) {
                                    results.add(null)
                                } else {
                                    val distanceInMeters = userDistances.optDouble(i)
                                    // ওএসআরএম মিটার এককে দূরত্ব দেয়, আমরা তা কিলোমিটারে রূপান্তর করব
                                    results.add(distanceInMeters / 1000.0)
                                }
                            }
                            Log.d(TAG, "Successfully fetched ${results.size} distances from OSRM.")
                            return@withContext results
                        }
                    }
                }
            } else {
                Log.e(TAG, "OSRM Response error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch distances from OSRM table API", e)
        }

        // নেটওয়ার্ক ফেইলুর বা এরর হলে পুরো লিস্টের জন্য null রিটার্ন করবে যাতে Haversine Fallback অ্যাক্টিভ হয়
        return@withContext List(destinations.size) { null }
    }
}
