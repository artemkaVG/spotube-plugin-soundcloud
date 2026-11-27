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

package io.github.artemkavg.spotube_plugin_soundcloud

import app.cash.zipline.Zipline
import io.github.artemkavg.soundcloud_client.SoundCloudClient
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealCoreAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataAlbumAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataArtistAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataBrowseAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataPlaylistAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataSearchAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataTrackAPI
import io.github.artemkavg.spotube_plugin_soundcloud.core.RealMetadataUserAPI
import org.spotube.plugin_interfaces.core.runPluginInitialized
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI
import org.spotube.plugin_interfaces.host_apis.HttpClientAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbumAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtistAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.browse.MetadataBrowseAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylistAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.search.MetadataSearchAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrackAPI_SERVICE_NAME
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI_SERVICE_NAME

private val zipline by lazy { Zipline.get() }

@OptIn(ExperimentalJsExport::class)
@JsExport
fun main() {
    runPluginInitialized {
        val http = zipline.take<HttpClientAPI>(HttpClientAPI_SERVICE_NAME)
        val client = SoundCloudClient(http)

        zipline.bind<CoreAPI>(CoreAPI_SERVICE_NAME, RealCoreAPI())
        zipline.bind<MetadataAlbumAPI>(MetadataAlbumAPI_SERVICE_NAME, RealMetadataAlbumAPI(client))
        zipline.bind<MetadataArtistAPI>(MetadataArtistAPI_SERVICE_NAME, RealMetadataArtistAPI(client))
        zipline.bind<MetadataBrowseAPI>(MetadataBrowseAPI_SERVICE_NAME, RealMetadataBrowseAPI(client) { console.log(it) })
        zipline.bind<MetadataPlaylistAPI>(MetadataPlaylistAPI_SERVICE_NAME, RealMetadataPlaylistAPI(client))
        zipline.bind<MetadataSearchAPI>(MetadataSearchAPI_SERVICE_NAME, RealMetadataSearchAPI(client))
        zipline.bind<MetadataTrackAPI>(MetadataTrackAPI_SERVICE_NAME, RealMetadataTrackAPI(client))
        zipline.bind<MetadataUserAPI>(MetadataUserAPI_SERVICE_NAME, RealMetadataUserAPI(client))
    }
}
