package pl.kvgx12.wiertarbot.fb

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

object Constants {
    val runtimeData = Path("data")
    val attachmentSavePath = runtimeData / "saved"

    const val timeToRemoveSentMessages = 24 * 60 * 60

    init {
        attachmentSavePath.createDirectories()
    }
}
