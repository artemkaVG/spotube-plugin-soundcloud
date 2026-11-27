/*
 * Copyright (C) 2026 artemkaVG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.artemkavg.soundcloud_client

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod

internal class SoundCloudHttpClient(private val http: HttpClientAPI) {
    private val clientIdMutex = Mutex()
    private var clientId: String? = null

    suspend fun get(path: String, query: Map<String, String?> = emptyMap()): JsonElement {
        var id = clientId ?: refreshClientId(null)
        var response = request(path, query, id)
        if (response.statusCode == 401 || response.statusCode == 403) {
            id = refreshClientId(id)
            response = request(path, query, id)
        }
        if (response.statusCode >= 400) {
            throw Exception("SoundCloud request $path failed with status ${response.statusCode}")
        }
        return JSON.parseToJsonElement(response.body ?: "null")
    }

    private suspend fun request(path: String, query: Map<String, String?>, id: String) =
        http.request(HttpMethod.Get, buildUrl(path, query + ("client_id" to id)), API_HEADERS, null)

    private suspend fun refreshClientId(stale: String?): String = clientIdMutex.withLock {
        val current = clientId
        if (current != null && current != stale) {
            current
        } else {
            val fresh = fetchClientId()
            clientId = fresh
            fresh
        }
    }

    private suspend fun fetchClientId(): String {
        val page = http.request(HttpMethod.Get, SITE_URL, PAGE_HEADERS, null).body.orEmpty()
        extractClientId(page)?.let { return it }

        val assets = ASSET_REGEX.findAll(page).map { it.value }.distinct().toList().asReversed()
        for (asset in assets.take(MAX_ASSETS)) {
            val script = runRequest { http.request(HttpMethod.Get, asset, PAGE_HEADERS, null) }
                .getOrNull()
                ?.takeIf { it.statusCode in 200..299 }
                ?.body
                ?: continue
            val found = extractClientId(script)
            if (found != null) return found
        }
        throw Exception("SoundCloud client_id not found on $SITE_URL")
    }

    companion object {
        val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

        private const val SITE_URL = "https://soundcloud.com/"
        private const val API_URL = "https://api-v2.soundcloud.com/"
        private const val MAX_ASSETS = 12
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

        private val PAGE_HEADERS = mapOf(
            "User-Agent" to USER_AGENT,
            "Accept" to "text/html,application/xhtml+xml,application/javascript,*/*",
        )
        private val API_HEADERS = mapOf(
            "User-Agent" to USER_AGENT,
            "Accept" to "application/json",
            "Origin" to "https://soundcloud.com",
            "Referer" to SITE_URL,
        )

        private val API_CLIENT_REGEX = Regex(""""apiClient"[\s\S]*?"id":"([^"]+)"""")
        private val CLIENT_ID_REGEX = Regex("""(?:client_id|clientId)\s*[:=]\s*["']?([A-Za-z0-9_-]{20,})""")
        private val ASSET_REGEX = Regex("""https://a-v2\.sndcdn\.com/assets/[^"'<>\s]+\.js""")

        fun extractClientId(content: String): String? =
            (API_CLIENT_REGEX.find(content) ?: CLIENT_ID_REGEX.find(content))?.groupValues?.get(1)

        fun buildUrl(path: String, query: Map<String, String?>): String {
            val params = query.entries
                .filter { !it.value.isNullOrBlank() }
                .joinToString("&") { "${encode(it.key)}=${encode(it.value!!)}" }
            val base = if (path.startsWith("https://")) path else API_URL + path.trimStart('/')
            return when {
                params.isEmpty() -> base
                '?' in base -> "$base&$params"
                else -> "$base?$params"
            }
        }

        fun encode(value: String): String = buildString {
            for (byte in value.encodeToByteArray()) {
                val c = byte.toInt() and 0xFF
                val unreserved = (c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) ||
                    (c in '0'.code..'9'.code) || c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
                if (unreserved) {
                    append(c.toChar())
                } else {
                    append('%')
                    append(HEX[c shr 4])
                    append(HEX[c and 0x0F])
                }
            }
        }

        private const val HEX = "0123456789ABCDEF"
    }
}
