package com.shoppingapp.testing

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.shoppingapp.money.Currency
import com.shoppingapp.money.CurrencyStore
import com.shoppingapp.onboarding.OnboardingStore

/**
 * Uyarlanmış testler uygulamanın **içine** bakmak ister; ilk açılış akışı her
 * testi engellemesin diye onboarding tamamlanmış sayılır ve para birimi avroya
 * sabitlenir. Akışın kendisi `OnboardingFlowTest` içinde ayrıca test edilir.
 */
class KorbTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        runCatching {
            val context = targetContext
            OnboardingStore.markCompleted(context)
            CurrencyStore.set(context, Currency.EUR)
        }
    }
}
