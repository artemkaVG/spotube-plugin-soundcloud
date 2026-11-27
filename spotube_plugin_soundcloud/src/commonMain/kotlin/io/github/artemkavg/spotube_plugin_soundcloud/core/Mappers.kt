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

import io.github.artemkavg.soundcloud_client.SoundCloudPlaylist
import io.github.artemkavg.soundcloud_client.SoundCloudTrack
import io.github.artemkavg.soundcloud_client.SoundCloudUser
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumType
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser

fun SoundCloudUser.toArtist(): MetadataArtist.Basic = MetadataArtist.Basic(
    id = id,
    name = username ?: fullName ?: permalink.orEmpty(),
    thumbnails = thumbnails(avatarUrl).orPlaceholder(id),
    externalUri = permalinkUrl,
)

fun SoundCloudUser.toArtistDetailed(): MetadataArtist.Detailed = MetadataArtist.Detailed(
    genres = emptyList(),
    biography = description,
    followersCount = followersCount,
    id = id,
    name = username ?: fullName ?: permalink.orEmpty(),
    thumbnails = thumbnails(avatarUrl).orPlaceholder(id),
    externalUri = permalinkUrl,
)

fun SoundCloudUser.toUser(): MetadataUser = MetadataUser(
    id = id,
    username = permalink ?: username.orEmpty(),
    displayName = username ?: fullName,
    thumbnails = thumbnails(avatarUrl).orPlaceholder(id),
    externalUri = permalinkUrl,
)

fun SoundCloudTrack.toTrack(): MetadataTrack = MetadataTrack(
    id = id,
    title = title.orEmpty(),
    durationMs = durationMs,
    trackNumber = null,
    discNumber = null,
    artists = listOfNotNull(user?.toArtist()),
    album = null,
    thumbnails = thumbnails(artworkUrl, user?.avatarUrl).orPlaceholder(id),
    explicit = publisher?.explicit,
    popularity = null,
    isrcCode = publisher?.isrc,
    externalUri = permalinkUrl,
)

private fun SoundCloudPlaylist.playlistArtwork(): List<Thumbnail> = thumbnails(
    artworkUrl,
    calculatedArtworkUrl,
    tracks.firstNotNullOfOrNull { it.artworkUrl },
    user?.avatarUrl,
).orPlaceholder(key)

fun SoundCloudPlaylist.toPlaylist(): MetadataPlaylist = MetadataPlaylist(
    id = key,
    title = title.orEmpty(),
    description = description,
    thumbnails = playlistArtwork(),
    trackCount = trackCount ?: tracks.size,
    externalUri = permalinkUrl,
    owner = user?.toUser(),
)

private fun SoundCloudPlaylist.albumType(): MetadataAlbumType = when (setType) {
    "single" -> MetadataAlbumType.Single
    "compilation" -> MetadataAlbumType.Collection
    else -> MetadataAlbumType.Album
}

fun SoundCloudPlaylist.toAlbumBasic(): MetadataAlbum.Basic = MetadataAlbum.Basic(
    id = key,
    title = title.orEmpty(),
    description = description,
    thumbnails = playlistArtwork(),
    albumType = albumType(),
    artists = listOfNotNull(user?.toArtist()),
    externalUri = permalinkUrl,
)

fun SoundCloudPlaylist.toAlbumDetailed(): MetadataAlbum.Detailed = MetadataAlbum.Detailed(
    releaseDate = releaseDate,
    genres = listOfNotNull(genre),
    trackCount = trackCount ?: tracks.size,
    id = key,
    title = title.orEmpty(),
    description = description,
    thumbnails = playlistArtwork(),
    albumType = albumType(),
    artists = listOfNotNull(user?.toArtist()),
    externalUri = permalinkUrl,
)
