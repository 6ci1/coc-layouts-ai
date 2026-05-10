package com.nanan.coc.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LayoutEntity::class, MetaEntity::class, FavoriteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun layoutDao(): LayoutDao
}
