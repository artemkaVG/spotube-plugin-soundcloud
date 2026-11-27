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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI

class SoundCloudClient(http: HttpClientAPI) {
    private val http = SoundCloudHttpClient(http)

    suspend fun track(id: String): SoundCloudTrack = http.get("tracks/$id").toSoundCloudTrack()

    suspend fun user(id: String): SoundCloudUser = http.get("users/$id").toSoundCloudUser()

    suspend fun playlist(id: String): SoundCloudPlaylist =
        if (id.isNotEmpty() && id.all { it.isDigit() }) {
            http.get("playlists/$id", mapOf("representation" to "full"))
        } else {
            http.get("resolve", mapOf("url" to id))
        }.toSoundCloudPlaylist()

    suspend fun playlistTracks(id: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudTrack> {
        val tracks = playlist(id).tracks
        return SoundCloudCollection(hydrateTracks(tracks.drop(offset).take(limit)), tracks.size)
    }

    suspend fun hydrateTracks(tracks: List<SoundCloudTrack>): List<SoundCloudTrack> {
        val missing = tracks.filterNot { it.isFull }.map { it.id }.filter { it.isNotEmpty() }.distinct()
        if (missing.isEmpty()) return tracks

        val loaded = HashMap<String, SoundCloudTrack>()
        for (chunk in missing.chunked(TRACK_IDS_PER_REQUEST)) {
            val response = runRequest { http.get("tracks", mapOf("ids" to chunk.joinToString(","))) }.getOrNull()
            (response as? JsonArray)?.forEach { item ->
                val track = item.toSoundCloudTrack()
                if (track.id.isNotEmpty()) loaded[track.id] = track
            }
        }
        return tracks.mapNotNull { if (it.isFull) it else loaded[it.id] }
    }

    suspend fun search(query: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudSearchItem> =
        collection("search", offset, limit, mapOf("q" to query)) { it.toSoundCloudSearchItem() }

    suspend fun searchTracks(query: String, offset: Int = 0, limit: Int = 20): SoundCloudCollection<SoundCloudTrack> =
        collection("search/tracks", offset, limit, mapOf("q" to query)) { it.toSoundCloudTrack() }

    suspend fun searchUsers(query: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudUser> =
        collection("search/users", offset, limit, mapOf("q" to query)) { it.toSoundCloudUser() }

    suspend fun searchAlbums(query: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudPlaylist> =
        collection("search/albums", offset, limit, mapOf("q" to query)) { it.toSoundCloudPlaylist() }

    suspend fun searchPlaylists(query: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudPlaylist> =
        collection("search/playlists_without_albums", offset, limit, mapOf("q" to query)) { it.toSoundCloudPlaylist() }

    suspend fun topTracks(userId: String, limit: Int): SoundCloudCollection<SoundCloudTrack> =
        collection("users/$userId/toptracks", 0, limit) { it.toSoundCloudTrack() }

    suspend fun relatedArtists(userId: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudUser> =
        collection("users/$userId/relatedartists", offset, limit) { it.toSoundCloudUser() }

    suspend fun userPlaylists(userId: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudPlaylist> =
        collection("users/$userId/playlists_without_albums", offset, limit) { it.toSoundCloudPlaylist() }

    suspend fun userAlbums(userId: String, offset: Int, limit: Int): SoundCloudCollection<SoundCloudPlaylist> =
        collection("users/$userId/albums", offset, limit) { it.toSoundCloudPlaylist() }

    suspend fun relatedTracks(trackId: String, limit: Int): List<SoundCloudTrack> =
        http.get("tracks/$trackId/related", mapOf("limit" to limit.toString())).collection().map { it.toSoundCloudTrack() }

    suspend fun chartSelections(): List<SoundCloudSelection> =
        http.get("charts/selections").toSoundCloudSelections("charts/selections")

    suspend fun discoverySelections(): List<SoundCloudSelection> =
        http.get("mixed-selections", mapOf("limit" to "10", "offset" to "0", "linked_partitioning" to "1"))
            .toSoundCloudSelections("mixed-selections")

    suspend fun resolveStream(transcoding: SoundCloudTranscoding, authorization: String?): SoundCloudStream? {
        val url = transcoding.url ?: return null
        val response = http.get(url, mapOf("track_authorization" to authorization))
        return SoundCloudStream(response.str("url"), response.str("licenseAuthToken") != null)
    }

    private suspend fun <T : Any> collection(
        path: String,
        offset: Int,
        limit: Int,
        query: Map<String, String?> = emptyMap(),
        decode: (JsonElement) -> T?,
    ): SoundCloudCollection<T> {
        val response = http.get(
            path,
            query + mapOf("offset" to offset.toString(), "limit" to limit.toString(), "linked_partitioning" to "1"),
        )
        return SoundCloudCollection(response.collection().mapNotNull(decode), response.int("total_results"))
    }

    private companion object {
        const val TRACK_IDS_PER_REQUEST = 50
    }
}
