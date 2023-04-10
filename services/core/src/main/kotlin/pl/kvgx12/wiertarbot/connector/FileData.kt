package pl.kvgx12.wiertarbot.connector

data class FileData(
    val uri: String,
    val content: ByteArray,
    val mediaType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileData

        if (uri != other.uri) return false
        if (!content.contentEquals(other.content)) return false
        if (mediaType != other.mediaType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + mediaType.hashCode()
        return result
    }
}
