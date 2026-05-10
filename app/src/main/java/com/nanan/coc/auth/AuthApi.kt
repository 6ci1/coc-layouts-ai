package com.nanan.coc.auth

import com.nanan.coc.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * llua.cn V2 协议网络层
 * 密钥通过 local.properties → BuildConfig 注入，不随源码提交
 */
object AuthApi {

    private const val BASE_URL = "https://wy.llua.cn/v2/"
    private val API_TOKEN = BuildConfig.AUTH_API_TOKEN
    private val APP_KEY = BuildConfig.AUTH_APP_KEY

    private const val ID_NOTICE = "577fCHEsf56"
    private const val ID_UPDATE = "2Qi6A9GzG66"
    private const val ID_LOGIN = "iACMYtiPXTA"
    private const val ID_UNBIND = "Ut9ZL9uusTl"
    private const val ID_HEARTBEAT = "iACMYtiPXTA" // heartbeat uses same ID as login

    // RC4 keys (from BuildConfig)
    private val RK_F52 = BuildConfig.AUTH_RK_F52
    private val RK_QOP = BuildConfig.AUTH_RK_QOP
    private val RK_BUK = BuildConfig.AUTH_RK_BUK
    private val RK_B43 = BuildConfig.AUTH_RK_B43

    // Response keys (from BuildConfig)
    private val RK_RSP_INNER = BuildConfig.AUTH_RK_RSP_INNER
    private val RK_RSP_OUTER = BuildConfig.AUTH_RK_RSP_OUTER
    private val RK_RSP_SIMPLE = BuildConfig.AUTH_RK_RSP_SIMPLE

    // Success codes
    private const val CODE_LOGIN_OK = 38683
    private const val CODE_UNBIND_OK = 36293
    private const val CODE_NOTICE_OK = 41776
    private const val CODE_UPDATE_OK = 17366

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── 请求加密管道 ───────────────────────────────────

    private fun encryptPostData(params: String): String {
        // 1. RC4 encrypt with f52bb → byte[]
        val s1 = CryptoUtil.rc4Encrypt(params, RK_F52)
        // 2. hex(byte[]) → String
        val s2 = CryptoUtil.bytesToHex(s1)
        // 3. custom base64(key=QOPx)
        val s3 = CryptoUtil.customBase64Encode(s2, RK_QOP)
        // 4. custom base64(key=Bukh)
        val s4 = CryptoUtil.customBase64Encode(s3, RK_BUK)
        // 5. RC4 encrypt with b436 → byte[]
        val s5 = CryptoUtil.rc4Encrypt(s4, RK_B43)
        // 6. hex(byte[]) → String
        val s6 = CryptoUtil.bytesToHex(s5)
        // 7. hex(String bytes) → String
        return CryptoUtil.stringToHex(s6)
    }

    private fun encryptNoticeUpdate(id: String): String {
        val s1 = CryptoUtil.rc4Encrypt("id=$id", RK_F52)
        val s2 = CryptoUtil.bytesToHex(s1)
        val s3 = CryptoUtil.customBase64Encode(s2, RK_QOP)
        val s4 = CryptoUtil.customBase64Encode(s3, RK_BUK)
        val s5 = CryptoUtil.rc4Encrypt(s4, RK_B43)
        val s6 = CryptoUtil.bytesToHex(s5)
        return CryptoUtil.stringToHex(s6)
    }

    // ── 响应解密 ────────────────────────────────────────

    private fun decryptLoginResponse(body: String): String {
        val d1 = CryptoUtil.hexToBytes(body)
        log("decr1 hexToBytes len=${d1.size}")
        val d2 = CryptoUtil.rc4DecryptToBytes(d1, RK_RSP_INNER)
        val d2str = String(d2, Charsets.UTF_8)
        log("decr2 rc4(rspInner) len=${d2.size} str(100)=${d2str.take(100)}")
        val d3 = CryptoUtil.standardBase64Decode(d2str)
        val d3str = String(d3, Charsets.UTF_8)
        log("decr3 stdB64 len=${d3.size} str(100)=${d3str.take(100)}")
        val d4 = CryptoUtil.standardBase64Decode(d3str)
        val d4str = String(d4, Charsets.UTF_8)
        log("decr4 stdB64 len=${d4.size} str(100)=${d4str.take(100)}")
        val d5 = CryptoUtil.hexToBytes(d4str)
        log("decr5 hexToBytes len=${d5.size}")
        val result = CryptoUtil.rc4DecryptToString(d5, RK_RSP_OUTER)
        log("decr6 final=${result.take(500)}")
        return result
    }

