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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal val JsonElement?.obj: JsonObject? get() = this as? JsonObject

internal fun JsonElement?.arr(key: String): List<JsonElement> = (obj?.get(key) as? JsonArray).orEmpty()

internal fun JsonElement?.child(key: String): JsonObject? = obj?.get(key) as? JsonObject

internal fun JsonElement?.str(key: String): String? {
    val value = obj?.get(key) as? JsonPrimitive ?: return null
    if (value is JsonNull) return null
    return value.content.takeIf { it.isNotBlank() }
}

internal fun JsonElement?.long(key: String): Long? = (obj?.get(key) as? JsonPrimitive)?.longOrNull

internal fun JsonElement?.int(key: String): Int? = (obj?.get(key) as? JsonPrimitive)?.intOrNull

internal fun JsonElement?.bool(key: String): Boolean? = (obj?.get(key) as? JsonPrimitive)?.booleanOrNull

internal fun JsonElement?.collection(): List<JsonElement> = arr("collection")
