package com.shoppingapp.feature.stats

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pfand_prefs")

@Singleton
class PfandManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val PFAND_PLASTIC = 0.25
        private val PFAND_GLASS = 0.15
        private val PFAND_CASE = 1.50
    }

    val pfandTotal: Flow<Double> = context.dataStore.data.map { preferences ->
        // In a real app, this would map to the total pfand from purchase history
        0.0
    }

    suspend fun calculatePfandForPurchase(itemCount: Int): Double {
        // Simulate calculation
        return itemCount * PFAND_PLASTIC
    }
}
