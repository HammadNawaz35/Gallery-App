package com.gallery.app.ui.screens.vault

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.gallery.app.data.models.MediaItem
import com.gallery.app.domain.usecases.*
import com.gallery.app.ui.components.*
import com.gallery.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val getHiddenMedia: GetHiddenMediaUseCase,
    private val unhideMedia: UnhideMediaUseCase,
    private val getPreferences: GetPreferencesUseCase,
    private val updatePreferences: UpdatePreferencesUseCase,
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    val prefs = getPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly,
            com.gallery.app.data.models.AppPreferences())

    val hiddenMedia: StateFlow<List<MediaItem>> =
        isUnlocked.flatMapLatest { unlocked ->
            if (unlocked) getHiddenMedia()
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> =
        selectedIds.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun unlock(pin: String? = null): Boolean {
        val storedPin = prefs.value.vaultPin
        return if (storedPin == null || storedPin == pin) {
            _isUnlocked.value = true
            true
        } else false
    }

    fun unlockBiometric() { _isUnlocked.value = true }

    fun lock() { 
        _isUnlocked.value = false 
        clearSelection()
    }

    fun setPin(pin: String) = viewModelScope.launch {
        updatePreferences { it.copy(vaultPin = pin, vaultAuthType = com.gallery.app.data.models.VaultAuthType.PIN) }
        _isUnlocked.value = true
    }

    fun toggleSelection(id: Long) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun onUnhideSelected() = viewModelScope.launch {
        unhideMedia(_selectedIds.value.toList())
        clearSelection()
    }

    fun onUnhide(mediaId: Long) = viewModelScope.launch {
        unhideMedia(listOf(mediaId))
    }
}

// ── Activity Helper ───────────────────────────────────────

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

// ── Screen ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    navController: NavController,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val hiddenMedia by viewModel.hiddenMedia.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()

    if (!isUnlocked) {
        LockScreen(
            hasPin           = prefs.vaultPin != null,
            onSetPin         = { pin -> viewModel.setPin(pin) },
            onUnlockWithPin  = { pin -> viewModel.unlock(pin) },
            onUnlockBiometric= { viewModel.unlockBiometric() },
        )
        return
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionToolbar(
                    selectedCount    = selectedIds.size,
                    onShare          = {},
                    onDelete         = {},
                    onFavorite       = {},
                    onHide           = { viewModel.onUnhideSelected() }, 
                    onClearSelection = { viewModel.clearSelection() },
                    hideIcon         = Icons.Filled.Visibility,
                    hideLabel        = "Unhide"
                )
            } else {
                GalleryTopBar(
                    title  = "Hidden Vault",
                    onBack = { navController.popBackStack() },
                    actions = {
                        IconButton(onClick = { viewModel.lock() }) {
                            Icon(Icons.Filled.Lock, "Lock vault")
                        }
                    },
                )
            }
        },
    ) { padding ->
        if (hiddenMedia.isEmpty()) {
            EmptyState(
                icon     = Icons.Filled.LockOpen,
                message  = "Vault is empty",
                subtitle = "Hide photos here to keep them private",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                contentPadding        = padding,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement   = Arrangement.spacedBy(2.dp),
                modifier              = Modifier.fillMaxSize(),
            ) {
                items(hiddenMedia, key = { it.id }) { item ->
                    MediaGridItem(
                        item        = item,
                        isSelected  = item.id in selectedIds,
                        onTap       = {
                            if (isSelectionMode) viewModel.toggleSelection(item.id)
                            else {
                                if (item.isVideo) navController.navigate(Screen.VideoPlayer.createRoute(item.id))
                                else navController.navigate(Screen.PhotoViewer.createRoute(item.id, "hidden"))
                            }
                        },
                        onLongPress = { viewModel.toggleSelection(item.id) },
                    )
                }
            }
        }
    }
}

// ── Lock Screen ───────────────────────────────────────────

@Composable
private fun LockScreen(
    hasPin: Boolean,
    onSetPin: (String) -> Unit,
    onUnlockWithPin: (String) -> Boolean,
    onUnlockBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val showBiometricPrompt = {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
        
        val canAuth = biometricManager.canAuthenticate(authenticators)
        
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val activity = context.findActivity()

            if (activity != null) {
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onUnlockBiometric()
                        }
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Vault")
                    .setSubtitle("Use fingerprint or face recognition")
                    .setAllowedAuthenticators(authenticators)
                    .setNegativeButtonText("Cancel")
                    .build()
                
                prompt.authenticate(promptInfo)
            } else {
                Toast.makeText(context, "Authentication error: Activity not found", Toast.LENGTH_SHORT).show()
            }
        } else if (canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            Toast.makeText(context, "Please set up a fingerprint in device settings", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-trigger on enter if has pin
    LaunchedEffect(hasPin) {
        if (hasPin) {
            delay(1000) // Longer delay for Android 14 transitions
            showBiometricPrompt()
        }
    }

    Box(
        modifier            = Modifier.fillMaxSize(),
        contentAlignment    = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector        = Icons.Filled.Lock,
                contentDescription = null,
                modifier           = Modifier.size(80.dp),
                tint               = MaterialTheme.colorScheme.primary,
            )
            Text("Hidden Vault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            
            if (!hasPin) {
                Text("Setup a PIN to secure your vault", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value         = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label         = { Text("New PIN (4-6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    modifier      = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick  = { if (pin.length >= 4) onSetPin(pin) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Setup PIN")
                }
            } else {
                Text("Enter PIN to unlock", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value         = pin,
                    onValueChange = { if (it.length <= 6) { pin = it; error = false } },
                    label         = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    isError       = error,
                    supportingText = if (error) ({ Text("Incorrect PIN") }) else null,
                    modifier      = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick  = {
                        if (!onUnlockWithPin(pin)) { error = true; pin = "" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlock")
                }

                OutlinedButton(
                    onClick  = { showBiometricPrompt() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Fingerprint, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use Fingerprint")
                }
            }
        }
    }
}
