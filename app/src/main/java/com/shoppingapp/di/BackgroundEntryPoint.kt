package com.shoppingapp.di

import com.shoppingapp.data.backup.BackupManager
import com.shoppingapp.data.dao.FlyerListDao
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.repository.ShoppingRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * WorkManager işi ve ana ekran widget'ı Hilt tarafından yönetilmeyen sınıflar
 * olduğu için bağımlılıklara bu kapıdan erişir.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackgroundEntryPoint {
    fun shoppingDao(): ShoppingDao
    fun flyerListDao(): FlyerListDao
    fun backupManager(): BackupManager
    fun shoppingRepository(): ShoppingRepository
}
