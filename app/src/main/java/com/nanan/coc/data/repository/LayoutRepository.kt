package com.nanan.coc.data.repository

import com.nanan.coc.data.api.DocParser
import com.nanan.coc.data.api.TencentDocService
import com.nanan.coc.data.cache.FavoriteEntity
import com.nanan.coc.data.cache.LayoutDao
import com.nanan.coc.data.cache.LayoutEntity
import com.nanan.coc.data.cache.MetaEntity
import com.nanan.coc.data.model.LayoutItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LayoutRepository @Inject constructor(
    private val api: TencentDocService,
    private val dao: LayoutDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    companion object {
        const val CN_DOC = "DVGtkeUJucWhJbVhp"
        const val EN_DOC = "DVFpmZ21uZ0ROeU54"

        private const val META_LAST_UPDATE = "last_update"
        private const val META_CN_DOC = "cn_doc_id"
        private const val META_EN_DOC = "en_doc_id"
        private const val META_CN_HASH = "cn_doc_hash"
        private const val META_EN_HASH = "en_doc_hash"
    }

    suspend fun getCachedLayouts(): List<LayoutItem> {
        return dao.getAll().map { entity ->
            LayoutItem(
                id = entity.position,
                imageUrl = entity.imageUrl,
                link = entity.link,
                updatedAt = entity.updatedAt,
                thLevel = extractThLevel(entity.link),
                server = entity.server
            )
        }
    }

    // Extract TH from link: id=TH18%3AHV...
    private fun extractThLevel(link: String): Int {
        return Regex("TH(\\d+)").find(link)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    suspend fun getCnDocId(): String = dao.getMeta(META_CN_DOC) ?: CN_DOC
    suspend fun getEnDocId(): String = dao.getMeta(META_EN_DOC) ?: EN_DOC

    suspend fun setDocIds(cnDoc: String, enDoc: String) {
        dao.setMeta(MetaEntity(META_CN_DOC, cnDoc))
        dao.setMeta(MetaEntity(META_EN_DOC, enDoc))
    }

    suspend fun getLastUpdateTime(): Long {
        val ts = dao.getMeta(META_LAST_UPDATE)
        return ts?.toLongOrNull() ?: 0L
    }

    /**
     * 先比对文档内容 hash，有变化才更新缓存。
     * 返回 true 表示数据有更新，false 表示无变化。
     */
    suspend fun checkAndUpdate(): Result<Boolean> {
        return try {
            val cnDocId = getCnDocId()
            val enDocId = getEnDocId()

            // 拉取文档原始内容
            val cnResp = api.getDoc(cnDocId)
            val cnRaw = if (cnResp.isSuccessful) cnResp.body() else null
            val enRaw = if (enDocId.isNotEmpty()) {
                val enResp = api.getDoc(enDocId)
                if (enResp.isSuccessful) enResp.body() else null
            } else null

            // 计算 hash 并与缓存比对
            val cnHash = cnRaw?.let { DocParser.contentHash(it) } ?: ""
            val enHash = enRaw?.let { DocParser.contentHash(it) } ?: ""
            val cachedCnHash = dao.getMeta(META_CN_HASH) ?: ""
            val cachedEnHash = dao.getMeta(META_EN_HASH) ?: ""

            if (cnHash == cachedCnHash && enHash == cachedEnHash && cachedCnHash.isNotEmpty()) {
                // 内容无变化，跳过更新
                return Result.success(false)
            }

            // 内容有变化，解析并更新
            val cnItems = cnRaw?.let { raw ->
                val pairs = DocParser.extractLayoutPairs(raw)
                pairs.mapIndexed { index, (link, imageUrl) ->
                    LayoutEntity(imageUrl = imageUrl, link = link, position = index, server = "cn")
                }
            } ?: emptyList()

            val enItems = enRaw?.let { raw ->
                val pairs = DocParser.extractLayoutPairs(raw)
                pairs.mapIndexed { index, (link, imageUrl) ->
                    LayoutEntity(imageUrl = imageUrl, link = link, position = index, server = "en")
                }
            } ?: emptyList()

            val allItems = cnItems + enItems
            if (allItems.isEmpty()) {
                return Result.failure(Exception("未找到阵型链接"))
            }

            dao.replaceAll(allItems)
            dao.setMeta(MetaEntity(META_LAST_UPDATE, System.currentTimeMillis().toString()))
            if (cnHash.isNotEmpty()) dao.setMeta(MetaEntity(META_CN_HASH, cnHash))
            if (enHash.isNotEmpty()) dao.setMeta(MetaEntity(META_EN_HASH, enHash))

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 收藏
    suspend fun getFavoriteLinks(): Set<String> {
        return dao.getAllFavoriteLinks().toSet()
    }

    suspend fun toggleFavorite(link: String, imageUrl: String): Boolean {
        return if (dao.isFavorite(link)) {
            dao.removeFavorite(link)
            false
        } else {
            dao.addFavorite(FavoriteEntity(link = link, imageUrl = imageUrl))
            true
        }
    }

    suspend fun isFavorite(link: String): Boolean {
        return dao.isFavorite(link)
    }

    fun getPrefs(): android.content.SharedPreferences {
        return context.getSharedPreferences("coc_layout_prefs", android.content.Context.MODE_PRIVATE)
    }
}
