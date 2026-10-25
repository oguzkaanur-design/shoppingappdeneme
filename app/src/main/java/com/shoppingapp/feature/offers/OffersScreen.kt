package com.shoppingapp.feature.offers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shoppingapp.R
import com.shoppingapp.data.offers.Flyer
import com.shoppingapp.data.offers.FlyerPageUrls
import com.shoppingapp.data.offers.FlyerPhase
import com.shoppingapp.data.offers.FlyerStore
import com.shoppingapp.data.offers.FlyerStores
import com.shoppingapp.ui.theme.AppUiStyle
import com.shoppingapp.ui.theme.LocalAppUiStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(viewModel: OffersViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var openedFlyer by remember { mutableStateOf<Flyer?>(null) }
    val context = LocalContext.current
    val uiStyle = LocalAppUiStyle.current

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(stringResource(R.string.s_prospektusler), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.s_haftalik_brosurler_uygulama_icinde),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            StoreSelector(
                selected = state.selectedStore,
                style = uiStyle,
                onSelect = viewModel::selectStore
            )

            if (state.fromCache && !state.loading) {
                CacheBanner(cachedAt = state.cachedAt, onRetry = viewModel::retry)
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> LoadingState(store = state.selectedStore)

                    state.error != null -> ErrorState(
                        message = state.error.orEmpty(),
                        store = state.selectedStore,
                        onRetry = viewModel::retry
                    )

                    state.flyers.isEmpty() -> EmptyState(
                        store = state.selectedStore,
                        onRetry = viewModel::retry
                    )

                    else -> FlyerList(
                        flyers = state.flyers,
                        store = state.selectedStore,
                        style = uiStyle,
                        onOpen = { openedFlyer = it }
                    )
                }
            }
        }
    }

    openedFlyer?.let { flyer ->
        FlyerReaderDialog(
            flyer = flyer,
            store = state.selectedStore,
            loadOffers = viewModel::offersForPage,
            onAddOffer = { offer, quantity ->
                viewModel.addOfferToFlyerList(offer, quantity)
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_offer_added, offer.name),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onClose = { openedFlyer = null }
        )
    }
}

@Composable
private fun StoreSelector(
    selected: FlyerStore,
    style: AppUiStyle,
    onSelect: (FlyerStore) -> Unit
) {
    val horizontalPadding = when (style) {
        AppUiStyle.COMPACT -> 10.dp
        AppUiStyle.GALLERY -> 16.dp
        else -> 16.dp
    }
    val verticalPadding = when (style) {
        AppUiStyle.COMPACT -> 4.dp
        AppUiStyle.GALLERY -> 8.dp
        else -> 6.dp
    }
    val chipShape = when (style) {
        AppUiStyle.CLASSIC -> RoundedCornerShape(0.dp)
        AppUiStyle.COMPACT -> MaterialTheme.shapes.extraSmall
        AppUiStyle.GALLERY -> MaterialTheme.shapes.large
        else -> RoundedCornerShape(14.dp)
    }
    val chipHorizontal = when (style) {
        AppUiStyle.COMPACT -> 10.dp
        AppUiStyle.GALLERY -> 18.dp
        else -> 16.dp
    }
    val chipVertical = when (style) {
        AppUiStyle.COMPACT -> 6.dp
        AppUiStyle.GALLERY -> 11.dp
        else -> 10.dp
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(if (style == AppUiStyle.COMPACT) 5.dp else 8.dp)
    ) {
        items(FlyerStores.all, key = { it.id }) { store ->
            val isSelected = store.id == selected.id
            val brand = Color(store.brandColor)
            if (style == AppUiStyle.CLASSIC) {
                Column(
                    modifier = Modifier
                        .clickable { onSelect(store) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        store.shortName,
                        color = if (isSelected) brand else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) brand else Color.Transparent)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(if (isSelected) brand else MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                            ),
                            chipShape
                        )
                        .clickable { onSelect(store) }
                        .padding(horizontal = chipHorizontal, vertical = chipVertical)
                ) {
                    Text(
                        store.shortName,
                        color = if (isSelected) Color.White else brand,
                        fontWeight = FontWeight.Black,
                        style = if (style == AppUiStyle.COMPACT) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.labelLarge
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CacheBanner(cachedAt: Long?, onRetry: () -> Unit) {
    val label = cachedAt?.let {
        val date = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.GERMANY)
            .format(java.util.Date(it))
        stringResource(R.string.offers_last_saved_date, date)
    } ?: stringResource(R.string.offers_last_saved)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.s_brosurler_su_an_alinamadi),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        TextButton(onClick = onRetry) { Text(stringResource(R.string.s_yenile)) }
    }
}

@Composable
private fun FlyerList(
    flyers: List<Flyer>,
    store: FlyerStore,
    style: AppUiStyle,
    onOpen: (Flyer) -> Unit
) {
    if (style == AppUiStyle.GALLERY) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "hero") {
                HeroBanner(AppUiStyle.GALLERY)
            }
            gridItems(flyers, key = { it.leafletId }) { flyer ->
                FlyerCard(
                    flyer = flyer,
                    store = store,
                    style = AppUiStyle.GALLERY,
                    onOpen = { onOpen(flyer) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                FlyerFooter()
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (style == AppUiStyle.COMPACT) 10.dp else 16.dp,
                end = if (style == AppUiStyle.COMPACT) 10.dp else 16.dp,
                top = if (style == AppUiStyle.COMPACT) 2.dp else 6.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (style == AppUiStyle.COMPACT) 4.dp else 12.dp
            )
        ) {
            item { HeroBanner(style) }
            items(flyers, key = { it.leafletId }) { flyer ->
                FlyerCard(
                    flyer = flyer,
                    store = store,
                    style = style,
                    onOpen = { onOpen(flyer) }
                )
            }
            item { FlyerFooter() }
        }
    }
}

