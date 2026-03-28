package com.polyinsights.nfccloner.nfc.keys

import android.content.Context
import android.content.SharedPreferences
import com.polyinsights.nfccloner.util.HexUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nfc_keys", Context.MODE_PRIVATE)

    private companion object {
        const val CUSTOM_KEYS_KEY = "custom_keys"
    }

    fun getAllKeys(): List<ByteArray> {
        val customKeys = getCustomKeys()
        // Custom keys first (user likely knows the right key), then defaults
        return customKeys + DefaultKeys.KEYS
    }

    fun getCustomKeys(): List<ByteArray> {
        val raw = prefs.getStringSet(CUSTOM_KEYS_KEY, emptySet()) ?: emptySet()
        return raw.mapNotNull { hex ->
            try {
                val bytes = HexUtil.hexToBytes(hex)
                if (bytes.size == 6) bytes else null
            } catch (_: Exception) { null }
        }
    }

    fun addCustomKey(hexKey: String): Boolean {
        val clean = hexKey.replace(" ", "").replace(":", "").uppercase()
        if (clean.length != 12) return false
        try { HexUtil.hexToBytes(clean) } catch (_: Exception) { return false }

        val existing = prefs.getStringSet(CUSTOM_KEYS_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existing.add(clean)
        prefs.edit().putStringSet(CUSTOM_KEYS_KEY, existing).apply()
        return true
    }

    fun removeCustomKey(hexKey: String): Boolean {
        val clean = hexKey.replace(" ", "").replace(":", "").uppercase()
        val existing = prefs.getStringSet(CUSTOM_KEYS_KEY, mutableSetOf())?.toMutableSet() ?: return false
        val removed = existing.remove(clean)
        if (removed) {
            prefs.edit().putStringSet(CUSTOM_KEYS_KEY, existing).apply()
        }
        return removed
    }

    fun importKeysFromText(text: String): Int {
        var count = 0
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && addCustomKey(trimmed)) {
                count++
            }
        }
        return count
    }
}
