package pl.kvgx12.telegram

class TelegramApiException(val errorCode: Int?, val description: String) : Exception(errorCode?.let { "$it - " }.orEmpty() + description)
