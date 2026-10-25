package com.shoppingapp.feature.offers

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import com.shoppingapp.R
import com.shoppingapp.data.offers.Flyer
import com.shoppingapp.data.offers.FlyerOffer
import com.shoppingapp.data.offers.FlyerPageUrls
import com.shoppingapp.data.offers.FlyerStore
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.min

private val ReaderBackground = Color(0xFF0B1220)
private val ReaderMuted = Color(0xFFCBD5E1)
private val MarkerColor = Color(0xFFD8F36A)

/**
 * Prospektüsü uygulama içinde gösterir. Sayfalar arasında kaydırma, çift dokunarak
 * yakınlaştırma ve iki parmakla büyütme desteklenir. Sayfadaki ürün alanlarına
 * dokununca ürün bilgisi ve fiyatı açılır, fırsat listesine eklenebilir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlyerReaderDialog(
    flyer: Flyer,
    store: FlyerStore,
    loadOffers: suspend (leafletId: Long, pageIndex: Int) -> List<FlyerOffer>,
    onAddOffer: (FlyerOffer, Int) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { flyer.pageCount })
    var zoomedPage by remember { mutableIntStateOf(-1) }
    var selectedOffer by remember { mutableStateOf<FlyerOffer?>(null) }
    var offersByPage by remember(flyer.leafletId) { mutableStateOf<Map<Int, List<FlyerOffer>>>(emptyMap()) }
    var offersLoading by remember(flyer.leafletId) { mutableStateOf(false) }

    LaunchedEffect(flyer.leafletId, pagerState.currentPage) {
        zoomedPage = -1
        val page = pagerState.currentPage

        val loader = context.imageLoader
        listOf(page - 1, page + 1)
            .filter { it in 0 until flyer.pageCount }
            .forEach { index ->
                loader.enqueue(
                    ImageRequest.Builder(context)
                        .data(FlyerPageUrls.page(flyer.leafletId, index, size = "large"))
                        .build()
                )
            }

        if (offersByPage[page] == null) {
            offersLoading = true
            val loaded = runCatching { loadOffers(flyer.leafletId, page) }.getOrDefault(emptyList())
            offersByPage = offersByPage + (page to loaded)
            offersLoading = false
        }
    }

    val currentOffers = offersByPage[pagerState.currentPage].orEmpty()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ReaderBackground)
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.s_kapat), tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.p_x_prospektu, store.name),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.p_x_x_sayfa, FlyerDateLabel.range(flyer), flyer.pageCount),
                        color = ReaderMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, store.pageUrl.toUri())
                    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = stringResource(R.string.s_resmi_sayfada_ac), tint = Color.White)
                }
            }
            Divider(color = Color.White.copy(alpha = 0.12f))

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = zoomedPage != pagerState.currentPage,
                modifier = Modifier.weight(1f)
            ) { index ->
                FlyerPageImage(
                    flyer = flyer,
                    index = index,
                    offers = offersByPage[index].orEmpty(),
                    onOfferClick = { selectedOffer = it },
                    onZoomChanged = { zoomed -> zoomedPage = if (zoomed) index else -1 }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    }
                ) {
                    Text(stringResource(R.string.s_onceki), color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.p_x_x_3, pagerState.currentPage + 1, flyer.pageCount),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when {
                            offersLoading && currentOffers.isEmpty() ->
                                stringResource(R.string.reader_loading)
                            currentOffers.any { it.hotspot != null } ->
                                stringResource(R.string.reader_tap_product)
                            else -> stringResource(R.string.reader_gesture_hint)
                        },
                        color = ReaderMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage + 1).coerceAtMost(flyer.pageCount - 1)
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.s_sonraki), color = Color.White)
                }
            }
        }
    }

    selectedOffer?.let { offer ->
        FlyerOfferSheet(
            offer = offer,
            store = store,
            onDismiss = { selectedOffer = null },
            onAdd = { quantity ->
                onAddOffer(offer, quantity)
                selectedOffer = null
            }
        )
    }
}

@Composable
private fun FlyerPageImage(
    flyer: Flyer,
    index: Int,
    offers: List<FlyerOffer>,
    onOfferClick: (FlyerOffer) -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val url = FlyerPageUrls.page(flyer.leafletId, index)
    var retryKey by remember(index) { mutableIntStateOf(0) }
    val painter = rememberAsyncImagePainter(
        model = remember(url, retryKey) {
            ImageRequest.Builder(context)
                // Fragment yalnızca önbellek anahtarını değiştirir; sunucuya gitmez.
                .data(if (retryKey == 0) url else "$url#retry=$retryKey")
                .crossfade(true)
                .build()
        }
    )

    var scale by remember(index) { mutableStateOf(1f) }
    var offset by remember(index) { mutableStateOf(Offset.Zero) }
    var boxSize by remember(index) { mutableStateOf(IntSize.Zero) }

    fun applyScale(target: Float) {
        scale = target.coerceIn(1f, 4f)
        if (scale <= 1.01f) {
            offset = Offset.Zero
            onZoomChanged(false)
        } else {
            onZoomChanged(true)
        }
    }

    val panModifier = if (scale > 1.01f) {
        Modifier.pointerInput(index) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                val nextScale = (scale * gestureZoom).coerceIn(1f, 4f)
                scale = nextScale
                if (nextScale <= 1.01f) {
                    offset = Offset.Zero
                    onZoomChanged(false)
                } else {
                    val maxX = boxSize.width * (nextScale - 1f) / 2f
                    val maxY = boxSize.height * (nextScale - 1f) / 2f
                    offset = Offset(
                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                        (offset.y + pan.y).coerceIn(-maxY, maxY)
                    )
                }
            }
        }
    } else {
        Modifier
    }

    // Görsel henüz ölçülmemişken intrinsicSize, boyutu okunamayan
    // Size.Unspecified olur; doğrudan width/height okumak uygulamayı çökertir.
    val intrinsic = painter.intrinsicSize
    val hasImage = intrinsic != Size.Unspecified &&
        intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
        intrinsic.width > 0f && intrinsic.height > 0f && boxSize.width > 0

    val displayWidth = if (hasImage) {
        intrinsic.width * min(boxSize.width / intrinsic.width, boxSize.height / intrinsic.height)
    } else {
        0f
    }
    val displayHeight = if (hasImage) {
        intrinsic.height * min(boxSize.width / intrinsic.width, boxSize.height / intrinsic.height)
    } else {
        0f
    }

    // Aşağıdaki pointerInput bloğu yeniden başlatılmadan güncel değerleri okusun.
    val latestOffers by rememberUpdatedState(offers)
    val latestDisplay by rememberUpdatedState(displayWidth to displayHeight)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
            .pointerInput(index) {
                detectTapGestures(
                    onDoubleTap = { applyScale(if (scale > 1.01f) 1f else 2.4f) },
                    // Ürün işaretleri kendi tıklama katmanını kullanamıyor: bu
                    // dedektör dokunuşu tüketiyor. Bu yüzden dokunuş, sayfanın
                    // o anki ölçeğine göre ürün alanlarına çevrilip eşleştirilir.
                    onTap = { position ->
                        offerAt(
                            position = position,
                            area = size,
                            display = latestDisplay,
                            scale = scale,
                            offset = offset,
                            offers = latestOffers
                        )?.let(onOfferClick)
                    }
                )
            }
            .pinchToZoom { factor -> applyScale(scale * factor) }
            .then(panModifier),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            val widthDp = with(density) { displayWidth.toDp() }
            val heightDp = with(density) { displayHeight.toDp() }

            Box(
                modifier = Modifier
                    .size(widthDp, heightDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                Image(
                    painter = painter,
                    contentDescription = stringResource(R.string.p_prospektus_sayfasi_x, index + 1),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                offers.forEach { offer ->
                    val spot = offer.hotspot ?: return@forEach
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(density) { (spot.fromX * displayWidth).toDp() },
                                y = with(density) { (spot.fromY * displayHeight).toDp() }
                            )
                            .size(
                                width = with(density) { (spot.width * displayWidth).toDp() },
                                height = with(density) { (spot.height * displayHeight).toDp() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(MarkerColor, CircleShape)
                                .border(1.dp, Color(0x55000000), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.s_metin),
                                color = Color(0xFF17352C),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        } else {
            // Görsel ölçülmeden önce de çizilmesi gerekir: Coil isteği ancak
            // painter ekrana çizildiğinde başlar. Bu dal olmadan sayfa sonsuza
            // kadar "yükleniyor" durumunda kalıyordu.
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.p_prospektus_sayfasi_x, index + 1),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        when (painter.state) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(color = Color.White)
            is AsyncImagePainter.State.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.s_sayfa_yuklenemedi), color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.s_internet_baglantini_kontrol_et),
                    color = ReaderMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { retryKey++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Text(stringResource(R.string.s_yeniden_dene), color = Color.White)
                }
            }

            else -> Unit
        }
    }
}

/**
 * Dokunuşu o anki yakınlaştırma/kaydırma durumuna göre sayfa koordinatına çevirip
 * ürün alanıyla eşleştirir. Küçük alanlarda ıskalamayı önlemek için işaret
 * merkezine %4 tolerans uygulanır.
 */
