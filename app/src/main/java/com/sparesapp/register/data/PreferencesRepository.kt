package com.sparesapp.register.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "spares_register_prefs")

/** A remembered OneDrive/SharePoint file or folder location: everything needed to re-fetch it without re-browsing. */
data class RememberedLocation(
    val driveId: String,
    val itemId: String,
    val name: String,
    val path: String,
    val isRoot: Boolean = false,
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val INV_DRIVE = stringPreferencesKey("inv_drive_id")
        val INV_ITEM = stringPreferencesKey("inv_item_id")
        val INV_NAME = stringPreferencesKey("inv_name")
        val INV_PATH = stringPreferencesKey("inv_path")

        val IMG_DRIVE = stringPreferencesKey("img_drive_id")
        val IMG_ITEM = stringPreferencesKey("img_item_id")
        val IMG_NAME = stringPreferencesKey("img_name")
        val IMG_PATH = stringPreferencesKey("img_path")
        val IMG_IS_ROOT = stringPreferencesKey("img_is_root")

        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    val inventoryLocation: Flow<RememberedLocation?> = context.dataStore.data.map { p ->
        val drive = p[Keys.INV_DRIVE] ?: return@map null
        val item = p[Keys.INV_ITEM] ?: return@map null
        RememberedLocation(drive, item, p[Keys.INV_NAME].orEmpty(), p[Keys.INV_PATH].orEmpty())
    }

    val imagesLocation: Flow<RememberedLocation?> = context.dataStore.data.map { p ->
        val drive = p[Keys.IMG_DRIVE] ?: return@map null
        val item = p[Keys.IMG_ITEM] ?: return@map null
        RememberedLocation(drive, item, p[Keys.IMG_NAME].orEmpty(), p[Keys.IMG_PATH].orEmpty(), p[Keys.IMG_IS_ROOT] == "true")
    }

    val lastSyncAt: Flow<Long?> = context.dataStore.data.map { it[Keys.LAST_SYNC_AT] }

    suspend fun setInventoryLocation(loc: RememberedLocation) {
        context.dataStore.edit { p ->
            p[Keys.INV_DRIVE] = loc.driveId
            p[Keys.INV_ITEM] = loc.itemId
            p[Keys.INV_NAME] = loc.name
            p[Keys.INV_PATH] = loc.path
        }
    }

    suspend fun setImagesLocation(loc: RememberedLocation) {
        context.dataStore.edit { p ->
            p[Keys.IMG_DRIVE] = loc.driveId
            p[Keys.IMG_ITEM] = loc.itemId
            p[Keys.IMG_NAME] = loc.name
            p[Keys.IMG_PATH] = loc.path
            p[Keys.IMG_IS_ROOT] = loc.isRoot.toString()
        }
    }

    suspend fun clearInventoryLocation() {
        context.dataStore.edit { p ->
            p.remove(Keys.INV_DRIVE); p.remove(Keys.INV_ITEM); p.remove(Keys.INV_NAME); p.remove(Keys.INV_PATH)
        }
    }

    suspend fun clearImagesLocation() {
        context.dataStore.edit { p ->
            p.remove(Keys.IMG_DRIVE); p.remove(Keys.IMG_ITEM); p.remove(Keys.IMG_NAME); p.remove(Keys.IMG_PATH); p.remove(Keys.IMG_IS_ROOT)
        }
    }

    suspend fun setLastSyncAt(ts: Long) {
        context.dataStore.edit { p -> p[Keys.LAST_SYNC_AT] = ts }
    }
}
