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
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI

class RealMetadataTrackAPI(private val client: SoundCloudClient) : MetadataTrackAPI {
    override suspend fun getTrack(id: String): MetadataTrack = client.track(id).toTrack()

    override suspend fun savedTracks(pagination: PaginationStrategy?): PaginationResult<MetadataTrack> =
        PaginationResult(emptyList(), 0, null)

    override suspend fun isSavedTracks(ids: List<String>): List<Boolean> = ids.map { false }

    override suspend fun saveTracks(ids: List<String>) = readOnly()

    override suspend fun removeSavedTracks(ids: List<String>) = readOnly()

    override suspend fun recommendationsBasedOnTracks(seedTrackIds: List<String>, limit: Int): List<MetadataTrack> {
        val seed = seedTrackIds.firstOrNull() ?: return emptyList()
        val related = client.relatedTracks(seed, limit)
        return related.filter { it.isFull }.map { it.toTrack() }
    }
}

fun readOnly(): Nothing =
    throw UnsupportedOperationException("The SoundCloud plugin works without an account and is read-only")
