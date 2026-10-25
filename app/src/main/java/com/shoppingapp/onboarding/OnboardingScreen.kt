package com.shoppingapp.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shoppingapp.R
import com.shoppingapp.account.AccountStore
import com.shoppingapp.account.AccountType
import com.shoppingapp.account.GoogleSignIn
import com.shoppingapp.account.GoogleSignInResult
import com.shoppingapp.account.LocalAccountDialog
import com.shoppingapp.feature.language.LanguageCatalog
import com.shoppingapp.feature.language.LanguageStore
import kotlinx.coroutines.launch

object OnboardingTestTags {
    const val CONTINUE = "onboarding_continue"
    const val BACK = "onboarding_back"
    const val COUNTRY_SEARCH = "onboarding_country_search"
    const val SKIP_ACCOUNT = "onboarding_skip_account"
    const val GOOGLE = "onboarding_google"
    const val LOCAL_ACCOUNT = "onboarding_local_account"
    const val TOUR_START = "onboarding_tour_start"
    const val TOUR_SKIP = "onboarding_tour_skip"
    fun country(code: String) = "onboarding_country_$code"
}

private const val STEP_COUNT = 4

/**
 * İlk açılış akışı: dil → ülke/para birimi → hesap → tanıtım turu.
 *
 * Adımlar `rememberSaveable` ile saklanır: dil değişimi (API 33 altında)
 * Activity'yi yeniden oluşturduğunda kullanıcı kaldığı yerden devam eder.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var countryCode by rememberSaveable {
        mutableStateOf(
            (OnboardingStore.country(context)
                ?: CountryCatalog.suggestFor(LanguageStore.currentTag(context))).code
        )
    }
    val country = CountryCatalog.of(countryCode) ?: CountryCatalog.defaultCountry()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (step < STEP_COUNT - 1) {
                StepIndicator(step = step, total = STEP_COUNT - 1)
            }
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "onboarding-step"
            ) { current ->
                when (current) {
                    0 -> LanguageStep(
                        activity = activity,
                        onContinue = { step = 1 }
                    )
                    1 -> CountryStep(
                        selected = country,
                        onSelect = { countryCode = it.code },
                        onBack = { step = 0 },
                        onContinue = {
                            OnboardingStore.setCountry(context, country)
                            step = 2
                        }
                    )
                    2 -> AccountStep(
                        onDone = { step = 3 }
                    )
                    else -> TourStep(
                        onFinished = {
                            OnboardingStore.markCompleted(context)
                            onFinished()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (index <= step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

// ---------------------------------------------------------------- 1. dil

@Composable
private fun LanguageStep(activity: Activity?, onContinue: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val currentTag = LanguageStore.currentTag(context)
    val languages = remember(query) { LanguageCatalog.filter(query) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.onboarding_language_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.language_search)) }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(languages, key = { it.tag }) { language ->
                SelectableRow(
                    leading = language.flag,
                    title = language.nativeName,
                    subtitle = language.englishName,
                    selected = currentTag?.equals(language.tag, ignoreCase = true) == true,
                    onClick = { activity?.let { LanguageStore.setTag(it, language.tag) } }
                )
            }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(OnboardingTestTags.CONTINUE)
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ------------------------------------------------------------- 2. ülke

@Composable
private fun CountryStep(
    selected: Country,
    onSelect: (Country) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val countries = remember(query) { CountryCatalog.search(query) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag(OnboardingTestTags.BACK)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.onboarding_back)
                )
            }
            Text(
                stringResource(R.string.onboarding_country_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            stringResource(R.string.onboarding_country_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.COUNTRY_SEARCH),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.onboarding_country_search)) }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(countries, key = { it.code }) { country ->
                SelectableRow(
                    leading = country.flag,
                    title = country.name,
                    subtitle = "${country.currency.code} · ${country.currency.symbol}",
                    selected = country.code == selected.code,
                    modifier = Modifier.testTag(OnboardingTestTags.country(country.code)),
                    onClick = { onSelect(country) }
                )
            }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(OnboardingTestTags.CONTINUE)
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ----------------------------------------------------------- 3. hesap

@Composable
private fun AccountStep(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLocalDialog by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val configured = GoogleSignIn.isConfigured()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.onboarding_account_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.onboarding_account_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = {
                busy = true
                status = null
                scope.launch {
                    when (val result = GoogleSignIn.signIn(context)) {
                        is GoogleSignInResult.Success -> {
                            AccountStore.signIn(
                                context,
                                AccountType.GOOGLE,
                                result.email,
                                result.displayName
                            )
                            onDone()
                        }
                        GoogleSignInResult.NotConfigured ->
                            status = context.getString(R.string.onboarding_google_unavailable)
                        GoogleSignInResult.Cancelled -> status = null
                        is GoogleSignInResult.Failed -> status = result.message
                    }
                    busy = false
                }
            },
            enabled = configured && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(OnboardingTestTags.GOOGLE)
        ) {
            Text(stringResource(R.string.onboarding_google))
        }
        if (!configured) {
            Text(
                stringResource(R.string.onboarding_google_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        OutlinedButton(
            onClick = { showLocalDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(top = 12.dp)
                .testTag(OnboardingTestTags.LOCAL_ACCOUNT)
        ) {
            Text(stringResource(R.string.onboarding_local))
        }

        TextButton(
            onClick = {
                AccountStore.signOut(context)
                onDone()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(OnboardingTestTags.SKIP_ACCOUNT)
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }

        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }

    if (showLocalDialog) {
        LocalAccountDialog(
            onDismiss = { showLocalDialog = false },
            onCreated = {
                showLocalDialog = false
                onDone()
            }
        )
    }
}

// ------------------------------------------------------------- 4. tur

private data class TourPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

private val tourPages = listOf(
    TourPage(Icons.Default.Checklist, R.string.tour_list_title, R.string.tour_list_desc),
    TourPage(Icons.Default.QrCodeScanner, R.string.tour_scan_title, R.string.tour_scan_desc),
    TourPage(Icons.Default.LocalOffer, R.string.tour_offers_title, R.string.tour_offers_desc),
    TourPage(Icons.Default.Savings, R.string.tour_money_title, R.string.tour_money_desc),
    TourPage(Icons.Default.ReceiptLong, R.string.tour_report_title, R.string.tour_report_desc)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TourStep(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { tourPages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == tourPages.lastIndex

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier.testTag(OnboardingTestTags.TOUR_SKIP)
            ) {
                Text(stringResource(R.string.onboarding_tour_later))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { index ->
            val page = tourPages[index]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        page.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Text(
                    stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    stringResource(page.descriptionRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(tourPages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pagerState.currentPage) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        Button(
            onClick = {
                if (lastPage) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(54.dp)
                .testTag(OnboardingTestTags.TOUR_START)
        ) {
            Text(
                stringResource(
                    if (lastPage) R.string.onboarding_tour_start
                    else R.string.onboarding_tour_next
                )
            )
        }
        Text(
            stringResource(
                R.string.onboarding_step_of,
                pagerState.currentPage + 1,
                tourPages.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp)
        )
    }
}

// ------------------------------------------------------------- ortak

@Composable
private fun SelectableRow(
    leading: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = leading, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
