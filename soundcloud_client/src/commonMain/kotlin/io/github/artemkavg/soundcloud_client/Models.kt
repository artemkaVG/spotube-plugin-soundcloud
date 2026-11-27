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

data class SoundCloudCollection<T>(val items: List<T>, val totalCount: Int?)

data class SoundCloudUser(
    val id: String = "",
    val username: String? = null,
    val fullName: String? = null,
    val permalink: String? = null,
    val permalinkUrl: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val followersCount: Int? = null,
    val verified: Boolean = false,
)

data class SoundCloudPublisher(
    val artist: String? = null,
    val albumTitle: String? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
)

data class SoundCloudTrack(
    val id: String = "",
    val title: String? = null,
    val durationMs: Long = 0,
    val permalinkUrl: String? = null,
    val artworkUrl: String? = null,
    val user: SoundCloudUser? = null,
    val publisher: SoundCloudPublisher? = null,
    val policy: String? = null,
    val trackAuthorization: String? = null,
    val transcodings: List<SoundCloudTranscoding> = emptyList(),
) {
    val isFull: Boolean get() = title != null
}

data class SoundCloudPlaylist(
    val id: String = "",
    val title: String? = null,
    val description: String? = null,
    val permalinkUrl: String? = null,
    val artworkUrl: String? = null,
    val calculatedArtworkUrl: String? = null,
    val user: SoundCloudUser? = null,
    val tracks: List<SoundCloudTrack> = emptyList(),
    val trackCount: Int? = null,
    val setType: String? = null,
    val isAlbum: Boolean = false,
    val releaseDate: String? = null,
    val genre: String? = null,
) {
    val key: String get() = if (id.isNotEmpty() && id.all { it.isDigit() }) id else permalinkUrl ?: id
}

data class SoundCloudSelection(
    val key: String,
    val title: String,
    val description: String?,
    val playlists: List<SoundCloudPlaylist>,
)

data class SoundCloudTranscoding(
    val url: String?,
    val protocol: String?,
    val mimeType: String?,
    val preset: String?,
    val snipped: Boolean,
)

data class SoundCloudStream(val url: String?, val requiresLicense: Boolean)

sealed interface SoundCloudSearchItem {
    data class Track(val data: SoundCloudTrack) : SoundCloudSearchItem
    data class User(val data: SoundCloudUser) : SoundCloudSearchItem
    data class Playlist(val data: SoundCloudPlaylist) : SoundCloudSearchItem
}
