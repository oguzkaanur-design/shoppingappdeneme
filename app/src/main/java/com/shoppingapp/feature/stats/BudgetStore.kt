package com.shoppingapp.feature.stats

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_prefs")

/**
 * Aylık harcama limiti. Veritabanı tablosu gerektirmeyen tek bir tercih olduğu için
 * DataStore'da tutulur; limit 0 ise bütçe takibi kapalıdır.
 */
@Singleton
class BudgetStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val limitKey = doublePreferencesKey("monthly_limit_euro")

    val monthlyLimit: Flow<Double> = context.budgetDataStore.data.map { prefs ->
        prefs[limitKey] ?: 0.0
    }

    suspend fun setMonthlyLimit(value: Double) {
        context.budgetDataStore.edit { prefs ->
            if (value > 0.0 && value.isFinite()) {
                prefs[limitKey] = value
            } else {
                prefs.remove(limitKey)
            }
        }
    }
}
