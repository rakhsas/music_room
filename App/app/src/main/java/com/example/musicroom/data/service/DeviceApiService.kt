package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.network.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Music Control Delegation (V.2.2 of the subject): register this device, delegate its
 * play/pause/skip control to a friend by email, and relay commands to/from the backend.
 */

data class DelegateInfo(val id: Int, val name: String, val email: String)

data class MyDevice(
    val deviceId: String,
    val name: String,
    val platform: String,
    val delegates: List<DelegateInfo>
)

data class DelegatedDevice(
    val deviceId: String,
    val name: String,
    val ownerId: Int,
    val ownerName: String
)

data class PendingCommand(
    val command: String,
    val issuedById: Int,
    val issuedByName: String
)

@Singleton
class DeviceApiService @Inject constructor(
    private val tokenManager: TokenManager
) {
    suspend fun registerDevice(deviceId: String, name: String, platform: String, appVersion: String): Result<Unit> {
        return request("POST", "/api/devices/register/", JSONObject().apply {
            put("deviceId", deviceId)
            put("name", name)
            put("platform", platform)
            put("appVersion", appVersion)
        }).map { }
    }

    // Backend returns a bare JSON array here (not wrapped in {"devices": [...]}).
    suspend fun getMyDevices(): Result<List<MyDevice>> {
        return requestArray("GET", "/api/devices/my-devices/").map { devices ->
            (0 until devices.length()).map { i ->
                val d = devices.getJSONObject(i)
                val delegatesArray = d.getJSONArray("delegates")
                MyDevice(
                    deviceId = d.getString("deviceId"),
                    name = d.optString("name"),
                    platform = d.optString("platform"),
                    delegates = (0 until delegatesArray.length()).map { j ->
                        val del = delegatesArray.getJSONObject(j)
                        DelegateInfo(del.getInt("id"), del.getString("name"), del.getString("email"))
                    }
                )
            }
        }
    }

    suspend fun getDelegatedToMe(): Result<List<DelegatedDevice>> {
        return requestArray("GET", "/api/devices/delegated-to-me/").map { devices ->
            (0 until devices.length()).map { i ->
                val d = devices.getJSONObject(i)
                val owner = d.getJSONObject("owner")
                DelegatedDevice(
                    deviceId = d.getString("deviceId"),
                    name = d.optString("name"),
                    ownerId = owner.getInt("id"),
                    ownerName = owner.getString("name")
                )
            }
        }
    }

    suspend fun delegateControl(deviceId: String, email: String): Result<String> {
        return request("POST", "/api/devices/$deviceId/delegate/", JSONObject().apply {
            put("email", email)
        }).map { it.optString("message") }
    }

    suspend fun revokeControl(deviceId: String, userId: Int): Result<String> {
        return request("DELETE", "/api/devices/$deviceId/delegate/$userId/").map { it.optString("message") }
    }

    suspend fun sendCommand(deviceId: String, action: String): Result<String> {
        return request("POST", "/api/devices/$deviceId/control/$action/").map { it.optString("message") }
    }

    suspend fun getPendingCommands(deviceId: String): Result<List<PendingCommand>> {
        return request("GET", "/api/devices/$deviceId/pending-commands/").map { json ->
            val commands = json.getJSONArray("commands")
            (0 until commands.length()).map { i ->
                val c = commands.getJSONObject(i)
                val issuer = c.getJSONObject("issuedBy")
                PendingCommand(
                    command = c.getString("command"),
                    issuedById = issuer.getInt("id"),
                    issuedByName = issuer.getString("name")
                )
            }
        }
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null): Result<JSONObject> =
        executeRaw(method, path, body).map { JSONObject(it) }

    private suspend fun requestArray(method: String, path: String, body: JSONObject? = null): Result<JSONArray> =
        executeRaw(method, path, body).map { JSONArray(it) }

    private suspend fun executeRaw(method: String, path: String, body: JSONObject? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL(NetworkConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    doInput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json")

                    tokenManager.getToken()?.let { setRequestProperty("Authorization", "Bearer $it") }
                    NetworkConfig.applyDeviceHeaders(this)

                    connectTimeout = NetworkConfig.Settings.CONNECT_TIMEOUT.toInt()
                    readTimeout = NetworkConfig.Settings.READ_TIMEOUT.toInt()
                }

                if (body != null) {
                    connection.doOutput = true
                    OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                }

                Log.d("DeviceAPI", "$method $path -> $responseCode: $responseText")

                if (responseCode in 200..299) {
                    Result.success(responseText)
                } else {
                    val error = try { JSONObject(responseText).optString("error", "Request failed") } catch (e: Exception) { "Request failed" }
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e("DeviceAPI", "$method $path failed: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
