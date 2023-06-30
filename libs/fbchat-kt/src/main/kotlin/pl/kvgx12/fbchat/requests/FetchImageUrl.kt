package pl.kvgx12.fbchat.requests

import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.fbchat.utils.SessionUtils.getJsmodsRequire
import pl.kvgx12.fbchat.utils.tryAsArray
import pl.kvgx12.fbchat.utils.tryAsString
import pl.kvgx12.fbchat.utils.tryGet

suspend fun Session.fetchImageUrl(imageId: String): String {
    val response = this.postBasic(
        "/mercury/attachments/photo/",
        mapOf(
            "photo_id" to imageId,
        ),
    ).let(Session.json::parseToJsonElement)

    return response
        .tryGet("jsmods")
        .tryGet("require")
        .tryAsArray()
        ?.getJsmodsRequire()
        ?.get("ServerRedirect.redirectPageTo")
        ?.firstOrNull()
        ?.tryAsString()!!
}
