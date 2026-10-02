package com.example.musicroom

import com.example.musicroom.data.network.NetworkConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain JVM tests (no device): NetworkConfig.init() is never called here, so there is no
 * SharedPreferences and BASE_URL falls back to the compiled default.
 */
class NetworkConfigTest {

    @Test
    fun baseUrl_fallsBackToCompiledDefault_withoutOverride() {
        assertNull(NetworkConfig.getBaseUrlOverride())
        assertEquals(NetworkConfig.getDefaultBaseUrl(), NetworkConfig.BASE_URL)
    }

    @Test
    fun baseUrl_hasNoTrailingSlash_soPathsConcatenateCleanly() {
        assertTrue(!NetworkConfig.BASE_URL.endsWith("/"))
    }

    @Test
    fun webSocketUrl_swapsHttpSchemeForWs_andKeepsHostAndPath() {
        val base = NetworkConfig.BASE_URL
        val expected = when {
            base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
            else -> "ws://" + base.removePrefix("http://")
        } + "/ws/playlists/1"
        assertEquals(expected, NetworkConfig.getWebSocketUrl("/ws/playlists/1"))
    }
}