    private fun decryptSimpleResponse(body: String): String {
        val d1 = CryptoUtil.hexToBytes(body)
        val result = CryptoUtil.rc4DecryptToString(d1, RK_RSP_SIMPLE)
        log("decrSimple len=${d1.size} result=${result.take(200)}")
        return result
    }

    // ── HTTP POST ───────────────────────────────────────

    private val debugLog = mutableListOf<String>()

    private fun log(msg: String) {
        debugLog.add(msg)
        android.util.Log.d("AuthApi", msg)
    }

    private suspend fun post(encryptedBody: String): String = withContext(Dispatchers.IO) {
        val url = "${BASE_URL}${API_TOKEN}"
        val requestBody = encryptedBody.toRequestBody("text/plain".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "text/plain")
            .build()
        client.newCall(request).execute().use { response ->
            val rawBody = response.body?.string() ?: throw Exception("Empty response")
            log("HTTP ${response.code}, bodyLen=${rawBody.length}, body(200)=${rawBody.take(200)}")
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${rawBody.take(100)}")
            }
            rawBody
        }
    }

    // ── 公共接口 ────────────────────────────────────────

    data class LoginResult(
        val success: Boolean,
        val token: String? = null,
        val expiry: Long? = null,       // 到期时间戳(秒)
        val kmtype: String? = null,     // 卡密类型
        val note: String? = null,       // 备注
        val remainCount: String? = null,// 剩余次数(次数卡)
        val message: String = ""
    )

    data class UnbindResult(
        val success: Boolean,
        val remainingCount: String? = null,
        val message: String = ""
    )

    data class HeartbeatResult(
        val valid: Boolean,
        val expiry: Long? = null,
        val message: String = ""
    )

    data class UpdateInfo(
        val version: String,
        val updateShow: String,
        val updateUrl: String,
        val mustUpdate: Boolean
    )

    suspend fun login(kami: String, deviceId: String): LoginResult {
        debugLog.clear()
        val timestamp = System.currentTimeMillis() / 1000
        val value = (10000..999999).random()
        val sign = CryptoUtil.md5("kami=$kami&markcode=$deviceId&t=$timestamp&$APP_KEY")
        val params = "id=$ID_LOGIN&kami=$kami&markcode=$deviceId&t=$timestamp&sign=$sign&value=$value"
        val encrypted = encryptPostData(params)
        log("encryptPostData len=${encrypted.length}")

        return try {
            val body = post(encrypted)
            val decrypted = decryptLoginResponse(body)
            val json = JSONObject(decrypted)

            // 服务端返回的 key 也是混淆的，遍历获取
            val keys = json.keys()
            if (!keys.hasNext()) throw Exception("Empty JSON")
            val codeKey = keys.next()
            val code = json.getInt(codeKey)
            if (code != CODE_LOGIN_OK) {
                val msg = if (keys.hasNext()) json.optString(keys.next(), "登录失败") else "登录失败"
                LoginResult(false, message = msg)
            } else {
                if (!keys.hasNext()) throw Exception("Missing data")
                val dataKey = keys.next()
                val msgObj = json.getJSONObject(dataKey)
                val token = msgObj.optString("token", "")
                val endtime = msgObj.optLong("endtime", 0)
                val kmtype = msgObj.optString("kmtype", "code")
                val note = msgObj.optString("note", "")
                val remainCount = msgObj.optString("num", null)
                LoginResult(true, token, endtime, kmtype, note, remainCount, "登录成功")
            }
        } catch (e: Exception) {
            log("EXCEPTION: ${e.message}")
            val debugInfo = debugLog.toList()
            debugLog.clear()
            LoginResult(false, message = debugInfo.joinToString("\n"))
        }
    }

    suspend fun unbind(kami: String, deviceId: String): UnbindResult {
        val timestamp = System.currentTimeMillis() / 1000
        val value = (10000..999999).random()
        val sign = CryptoUtil.md5("kami=$kami&markcode=$deviceId&t=$timestamp&$APP_KEY")
        val params = "id=$ID_UNBIND&kami=$kami&markcode=$deviceId&t=$timestamp&sign=$sign&value=$value"
        val encrypted = encryptPostData(params)

        return try {
            val body = post(encrypted)
            val decrypted = decryptSimpleResponse(body)
            val json = JSONObject(decrypted)

            val key = json.keys().next()
            if (json.getInt(key) == CODE_UNBIND_OK) {
                val msg = json.opt(key)
                val num = if (msg is JSONObject) msg.optString("num") else null
                UnbindResult(true, num)
            } else {
                UnbindResult(false, message = json.optString(key, "解绑失败"))
            }
        } catch (e: Exception) {
            UnbindResult(false, message = "网络错误: ${e.message}")
        }
    }

    suspend fun heartbeat(kami: String, deviceId: String, token: String): HeartbeatResult {
        val timestamp = System.currentTimeMillis() / 1000
        val value = (10000..999999).random()
        val sign = CryptoUtil.md5("kami=$kami&markcode=$deviceId&t=$timestamp&kamitoken=$token&$APP_KEY")
        val params = "id=$ID_HEARTBEAT&kami=$kami&markcode=$deviceId&t=$timestamp&sign=$sign&kamitoken=$token&value=$value"
        val encrypted = encryptPostData(params)

        return try {
            val body = post(encrypted)
            val decrypted = decryptLoginResponse(body)
            val json = JSONObject(decrypted)

            val keys = json.keys()
            if (!keys.hasNext()) return HeartbeatResult(false, message = "Empty response")
            val code = json.getInt(keys.next())
            if (code == CODE_LOGIN_OK) {
                if (keys.hasNext()) {
                    val msgObj = json.optJSONObject(keys.next())
                    val endtime = msgObj?.optLong("endtime", 0) ?: 0
                    HeartbeatResult(true, endtime)
                } else {
                    HeartbeatResult(true)
                }
            } else {
                val msg = if (keys.hasNext()) json.optString(keys.next(), "验证失败") else "验证失败"
                HeartbeatResult(false, message = msg)
            }
        } catch (e: Exception) {
            HeartbeatResult(false, message = "网络错误: ${e.message}")
        }
    }

    suspend fun getNotice(): String? {
        return try {
            val encrypted = encryptNoticeUpdate(ID_NOTICE)
            val body = post(encrypted)
            val decrypted = decryptSimpleResponse(body)
            val json = JSONObject(decrypted)
            val keys = json.keys()
            if (!keys.hasNext()) return null
            val codeKey = keys.next()
            if (json.getInt(codeKey) == CODE_NOTICE_OK) {
                if (keys.hasNext()) {
                    json.optJSONObject(keys.next())?.optString("app_gg")
                } else null
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun checkUpdate(currentVersion: String): UpdateInfo? {
        return try {
            val encrypted = encryptNoticeUpdate(ID_UPDATE)
            val body = post(encrypted)
            val decrypted = decryptSimpleResponse(body)
            val json = JSONObject(decrypted)
            val keys = json.keys()
            if (!keys.hasNext()) return null
            val codeKey = keys.next()
            if (json.getInt(codeKey) == CODE_UPDATE_OK) {
                if (!keys.hasNext()) return null
                val msg = json.getJSONObject(keys.next())
                val version = msg.getString("version")
                if (version != currentVersion) {
                    UpdateInfo(
                        version = version,
                        updateShow = msg.optString("updateshow", ""),
                        updateUrl = msg.optString("updateurl", ""),
                        mustUpdate = msg.optString("updatemust", "n") == "y"
                    )
                } else null
            } else null
        } catch (_: Exception) { null }
    }
}