@Composable
private fun FlyerFooter() {
    Text(
        stringResource(R.string.s_brosurler_marketlerin_yayinladigi_haftalik_prosp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun HeroBanner(style: AppUiStyle) {
    when (style) {
        AppUiStyle.CLASSIC -> Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        stringResource(R.string.s_firsatlar_kapinda).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp
                    )
                    Text(
                        stringResource(R.string.s_brosuru_sec_sayfalari_kaydir_yakinlastir),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        AppUiStyle.COMPACT -> Surface(
            shape = MaterialTheme.shapes.small,
            color = Color(0xFF14583D)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(30.dp).background(Color(0xFFD8F36A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color(0xFF17352C),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    stringResource(R.string.s_firsatlar_kapinda),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        AppUiStyle.GALLERY -> Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14583D))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFFD8F36A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color(0xFF17352C),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    stringResource(R.string.s_firsatlar_kapinda),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    stringResource(R.string.s_brosuru_sec_sayfalari_kaydir_yakinlastir),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD7E7DD),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        AppUiStyle.MODERN -> Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14583D))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFFD8F36A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF17352C))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(R.string.s_firsatlar_kapinda),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(stringResource(R.string.s_brosuru_sec_sayfalari_kaydir_yakinlastir),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD7E7DD)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlyerCard(
    flyer: Flyer,
    store: FlyerStore,
    style: AppUiStyle,
    onOpen: () -> Unit
) {
    when (style) {
        AppUiStyle.MODERN -> ModernFlyerCard(flyer, store, onOpen)
        AppUiStyle.CLASSIC -> ClassicFlyerCard(flyer, store, onOpen)
        AppUiStyle.COMPACT -> CompactFlyerCard(flyer, store, onOpen)
        AppUiStyle.GALLERY -> GalleryFlyerCard(flyer, store, onOpen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernFlyerCard(flyer: Flyer, store: FlyerStore, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlyerCover(flyer, width = 86.dp, height = 122.dp, corner = 12.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                PhaseBadge(flyer, AppUiStyle.MODERN)
                Spacer(Modifier.height(6.dp))
                FlyerCardTitle(flyer, store, maxLines = 2)
                Text(
                    FlyerDateLabel.range(flyer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlyerCounts(flyer)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.s_uygulamada_ac),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(store.brandColor)
                )
            }
        }
    }
}

@Composable
private fun ClassicFlyerCard(flyer: Flyer, store: FlyerStore, onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlyerCover(flyer, width = 74.dp, height = 104.dp, corner = 0.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.p_x_prospektu, store.shortName).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 0.8.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    FlyerDateLabel.range(flyer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlyerCounts(flyer)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 5.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    FlyerDateLabel.phase(flyer).let {
                        when (it) {
                            FlyerPhase.CURRENT -> stringResource(R.string.flyer_phase_current)
                            FlyerPhase.UPCOMING -> stringResource(R.string.flyer_phase_upcoming)
                            FlyerPhase.PAST -> stringResource(R.string.flyer_phase_past)
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(store.brandColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactFlyerCard(flyer: Flyer, store: FlyerStore, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlyerCover(flyer, width = 52.dp, height = 72.dp, corner = 4.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                Text(
                    stringResource(R.string.p_x_prospektu, store.shortName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    FlyerDateLabel.range(flyer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    stringResource(R.string.offers_page_count, flyer.pageCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PhaseBadge(flyer, AppUiStyle.COMPACT)
            Icon(
                Icons.Default.LocalOffer,
                contentDescription = stringResource(R.string.s_uygulamada_ac),
                modifier = Modifier.padding(horizontal = 6.dp).size(17.dp),
                tint = Color(store.brandColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryFlyerCard(flyer: Flyer, store: FlyerStore, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                FlyerCover(
                    flyer,
                    width = 0.dp,
                    height = 176.dp,
                    corner = 0.dp,
                    fillWidth = true
                )
                Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    PhaseBadge(flyer, AppUiStyle.GALLERY)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Text(
                    stringResource(R.string.p_x_prospektu, store.shortName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    FlyerDateLabel.range(flyer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.offers_page_count, flyer.pageCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FlyerCover(
    flyer: Flyer,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    corner: androidx.compose.ui.unit.Dp,
    fillWidth: Boolean = false
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(FlyerPageUrls.cover(flyer.leafletId))
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.size(width = width, height = height))
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun FlyerCardTitle(flyer: Flyer, store: FlyerStore, maxLines: Int) {
    Text(
        stringResource(R.string.p_x_prospektu, store.shortName),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun FlyerCounts(flyer: Flyer) {
    Text(
        buildString {
            append(stringResource(R.string.offers_page_count, flyer.pageCount))
            if (flyer.offerCount > 0) {
                append(" · ")
                append(stringResource(R.string.offers_offer_count, flyer.offerCount))
            }
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PhaseBadge(flyer: Flyer, style: AppUiStyle) {
    val (container, content) = when (flyer.phase()) {
        FlyerPhase.CURRENT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        FlyerPhase.UPCOMING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        FlyerPhase.PAST -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = when (style) {
        AppUiStyle.CLASSIC -> RoundedCornerShape(0.dp)
        AppUiStyle.COMPACT -> RoundedCornerShape(3.dp)
        else -> MaterialTheme.shapes.extraSmall
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(container)
            .padding(horizontal = if (style == AppUiStyle.COMPACT) 5.dp else 8.dp, vertical = 3.dp)
    ) {
        Text(
            when (FlyerDateLabel.phase(flyer)) {
                FlyerPhase.CURRENT -> stringResource(R.string.flyer_phase_current)
                FlyerPhase.UPCOMING -> stringResource(R.string.flyer_phase_upcoming)
                FlyerPhase.PAST -> stringResource(R.string.flyer_phase_past)
            },
            style = if (style == AppUiStyle.COMPACT) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun LoadingState(store: FlyerStore) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.p_x_prospektusleri_getiriliyor, store.name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(stringResource(R.string.s_brosur_listesi_internetten_aliniyor),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, store: FlyerStore, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.s_prospektusler_yuklenemedi),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(stringResource(R.string.s_yeniden_dene))
        }
        TextButton(onClick = { openUrl(context, store.pageUrl) }) {
            Text(stringResource(R.string.s_resmi_sayfayi_tarayicida_ac))
        }
    }
}

@Composable
private fun EmptyState(store: FlyerStore, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.p_x_icin_guncel_brosur_bulunamadi, store.name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.s_yeni_haftanin_brosuru_henuz_yayinlanmamis_olabil),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(stringResource(R.string.s_yeniden_dene))
        }
        TextButton(onClick = { openUrl(context, store.pageUrl) }) {
            Text(stringResource(R.string.s_resmi_sayfayi_tarayicida_ac))
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}
