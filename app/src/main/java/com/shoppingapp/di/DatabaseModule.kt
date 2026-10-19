package com.shoppingapp.di

import android.content.Context
import androidx.room.Room
import com.shoppingapp.data.dao.ShoppingDao
import com.shoppingapp.data.local.AppMigrations
import com.shoppingapp.data.local.ShoppingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    const val DATABASE_NAME = "shopping_db"

    /**
     * Veritabanının tek yapılandırma noktası; testler de aynı fonksiyonu çağırır
     * (geçiş testleri üretim yapılandırmasını doğrulasın diye).
     *
     * `fallbackToDestructiveMigration()` bilinçli olarak YOK: geçişi yazılmamış bir şema
     * değişimi kullanıcı verisini silmek yerine açıkça hata vermeli. Yeni geçiş için
     * [AppMigrations]'a migration ekle.
     */
    fun buildDatabase(
        context: Context,
        name: String = DATABASE_NAME
    ): ShoppingDatabase {
        return Room.databaseBuilder(context, ShoppingDatabase::class.java, name)
            .addMigrations(*AppMigrations.ALL)
            .build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShoppingDatabase {
        return buildDatabase(context)
    }

    @Provides
    @Singleton
    fun provideShoppingDao(database: ShoppingDatabase): ShoppingDao {
        return database.shoppingDao()
    }
}
