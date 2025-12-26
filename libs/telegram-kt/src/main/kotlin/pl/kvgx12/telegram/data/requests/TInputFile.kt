package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.Serializable

sealed interface TInputFile {
    @Serializable
    @JvmInline
    value class UrlOrId(val file: String) : TInputFile


    data class Upload(val fileData: ByteArray, val fileName: String) : TInputFile {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Upload

            if (!fileData.contentEquals(other.fileData)) return false
            if (fileName != other.fileName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fileData.contentHashCode()
            result = 31 * result + fileName.hashCode()
            return result
        }
    }
}
