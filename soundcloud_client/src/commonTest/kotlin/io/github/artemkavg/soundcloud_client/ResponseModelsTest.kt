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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponseModelsTest {
    @Test
    fun mixedSearchDecodesBothIdFormsAndSkipsUnknownKinds() = runTest {
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/search?" to { _: String ->
                    ok("""{"collection":[
                        {"kind":"track","id":"9876543210123","title":"Song","duration":1234},
                        {"kind":"user","id":2147483648,"username":"Artist","badges":{"verified":true}},
                        {"kind":"advertisement","id":3}
                    ],"total_results":3}""")
                },
            )
        )

        val result = SoundCloudClient(http).search("Song", 0, 20)
        val track = (result.items[0] as SoundCloudSearchItem.Track).data
        val user = (result.items[1] as SoundCloudSearchItem.User).data

        assertEquals(2, result.items.size)
        assertEquals(3, result.totalCount)
        assertEquals("9876543210123", track.id)
        assertEquals(1234L, track.durationMs)
        assertEquals(null, track.publisher)
        assertTrue(track.transcodings.isEmpty())
        assertEquals("2147483648", user.id)
        assertTrue(user.verified)
    }

    @Test
    fun selectionsDecodeArrayAndWrappedPlaylistItems() = runTest {
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/charts/selections" to { _: String ->
                    ok("""{"collection":[{"title":"Selections","items":[
                        {"system_playlist":{"id":"generated","title":"Chart","permalink_url":"https://soundcloud.com/charts/set"}},
                        {"playlist":{"id":42,"title":"EP","set_type":"ep","tracks":[{"id":9}]}}
                    ]}]}""")
                },
            )
        )

        val selection = SoundCloudClient(http).chartSelections().single()

        assertEquals("charts/selections:0", selection.key)
        assertEquals("https://soundcloud.com/charts/set", selection.playlists[0].key)
        assertFalse(selection.playlists[0].isAlbum)
        assertEquals("42", selection.playlists[1].key)
        assertTrue(selection.playlists[1].isAlbum)
        assertEquals("9", selection.playlists[1].tracks.single().id)
        assertFalse(selection.playlists[1].tracks.single().isFull)
    }
}
