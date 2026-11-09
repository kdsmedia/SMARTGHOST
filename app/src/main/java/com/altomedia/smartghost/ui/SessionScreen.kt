package com.altomedia.smartghost.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.altomedia.smartghost.coreui.component.SecondaryActionButton
import com.altomedia.smartghost.feature.player.PlayerScreen

/**
 * Session screen (Bab 4, 6): a single official YouTube player for the video the
 * user entered, with a HOME control in a bottom bar (Bab 8).
 *
 * The bottom bar applies [navigationBarsPadding] so the phone's system
 * navigation buttons never cover it.
 */
@Composable
fun SessionScreen(
    videoId: String,
    onHome: () -> Unit,
    onOpenInYouTube: (String) -> Unit,
) {
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
        ) {
            PlayerScreen(
                videoId = videoId,
                onOpenInYouTube = onOpenInYouTube,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
