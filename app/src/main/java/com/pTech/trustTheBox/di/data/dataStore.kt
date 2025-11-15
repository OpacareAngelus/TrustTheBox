package com.pTech.trustTheBox.di.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trustthebox_prefs")

object PassphraseDataStore {
    private val PASSPHRASE_KEY = stringPreferencesKey("passphrase")

    suspend fun savePassphrase(context: Context, passphrase: String) {
        context.dataStore.edit { prefs ->
            prefs[PASSPHRASE_KEY] = passphrase
        }
    }

    fun getPassphrase(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[PASSPHRASE_KEY] ?: ""
        }
    }
}