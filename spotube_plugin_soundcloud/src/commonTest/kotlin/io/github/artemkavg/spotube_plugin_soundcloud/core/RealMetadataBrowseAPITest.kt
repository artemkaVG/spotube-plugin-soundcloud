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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.spotube.plugin_interfaces.host_apis.HttpResponse
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RealMetadataBrowseAPITest {
    @Test
    fun cancelledHomeRequestDoesNotLoadAnotherSection() = runTest {
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/charts/selections" to { _: String -> throw CancellationException("Cancelled") },
                "https://api-v2.soundcloud.com/mixed-selections" to { _: String -> ok("""{"collection":[]}""") },
            )
        )

        assertFailsWith<CancellationException> { RealMetadataBrowseAPI(SoundCloudClient(http)).genres() }
        assertTrue(http.requested.none { "mixed-selections" in it })
    }

    @Test
    fun unavailableHomeReportsFailureInsteadOfAnEmptyCatalogue() = runTest {
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/" to { _: String -> HttpResponse(503, emptyMap(), "Unavailable") },
            )
        )

        val error = assertFailsWith<Exception> { RealMetadataBrowseAPI(SoundCloudClient(http)).genres() }
        assertTrue(error.message.orEmpty().contains("503"))
    }

    @Test
    fun homeIsLaidOutLikeSpotifyFromChartSelections() = runTest {
        fun chart(region: String, slug: String, title: String) = """
            {"kind":"playlist","id":"${region}$slug","title":"$title",
             "permalink_url":"https://soundcloud.com/music-charts-$region/sets/$slug",
             "artwork_url":"https://i1.sndcdn.com/artworks-c-large.jpg"}
        """.trimIndent()
        val selections = """
            {"collection":[
              {"kind":"selection","urn":"soundcloud:selections:charts-us","title":"Music Charts US",
               "items":{"collection":[
                 ${chart("us", "all-music-genres", "All music genres")},
                 ${chart("us", "new-hot", "New & Hot")},
                 ${chart("us", "hip-hop", "Hip Hop")},
                 {"kind":"playlist","id":"x","title":"Artist Pro","permalink_url":"https://soundcloud.com/pro"}
               ]}},
              {"kind":"selection","urn":"soundcloud:selections:charts-uk","title":"Music Charts UK",
               "items":{"collection":[
                 ${chart("uk", "all-music-genres", "All music genres")},
                 ${chart("uk", "dance", "Dance")},
                 ${chart("uk", "hip-hop", "Hip Hop")}
               ]}},
              {"kind":"selection","urn":"soundcloud:selections:curated","title":"Curated",
               "items":{"collection":[{"kind":"playlist","id":99,"title":"Chill","permalink_url":"https://soundcloud.com/a/sets/chill"}]}},
              {"kind":"selection","urn":"soundcloud:selections:empty","title":"Empty","items":{"collection":[]}}
            ]}
        """.trimIndent()
        val chartPlaylist = """{"kind":"playlist","title":"chart","tracks":[${track("1", "One")},${track("2", "Two")}]}"""
        val discover = """
            {"collection":[
              {"kind":"selection","urn":"soundcloud:selections:buzzing","title":"Artists to watch out for",
               "items":{"collection":[{"kind":"playlist","id":501,"title":"Buzzing Electronic",
                 "permalink_url":"https://soundcloud.com/buzzing-playlists/sets/buzzing-electronic"}]}},
              {"kind":"selection","urn":"soundcloud:selections:curated-by-sc","title":"Curated by SoundCloud",
               "items":{"collection":[{"playlist":{"kind":"playlist","id":502,"title":"Emerging Indie: Dreams",
                 "permalink_url":"https://soundcloud.com/soundcloud-scenes/sets/dreams-emerging-indie"}}]}}
            ]}
        """.trimIndent()
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/charts/selections" to { _: String -> ok(selections) },
                "https://api-v2.soundcloud.com/mixed-selections" to { _: String -> ok(discover) },
                "https://api-v2.soundcloud.com/resolve?" to { _: String -> ok(chartPlaylist) },
                "https://api-v2.soundcloud.com/playlists/" to { _: String -> ok(chartPlaylist) },
            )
        )
        val browse = RealMetadataBrowseAPI(SoundCloudClient(http))

        assertEquals(listOf("All", "Hip Hop"), browse.genres().map { it.name })

        val all = browse.list("all", null).items
        assertEquals(
            listOf("Top 50", "Popular artists", "Artists to watch out for", "Curated by SoundCloud", "New & Hot", "Charts", "Curated"),
            all.map { it.title },
        )
        assertEquals("Emerging Indie: Dreams", (all[3].items.single() as MetadataBrowseItem.Playlist).data.title)
        assertTrue(all[0].items.all { it is MetadataBrowseItem.Track })
        assertEquals(listOf("Artist"), all[1].items.map { (it as MetadataBrowseItem.Artist).data.name })
        val chartItem = all[5].items.first() as MetadataBrowseItem.Playlist
        assertEquals("All music genres", chartItem.data.title)
        assertEquals("https://i1.sndcdn.com/artworks-c-t500x500.jpg", chartItem.data.thumbnails.first().url)
        assertEquals(3, all[5].items.size)

        val hiphop = browse.list("hip-hop", null).items
        assertEquals(listOf("Top 50", "Popular artists", "Charts"), hiphop.map { it.title })

        val more = browse.sublist("all", all[0].moreLink!!, null)
        assertEquals(listOf("One", "Two"), more.items.map { (it as MetadataBrowseItem.Track).data.title })
        assertEquals(1, http.requested.count { it.contains("charts/selections") })
        assertEquals(1, http.requested.count { it.contains("mixed-selections") })
        assertEquals(listOf("One", "Two"), browse.featured().map { (it as MetadataBrowseItem.Track).data.title })
    }
}
