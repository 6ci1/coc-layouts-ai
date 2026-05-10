package com.nanan.coc.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LayoutDao {

    @Query("SELECT * FROM layouts ORDER BY position ASC")
    suspend fun getAll(): List<LayoutEntity>

    @Transaction
    suspend fun replaceAll(items: List<LayoutEntity>) {
        deleteAll()
        insertAll(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LayoutEntity>)

    @Query("DELETE FROM layouts")
    suspend fun deleteAll()

    @Query("SELECT value FROM meta WHERE key = :key")
    suspend fun getMeta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMeta(meta: MetaEntity)

    // 收藏
    @Query("SELECT link FROM favorites")
    suspend fun getAllFavoriteLinks(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE link = :link")
    suspend fun removeFavorite(link: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE link = :link)")
    suspend fun isFavorite(link: String): Boolean
}
