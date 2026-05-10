package com.nanan.coc.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
