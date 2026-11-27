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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.spotube.plugin_interfaces.host_apis.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoundCloudClientTest {
    @Test
    fun cancelledClientIdBundleLoadStopsImmediately() = runTest {
        val http = FakeHttp(
            listOf(
                SITE,
                "https://a-v2.sndcdn.com/assets/49-bbbb.js" to { _: String -> throw CancellationException("Cancelled") },
                ASSET_WITHOUT_ID,
            )
        )

        assertFailsWith<CancellationException> { SoundCloudClient(http).track("1") }
        assertEquals(2, http.requested.size)
    }

    @Test
    fun findsClientIdInBundlesAndSendsIt() = runTest {
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITHOUT_ID, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/tracks/2147483648" to { _: String -> ok(track("2147483648", "Song")) },
            )
        )
        val track = SoundCloudClient(http).track("2147483648")

        assertTrue(http.requested.none { it.endsWith("0-aaaa.js") }, http.requested.toString())
        assertTrue(http.requested.last().contains("client_id=$CLIENT_ID"), http.requested.last())

        assertEquals("2147483648", track.id)
        assertEquals(215000L, track.durationMs)
        assertEquals("USRC17607839", track.publisher?.isrc)
        assertEquals(true, track.publisher?.explicit)
        assertEquals("https://i1.sndcdn.com/artworks-2147483648-large.jpg", track.artworkUrl)
        assertEquals("Artist", track.user?.username)
    }

    @Test
    fun refetchesClientIdOnUnauthorizedAndRetries() = runTest {
        var calls = 0
        val http = FakeHttp(
            listOf(
                SITE, ASSET_WITH_ID,
                "https://api-v2.soundcloud.com/tracks/1" to { _: String ->
                    calls++
                    if (calls == 1) HttpResponse(401, emptyMap(), "{}") else ok(track("1", "Again"))
                },
            )
        )
        val track = SoundCloudClient(http).track("1")

        assertEquals("Again", track.title)
        assertEquals(2, calls)
        assertEquals(2, http.requested.count { it == "https://soundcloud.com/" })
    }
}
