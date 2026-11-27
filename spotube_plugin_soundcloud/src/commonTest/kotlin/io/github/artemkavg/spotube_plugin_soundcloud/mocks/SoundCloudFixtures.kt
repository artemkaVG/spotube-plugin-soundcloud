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

package io.github.artemkavg.spotube_plugin_soundcloud.mocks

import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpMethod
import org.spotube.plugin_interfaces.host_apis.HttpResponse

internal class FakeHttp(internal val routes: List<Pair<String, (String) -> HttpResponse>>) : HttpClientAPI {
    val requested = mutableListOf<String>()

    override suspend fun request(
        method: HttpMethod,
        url: String,
        requestHeaders: Map<String, String>?,
        body: String?,
    ): HttpResponse {
        requested += url
        val handler = routes.firstOrNull { url.startsWith(it.first) }?.second
            ?: return HttpResponse(404, emptyMap(), "{}")
        return handler(url)
    }
}

internal fun ok(body: String) = HttpResponse(200, emptyMap(), body)

internal const val CLIENT_ID = "abcdefghijklmnopqrstuvwxyz012345"

internal val SITE = "https://soundcloud.com/" to { _: String ->
    ok(
        """<script crossorigin src="https://a-v2.sndcdn.com/assets/0-aaaa.js"></script>
           <script crossorigin src="https://a-v2.sndcdn.com/assets/49-bbbb.js"></script>"""
    )
}
internal val ASSET_WITH_ID = "https://a-v2.sndcdn.com/assets/49-bbbb.js" to { _: String ->
    ok("""e.exports={client_id:"$CLIENT_ID",env:"production"}""")
}

internal fun track(id: String, title: String) = """
    {"kind":"track","id":$id,"title":"$title","duration":30000,"full_duration":215000,
     "permalink_url":"https://soundcloud.com/artist/$title",
     "artwork_url":"https://i1.sndcdn.com/artworks-$id-large.jpg",
     "user":{"kind":"user","id":42,"username":"Artist","permalink":"artist",
             "avatar_url":"https://i1.sndcdn.com/avatars-42-large.jpg","permalink_url":"https://soundcloud.com/artist"},
     "publisher_metadata":{"isrc":"USRC17607839","explicit":true}}
""".trimIndent()
