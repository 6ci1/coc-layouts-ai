package com.nanan.coc.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val link: String,
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
