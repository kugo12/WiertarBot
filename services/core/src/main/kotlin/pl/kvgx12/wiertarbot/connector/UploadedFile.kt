package pl.kvgx12.wiertarbot.connector

data class UploadedFile @JvmOverloads constructor(
    val id: String,
    val mimeType: String,
    val content: ByteArray? = null,
) {
    override fun toString(): String {
        return "UploadedFile(id='$id', mimeType='$mimeType')"
    }
}
