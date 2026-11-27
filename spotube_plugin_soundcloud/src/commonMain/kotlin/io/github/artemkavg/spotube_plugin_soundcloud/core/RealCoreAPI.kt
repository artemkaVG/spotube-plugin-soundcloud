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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.swiftzer.semver.SemVer
import org.spotube.plugin_interfaces.plugin_apis.core.CoreAPI
import org.spotube.plugin_interfaces.plugin_apis.core.PluginUpdateInfo

class RealCoreAPI : CoreAPI {
    override suspend fun checkPluginUpdates(currentVersion: SemVer): PluginUpdateInfo? = null

    override fun supportMarkdownText(currentVersion: SemVer): String = ""

    override val requiresAuthentication = false

    override val loggedInFlow: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override suspend fun login() {}

    override suspend fun logout() {}
}
