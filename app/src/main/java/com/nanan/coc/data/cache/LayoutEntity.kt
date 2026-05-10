package com.nanan.coc.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layouts")
data class LayoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val imageUrl: String,
    val link: String,
    val position: Int,
    val server: String = "cn",
    val updatedAt: Long = System.currentTimeMillis()
)
