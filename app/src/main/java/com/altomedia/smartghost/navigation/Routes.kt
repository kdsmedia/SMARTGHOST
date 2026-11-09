package com.altomedia.smartghost.navigation

/** Type-safe navigation routes used by the app's NavHost (Bab 4). */
object Routes {
    const val HOME = "home"
    const val SESSION = "session"
    const val SETTINGS = "settings"

    const val SESSION_ARG_VIDEO_ID = "videoId"
    const val SESSION_PATTERN = "$SESSION/{$SESSION_ARG_VIDEO_ID}"

    fun session(videoId: String): String = "$SESSION/$videoId"
}
