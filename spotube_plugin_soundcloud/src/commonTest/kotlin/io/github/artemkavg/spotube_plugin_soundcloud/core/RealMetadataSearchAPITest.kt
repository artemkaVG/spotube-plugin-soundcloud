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

package io.github.artemkavg.spotube_plugin_soundcloud.core

import io.github.artemkavg.soundcloud_client.SoundCloudClient
import io.github.artemkavg.spotube_plugin_soundcloud.mocks.ASSET_WITH_ID
import io.github.artemkavg.spotube_plugin_soundcloud.mocks.FakeHttp
import io.github.artemkavg.spotube_plugin_soundcloud.mocks.SITE
import io.github.artemkavg.spotube_plugin_soundcloud.mocks.ok
import io.github.artemkavg.spotube_plugin_soundcloud.mocks.track
import kotlinx.coroutines.test.runTest
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealMetadataSearchAPITest {
    @Test
    fun realArtistComesFirstAmongAccounts() = runTest {
        fun user(id: Int, name: String, followers: Int, verified: Boolean) = """
            {"kind":"user","id":$id,"username":"$name","permalink":"p$id","followers_count":$followers,
             "verified":$verified,"permalink_url":"https://soundcloud.com/p$id"}
        """.trimIndent()
        val users = """{"collection":[${user(1, "MIRAMI music", 300, false)},${user(2, "mirami_fan", 10, false)},${user(3, "Mirami", 8500, true)}]}"""
        val mixed = """{"collection":[${track("9", "Hit")},${user(1, "MIRAMI music", 300, false)},${user(3, "Mirami", 8500, true)}]}"""
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/search/users?" to { _: String -> ok(users) },
                "https://api-v2.soundcloud.com/search?" to { _: String -> ok(mixed) },
            )
        )
        val search = RealMetadataSearchAPI(SoundCloudClient(http))

        val artists = search.searchArtists("mirami", null).items.map { it.data.name }
        assertEquals(listOf("Mirami", "MIRAMI music", "mirami_fan"), artists)

        val all = search.search("Mirami").map {
            when (it) {
                is MetadataSearchResult.Track -> "track:" + it.data.title
                is MetadataSearchResult.Artist -> "artist:" + it.data.name
                else -> "other"
            }
        }
        assertEquals(listOf("track:Hit", "artist:Mirami", "artist:MIRAMI music"), all)
    }

    @Test
    fun mixedSearchSplitsTracksArtistsAlbumsAndPlaylists() = runTest {
        val search = """
            {"collection":[
              ${track("5", "Hit")},
              {"kind":"user","id":42,"username":"Artist","permalink_url":"https://soundcloud.com/artist"},
              {"kind":"playlist","id":10,"title":"LP","set_type":"album","permalink_url":"https://soundcloud.com/a/sets/lp"},
              {"kind":"playlist","id":11,"title":"Set","set_type":"","permalink_url":"https://soundcloud.com/a/sets/set"}
            ],"total_results":4}
        """.trimIndent()
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/search?" to { url: String ->
                    assertTrue(url.contains("q=Mirami%20Mix"), url)
                    ok(search)
                },
            )
        )
        val results = RealMetadataSearchAPI(SoundCloudClient(http)).search("Mirami Mix")

        assertEquals(
            listOf("Track", "Artist", "Album", "Playlist"),
            results.map { it::class.simpleName },
        )
        assertEquals("LP", (results[2] as MetadataSearchResult.Album).data.title)
    }
}
