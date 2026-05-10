package com.nanan.coc.data.api

object DocParser {

    /**
     * 从单个文档中按顺序提取阵型链接和图片，配对返回
     * 每个链接下面紧跟的图片就是对应的阵型图
     */
    fun extractLayoutPairs(raw: String): List<Pair<String, String>> {
        val links = extractLayoutLinks(raw)
        val images = extractImageUrls(raw)
        return links.mapIndexed { index, link ->
            link to if (index < images.size) images[index] else ""
        }
    }

    fun extractLayoutLinks(raw: String): List<String> {
        val hyperRegex = Regex("""HYPERLINK%20(https%3A//link\.clashofclans\.com/(?:cn|en)%3Faction%3DOpenLayout%26id%3D[^%]+(?:%[0-9A-Fa-f]{2}[^%]*)*?)(?:%20|%5Cu0014)""")
        val links = hyperRegex.findAll(raw)
            .map { decodeUrlEncoding(it.groupValues[1]) }
            .filter { it.startsWith("https://link.clashofclans.com/") }
            .distinct()
            .toList()

        if (links.isNotEmpty()) return links

        val standaloneRegex = Regex("""(?:^|[^%])(https%3A//link\.clashofclans\.com/(?:cn|en)%3Faction%3DOpenLayout%26id%3D[A-Za-z0-9%_-]+)%20""")
        return standaloneRegex.findAll(raw)
            .map { decodeUrlEncoding(it.groupValues[1]) }
            .filter { it.startsWith("https://link.clashofclans.com/") }
            .distinct()
            .toList()
    }

    fun extractImageUrls(raw: String): List<String> {
        val encodedRegex = Regex("""https%3A//docimg\d+\.docs\.qq\.com/image/[A-Za-z0-9_=-]+\.(?:jpeg|png)""")
        val encoded = encodedRegex.findAll(raw)
            .map { decodeUrlEncoding(it.value) }
            .distinct()
            .toList()

        if (encoded.isNotEmpty()) return encoded

        val plainRegex = Regex("""https://docimg\d+\.docs\.qq\.com/image/[A-Za-z0-9_=-]+\.(?:jpeg|png)""")
        return plainRegex.findAll(raw)
            .map { it.value }
            .distinct()
            .toList()
    }

    fun contentHash(raw: String): String {
        var hash = 0L
        for (ch in raw) {
            hash = (hash * 31 + ch.code) and 0xFFFFFFFFL
        }
        return hash.toString(16)
    }

    private fun decodeUrlEncoding(text: String): String {
        var result = text
        result = result.replace("%253A", "%3A")
            .replace("%252F", "%2F")
            .replace("%253F", "%3F")
            .replace("%253D", "%3D")
            .replace("%2526", "%26")
        result = result.replace("%3A", ":")
            .replace("%2F", "/")
            .replace("%3F", "?")
            .replace("%3D", "=")
            .replace("%26", "&")
            .replace("%25", "%")
        return result
    }
}
