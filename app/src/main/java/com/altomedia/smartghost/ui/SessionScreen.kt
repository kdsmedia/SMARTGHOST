package com.altomedia.smartghost.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.smartghost.coreui.component.SecondaryActionButton
import com.altomedia.smartghost.feature.player.PlayerScreen
import com.altomedia.smartghost.feature.player.PlayerViewModel
import com.altomedia.smartghost.feature.player.VideoWall
import com.altomedia.smartghost.feature.player.sanitizeVideoId

/**
 * Session screen (Bab 4, 6, 8): the single official YouTube player for the video
 * the user entered, a ten-card monitoring wall (5 x 2), and a HOME control.
 *
 * The [PlayerViewModel] is hoisted here so both the player and the wall observe
 * one source of truth: the wall's cards show the live status of the same video.
 * The wall previews the video; it is not ten players and never a viewer count.
 */
@Composable
fun SessionScreen(
    videoId: String,
    onHome: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SecondaryActionButton(
                text = "HOME",
                icon = Icons.Filled.Home,
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayerScreen(
                videoId = videoId,
                viewModel = playerViewModel,
                modifier = Modifier.padding(top = 8.dp),
            )
            VideoWall(videoId = sanitizeVideoId(videoId), status = playerState.status)
        }
    }
}
