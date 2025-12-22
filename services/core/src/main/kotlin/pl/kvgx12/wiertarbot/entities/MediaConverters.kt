@file:OptIn(ExperimentalSerializationApi::class)

package pl.kvgx12.wiertarbot.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.springframework.ai.content.Media
import org.springframework.core.convert.converter.Converter
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.util.MimeTypeUtils
import java.net.URI

@Serializable
data class MediaSurrogate(
    val mimeType: String = "",
    val data: ByteArray = byteArrayOf(),
    val isUrl: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaSurrogate

        if (mimeType != other.mimeType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int = 31 * mimeType.hashCode() + data.contentHashCode()
}

data class MediaWrapper(val items: List<Media>)

@WritingConverter
class MediaWrapperToByteArrayConverter : Converter<MediaWrapper, ByteArray> {
    override fun convert(source: MediaWrapper): ByteArray {
        val surrogates = source.items.map {
            val dataBytes = when (val data = it.data) {
                is ByteArray -> data
                is Resource -> data.contentAsByteArray
                is String -> data.toByteArray()
                else -> throw IllegalArgumentException("Unsupported media data type: ${data?.javaClass}")
            }

            MediaSurrogate(it.mimeType.toString(), dataBytes, it.data is String)
        }
        return Cbor.encodeToByteArray(surrogates)
    }
}

@ReadingConverter
class ByteArrayToMediaWrapperConverter : Converter<ByteArray, MediaWrapper> {
    override fun convert(source: ByteArray): MediaWrapper {
        if (source.isEmpty()) return MediaWrapper(emptyList())
        return try {
            val surrogates = Cbor.decodeFromByteArray<List<MediaSurrogate>>(source)
            val items = surrogates.map {
                if (it.isUrl) {
                    Media(MimeTypeUtils.parseMimeType(it.mimeType), URI(it.data.toString(Charsets.UTF_8)))
                } else {
                    Media(MimeTypeUtils.parseMimeType(it.mimeType), ByteArrayResource(it.data))
                }
            }
            MediaWrapper(items)
        } catch (e: Exception) {
            MediaWrapper(emptyList())
        }
    }
}
