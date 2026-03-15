package com.gallery.app.ui.screens.video

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.gallery.app.data.models.MediaItem
import com.gallery.app.domain.usecases.GetMediaByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.abs

// ── ViewModel ─────────────────────────────────────────────

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getMediaById: GetMediaByIdUseCase,
) : ViewModel() {

    private val _media = MutableStateFlow<MediaItem?>(null)
    val media: StateFlow<MediaItem?> = _media.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun loadMedia(mediaId: Long) = viewModelScope.launch {
        val item = getMediaById(mediaId) ?: return@launch
        _media.value = item
        player.setMediaItem(ExoMediaItem.fromUri(item.uri))
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}

// ── Screen ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    mediaId: Long,
    viewModel: VideoPlayerViewModel = hiltViewModel(),
) {
    val media by viewModel.media.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    LaunchedEffect(mediaId) { viewModel.loadMedia(mediaId) }
    DisposableEffect(Unit) { onDispose { viewModel.player.pause() } }

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(1L) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3_000)
            showControls = false
        }
    }

    // Position ticker
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            currentPosition = viewModel.player.currentPosition
            duration = viewModel.player.duration.coerceAtLeast(1L)
            isPlaying = viewModel.player.isPlaying
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap         = { showControls = !showControls },
                    onDoubleTap   = { offset ->
                        val midX = size.width / 2f
                        if (offset.x < midX) viewModel.player.seekBack()
                        else viewModel.player.seekForward()
                    },
                )
            }
            .pointerInput(Unit) {
                // Swipe up/down: left half = brightness, right half = volume
                detectVerticalDragGestures { change, dragAmount ->
                    val midX = size.width / 2f
                    if (change.position.x < midX) {
                        // brightness – requires window attributes
                    } else {
                        // volume
                        val am = context.getSystemService(android.media.AudioManager::class.java)
                        val maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                        val delta = if (dragAmount < 0) 1 else -1
                        am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, delta, 0)
                    }
                }
            },
    ) {
        // ── ExoPlayer Surface ──────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false     // we use our own controls
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Controls Overlay ───────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
                ) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Text(
                            text     = media?.displayName ?: "",
                            color    = Color.White,
                            style    = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        // PiP button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(onClick = {
                                activity?.enterPictureInPictureMode(
                                    PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                )
                            }) {
                                Icon(Icons.Filled.PictureInPicture, "PiP", tint = Color.White)
                            }
                        }
                        // Speed
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Text("${playbackSpeed}x", color = Color.White, fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded        = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false },
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text    = { Text("${speed}x") },
                                        onClick = {
                                            playbackSpeed = speed
                                            viewModel.player.setPlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Centre play controls
                Row(
                    modifier              = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick  = { viewModel.player.seekTo(maxOf(0, currentPosition - 10_000)) },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(Icons.Filled.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    IconButton(
                        onClick  = {
                            if (viewModel.player.isPlaying) viewModel.player.pause()
                            else viewModel.player.play()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.White.copy(0.2f)),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint     = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    IconButton(
                        onClick  = { viewModel.player.seekTo(minOf(duration, currentPosition + 10_000)) },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(Icons.Filled.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }

                // Bottom seek bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                        .navigationBarsPadding()
                        .padding(16.dp),
                ) {
                    Column {
                        Slider(
                            value         = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = { viewModel.player.seekTo((it * duration).toLong()) },
                            colors        = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
                        )
                        Row(
                            modifier                  = Modifier.fillMaxWidth(),
                            horizontalArrangement     = Arrangement.SpaceBetween,
                        ) {
                            Text(currentPosition.formatDuration(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            Text(duration.formatDuration(),        color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun Long.formatDuration(): String {
    val s = this / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
