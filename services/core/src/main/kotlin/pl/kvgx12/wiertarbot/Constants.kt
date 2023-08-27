package pl.kvgx12.wiertarbot

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

object Constants {
    val runtimeData = Path("data")
    val commandMediaPath = runtimeData / "media"

    const val imageEditTimeout = 5 * 60
}
