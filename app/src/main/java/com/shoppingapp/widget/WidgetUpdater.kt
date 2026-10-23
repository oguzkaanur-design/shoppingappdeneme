package com.shoppingapp.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Liste değiştiğinde ana ekran widget'ını tazeler. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun refresh() = ShoppingWidgetProvider.updateAll(context)
}
