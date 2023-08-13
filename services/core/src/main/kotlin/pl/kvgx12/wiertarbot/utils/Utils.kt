@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

suspend fun Path.contentType(): String? = withContext(Dispatchers.IO) { Files.probeContentType(this@contentType) }
