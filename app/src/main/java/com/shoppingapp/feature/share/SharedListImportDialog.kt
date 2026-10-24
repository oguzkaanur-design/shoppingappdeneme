package com.shoppingapp.feature.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareImportViewModel @Inject constructor(
    private val repository: ShoppingRepository
) : ViewModel() {

    /** Paylaşılan satırları listeye ekler; eklenen ürün sayısını döndürür. */
    fun import(lines: List<ImportedLine>, onDone: (Int) -> Unit) {
        if (lines.isEmpty()) {
            onDone(0)
            return
        }
        viewModelScope.launch {
            val added = runCatching { repository.importLines(lines) }.getOrDefault(0)
            onDone(added)
        }
    }
}

/**
 * Paylaşım menüsünden gelen metni önizleyip tek dokunuşla listeye ekler.
 * Kullanıcı eklemeden önce ne okunduğunu görür; yanlış ayrıştırma sessizce
 * listeye sızmaz.
 */
@Composable
fun SharedListImportDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: (List<ImportedLine>) -> Unit
) {
    val lines = remember(text) { ListImport.parse(text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Paylaşılan liste", fontWeight = FontWeight.Bold)
                Text(
                    if (lines.isEmpty()) {
                        "Metinden ürün çıkarılamadı."
                    } else {
                        "${lines.size} ürün bulundu"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(lines, key = { it.name.lowercase() }) { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(line.name, modifier = Modifier.weight(1f))
                        Text(
                            formatQuantity(line),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(lines) },
                enabled = lines.isNotEmpty()
            ) {
                Text("Listeye ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

private fun formatQuantity(line: ImportedLine): String {
    val quantity = if (line.quantity % 1.0 == 0.0) {
        line.quantity.toInt().toString()
    } else {
        String.format(java.util.Locale.GERMANY, "%.1f", line.quantity)
    }
    return if (line.unit == "Adet") "$quantity ×" else "$quantity ${line.unit}"
}
