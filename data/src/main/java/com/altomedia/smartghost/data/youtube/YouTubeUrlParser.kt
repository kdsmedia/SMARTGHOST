package com.altomedia.smartghost.data.youtube

/**
 * A recognised YouTube link. SMARTGHOST only ever points the user at the
 * official YouTube player / app for a single, user-initiated session (Bab 6).
 */
data class YouTubeLink(
    val videoId: String,
    val kind: Kind,
    val canonicalUrl: String,
) {
    enum class Kind {
        /** youtube.com/watch?v=... */
        WATCH,

        /** youtu.be/<id> */
        SHORT,

        /** youtube.com/live/<id> or /watch?v=...&live=1 */
        LIVE,

        /** youtube.com/shorts/<id> */
        SHORTS,
    }
}

/** Outcome of parsing a user-supplied URL. */
sealed interface UrlParseResult {
    data class Valid(val link: YouTubeLink) : UrlParseResult

    /** Empty input — nothing typed yet. */
    data object Empty : UrlParseResult

    /** Well-formed but not a YouTube URL we recognise. */
    data class NotYouTube(val host: String?) : UrlParseResult

    /** Looks like YouTube but the video id is missing or malformed. */
    data class InvalidVideoId(val reason: String) : UrlParseResult

    /** The text could not be parsed as a URL at all. */
    data object Malformed : UrlParseResult
}

/**
 * Parses YouTube URLs without any network access. Recognises the formats
 * required by Bab 6: watch, youtu.be, live and shorts.
 */
object YouTubeUrlParser {

    private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com",
    )

    private val SHORT_HOSTS = setOf("youtu.be", "www.youtu.be")

    fun parse(rawInput: String?): UrlParseResult {
        val input = rawInput?.trim().orEmpty()
        if (input.isEmpty()) return UrlParseResult.Empty

        val uri = try {
            java.net.URI(withScheme(input))
        } catch (e: Exception) {
            return UrlParseResult.Malformed
        }

        val host = uri.host?.lowercase()
            ?: return UrlParseResult.Malformed
        val path = uri.path.orEmpty()
        val query = parseQuery(uri.rawQuery)

        // youtu.be/<id>
        if (host in SHORT_HOSTS) {
            val id = path.trim('/').substringBefore('/')
            return validateId(id, YouTubeLink.Kind.SHORT)
        }

        if (host !in YOUTUBE_HOSTS) {
            return UrlParseResult.NotYouTube(host)
        }

        // /shorts/<id>
        if (path.startsWith("/shorts/")) {
            val id = path.removePrefix("/shorts/").substringBefore('/')
            return validateId(id, YouTubeLink.Kind.SHORTS)
        }

        // /live/<id>
        if (path.startsWith("/live/")) {
            val id = path.removePrefix("/live/").substringBefore('/')
            return validateId(id, YouTubeLink.Kind.LIVE)
        }

        // /watch?v=<id>
        if (path.startsWith("/watch")) {
            val id = query["v"]
            val isLive = query["live"] == "1"
            val kind = if (isLive) YouTubeLink.Kind.LIVE else YouTubeLink.Kind.WATCH
            if (id.isNullOrBlank()) {
                return UrlParseResult.InvalidVideoId("Parameter 'v' tidak ditemukan pada URL watch.")
            }
            return validateId(id, kind)
        }

        // /embed/<id> — accepted as a WATCH link
        if (path.startsWith("/embed/")) {
            val id = path.removePrefix("/embed/").substringBefore('/')
            return validateId(id, YouTubeLink.Kind.WATCH)
        }

        return UrlParseResult.NotYouTube(host)
    }

    private fun validateId(id: String, kind: YouTubeLink.Kind): UrlParseResult {
        if (!VIDEO_ID_REGEX.matches(id)) {
            return UrlParseResult.InvalidVideoId(
                "ID video '$id' tidak sesuai format YouTube (11 karakter).",
            )
        }
        val canonical = when (kind) {
            YouTubeLink.Kind.SHORTS -> "https://www.youtube.com/shorts/$id"
            YouTubeLink.Kind.LIVE -> "https://www.youtube.com/live/$id"
            else -> "https://www.youtube.com/watch?v=$id"
        }
        return UrlParseResult.Valid(YouTubeLink(id, kind, canonical))
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        return rawQuery.split('&').mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = decode(pair.substring(0, idx))
            val value = decode(pair.substring(idx + 1))
            key to value
        }.toMap()
    }

    private fun decode(value: String): String =
        try {
            java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
        } catch (e: Exception) {
            value
        }

    /** Adds https:// when the user pastes a bare youtube.com/... link. */
    private fun withScheme(input: String): String =
        if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else {
            "https://$input"
        }
}
