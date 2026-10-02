package com.example.musicroom.data.service

import android.util.Log
import com.example.musicroom.data.auth.TokenManager
import com.example.musicroom.data.network.NetworkConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time multi-user playlist editing (V.2.3): connects to the backend's Socket.IO
 * playlists gateway so every connected editor sees track add/remove/reorder events
 * live, instead of polling.
 *
 * Speaks the Socket.IO v4 wire protocol over OkHttp's plain WebSocket instead of pulling
 * in the socket.io client library - the backend only ever emits one event ("update"), so
 * the handful of packet types handled below is all of it:
 *   "0{...}" engine open    -> reply "40" (join the default namespace)
 *   "2"      server ping    -> reply "3"  (pong, or the server drops us after ~45 s)
 *   "42[...]" event         -> ["update", { event, tracks, trackId }]
 */
@Singleton
class PlaylistRealtimeClient @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val client = OkHttpClient()

    /** Returns the open WebSocket; call [WebSocket.close] (e.g. in DisposableEffect) when done. */
    fun connect(playlistId: String, onUpdate: (String) -> Unit): WebSocket? {
        val token = tokenManager.getToken() ?: run {
            Log.w("PlaylistRealtime", "No auth token, skipping realtime connection")
            return null
        }

        // The gateway reads playlistId + token from the handshake query and disconnects
        // if the token is invalid or the user can't view the playlist.
        val url = NetworkConfig.getWebSocketUrl(
            "/socket.io/?EIO=4&transport=websocket&playlistId=$playlistId&token=$token"
        )
        val request = Request.Builder().url(url).build()

        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("PlaylistRealtime", "Connected to playlist $playlistId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when {
                    text.startsWith("0") -> webSocket.send("40")
                    text == "2" -> webSocket.send("3")
                    text.startsWith("42") -> {
                        Log.d("PlaylistRealtime", "Update received: $text")
                        onUpdate(text.removePrefix("42"))
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("PlaylistRealtime", "Connection failed: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }
        })
    }
}