internal fun offerAt(
    position: Offset,
    area: IntSize,
    display: Pair<Float, Float>,
    scale: Float,
    offset: Offset,
    offers: List<FlyerOffer>
): FlyerOffer? {
    val (width, height) = display
    if (width <= 0f || height <= 0f || area.width == 0) return null

    val left = (area.width - width * scale) / 2f + offset.x
    val top = (area.height - height * scale) / 2f + offset.y
    val x = (position.x - left) / (width * scale)
    val y = (position.y - top) / (height * scale)
    if (x < 0f || x > 1f || y < 0f || y > 1f) return null

    val spots = offers.mapNotNull { offer -> offer.hotspot?.let { offer to it } }

    spots.filter { (_, spot) -> x in spot.fromX..spot.toX && y in spot.fromY..spot.toY }
        .minByOrNull { (_, spot) -> spot.width * spot.height }
        ?.let { return it.first }

    return spots
        .map { (offer, spot) -> offer to hypot(x - spot.centerX, y - spot.centerY) }
        .filter { it.second <= 0.04f }
        .minByOrNull { it.second }
        ?.first
}

/** İki parmak hareketini yakalar; tek parmak kaydırmayı sayfa geçişine bırakır. */
private fun Modifier.pinchToZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    onZoom(zoom)
                    event.changes.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
