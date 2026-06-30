package com.omniweb.app.util.adblock

import java.io.InputStreamReader

class HostsFileParser {
    fun parseInput(input: InputStreamReader): List<String> {
        val domains = mutableListOf<String>()
        input.forEachLine { line ->
            val parsed = parseLine(line)
            if (parsed != null) {
                domains.add(parsed)
            }
        }
        return domains
    }

    private fun parseLine(line: String): String? {
        var processed = line.trim()
        if (processed.isEmpty() || processed.startsWith("#")) return null

        val commentIndex = processed.indexOf("#")
        if (commentIndex != -1) {
            processed = processed.substring(0, commentIndex).trim()
        }

        val parts = processed.split(Regex("\\s+"))
        if (parts.size >= 2) {
            val domain = parts[1]
            if (domain != "localhost" && domain != "127.0.0.1") {
                return domain
            }
        } else if (parts.size == 1 && parts[0].contains(".")) {
            return parts[0]
        }

        return null
    }
}
