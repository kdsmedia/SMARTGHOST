package com.altomedia.smartghost.security

/**
 * Redacts secrets from anything that might reach a log, a crash report or the
 * UI (Bab 3, 9). Shared tokens, keystore passwords and proxy credentials must
 * never appear in logs, so every log call routes through here first.
 */
object SecretRedactor {

    private val PATTERNS = listOf(
        // GitHub classic & fine-grained tokens
        Regex("gh[pousr]_[A-Za-z0-9]{20,}"),
        Regex("github_pat_[A-Za-z0-9_]{20,}"),
        // Generic bearer tokens
        Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]{10,}"),
        // JWT
        Regex("eyJ[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}\\.[A-Za-z0-9_\\-]{10,}"),
        // key=value secrets (password, token, api_key, secret)
        Regex("(?i)(password|passwd|token|api[_-]?key|secret)\\s*[=:]\\s*\\S+"),
        // Credentials embedded in a URL: scheme://user:pass@host
        Regex("://[^/@\\s:]+:[^/@\\s]+@"),
    )

    private const val MASK = "***REDACTED***"

    fun redact(input: String?): String {
        if (input.isNullOrEmpty()) return input.orEmpty()
        var output: String = input
        for (pattern in PATTERNS) {
            val replaced: String = pattern.replace(output, MASK)
            output = replaced
        }
        return output
    }

    /** True when the text appears to contain something secret-like. */
    fun containsSecret(input: String?): Boolean {
        if (input.isNullOrEmpty()) return false
        return PATTERNS.any { it.containsMatchIn(input) }
    }
}
