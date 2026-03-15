package com.gallery.app.ui.screens.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallery.app.domain.usecases.GetMediaByIdUseCase
import com.gallery.app.ui.components.GalleryTopBar
import com.gallery.app.ui.components.LoadingOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ── Edit Tool enum ────────────────────────────────────────

enum class EditTool(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    BRIGHTNESS ("Brightness", Icons.Filled.BrightnessMedium),
    CONTRAST   ("Contrast",   Icons.Filled.Contrast),
    SATURATION ("Saturation", Icons.Filled.ColorLens),
    ROTATE     ("Rotate",     Icons.Filled.RotateRight),
    FLIP       ("Flip",       Icons.Filled.Flip),
    CROP       ("Crop",       Icons.Filled.Crop),
}

// ── ViewModel ─────────────────────────────────────────────

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val getMediaById: GetMediaByIdUseCase,
) : ViewModel() {

    private val _uri = MutableStateFlow<android.net.Uri?>(null)
    val uri: StateFlow<android.net.Uri?> = _uri.asStateFlow()

    var brightness by mutableStateOf(0f)    // -1f .. 1f
    var contrast   by mutableStateOf(1f)    // 0.5 .. 2f
    var saturation by mutableStateOf(1f)    // 0f .. 2f
    var rotation   by mutableStateOf(0f)    // 0, 90, 180, 270
    var isFlipped  by mutableStateOf(false)
    var activeTool by mutableStateOf(EditTool.BRIGHTNESS)

    fun load(mediaId: Long) = viewModelScope.launch {
        _uri.value = getMediaById(mediaId)?.uri
    }

    fun rotateRight() { rotation = (rotation + 90f) % 360f }
    fun flipHorizontal() { isFlipped = !isFlipped }
    fun resetAll() { brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0f; isFlipped = false }

    /** Build a ColorMatrix from current adjustment values for Coil overlay. */
    fun buildColorMatrix(): ColorMatrix {
        // Brightness: shift all channels
        val b = brightness * 255f
        val cm = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, b,
                0f, 1f, 0f, 0f, b,
                0f, 0f, 1f, 0f, b,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        // Contrast
        val c = contrast
        val t = (1f - c) / 2f * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                c,  0f, 0f, 0f, t,
                0f, c,  0f, 0f, t,
                0f, 0f, c,  0f, t,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        cm.timesAssign(contrastMatrix)
        // Saturation
        cm.setToSaturation(saturation)
        return cm
    }
}

// ── Screen ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavController,
    mediaId: Long,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uri by viewModel.uri.collectAsStateWithLifecycle()
    LaunchedEffect(mediaId) { viewModel.load(mediaId) }

    Scaffold(
        topBar = {
            GalleryTopBar(
                title  = "Edit",
                onBack = { navController.popBackStack() },
                actions = {
                    TextButton(onClick = { viewModel.resetAll() }) { Text("Reset") }
                    TextButton(onClick = { /* save to gallery */ navController.popBackStack() }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
        bottomBar = {
            Column {
                // Active tool slider
                when (viewModel.activeTool) {
                    EditTool.BRIGHTNESS -> AdjustSlider("Brightness", viewModel.brightness, -1f, 1f) { viewModel.brightness = it }
                    EditTool.CONTRAST   -> AdjustSlider("Contrast",   viewModel.contrast,   0.5f, 2f) { viewModel.contrast = it }
                    EditTool.SATURATION -> AdjustSlider("Saturation", viewModel.saturation, 0f, 2f)   { viewModel.saturation = it }
                    else -> {}
                }

                // Tool picker
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(EditTool.values()) { tool ->
                        ToolChip(
                            tool       = tool,
                            isActive   = viewModel.activeTool == tool,
                            onClick    = {
                                when (tool) {
                                    EditTool.ROTATE -> viewModel.rotateRight()
                                    EditTool.FLIP   -> viewModel.flipHorizontal()
                                    else            -> viewModel.activeTool = tool
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (uri == null) {
            LoadingOverlay(Modifier.padding(padding))
        } else {
            Box(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                contentAlignment    = Alignment.Center,
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Edit preview",
                    contentScale       = ContentScale.Fit,
                    colorFilter        = ColorFilter.colorMatrix(viewModel.buildColorMatrix()),
                    modifier           = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            rotationZ   = viewModel.rotation,
                            scaleX      = if (viewModel.isFlipped) -1f else 1f,
                        ),
                )
            }
        }
    }
}

@Composable
private fun AdjustSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("${"%.2f".format(value)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = min..max,
        )
    }
}

@Composable
private fun ToolChip(
    tool: EditTool,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label       = "chipBg",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(tool.icon, null, modifier = Modifier.size(24.dp),
            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(tool.label, style = MaterialTheme.typography.labelSmall)
    }
}
