package com.omniweb.app.util.adblock

import java.io.InputStreamReader

class HostsFileParser {
    fun parseInput(input: InputStreamReader): Sequence<String> {
        return input.buffered().lineSequence()
            .flatMap { parseLineToDomains(it) }
    }

    private fun parseLineToDomains(line: String): Sequence<String> {
        var processed = line.trim()
        if (processed.isEmpty() || processed.startsWith("#")) return emptySequence()

        val commentIndex = processed.indexOf("#")
        if (commentIndex != -1) {
            processed = processed.substring(0, commentIndex).trim()
        }

        // Split by whitespace first to check for standard hosts format: 127.0.0.1 domain.com
        val spaceParts = processed.split(Regex("\\s+"))
        if (spaceParts.size >= 2 && (spaceParts[0] == "127.0.0.1" || spaceParts[0] == "0.0.0.0")) {
            val domain = spaceParts[1]
            return if (domain != "localhost") sequenceOf(domain) else emptySequence()
        }

        // Handle comma-separated lists or single domains
        return processed.split(Regex("[\\s,]+"))
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(".") &&
                     it != "127.0.0.1" && it != "0.0.0.0" && it != "localhost" && it != "::1" }
    }
}
