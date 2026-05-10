package com.nanan.coc.data.model

data class LayoutItem(
    val id: Int,
    val imageUrl: String,
    val link: String,
    val updatedAt: Long = 0L,
    val thLevel: Int = 0,
    val server: String = ""  // "cn" or "en"
)
