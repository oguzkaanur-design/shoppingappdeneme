package com.shoppingapp.feature.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.shoppingapp.BuildConfig
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shoppingapp.data.model.Store
import com.shoppingapp.data.repository.PurchaseReceipt
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ReceiptUiState(
    val analyzing: Boolean = false,
    val lines: List<ReceiptLine> = emptyList(),
    val total: Double? = null,
    val storeId: Long = 1L,
    val error: String? = null,
    val saved: PurchaseReceipt? = null
) {
    val sum: Double get() = lines.sumOf { it.lineTotal }
}

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val repository: ShoppingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptUiState())
    val state: StateFlow<ReceiptUiState> = _state.asStateFlow()

    val stores: StateFlow<List<Store>> = repository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Fiş fotoğrafını okur ve satırları çıkarır. */
    fun analyze(uri: Uri) {
        if (_state.value.analyzing) return
        _state.update { it.copy(analyzing = true, error = null) }

        viewModelScope.launch {
            val parsed = runCatching {
                val bitmap = withContext(Dispatchers.IO) { loadBitmap(uri) }
                    ?: error("Fotoğraf açılamadı")
                val text = recognize(bitmap)
                bitmap.recycle()
                if (BuildConfig.DEBUG) {
                    // Yalnızca hata ayıklamada: OCR metnini ayrıştırma sorunlarını görmek için sakla.
                    runCatching {
                        java.io.File(context.cacheDir, "receipts/last_ocr.txt")
                            .apply { parentFile?.mkdirs() }
                            .writeText(text)
                    }
                }
                ReceiptParser.parse(text)
            }

            parsed.onSuccess { receipt ->
                _state.update {
                    it.copy(
                        analyzing = false,
                        lines = receipt.lines,
                        total = receipt.total,
                        error = if (receipt.lines.isEmpty()) {
                            "Fişte ürün bulunamadı. Daha net bir fotoğraf dene."
                        } else {
                            null
                        }
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        analyzing = false,
                        error = "Fiş okunamadı. Fotoğrafı tekrar çekmeyi dene."
                    )
                }
            }
        }
    }

    fun selectStore(storeId: Long) = _state.update { it.copy(storeId = storeId) }

    fun removeLine(index: Int) {
        _state.update { state ->
            state.copy(lines = state.lines.filterIndexed { i, _ -> i != index })
        }
    }

    fun updatePrice(index: Int, price: Double) {
        _state.update { state ->
            state.copy(
                lines = state.lines.mapIndexed { i, line ->
                    if (i == index) line.copy(unitPrice = price.coerceAtLeast(0.0)) else line
                }
            )
        }
    }

    fun reset() = _state.update { ReceiptUiState(storeId = it.storeId) }

    fun clearError() = _state.update { it.copy(error = null) }

    fun dismissSaved() = _state.update { it.copy(saved = null) }

    /** Fiş satırlarını alışveriş geçmişine yazar ve listeyi boşaltır. */
    fun save() {
        val current = _state.value
        if (current.lines.isEmpty() || current.analyzing) return
        viewModelScope.launch {
            val receipt = repository.completePurchaseFromReceipt(
                lines = current.lines,
                storeId = current.storeId
            )
            _state.update { it.copy(saved = receipt, lines = emptyList(), total = null) }
        }
    }

    private suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                // Tutar sütunu ayrı blokta geldiği için satırlar konuma göre yeniden kurulur.
                val lines = result.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    OcrLine(
                        text = line.text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }
                cont.resume(ReceiptTextLayout.rows(lines).joinToString("\n"))
            }
            .addOnFailureListener { error -> cont.resumeWithException(error) }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}
