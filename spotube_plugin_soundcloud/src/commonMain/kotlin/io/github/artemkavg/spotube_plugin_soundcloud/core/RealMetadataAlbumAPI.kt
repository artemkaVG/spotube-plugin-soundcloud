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
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack

class RealMetadataAlbumAPI(private val client: SoundCloudClient) : MetadataAlbumAPI {
    override suspend fun getAlbum(id: String): MetadataAlbum.Detailed = client.playlist(id).toAlbumDetailed()

    override suspend fun getTrackAlbum(track: MetadataTrack): MetadataAlbum.Detailed =
        throw UnsupportedOperationException("SoundCloud tracks are not linked to albums")

    override suspend fun getAlbumTracks(id: String, pagination: PaginationStrategy?): PaginationResult<MetadataTrack> {
        val page = pagination.page()
        val (tracks, total) = client.playlistTracks(id, page.offset, page.limit)
        return page.result(tracks.map { it.toTrack() }, total)
    }

    override suspend fun savedAlbums(pagination: PaginationStrategy?): PaginationResult<MetadataAlbum.Detailed> =
        PaginationResult(emptyList(), 0, null)

    override suspend fun isSavedAlbums(ids: List<String>): List<Boolean> = ids.map { false }

    override suspend fun saveAlbums(ids: List<String>) = readOnly()

    override suspend fun removeSavedAlbums(ids: List<String>) = readOnly()
}
