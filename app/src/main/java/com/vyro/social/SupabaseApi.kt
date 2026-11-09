package com.vyro.social

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object SupabaseConfig {
    const val URL = "https://xerbvvrldxaadynuphyz.supabase.co"
    const val ANON_KEY = "sb_publishable_vdHKa1-Z8ALG5Nj0V2T3AA_4fNHTZs-"
}

internal data class RemoteSession(val accessToken: String, val userId: String)
internal data class RemotePost(val id: String, val author: String, val handle: String, val body: String, val likes: Int, val liked: Boolean)
internal data class LiveKitToken(val token: String, val url: String)
internal data class RemoteProfile(val username: String, val displayName: String, val bio: String, val followers: Int, val following: Int, val posts: Int)

internal object SupabaseApi {
    suspend fun signIn(email: String, password: String): RemoteSession = auth("/auth/v1/token?grant_type=password", email, password)
    suspend fun signUp(email: String, password: String, username: String, displayName: String): RemoteSession = auth("/auth/v1/signup", email, password, displayName, username)

    private suspend fun auth(path: String, email: String, password: String, displayName: String = "", username: String = "") = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).put("password", password)
        if (displayName.isNotBlank() || username.isNotBlank()) body.put("data", JSONObject().put("display_name", displayName).put("username", username))
        val json = JSONObject(request("POST", path, body.toString()))
        val token = json.optString("access_token")
        val user = json.optJSONObject("user") ?: throw IllegalStateException("لم يتم إنشاء جلسة. تحقق من تأكيد البريد في Supabase.")
        if (token.isBlank()) throw IllegalStateException("الحساب يحتاج تأكيد البريد الإلكتروني قبل تسجيل الدخول.")
        RemoteSession(token, user.optString("id"))
    }

    suspend fun profile(token: String, userId: String): RemoteProfile = withContext(Dispatchers.IO) {
        val item = JSONArray(request("GET", "/rest/v1/profiles?id=eq.$userId&select=username,display_name,bio", null, token)).getJSONObject(0)
        val followers = JSONArray(request("GET", "/rest/v1/follows?following_id=eq.$userId&select=follower_id", null, token)).length()
        val following = JSONArray(request("GET", "/rest/v1/follows?follower_id=eq.$userId&select=following_id", null, token)).length()
        val posts = JSONArray(request("GET", "/rest/v1/posts?author_id=eq.$userId&select=id", null, token)).length()
        RemoteProfile(item.optString("username"), item.optString("display_name"), item.optString("bio"), followers, following, posts)
    }

    suspend fun follow(token: String, userId: String, targetId: String, following: Boolean) = withContext(Dispatchers.IO) {
        if (following) request("POST", "/rest/v1/follows", JSONObject().put("follower_id", userId).put("following_id", targetId).toString(), token, "return=minimal")
        else request("DELETE", "/rest/v1/follows?follower_id=eq.$userId&following_id=eq.$targetId", null, token)
    }

    suspend fun posts(token: String, userId: String): List<RemotePost> = withContext(Dispatchers.IO) {
        val json = JSONArray(request("GET", "/rest/v1/posts?select=id,body,author_id,profiles(username,display_name)&order=created_at.desc&limit=50", null, token))
        (0 until json.length()).map { i ->
            val item = json.getJSONObject(i); val profile = item.optJSONObject("profiles")
            RemotePost(item.getString("id"), profile?.optString("display_name").orEmpty().ifBlank { "VYRO User" }, "@${profile?.optString("username").orEmpty()}", item.getString("body"), 0, false)
        }
    }

    suspend fun createPost(token: String, userId: String, body: String): RemotePost = withContext(Dispatchers.IO) {
        val payload = JSONArray().put(JSONObject().put("author_id", userId).put("body", body))
        val json = JSONArray(request("POST", "/rest/v1/posts?select=id,body,author_id", payload.toString(), token, prefer = "return=representation"))
        val item = json.getJSONObject(0)
        RemotePost(item.getString("id"), "أنت", "@me", item.getString("body"), 0, false)
    }

    suspend fun like(token: String, postId: String, userId: String, liked: Boolean) = withContext(Dispatchers.IO) {
        if (liked) request("POST", "/rest/v1/post_likes", JSONObject().put("post_id", postId).put("user_id", userId).toString(), token, "return=minimal")
        else request("DELETE", "/rest/v1/post_likes?post_id=eq.$postId&user_id=eq.$userId", null, token)
    }

    suspend fun comment(token: String, postId: String, userId: String, body: String) = withContext(Dispatchers.IO) {
        request("POST", "/rest/v1/comments", JSONObject().put("post_id", postId).put("author_id", userId).put("body", body).toString(), token, "return=minimal")
    }

    suspend fun liveKitToken(token: String, room: String): LiveKitToken = withContext(Dispatchers.IO) {
        val json = JSONObject(request("POST", "/functions/v1/livekit-token", JSONObject().put("room", room).toString(), token))
        LiveKitToken(json.getString("token"), json.getString("url"))
    }

    private fun request(method: String, path: String, body: String?, token: String? = null, prefer: String? = null): String {
        val connection = (URL(SupabaseConfig.URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 20000; doInput = true
            setRequestProperty("apikey", SupabaseConfig.ANON_KEY); setRequestProperty("Content-Type", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (prefer != null) setRequestProperty("Prefer", prefer)
        }
        if (body != null) { connection.doOutput = true; connection.outputStream.use { it.write(body.toByteArray()) } }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException(try { JSONObject(text).optString("message").ifBlank { text } } catch (_: Exception) { text.ifBlank { "خطأ في الاتصال ($code)" } })
        return text
    }
}
