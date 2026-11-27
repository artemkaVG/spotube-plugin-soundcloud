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

import io.github.artemkavg.soundcloud_client.SoundCloudPublisher
import io.github.artemkavg.soundcloud_client.SoundCloudTrack
import io.github.artemkavg.soundcloud_client.SoundCloudUser
import io.github.artemkavg.spotube_plugin_soundcloud.mocks.track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MappersTest {
    @Test
    fun missingAvatarsGetSoundCloudGradients() {
        fun user(id: Int, avatar: String?) = SoundCloudUser(id = id.toString(), username = "u$id", avatarUrl = avatar)
        val stock = user(1, "https://a1.sndcdn.com/images/default_avatar_large.png").toArtist().thumbnails.single().url
        val none = user(2, null).toArtist().thumbnails.single().url
        val real = user(3, "https://i1.sndcdn.com/avatars-3-large.jpg").toArtist().thumbnails.single().url

        assertTrue(stock.startsWith("data:image/png;base64,"), stock.take(40))
        assertTrue(none.startsWith("data:image/png;base64,"), none.take(40))
        assertEquals(stock, user(1, null).toArtist().thumbnails.single().url)
        assertEquals("https://i1.sndcdn.com/avatars-3-t500x500.jpg", real)
    }

    @Test
    fun mapsTypedTrackToSpotubeMetadata() {
        val track = SoundCloudTrack(
            id = "2147483648",
            title = "Song",
            durationMs = 215000L,
            artworkUrl = "https://i1.sndcdn.com/artworks-2147483648-large.jpg",
            user = SoundCloudUser(id = "42", username = "Artist"),
            publisher = SoundCloudPublisher(isrc = "USRC17607839", explicit = true),
        ).toTrack()

        assertEquals("2147483648", track.id)
        assertEquals(215000L, track.durationMs)
        assertEquals("USRC17607839", track.isrcCode)
        assertEquals(true, track.explicit)
        assertEquals("https://i1.sndcdn.com/artworks-2147483648-t500x500.jpg", track.thumbnails?.first()?.url)
        assertEquals("Artist", track.artists.single().name)
    }
}
