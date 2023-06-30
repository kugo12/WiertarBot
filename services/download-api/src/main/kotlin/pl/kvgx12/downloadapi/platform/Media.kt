package pl.kvgx12.downloadapi.platform

import java.io.File

sealed interface Media {
    data class Video(val videoFile: File) : Media
    data class VideoAndAudio(val audioFile: File, val videoFile: File) : Media
}

inline fun File.asVideo() = Media.Video(this)

const val KiB = 1024
const val MiB = 1024 * KiB
