package com.safetravel.tracker.util

/**
 * Utility for formatting and sanitizing address and location strings across the app.
 * Eliminates ugly placeholders like "Selected Point", "Pinned Point", or raw bracket coordinates.
 */
object AddressUtils {

    /**
     * Checks if a string is null, empty, or a generic placeholder that requires active reverse-geocoding.
     */
    fun isGenericOrPlaceholder(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return true
        val clean = raw.trim()
        val beforeBracket = if (clean.contains("[")) clean.substringBefore("[").trim() else clean
        val lower = beforeBracket.lowercase()
        return lower.isBlank() ||
                lower == "selected point" ||
                lower == "pinned point" ||
                lower == "destination" ||
                lower == "selected destination" ||
                lower == "selected location" ||
                lower == "my location" ||
                lower == "current location" ||
                lower == "unknown location" ||
                lower == "live beacon navigation" ||
                lower.startsWith("locating") ||
                lower.startsWith("finding") ||
                lower.startsWith("location (") ||
                lower.startsWith("custom pin") ||
                lower.startsWith("dropped pin") ||
                lower.startsWith("lat:")
    }

    /**
     * Extracts a clean, human-readable address for UI display.
     * Examples:
     * - "Dhanmondi 27, Dhaka [90.375, 23.75]" -> "Dhanmondi 27, Dhaka"
     * - "Selected Point [90.375, 23.75]" -> "Location (23.7500, 90.3750)"
     * - "Selected Point" -> "Selected Destination"
     * - "Mirpur 10, Dhaka" -> "Mirpur 10, Dhaka"
     */
    fun formatDisplayAddress(raw: String?): String {
        if (raw.isNullOrBlank()) return "Selected Location"
        val clean = raw.trim()

        if (clean.startsWith("Locating", ignoreCase = true) || clean.startsWith("Finding", ignoreCase = true)) {
            return "Locating address..."
        }

        // 1. If it has a prefix before coordinate brackets: "Address [lng, lat]"
        val beforeBracket = if (clean.contains("[")) clean.substringBefore("[").trim() else clean
        if (!isGenericOrPlaceholder(beforeBracket)) {
            return beforeBracket
        }

        // 2. If it's a generic placeholder but contains coordinates in brackets: "[lng, lat]"
        val coords = extractCoordinates(clean)
        if (coords != null) {
            return "Location (${String.format(java.util.Locale.US, "%.4f, %.4f", coords.first, coords.second)})"
        }

        // 3. Fallback
        return if (beforeBracket.isNotBlank() && !isGenericOrPlaceholder(beforeBracket)) {
            beforeBracket
        } else {
            "Selected Destination"
        }
    }

    /**
     * Cleans an address string before persisting it to Supabase or Room database,
     * stripping any embedded "[lng, lat]" tags.
     */
    fun cleanAddressForStorage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Destination"
        val clean = raw.trim()
        val beforeBracket = if (clean.contains("[")) clean.substringBefore("[").trim() else clean
        if (!isGenericOrPlaceholder(beforeBracket)) {
            return beforeBracket
        }
        val coords = extractCoordinates(clean)
        return if (coords != null) {
            "Location (${String.format(java.util.Locale.US, "%.4f, %.4f", coords.first, coords.second)})"
        } else if (beforeBracket.isNotBlank() && !isGenericOrPlaceholder(beforeBracket)) {
            beforeBracket
        } else "Destination"
    }

    /**
     * Safely extracts (latitude, longitude) from address strings formatted as:
     * - "... [90.375, 23.75]" (Mapbox format: [lng, lat])
     * - "23.75, 90.375" (standard lat, lng)
     */
    fun extractCoordinates(raw: String?): Pair<Double, Double>? {
        if (raw.isNullOrBlank()) return null
        val clean = raw.trim()
        try {
            if (clean.contains("[") && clean.contains("]")) {
                val content = clean.substringAfter("[").substringBefore("]")
                val parts = content.split(",")
                if (parts.size >= 2) {
                    val p0 = parts[0].trim().toDoubleOrNull()
                    val p1 = parts[1].trim().toDoubleOrNull()
                    if (p0 != null && p1 != null) {
                        // In GeoJSON/Mapbox [lng, lat]: longitude in Bangladesh is ~88-92, lat is ~20-26
                        return if (p0 > 50.0) Pair(p1, p0) else Pair(p0, p1)
                    }
                }
            } else if (clean.contains(",")) {
                val parts = clean.split(",")
                if (parts.size == 2) {
                    val p0 = parts[0].trim().toDoubleOrNull()
                    val p1 = parts[1].trim().toDoubleOrNull()
                    if (p0 != null && p1 != null) {
                        return if (p0 > 50.0) Pair(p1, p0) else Pair(p0, p1)
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
