package com.altomedia.smartghost.feature.home

import com.altomedia.smartghost.data.youtube.UrlParseResult

/**
 * UI state for the home screen (Bab 4). Holds only what the UI needs to render;
 * the ViewModel is the single source of truth (Bab 5).
 */
data class HomeUiState(
    val urlInput: String = "",
    val urlValidation: UrlValidation = UrlValidation.Idle,
    val canStart: Boolean = false,
    val message: String? = null,
)

/** Validation feedback shown under the URL field. */
sealed interface UrlValidation {
    data object Idle : UrlValidation
    data object Valid : UrlValidation
    data class Invalid(val message: String) : UrlValidation
}

/** One-shot navigation event from home to the session screen. */
data class StartSessionRequest(
    val canonicalUrl: String,
    val videoId: String,
)

internal fun UrlParseResult.toValidation(): UrlValidation = when (this) {
    is UrlParseResult.Valid -> UrlValidation.Valid
    UrlParseResult.Empty -> UrlValidation.Idle
    UrlParseResult.Malformed ->
        UrlValidation.Invalid("Format URL tidak dikenali.")
    is UrlParseResult.NotYouTube ->
        UrlValidation.Invalid("Bukan tautan YouTube yang didukung.")
    is UrlParseResult.InvalidVideoId ->
        UrlValidation.Invalid(reason)
}
