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
import io.github.artemkavg.soundcloud_client.SoundCloudSearchItem
import io.github.artemkavg.soundcloud_client.SoundCloudUser
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSupportedSearchType

class RealMetadataSearchAPI(private val client: SoundCloudClient) : MetadataSearchAPI {
    override val supportedSearchTypes: List<MetadataSupportedSearchType> = listOf(
        MetadataSupportedSearchType.ALL,
        MetadataSupportedSearchType.TRACK,
        MetadataSupportedSearchType.ARTIST,
        MetadataSupportedSearchType.ALBUM,
        MetadataSupportedSearchType.PLAYLIST,
    )

    override suspend fun search(query: String): List<MetadataSearchResult> {
        val (items, _) = client.search(query, 0, DEFAULT_LIMIT)
        val users = items.filterIsInstance<SoundCloudSearchItem.User>().map { it.data }.realArtistFirst(query).iterator()
        return items.map { if (it is SoundCloudSearchItem.User) SoundCloudSearchItem.User(users.next()) else it }
            .mapNotNull { it.toSearchResult() }
    }

    override suspend fun searchTracks(query: String, pagination: PaginationStrategy?): PaginationResult<MetadataSearchResult.Track> {
        val page = pagination.page()
        val (tracks, total) = client.searchTracks(query, page.offset, page.limit)
        return page.result(tracks.filter { it.isFull }.map { MetadataSearchResult.Track(it.toTrack()) }, total)
    }

    override suspend fun searchArtists(query: String, pagination: PaginationStrategy?): PaginationResult<MetadataSearchResult.Artist> {
        val page = pagination.page()
        val (users, total) = client.searchUsers(query, page.offset, page.limit)
        val ordered = if (page.offset == 0) users.realArtistFirst(query) else users
        return page.result(ordered.map { MetadataSearchResult.Artist(it.toArtist()) }, total)
    }

    override suspend fun searchAlbums(query: String, pagination: PaginationStrategy?): PaginationResult<MetadataSearchResult.Album> {
        val page = pagination.page()
        val (albums, total) = client.searchAlbums(query, page.offset, page.limit)
        return page.result(albums.map { MetadataSearchResult.Album(it.toAlbumBasic()) }, total)
    }

    override suspend fun searchPlaylists(query: String, pagination: PaginationStrategy?): PaginationResult<MetadataSearchResult.Playlist> {
        val page = pagination.page()
        val (playlists, total) = client.searchPlaylists(query, page.offset, page.limit)
        return page.result(playlists.map { MetadataSearchResult.Playlist(it.toPlaylist()) }, total)
    }

    override suspend fun searchUsers(query: String, pagination: PaginationStrategy?): PaginationResult<MetadataSearchResult.User> {
        val page = pagination.page()
        val (users, total) = client.searchUsers(query, page.offset, page.limit)
        return page.result(users.map { MetadataSearchResult.User(it.toUser()) }, total)
    }

    private fun List<SoundCloudUser>.realArtistFirst(query: String): List<SoundCloudUser> {
        if (size < 2) return this
        val wanted = query.normalized()
        val byName = filter { user ->
            listOfNotNull(user.username, user.fullName, user.permalink).any { it.normalized() == wanted }
        }
        val best = byName.ifEmpty { filter { it.verified } }
            .maxWithOrNull(compareBy<SoundCloudUser>({ it.verified }, { it.followersCount ?: 0 }))
            ?: return this
        return listOf(best) + filter { it !== best }
    }

    private fun String.normalized(): String = lowercase().filter { it.isLetterOrDigit() }

    private fun SoundCloudSearchItem.toSearchResult(): MetadataSearchResult? = when (this) {
        is SoundCloudSearchItem.Track -> if (data.isFull) MetadataSearchResult.Track(data.toTrack()) else null
        is SoundCloudSearchItem.User -> MetadataSearchResult.Artist(data.toArtist())
        is SoundCloudSearchItem.Playlist -> if (data.isAlbum) {
            MetadataSearchResult.Album(data.toAlbumBasic())
        } else {
            MetadataSearchResult.Playlist(data.toPlaylist())
        }
    }
}
