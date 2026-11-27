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

import org.spotube.plugin_interfaces.plugin_apis.metadata.common.Thumbnail

const val ARTWORK_SIZE = 500

fun upgradeArtwork(url: String?): String? = url
    ?.replace("-large.", "-t500x500.")
    ?.replace("-t300x300.", "-t500x500.")

fun thumbnails(vararg urls: String?): List<Thumbnail> =
    urls.mapNotNull { upgradeArtwork(it) }
        .filterNot { it.contains("default_avatar") }
        .distinct()
        .map { Thumbnail(it, ARTWORK_SIZE, ARTWORK_SIZE) }

fun List<Thumbnail>.orPlaceholder(seed: String?): List<Thumbnail> =
    ifEmpty { listOf(Thumbnail(placeholderArtwork(seed), ARTWORK_SIZE, ARTWORK_SIZE)) }
