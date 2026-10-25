package com.shoppingapp.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Örnek ürünlerin yalnızca ilk kurulumda eklenmesini sağlar.
 *
 * Daha önce her açılışta "ürün tablosu boşsa örnekleri ekle" kontrolü vardı;
 * bu yüzden "Tüm verileri sil" sonrası uygulama yeniden açıldığında demo liste
 * geri geliyordu. Bayrak, silme işleminden sonra da true kalır.
 */
@Singleton
class SeedState @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences("seed_state", Context.MODE_PRIVATE)

    var isSeeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SEEDED, value).apply()
        }

    private companion object {
        const val KEY_SEEDED = "seeded"
    }
}
