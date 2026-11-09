package com.altomedia.smartghost.data.proxy

/**
 * A single validated proxy endpoint from proxy.txt. These endpoints are used
 * ONLY to test connectivity to servers the user owns or is authorised to test
 * (Bab 7). SMARTGHOST never uses proxies to disguise viewers or evade limits.
 */
data class ProxyEndpoint(
    val host: String,
    val port: Int,
    val scheme: String,
    val sourceLine: Int,
) {
    val display: String get() = "$scheme://$host:$port"
}

/** A rejected proxy.txt line, with the line number for user feedback (Bab 7). */
data class ProxyParseError(
    val line: Int,
    val raw: String,
    val reason: String,
)

data class ProxyParseResult(
    val endpoints: List<ProxyEndpoint>,
    val errors: List<ProxyParseError>,
    val duplicateCount: Int,
) {
    val totalLines: Int get() = endpoints.size + errors.size
}

/**
 * Parses proxy.txt content. Accepts `host:port` or `scheme://host:port`,
 * ignores blank lines and `#` comments, validates port range and duplicates.
 */
object ProxyFileParser {

    private val ALLOWED_SCHEMES = setOf("http", "https", "socks5", "socks")
    private val HOST_REGEX = Regex("^[A-Za-z0-9._-]+$")

    fun parse(content: String?): ProxyParseResult {
        if (content.isNullOrBlank()) {
            return ProxyParseResult(emptyList(), emptyList(), 0)
        }

        val endpoints = mutableListOf<ProxyEndpoint>()
        val errors = mutableListOf<ProxyParseError>()
        val seen = mutableSetOf<String>()
        var duplicates = 0

        content.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) return@forEachIndexed

            val parsed = parseLine(line)
            when (parsed) {
                is LineResult.Ok -> {
                    val key = "${parsed.scheme}://${parsed.host}:${parsed.port}"
                    if (seen.add(key)) {
                        endpoints += ProxyEndpoint(
                            host = parsed.host,
                            port = parsed.port,
                            scheme = parsed.scheme,
                            sourceLine = lineNumber,
                        )
                    } else {
                        duplicates++
                    }
                }

                is LineResult.Err -> errors += ProxyParseError(
                    line = lineNumber,
                    raw = rawLine.trim(),
                    reason = parsed.reason,
                )
            }
        }

        return ProxyParseResult(endpoints, errors, duplicates)
    }

    private sealed interface LineResult {
        data class Ok(val host: String, val port: Int, val scheme: String) : LineResult
        data class Err(val reason: String) : LineResult
    }

    private fun parseLine(line: String): LineResult {
        val scheme: String
        val remainder: String
        val schemeIdx = line.indexOf("://")
        if (schemeIdx >= 0) {
            scheme = line.substring(0, schemeIdx).lowercase()
            remainder = line.substring(schemeIdx + 3)
            if (scheme !in ALLOWED_SCHEMES) {
                return LineResult.Err("Skema '$scheme' tidak didukung.")
            }
        } else {
            scheme = "http"
            remainder = line
        }

        val parts = remainder.split(":")
        if (parts.size != 2) {
            return LineResult.Err("Format harus host:port atau skema://host:port.")
        }
        val host = parts[0].trim()
        val portText = parts[1].trim()

        if (host.isEmpty() || !HOST_REGEX.matches(host)) {
            return LineResult.Err("Host '$host' tidak valid.")
        }
        val port = portText.toIntOrNull()
            ?: return LineResult.Err("Port '$portText' bukan angka.")
        if (port !in 1..65535) {
            return LineResult.Err("Port $port di luar rentang 1-65535.")
        }

        return LineResult.Ok(host, port, scheme)
    }
}
