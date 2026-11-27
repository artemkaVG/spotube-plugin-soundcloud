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

import kotlinx.serialization.json.JsonElement

internal fun JsonElement.toSoundCloudUser(): SoundCloudUser = SoundCloudUser(
    id = str("id").orEmpty(),
    username = str("username"),
    fullName = str("full_name"),
    permalink = str("permalink"),
    permalinkUrl = str("permalink_url"),
    avatarUrl = str("avatar_url"),
    description = str("description"),
    followersCount = int("followers_count"),
    verified = bool("verified") == true || child("badges")?.bool("verified") == true,
)

internal fun JsonElement.toSoundCloudTrack(): SoundCloudTrack = SoundCloudTrack(
    id = str("id").orEmpty(),
    title = str("title"),
    durationMs = long("full_duration") ?: long("duration") ?: 0L,
    permalinkUrl = str("permalink_url"),
    artworkUrl = str("artwork_url"),
    user = child("user")?.toSoundCloudUser(),
    publisher = child("publisher_metadata")?.let { publisher ->
        SoundCloudPublisher(
            artist = publisher.str("artist"),
            albumTitle = publisher.str("album_title"),
            isrc = publisher.str("isrc"),
            explicit = publisher.bool("explicit"),
        )
    },
    policy = str("policy"),
    trackAuthorization = str("track_authorization"),
    transcodings = child("media").arr("transcodings").map { transcoding ->
        SoundCloudTranscoding(
            url = transcoding.str("url"),
            protocol = transcoding.child("format")?.str("protocol"),
            mimeType = transcoding.child("format")?.str("mime_type"),
            preset = transcoding.str("preset"),
            snipped = transcoding.bool("snipped") == true,
        )
    },
)

internal fun JsonElement.toSoundCloudPlaylist(): SoundCloudPlaylist = SoundCloudPlaylist(
    id = str("id").orEmpty(),
    title = str("title"),
    description = str("description"),
    permalinkUrl = str("permalink_url"),
    artworkUrl = str("artwork_url"),
    calculatedArtworkUrl = str("calculated_artwork_url"),
    user = child("user")?.toSoundCloudUser(),
    tracks = arr("tracks").map { it.toSoundCloudTrack() },
    trackCount = int("track_count"),
    setType = str("set_type"),
    isAlbum = bool("is_album") == true || str("set_type") in setOf("album", "ep", "single", "compilation"),
    releaseDate = (str("release_date") ?: str("display_date") ?: str("published_at"))?.take(10),
    genre = str("genre"),
)

internal fun JsonElement.toSoundCloudSearchItem(): SoundCloudSearchItem? = when (str("kind")) {
    "track" -> SoundCloudSearchItem.Track(toSoundCloudTrack())
    "user" -> SoundCloudSearchItem.User(toSoundCloudUser())
    "playlist" -> SoundCloudSearchItem.Playlist(toSoundCloudPlaylist())
    else -> null
}

internal fun JsonElement.toSoundCloudSelections(path: String): List<SoundCloudSelection> =
    collection().mapIndexedNotNull { index, selection ->
        val title = selection.str("title") ?: return@mapIndexedNotNull null
        val items = selection.child("items")?.collection()?.takeIf { it.isNotEmpty() } ?: selection.arr("items")
        SoundCloudSelection(
            key = selection.str("urn") ?: selection.str("id") ?: "$path:$index",
            title = title,
            description = selection.str("description"),
            playlists = items.map { (it.child("playlist") ?: it.child("system_playlist") ?: it).toSoundCloudPlaylist() },
        )
    }
