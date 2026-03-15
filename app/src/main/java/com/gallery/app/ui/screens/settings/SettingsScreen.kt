package com.gallery.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.gallery.app.data.models.*
import com.gallery.app.ui.components.GalleryTopBar
import com.gallery.app.ui.navigation.Screen
import com.gallery.app.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val prefs   by viewModel.preferences.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { GalleryTopBar("Settings", onBack = { navController.popBackStack() }) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {

            // ── Appearance ────────────────────────────────
            item { SectionHeader("Appearance") }
            item {
                SwitchItem(
                    icon    = Icons.Filled.DarkMode,
                    title   = "Dark Mode",
                    checked = prefs.darkMode,
                    onCheckedChange = viewModel::setDarkMode,
                )
            }
            item {
                SwitchItem(
                    icon     = Icons.Filled.Palette,
                    title    = "Dynamic Color",
                    subtitle = "Use wallpaper colors (Android 12+)",
                    checked  = prefs.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            // ── Grid ──────────────────────────────────────
            item { SectionHeader("Grid") }
            item {
                DropdownItem(
                    icon    = Icons.Filled.GridView,
                    title   = "Grid Size",
                    current = "${prefs.gridSize.columns} columns",
                    options = GridSize.values().map { "${it.columns} columns" to it },
                    onSelect = viewModel::setGridSize,
                )
            }

            // ── Sorting ───────────────────────────────────
            item { SectionHeader("Sorting") }
            item {
                DropdownItem(
                    icon    = Icons.Filled.Sort,
                    title   = "Default Sort Order",
                    current = prefs.sortOrder.label(),
                    options = SortOrder.values().map { it.label() to it },
                    onSelect = viewModel::setSortOrder,
                )
            }

            // ── Slideshow ─────────────────────────────────
            item { SectionHeader("Slideshow") }
            item {
                DropdownItem(
                    icon    = Icons.Filled.Slideshow,      // was "SlideshowOutlined" which doesn't exist
                    title   = "Slideshow Speed",
                    current = prefs.slideshowSpeed.label,
                    options = SlideshowSpeed.values().map { it.label to it },
                    onSelect = { speed ->
                        viewModel.setSlideshowSpeed(speed)
                    },
                )
            }
            // ── Privacy ───────────────────────────────────
            item { SectionHeader("Privacy") }
            item {
                ClickableItem(
                    icon     = Icons.Filled.Lock,
                    title    = "Hidden Vault",
                    subtitle = "Manage your hidden photos",
                    onClick  = { navController.navigate(Screen.Vault.route) },
                )
            }
            item {
                ClickableItem(
                    icon     = Icons.Filled.Delete,
                    title    = "Recycle Bin",
                    subtitle = "Recently deleted items",
                    onClick  = { navController.navigate(Screen.RecycleBin.route) },
                )
            }

            // ── About ─────────────────────────────────────
            item { SectionHeader("About") }

            item {
                ClickableItem(
                    icon     = Icons.Filled.Info,
                    title    = "About Gallery",
                    subtitle = "Version 1.0 ",
                    onClick  = { showAboutDialog = true },
                )
            }

            item {
                ClickableItem(
                    icon     = Icons.Filled.Email,
                    title    = "Contact Us",
                    subtitle = "Email",
                    onClick  = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data    = Uri.parse("mailto:hammadnawaz35@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Gallery App Feedback")
                        }
                        context.startActivity(
                            Intent.createChooser(emailIntent, "Send Email")
                        )
                    },
                )
            }

            item {
                ClickableItem(
                    icon     = Icons.Filled.StarRate,
                    title    = "Rate the App",
                    subtitle = "Love the app? Leave a review!",
                    onClick  = {
                        val playIntent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.gallery.app"))
                        context.startActivity(playIntent)
                    },
                )
            }

            item {
                ListItem(
                    leadingContent  = { Icon(Icons.Filled.Code, null,
                        tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Version") },
                    trailingContent = {
                        Text("1.0.0", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                )
            }
        }
    }

    // ── About Dialog ──────────────────────────────────────
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(Icons.Filled.PhotoLibrary, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp))
            },
            title = {
                Text("Gallery", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Version 1.0",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Developed by", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Hammad Nawaz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))
                    Text("A flagship-quality media gallery featuring photo viewer, " +
                         "video playback, smart search, hidden vault, and much more.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Contact: hammadnawaz35@gmail.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            },
            shape = RoundedCornerShape(20.dp),
        )
    }
}

// ── Section header ────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

// ── Switch row ────────────────────────────────────────────

@Composable
private fun SwitchItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    ListItem(
        leadingContent   = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent  = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent  = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

// ── Dropdown row ──────────────────────────────────────────

@Composable
private fun <T> DropdownItem(
    icon: ImageVector,
    title: String,
    current: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        leadingContent  = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        trailingContent = {
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(current)
                    Icon(Icons.Filled.ArrowDropDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (label, value) ->
                        DropdownMenuItem(
                            text    = { Text(label) },
                            onClick = { onSelect(value); expanded = false },
                        )
                    }
                }
            }
        },
    )
}

// ── Clickable row ─────────────────────────────────────────

@Composable
private fun ClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        modifier        = Modifier.clickable(onClick = onClick),
        leadingContent  = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it,
            color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        trailingContent = { Icon(Icons.Filled.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant) },
    )
}

// ── Label helpers ─────────────────────────────────────────

private fun SortOrder.label() = when (this) {
    SortOrder.NEWEST    -> "Newest first"
    SortOrder.OLDEST    -> "Oldest first"
    SortOrder.NAME_ASC  -> "Name A–Z"
    SortOrder.NAME_DESC -> "Name Z–A"
    SortOrder.SIZE_DESC -> "Largest first"
    SortOrder.SIZE_ASC  -> "Smallest first"
}
