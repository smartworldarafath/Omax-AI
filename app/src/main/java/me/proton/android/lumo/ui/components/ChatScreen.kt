package me.proton.android.lumo.ui.components


import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.proton.android.lumo.MainActivity
import me.proton.android.lumo.R
import me.proton.android.lumo.config.LumoConfig
import me.proton.android.lumo.ui.theme.LumoTheme
import timber.log.Timber

private const val FADE_IN_DURATION_MS = 150
private const val FADE_OUT_DURATION_MS = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    webView: WebView,
    chatScreenFlags: ChatScreenFlags,
    onOpenUpdateScreen: () -> Unit = {},
    isUpdateAvailable: Boolean = false,
    updateVersionName: String? = null,
    modifier: Modifier = Modifier,
) {
    with(chatScreenFlags) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                if (shouldShowBackButton) {
                    TopBarWithNavigation(
                        handleBack = {
                            if (webView.canGoBack()) {
                                webView.goBack()
                            } else {
                                webView.loadUrl(LumoConfig.LUMO_URL)
                                webView.clearHistory()
                            }
                        },
                        onOpenUpdate = onOpenUpdateScreen
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                )

                // Top Floating Update / Info Pill Button when back button is not visible
                if (!shouldShowBackButton) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                    ) {
                        Surface(
                            onClick = onOpenUpdateScreen,
                            shape = RoundedCornerShape(20.dp),
                            color = if (isUpdateAvailable) Color(0xFF6C2BD9) else Color(0xCC202028),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isUpdateAvailable) Icons.Default.Refresh else Icons.Default.Favorite,
                                    contentDescription = "Updates & Info",
                                    tint = if (isUpdateAvailable) Color.White else Color(0xFFFF80AB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isUpdateAvailable) "Update v$updateVersionName" else "Info",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                val showLoading = isLoading && !hasSeenLumoContainer && isLumoPage
                LoadingScreen(show = showLoading)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBarWithNavigation(
    handleBack: () -> Unit,
    onOpenUpdate: () -> Unit = {}
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true)
                    ) {
                        Timber.tag(MainActivity.TAG).i("Back button clicked, navigating to Lumo")
                        handleBack()
                    }
                    .padding(all = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.lumo_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.height(25.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.back_to_lumo),
                    style = MaterialTheme.typography.titleLarge,
                    color = LumoTheme.colors.textNorm
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenUpdate) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Updates & About",
                    tint = Color(0xFFFF80AB)
                )
            }
        }
    )
}

@Composable
private fun LoadingScreen(show: Boolean) {
    // Overlay LoadingScreen if loading (use only ViewModel state)
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(
            animationSpec = tween(FADE_IN_DURATION_MS)
        ),
        exit = fadeOut(
            animationSpec = tween(FADE_OUT_DURATION_MS)
        )
    ) {
        LoadingScreen()
    }
}

@Immutable
data class ChatScreenFlags(
    val hasSeenLumoContainer: Boolean,
    val shouldShowBackButton: Boolean,
    val isLoading: Boolean,
    val isLumoPage: Boolean,
)
