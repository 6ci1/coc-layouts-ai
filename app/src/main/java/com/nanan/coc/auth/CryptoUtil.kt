package com.nanan.coc.auth

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * llua.cn V2 API 加密工具集
 * 包含 RC4、自定义 Base64、MD5/SHA1/SHA256、Hex 转换
 */
object CryptoUtil {

    // ── RC4 ────────────────────────────────────────────
    fun rc4(input: ByteArray, key: String): ByteArray {
        val bkey = key.toByteArray(Charsets.UTF_8)
        val state = ByteArray(256) { it.toByte() }

        var idx2 = 0
        var idx1 = 0
        for (i in 0 until 256) {
            idx2 = ((bkey[idx1].toInt() and 0xff) + (state[i].toInt() and 0xff) + idx2) and 0xff
            val tmp = state[i]
            state[i] = state[idx2]
            state[idx2] = tmp
            idx1 = (idx1 + 1) % bkey.size
        }

        var x = 0
        var y = 0
        val result = ByteArray(input.size)
        for (i in input.indices) {
            x = (x + 1) and 0xff
            y = ((state[x].toInt() and 0xff) + y) and 0xff
            val tmp = state[x]
            state[x] = state[y]
            state[y] = tmp
            val xorIdx = ((state[x].toInt() and 0xff) + (state[y].toInt() and 0xff)) and 0xff
            result[i] = (input[i].toInt() xor (state[xorIdx].toInt() and 0xff)).toByte()
        }
        return result
    }

    fun rc4Encrypt(data: String, key: String): ByteArray =
        rc4(data.toByteArray(Charsets.UTF_8), key)

    fun rc4DecryptToString(data: ByteArray, key: String): String =
        String(rc4(data, key), Charsets.UTF_8)

    fun rc4DecryptToBytes(data: ByteArray, key: String): ByteArray =
        rc4(data, key)

    // ── 标准 Base64 (RFC charset) ─────────────────────
    private val STD_CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

    fun standardBase64Decode(input: String): ByteArray {
        val indexMap = IntArray(128) { -1 }
        for (i in STD_CHARSET.indices) {
            indexMap[STD_CHARSET[i].code] = i
        }
        indexMap['='.code] = 0

        val len = input.length
        var padding = 0
        if (len > 0 && input[len - 1] == '=') padding++
        if (len > 1 && input[len - 2] == '=') padding++

        val byteLen = (len * 6) / 8 - padding
        val decoded = ByteArray(byteLen)
        var dataIdx = 0
        var buffer = 0
        var bitsLeft = 0

        for (i in 0 until len) {
            val ch = input[i]
            if (ch == '=') break
            val value = indexMap[ch.code]
            buffer = (buffer shl 6) or value
            bitsLeft += 6
            if (bitsLeft >= 8) {
                decoded[dataIdx++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                bitsLeft -= 8
            }
        }
        return decoded
    }

    // ── 自定义 Base64 (用 key 作为字符表) ────────────────
    fun customBase64Encode(input: String, charsetKey: String): String {
        val charset = charsetKey.toCharArray()
        val data = input.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder()

        var i = 0
        while (i < data.size) {
            var block = 0
            val remaining = minOf(3, data.size - i)
            for (j in 0 until remaining) {
                block = block or ((data[i + j].toInt() and 0xFF) shl (16 - 8 * j))
            }
            for (j in 0 until 4) {
                if (j < remaining + 1) {
                    sb.append(charset[(block shr (18 - 6 * j)) and 0x3F])
                } else {
                    sb.append('=')
                }
            }
            i += 3
        }
        return sb.toString()
    }

    fun customBase64Decode(input: String, charsetKey: String): ByteArray {
        val charset = charsetKey.toCharArray()
        val indexMap = IntArray(128) { -1 }
        for (i in charset.indices) {
            indexMap[charset[i].code] = i
        }
        indexMap['='.code] = 0

        val len = input.length
        var padding = 0
        if (len > 0 && input[len - 1] == '=') padding++
        if (len > 1 && input[len - 2] == '=') padding++

        val byteLen = (len * 6) / 8 - padding
        val decoded = ByteArray(byteLen)
        var dataIdx = 0
        var buffer = 0
        var bitsLeft = 0

        for (i in 0 until len) {
            val ch = input[i]
            if (ch == '=') break
            val value = indexMap[ch.code]
            buffer = (buffer shl 6) or value
            bitsLeft += 6
            if (bitsLeft >= 8) {
                decoded[dataIdx++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                bitsLeft -= 8
            }
        }
        return decoded
    }

    fun customBase64EncodeFromBytes(input: ByteArray, charsetKey: String): String =
        customBase64Encode(String(input, Charsets.UTF_8), charsetKey)

    // ── Hex ────────────────────────────────────────────
    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val hex = (b.toInt() and 0xFF).toString(16)
            if (hex.length < 2) sb.append('0')
            sb.append(hex)
        }
        return sb.toString()
    }

    fun stringToHex(str: String): String =
        bytesToHex(str.toByteArray(Charsets.UTF_8))

    fun hexToBytes(hex: String): ByteArray {
        var h = hex
        if (h.length % 2 != 0) h = "0$h"
        val result = ByteArray(h.length / 2)
        var j = 0
        var i = 0
        while (i < h.length) {
            result[j] = h.substring(i, i + 2).toInt(16).toByte()
            j++
            i += 2
        }
        return result
    }

    // ── 哈希 ───────────────────────────────────────────
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(md.digest())
    }

    fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(md.digest())
    }

    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(md.digest())
    }
}
