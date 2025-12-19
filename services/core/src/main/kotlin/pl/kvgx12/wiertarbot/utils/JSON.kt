package pl.kvgx12.wiertarbot.utils

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

fun StringBuilder.appendJsonSchema(descriptor: SerialDescriptor) {
    append("{\"type\": \"object\", \"properties\": {")

    for (index in 0 until descriptor.elementsCount) {
        if (index > 0) append(", ")
        val name = descriptor.getElementName(index)
        val elementDescriptor = descriptor.getElementDescriptor(index)
        append("\"$name\": ")
        appendJsonType(elementDescriptor)
    }

    append("}, \"required\": [")

    var first = true
    for (index in 0 until descriptor.elementsCount) {
        if (!descriptor.isElementOptional(index)) {
            if (!first) append(", ")
            first = false
            append("\"${descriptor.getElementName(index)}\"")
        }
    }

    append("]}")
}

fun StringBuilder.appendJsonType(descriptor: SerialDescriptor) {
    when (descriptor.kind) {
        PrimitiveKind.STRING -> append("{\"type\": \"string\"}")
        PrimitiveKind.INT,
        PrimitiveKind.LONG,
        PrimitiveKind.SHORT,
        PrimitiveKind.BYTE -> append("{\"type\": \"integer\"}")

        PrimitiveKind.FLOAT,
        PrimitiveKind.DOUBLE -> append("{\"type\": \"number\"}")

        PrimitiveKind.BOOLEAN -> append("{\"type\": \"boolean\"}")
        StructureKind.LIST -> {
            val itemDescriptor = descriptor.getElementDescriptor(0)
            append("{\"type\": \"array\", \"items\": ")
            appendJsonType(itemDescriptor)
            append("}")
        }

        StructureKind.CLASS,
        StructureKind.OBJECT -> appendJsonSchema(descriptor)

        else -> append("{\"type\": \"string\"}")
    }
}
