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
import io.github.artemkavg.soundcloud_client.SoundCloudPlaylist
import io.github.artemkavg.soundcloud_client.SoundCloudSelection
import io.github.artemkavg.soundcloud_client.SoundCloudTrack
import io.github.artemkavg.soundcloud_client.runRequest
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseGenre
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseItem
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseSection
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy

class RealMetadataBrowseAPI(
    private val client: SoundCloudClient,
    private val log: (String) -> Unit = {},
) : MetadataBrowseAPI {

    private class Chart(val kind: String, val genre: String, val genreName: String, val playlist: SoundCloudPlaylist)

    private class Home(val charts: List<Chart>, val discover: List<SoundCloudSelection>, val selections: List<SoundCloudSelection>)

    private var cachedHome: Home? = null

    private suspend fun home(): Home = cachedHome ?: loadHome().also { home ->
        if (home.charts.isNotEmpty() || home.discover.isNotEmpty() || home.selections.isNotEmpty()) cachedHome = home
    }

    private suspend fun loadHome(): Home {
        val chartResult = runRequest { client.chartSelections().exposed() }
        val discoveryResult = runRequest {
            client.discoverySelections().exposed()
        }
        if (chartResult.isFailure && discoveryResult.isFailure) chartResult.getOrThrow()
        val selections = chartResult.getOrDefault(emptyList())
        val discover = discoveryResult.getOrDefault(emptyList())

        val chartSelections = selections.filter { it.isCharts() }
        val usCharts = chartSelections.firstOrNull { it.title.trim().endsWith(" US", ignoreCase = true) }
            ?: chartSelections.firstOrNull()
        val charts = usCharts?.playlists.orEmpty().mapNotNull { it.toChart() }.distinctBy { it.kind to it.genre }
        log(
            "[SoundCloud] selections: " + (selections + discover).joinToString(" | ") { s ->
                s.title + " -> " + s.playlists.joinToString(", ") { it.permalinkUrl?.substringAfterLast('/') ?: "?" }
            } + " ; charts: " + charts.joinToString(", ") { "${it.kind}/${it.genre}" }
        )
        return Home(charts, discover, selections.filterNot { it.isCharts() })
    }

    private fun List<SoundCloudSelection>.exposed(): List<SoundCloudSelection> = mapNotNull { selection ->
        val playlists = selection.playlists.filter { playlist ->
            playlist.title != null && playlist.permalinkUrl != null &&
                !playlist.title.orEmpty().trim().equals("Artist Pro", ignoreCase = true)
        }
        if (playlists.isEmpty()) null else selection.copy(playlists = playlists)
    }

    private fun SoundCloudSelection.isCharts(): Boolean = title.contains("chart", ignoreCase = true)

    private fun SoundCloudPlaylist.toChart(): Chart? {
        val slug = permalinkUrl?.substringBefore('?')?.trimEnd('/')?.substringAfterLast('/')
            ?.takeIf { it.isNotEmpty() } ?: return null
        val chartTitle = title ?: slug
        return when (slug) {
            "new-hot" -> Chart("trending", ALL_MUSIC, chartTitle, this)
            "all-music-genres" -> Chart("top", ALL_MUSIC, chartTitle, this)
            else -> Chart("top", slug, chartTitle, this)
        }
    }

    private fun SoundCloudPlaylist.toBrowseItem(): MetadataBrowseItem =
        if (isAlbum) MetadataBrowseItem.Album(toAlbumBasic()) else MetadataBrowseItem.Playlist(toPlaylist())

    private suspend fun chartTracks(chart: Chart, limit: Int): List<SoundCloudTrack> =
        runRequest {
            client.playlistTracks(chart.playlist.key, 0, limit).items
        }.getOrDefault(emptyList())

    private fun kindTitle(kind: String): String = when (kind) {
        "trending" -> "New & Hot"
        "top" -> "Top 50"
        else -> kind.replaceFirstChar { it.uppercase() }
    }

    private fun genreOf(home: Home, genreId: String): String =
        if (genreId == ALL_GENRE) home.charts.firstOrNull { it.genre == ALL_MUSIC }?.genre ?: ALL_MUSIC else genreId

    override suspend fun featured(): List<MetadataBrowseItem> {
        val home = home()
        val chart = home.charts.firstOrNull { it.kind == "trending" && it.genre == ALL_MUSIC }
            ?: home.charts.firstOrNull { it.genre == ALL_MUSIC }
            ?: return (home.discover + home.selections).firstOrNull()?.playlists.orEmpty().map { it.toBrowseItem() }
        return chartTracks(chart, SECTION_SIZE).map { MetadataBrowseItem.Track(it.toTrack()) }
    }

    override suspend fun genres(): List<MetadataBrowseGenre> {
        val home = home()
        return listOf(MetadataBrowseGenre(ALL_GENRE, "All")) + home.charts
            .filter { it.genre != ALL_MUSIC }
            .distinctBy { it.genre }
            .map { MetadataBrowseGenre(it.genre, it.genreName) }
    }

    override suspend fun list(genreId: String, pagination: PaginationStrategy?): PaginationResult<MetadataBrowseSection> {
        val page = pagination.page()
        val all = sections(genreId, home(), SECTION_SIZE)
        return page.result(all.drop(page.offset).take(page.limit), all.size)
    }

    override suspend fun sublist(genreId: String, sectionId: String, pagination: PaginationStrategy?): PaginationResult<MetadataBrowseItem> {
        val page = pagination.page()
        val items = sections(genreId, home(), FULL_SECTION_SIZE).firstOrNull { it.moreLink == sectionId }?.items.orEmpty()
        return page.result(items.drop(page.offset).take(page.limit), items.size)
    }

    private suspend fun sections(genreId: String, home: Home, size: Int): List<MetadataBrowseSection> {
        val genre = genreOf(home, genreId)
        val genreCharts = home.charts.filter { it.genre == genre }.sortedBy { if (it.kind == "trending") 0 else 1 }

        val trackSections = genreCharts.map { chart ->
            val tracks = chartTracks(chart, size)
            chart to tracks
        }.filter { it.second.isNotEmpty() }

        val result = mutableListOf<MetadataBrowseSection>()
        fun trackSection(chart: Chart, tracks: List<SoundCloudTrack>) = MetadataBrowseSection(
            title = kindTitle(chart.kind),
            items = tracks.map { MetadataBrowseItem.Track(it.toTrack()) },
            moreLink = "$genre/tracks/${chart.kind}",
        )

        val (later, first) = trackSections.partition { (chart, _) -> genreId == ALL_GENRE && chart.kind == "trending" }
        first.forEach { (chart, tracks) -> result += trackSection(chart, tracks) }

        val artists = trackSections.flatMap { it.second }.mapNotNull { it.user }.distinctBy { it.id }.take(size)
        if (artists.isNotEmpty()) {
            result += MetadataBrowseSection(
                title = "Popular artists",
                items = artists.map { MetadataBrowseItem.Artist(it.toArtist()) },
                moreLink = "$genre/artists",
            )
        }

        if (genreId == ALL_GENRE) home.discover.forEach { result += it.toSection() }
        later.forEach { (chart, tracks) -> result += trackSection(chart, tracks) }

        val chartPlaylists = if (genreId == ALL_GENRE) home.charts else genreCharts
        if (chartPlaylists.isNotEmpty()) {
            result += MetadataBrowseSection(
                title = "Charts",
                items = chartPlaylists.map { it.playlist.toBrowseItem() },
                moreLink = "$genre/charts",
            )
        }

        if (genreId == ALL_GENRE) home.selections.forEach { result += it.toSection() }
        return result
    }

    private fun SoundCloudSelection.toSection() = MetadataBrowseSection(
        title = title,
        description = description,
        items = playlists.map { it.toBrowseItem() },
        moreLink = key,
    )

    private companion object {
        const val ALL_GENRE = "all"
        const val ALL_MUSIC = "all-music"
        const val SECTION_SIZE = 20
        const val FULL_SECTION_SIZE = 50
    }
}
