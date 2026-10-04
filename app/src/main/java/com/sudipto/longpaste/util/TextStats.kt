package com.sudipto.longpaste.util

import java.nio.charset.StandardCharsets

object TextStats {
    fun utf8Bytes(text: CharSequence): Long =
        text.toString().toByteArray(StandardCharsets.UTF_8).size.toLong()

    fun preview(text: CharSequence, maxChars: Int = 140): String {
        require(maxChars > 0)
        val normalized = text.toString().replace("\\s+".toRegex(), " ").trim()
        return if (normalized.length <= maxChars) normalized
        else normalized.take(maxChars - 1) + "…"
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
