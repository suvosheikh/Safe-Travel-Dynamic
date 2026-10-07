package com.safetravel.tracker.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object SafeErrorFormatter {

    fun format(throwable: Throwable?, fallback: String = "An unexpected error occurred. Please try again."): String {
        if (throwable == null) return fallback
        return format(throwable.localizedMessage ?: throwable.message, fallback)
    }

    fun format(rawMessage: String?, fallback: String = "An unexpected error occurred. Please try again."): String {
        if (rawMessage.isNullOrBlank()) return fallback

        val lower = rawMessage.lowercase()

        // Network / connectivity issues
        if (lower.contains("unknownhost") ||
            lower.contains("connectexception") ||
            lower.contains("no address associated with hostname") ||
            lower.contains("failed to connect") ||
            lower.contains("network is unreachable") ||
            lower.contains("route to host") ||
            lower.contains("connection refused")
        ) {
            return "Unable to connect to safety server. Please check your internet connection."
        }

        // Timeouts
        if (lower.contains("sockettimeoutexception") || lower.contains("timeout")) {
            return "Connection timed out. Please try again in a moment."
        }

        // Authentication & Session
        if (lower.contains("jwt") || lower.contains("token") || lower.contains("session expired") || lower.contains("unauthorized") || lower.contains("401")) {
            return "Session expired. Please sign in again."
        }

        // Forbidden
        if (lower.contains("403") || lower.contains("forbidden") || lower.contains("permission denied")) {
            return "You do not have permission to perform this action."
        }

        // Not Found
        if (lower.contains("404") || lower.contains("not found")) {
            return "The requested record was not found."
        }

        // Duplicate / Conflict
        if (lower.contains("409") || lower.contains("conflict") || lower.contains("already exists") || lower.contains("23505")) {
            return "This item or account already exists."
        }

        // Strip backend references and technical URLs
        var sanitized = rawMessage
            .replace(Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[a-zA-Z0-9.-]+\\.supabase\\.co", RegexOption.IGNORE_CASE), "")
            .replace(Regex("supabase", RegexOption.IGNORE_CASE), "Cloud")
            .replace(Regex("postgrest", RegexOption.IGNORE_CASE), "Server")
            .trim()

        if (sanitized.isBlank() || sanitized.length > 120 || sanitized.contains("Exception") || sanitized.contains("Stack trace")) {
            return fallback
        }

        return sanitized
    }
}
