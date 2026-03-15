package com.gallery.app.ui.screens.mediainfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.gallery.app.data.models.MediaMetadata
import com.gallery.app.domain.usecases.GetMediaByIdUseCase
import com.gallery.app.domain.usecases.GetMetadataUseCase
import com.gallery.app.ui.components.GalleryTopBar
import com.gallery.app.ui.components.LoadingOverlay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaInfoViewModel @Inject constructor(
    private val getMetadata: GetMetadataUseCase,
    private val getMediaById: GetMediaByIdUseCase,
) : ViewModel() {

    private val _meta = MutableStateFlow<MediaMetadata?>(null)
    val metadata: StateFlow<MediaMetadata?> = _meta.asStateFlow()

    private val _uri = MutableStateFlow<android.net.Uri?>(null)
    val uri: StateFlow<android.net.Uri?> = _uri.asStateFlow()

    fun load(mediaId: Long) = viewModelScope.launch {
        _meta.value = getMetadata(mediaId)
        _uri.value  = getMediaById(mediaId)?.uri
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoScreen(
    navController: NavController,
    mediaId: Long,
    viewModel: MediaInfoViewModel = hiltViewModel(),
) {
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val uri by viewModel.uri.collectAsStateWithLifecycle()
    LaunchedEffect(mediaId) { viewModel.load(mediaId) }

    Scaffold(topBar = {
        GalleryTopBar("Media Info", onBack = { navController.popBackStack() })
    }) { padding ->
        if (metadata == null) {
            LoadingOverlay(Modifier.padding(padding))
            return@Scaffold
        }

        val meta = metadata!!
        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            // Thumbnail preview
            item {
                uri?.let {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it).crossfade(true).build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxWidth().height(220.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Resolution & size
            item {
                MetaSection("File Details") {
                    MetaRow(Icons.Filled.AspectRatio,     "Resolution", meta.resolution)
                    MetaRow(Icons.Filled.Storage,         "File Size",  meta.fileSize)
                    MetaRow(Icons.Filled.ImageAspectRatio,"Type",       meta.mimeType)
                    meta.duration?.let { MetaRow(Icons.Filled.Timer, "Duration", it) }
                    meta.dateTaken?.let { MetaRow(Icons.Filled.CalendarToday, "Date Taken", it) }
                }
            }

            // Camera
            if (meta.cameraMake != null || meta.cameraModel != null) {
                item {
                    MetaSection("Camera") {
                        meta.cameraMake?.let  { MetaRow(Icons.Filled.CameraAlt, "Make",  it) }
                        meta.cameraModel?.let { MetaRow(Icons.Filled.Camera,    "Model", it) }
                        meta.aperture?.let    { MetaRow(Icons.Filled.Lens,      "Aperture", it) }
                        meta.shutterSpeed?.let{ MetaRow(Icons.Filled.Timelapse, "Shutter Speed", it) }
                        meta.iso?.let         { MetaRow(Icons.Filled.Iso,       "ISO",    it) }
                        meta.focalLength?.let { MetaRow(Icons.Filled.ZoomIn,    "Focal Length", it) }
                    }
                }
            }

            // Location
            if (meta.gpsLatitude != null && meta.gpsLongitude != null) {
                item {
                    MetaSection("Location") {
                        MetaRow(Icons.Filled.LocationOn, "Latitude",  "%.6f".format(meta.gpsLatitude))
                        MetaRow(Icons.Filled.LocationOn, "Longitude", "%.6f".format(meta.gpsLongitude))
                        meta.locationName?.let { MetaRow(Icons.Filled.Place, "Place", it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style    = MaterialTheme.typography.labelLarge,
            color    = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun MetaRow(icon: ImageVector, label: String, value: String) {
    ListItem(
        leadingContent  = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
        headlineContent = { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
        tonalElevation  = 0.dp,
    )
}
