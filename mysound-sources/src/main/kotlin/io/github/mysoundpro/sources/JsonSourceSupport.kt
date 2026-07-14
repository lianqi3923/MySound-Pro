package io.github.mysoundpro.sources

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** 公共 JSON 字段读取器；缺失字段返回 null，由各站点 Parser 决定是否为解析错误。 */
internal fun JsonObject.textValue(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }

internal fun JsonObject.longValue(name: String): Long? = textValue(name)?.toLongOrNull()

internal fun JsonObject.objectValue(name: String): JsonObject? = this[name] as? JsonObject

internal fun JsonObject.objectList(name: String): List<JsonObject> =
    (this[name] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
