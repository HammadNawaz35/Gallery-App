package com.gallery.app.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.gallery.app.data.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_prefs")

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    private object Keys {
        val DARK_MODE       = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLOR   = booleanPreferencesKey("dynamic_color")
        val GRID_SIZE       = stringPreferencesKey("grid_size")
        val SORT_ORDER      = stringPreferencesKey("sort_order")
        val SLIDESHOW_SPEED = stringPreferencesKey("slideshow_speed")
        val VAULT_PIN       = stringPreferencesKey("vault_pin")
        val VAULT_AUTH_TYPE = stringPreferencesKey("vault_auth_type")
    }

    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppPreferences(
                darkMode       = prefs[Keys.DARK_MODE]       ?: false,
                dynamicColor   = prefs[Keys.DYNAMIC_COLOR]   ?: true,
                gridSize       = GridSize.valueOf(prefs[Keys.GRID_SIZE]       ?: GridSize.MEDIUM.name),
                sortOrder      = SortOrder.valueOf(prefs[Keys.SORT_ORDER]     ?: SortOrder.NEWEST.name),
                slideshowSpeed = SlideshowSpeed.valueOf(prefs[Keys.SLIDESHOW_SPEED] ?: SlideshowSpeed.NORMAL.name),
                vaultPin       = prefs[Keys.VAULT_PIN],
                vaultAuthType  = VaultAuthType.valueOf(prefs[Keys.VAULT_AUTH_TYPE] ?: VaultAuthType.NONE.name),
            )
        }

    suspend fun setDarkMode(enabled: Boolean) =
        dataStore.edit { it[Keys.DARK_MODE] = enabled }

    suspend fun setDynamicColor(enabled: Boolean) =
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setGridSize(size: GridSize) =
        dataStore.edit { it[Keys.GRID_SIZE] = size.name }

    suspend fun setSortOrder(order: SortOrder) =
        dataStore.edit { it[Keys.SORT_ORDER] = order.name }

    suspend fun setSlideshowSpeed(speed: SlideshowSpeed) =
        dataStore.edit { it[Keys.SLIDESHOW_SPEED] = speed.name }

    suspend fun setVaultPin(pin: String?) {
        dataStore.edit { prefs ->
            if (pin != null) prefs[Keys.VAULT_PIN] = pin
            else prefs.remove(Keys.VAULT_PIN)
        }
    }

    suspend fun setVaultAuthType(type: VaultAuthType) =
        dataStore.edit { it[Keys.VAULT_AUTH_TYPE] = type.name }
}
