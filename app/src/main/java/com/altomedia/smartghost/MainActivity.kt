package com.altomedia.smartghost

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.altomedia.smartghost.coreui.theme.SmartGhostTheme
import com.altomedia.smartghost.data.prefs.SettingsRepository
import com.altomedia.smartghost.feature.home.HomeScreen
import com.altomedia.smartghost.navigation.Routes
import com.altomedia.smartghost.ui.SessionScreen
import com.altomedia.smartghost.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SmartGhostRoot() }
    }
}

/** Holds the persisted theme choice so the whole app can react to it. */
@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val darkTheme: StateFlow<Boolean> = settingsRepository.settings
        .map { it.darkTheme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}

@Composable
private fun SmartGhostRoot(viewModel: RootViewModel = hiltViewModel()) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    SmartGhostTheme(darkTheme = darkTheme) {
        SmartGhostNavHost()
    }
}

@Composable
private fun SmartGhostNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartSession = { request ->
                    navController.navigate(Routes.session(request.videoId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.SESSION_PATTERN,
            arguments = listOf(
                navArgument(Routes.SESSION_ARG_VIDEO_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val videoId = entry.arguments?.getString(Routes.SESSION_ARG_VIDEO_ID).orEmpty()
            SessionScreen(
                videoId = videoId,
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onOpenInYouTube = { url -> openExternal(context, url) },
                onReload = { navController.navigate(Routes.session(videoId)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun openExternal(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
