package io.cyborgsquirrel.test_helpers

import io.micronaut.serde.ObjectMapper

fun objectToMap(objectMapper: ObjectMapper, obj: Any): Map<String, Any> {
    val jsonNode = objectMapper.writeValueToTree(obj)
    return jsonNode.entries().mapNotNull { entry -> entry.value.value?.let { entry.key to it } }.toMap()
}