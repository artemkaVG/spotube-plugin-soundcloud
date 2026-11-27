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

import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationResult
import org.spotube.plugin_interfaces.plugin_apis.metadata.common.PaginationStrategy

const val DEFAULT_LIMIT = 20
private const val TRACK_IDS_PER_REQUEST = 50

data class Page(val offset: Int, val limit: Int)

fun PaginationStrategy?.page(defaultLimit: Int = DEFAULT_LIMIT): Page = when (this) {
    is PaginationStrategy.Offset -> Page(offset.coerceAtLeast(0), limit.takeIf { it > 0 } ?: defaultLimit)
    is PaginationStrategy.Page -> Page((page.coerceAtLeast(0)) * pageSize, pageSize.takeIf { it > 0 } ?: defaultLimit)
    is PaginationStrategy.Cursor -> Page(cursor?.toIntOrNull() ?: 0, limit.takeIf { it > 0 } ?: defaultLimit)
    is PaginationStrategy.Continuation -> Page(continuationToken?.toIntOrNull() ?: 0, defaultLimit)
    null -> Page(0, defaultLimit)
}

fun <T> emptyPage(): PaginationResult<T> = PaginationResult(emptyList(), 0, null)

fun <T> Page.result(items: List<T>, total: Int? = null): PaginationResult<T> {
    val hasMore = if (total != null) offset + items.size < total else items.size >= limit
    return PaginationResult(
        items = items,
        totalCount = total ?: (offset + items.size + if (hasMore) 1 else 0),
        nextPagination = if (hasMore && items.isNotEmpty()) PaginationStrategy.Offset(offset + items.size, limit) else null,
    )
}
