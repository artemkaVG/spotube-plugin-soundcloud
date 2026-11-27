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
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealMetadataPlaylistAPITest {
    @Test
    fun hydratesStubTracksInPlaylistOrder() = runTest {
        val playlist = """
            {"kind":"playlist","id":777,"title":"Mix","track_count":3,"permalink_url":"https://soundcloud.com/a/sets/mix",
             "tracks":[${track("1", "First")},{"id":3,"kind":"track"},{"id":2,"kind":"track"}]}
        """.trimIndent()
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/playlists/777" to { _: String -> ok(playlist) },
                "https://api-v2.soundcloud.com/tracks?" to { url: String ->
                    assertTrue(url.contains("ids=3%2C2"), url)
                    ok("[${track("2", "Second")},${track("3", "Third")}]")
                },
            )
        )
        val api = RealMetadataPlaylistAPI(SoundCloudClient(http))

        val page = api.getPlaylistTracks("777", PaginationStrategy.Offset(0, 20))

        assertEquals(listOf("First", "Third", "Second"), page.items.map { it.title })
        assertEquals(3, page.totalCount)
        assertEquals(null, page.nextPagination)
        assertEquals(3, api.getPlaylist("777").trackCount)
    }
}
