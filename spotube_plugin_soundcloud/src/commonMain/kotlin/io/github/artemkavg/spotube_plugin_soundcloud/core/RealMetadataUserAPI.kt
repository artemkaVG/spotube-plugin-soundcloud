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
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUserAPI

class RealMetadataUserAPI(private val client: SoundCloudClient) : MetadataUserAPI {
    override suspend fun getUser(id: String): MetadataUser? =
        if (id.isNotEmpty() && id.all { it.isDigit() }) runRequest { client.user(id).toUser() }.getOrNull() else null
}
