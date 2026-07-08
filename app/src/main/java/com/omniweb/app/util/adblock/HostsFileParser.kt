package com.omniweb.app.util.adblock

import java.io.InputStreamReader

class HostsFileParser {
    fun parseInput(input: InputStreamReader): List<String> {
        val domains = mutableListOf<String>()
        input.forEachLine { line ->
            domains.addAll(parseLineToDomains(line))
        }
        return domains
    }

    private fun parseLineToDomains(line: String): List<String> {
        var processed = line.trim()
        if (processed.isEmpty() || processed.startsWith("#")) return emptyList()

        val commentIndex = processed.indexOf("#")
        if (commentIndex != -1) {
            processed = processed.substring(0, commentIndex).trim()
        }

        // Split by whitespace first to check for standard hosts format: 127.0.0.1 domain.com
        val spaceParts = processed.split(Regex("\\s+"))
        if (spaceParts.size >= 2 && (spaceParts[0] == "127.0.0.1" || spaceParts[0] == "0.0.0.0")) {
            val domain = spaceParts[1]
            return if (domain != "localhost") listOf(domain) else emptyList()
        }

        // Handle comma-separated lists or single domains
        return processed.split(Regex("[\\s,]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(".") &&
                     it != "127.0.0.1" && it != "0.0.0.0" && it != "localhost" && it != "::1" }
    }
}
