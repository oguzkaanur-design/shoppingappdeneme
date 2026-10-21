package com.shoppingapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppingapp.data.model.FlyerListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FlyerListDao {

    @Query("SELECT * FROM flyer_list_items ORDER BY validToEpochMs ASC, addedAt DESC")
    fun observeAll(): Flow<List<FlyerListItem>>

    @Query("SELECT * FROM flyer_list_items")
    suspend fun getAll(): List<FlyerListItem>

    @Query("SELECT * FROM flyer_list_items WHERE offerId = :offerId LIMIT 1")
    suspend fun findByOfferId(offerId: Long): FlyerListItem?

    @Query("SELECT COUNT(*) FROM flyer_list_items")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FlyerListItem): Long

    @Update
    suspend fun update(item: FlyerListItem)

    @Delete
    suspend fun delete(item: FlyerListItem)

    @Query("DELETE FROM flyer_list_items")
    suspend fun clear()
}
