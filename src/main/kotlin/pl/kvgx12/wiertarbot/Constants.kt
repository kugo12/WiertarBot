package pl.kvgx12.wiertarbot

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

object Constants {
    val runtimeData = Path("data")
    val uploadSavePath = runtimeData / "upload"
    val attachmentSavePath = runtimeData / "saved"
    val commandMediaPath = runtimeData / "media"
    val cookiePath = runtimeData / "cookies.json"

    const val imageEditTimeout = 5 * 60
    const val timeToRemoveSentMessages = 24 * 60 * 60

    init {
        listOf(uploadSavePath, attachmentSavePath).forEach {
            it.createDirectories()
        }
    }
}