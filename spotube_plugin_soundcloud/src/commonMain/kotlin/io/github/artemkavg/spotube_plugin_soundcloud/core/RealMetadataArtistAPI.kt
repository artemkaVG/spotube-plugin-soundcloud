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
import io.github.artemkavg.soundcloud_client.runRequest
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistOverview
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

class RealMetadataArtistAPI(private val client: SoundCloudClient) : MetadataArtistAPI {
    override suspend fun getArtist(id: String): MetadataArtist.Detailed = client.user(id).toArtistDetailed()

    override suspend fun artistOverview(id: String): MetadataArtistOverview = MetadataArtistOverview(
        artist = getArtist(id),
        top10Tracks = runRequest { getArtistTop10Tracks(id) }.getOrDefault(emptyList()),
        albums = runRequest { getArtistAlbums(id, null) }.getOrDefault(emptyPage()),
        relatedArtists = runRequest { relatedArtists(id, null) }.getOrDefault(emptyPage()),
        featuredPlaylists = runRequest { featuredPlaylists(id, null) }.getOrDefault(emptyPage()),
    )

    override suspend fun getArtistTop10Tracks(id: String): List<MetadataTrack> {
        val (tracks, _) = client.topTracks(id, 10)
        return tracks.filter { it.isFull }.map { it.toTrack() }
    }

    override suspend fun relatedArtists(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataArtist.Basic> {
        val page = pagination.page()
        val (users, total) = client.relatedArtists(id, page.offset, page.limit)
        return page.result(users.map { it.toArtist() }, total)
    }

    override suspend fun featuredPlaylists(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataPlaylist> {
        val page = pagination.page()
        val (playlists, total) = client.userPlaylists(id, page.offset, page.limit)
        return page.result(playlists.map { it.toPlaylist() }, total)
    }

    override suspend fun getArtistAlbums(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataAlbum.Detailed> {
        val page = pagination.page()
        val (albums, total) = client.userAlbums(id, page.offset, page.limit)
        return page.result(albums.map { it.toAlbumDetailed() }, total)
    }

    override suspend fun savedArtists(pagination: PaginationStrategy?): PaginationResult<MetadataArtist.Detailed> =
        PaginationResult(emptyList(), 0, null)

    override suspend fun isSavedArtists(ids: List<String>): List<Boolean> = ids.map { false }

    override suspend fun saveArtists(ids: List<String>) = readOnly()

    override suspend fun removeSavedArtists(ids: List<String>) = readOnly()
}
