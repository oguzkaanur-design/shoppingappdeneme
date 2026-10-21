package com.shoppingapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String,
    val categoryId: Long,
    val imageUrl: String? = null,
    val unit: String,
    val defaultStoreId: Long? = null,
    val pfandAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val priceEuro: Double = 0.0
)

@Entity(tableName = "stores")
data class Store(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val aisleOrderJson: String
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameDe: String,
    val nameTr: String,
    val iconName: String,
    val colorHex: String
)

@Entity(tableName = "list_items")
data class ListItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val storeId: Long,
    val quantity: Double,
    val isPurchased: Boolean = false,
    val addedAt: Long
)

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val currentQuantity: Double,
    val minThreshold: Double,
    val expiryDate: Long?
)

@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeId: Long,
    val totalAmount: Double,
    val pfandTotal: Double,
    val purchaseDate: Long
)

@Entity(tableName = "pfand_entries")
data class PfandEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val unitAmount: Double,
    val quantity: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isReturned: Boolean = false
)

@Entity(tableName = "purchase_history_items")
data class PurchaseHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val historyId: Long,
    val productId: Long,
    val priceEuro: Double,
    val quantity: Double
)

/**
 * Prospektüsten tıklanarak eklenen geçici fırsat ürünü. Normal alışveriş listesinden
 * ayrı tutulur çünkü fiyatı ve son geçerlilik tarihi broşüre bağlıdır.
 */
@Entity(tableName = "flyer_list_items")
data class FlyerListItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val offerId: Long,
    val storeName: String,
    val storeColor: Long,
    val productName: String,
    val description: String = "",
    val unitPrice: Double,
    val quantity: Int = 1,
    val imagePath: String? = null,
    val validFromEpochMs: Long,
    val validToEpochMs: Long,
    val addedAt: Long = System.currentTimeMillis()
)
