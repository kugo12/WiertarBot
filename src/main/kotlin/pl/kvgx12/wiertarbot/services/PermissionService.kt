package pl.kvgx12.wiertarbot.services

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import pl.kvgx12.wiertarbot.entities.Permission
import pl.kvgx12.wiertarbot.repositories.PermissionRepository

typealias PermissionList = Map<String, JsonElement>

class PermissionService(
    private val permissionRepository: PermissionRepository,
    private val permissionDecoderService: PermissionDecoderService,
) {
    fun initPermissionByCommand(command: String) {
        permissionRepository.findFirstByCommand(command)
            ?: Permission(command = command, whitelist = """{"*": 0}""", blacklist = "{}")
                .let { permissionRepository.saveAndFlush(it) }
    }

    fun edit(
        command: String,
        uids: List<String>,
        bl: Boolean = false,
        add: Boolean = true,
        threadId: String? = null
    ): Boolean {
        val permission = permissionRepository.findFirstByCommand(command) ?: when {
            add -> Permission(command = command, whitelist = "{}", blacklist = "{}")
            else -> return false
        }

        val (whitelist, blacklist) = with(permissionDecoderService) {
            permission.decodeLists()
        }

        val currentlyEdited = (if (bl) blacklist else whitelist).toMutableMap()

        for (uid in uids) {
            if (uid.toIntOrNull() == null && uid != "*")
                continue

            val userId = JsonPrimitive(uid)

            if (add) {
                if (threadId != null) {
                    currentlyEdited[threadId] =
                        if (threadId in currentlyEdited)
                            JsonArray(currentlyEdited[threadId]!!.jsonArray + userId)
                        else
                            JsonArray(listOf(userId))
                } else {
                    currentlyEdited[uid] = JsonPrimitive(0)
                }
            } else {
                if (threadId != null && threadId in currentlyEdited) {
                    val isEmpty: Boolean
                    currentlyEdited[threadId] = JsonArray(currentlyEdited[threadId]!!.jsonArray.filter {
                        it.jsonPrimitive != userId
                    }.also {
                        isEmpty = it.isEmpty()
                    })
                    if (isEmpty)
                        currentlyEdited.remove(threadId)
                } else {
                    currentlyEdited.remove(uid)
                }
            }
        }

        permission.whitelist = Json.encodeToString(whitelist)
        permission.blacklist = Json.encodeToString(blacklist)
        permissionRepository.saveAndFlush(permission)

        return true
    }

    fun isAuthorized(command: String, threadId: String, userId: String): Boolean {  // TODO: rbac -_-
        val (whitelist, blacklist) = permissionDecoderService.getListsByCommand(command) ?: return false

        return when {
            "*" in blacklist -> when {
                userId != threadId && threadId in whitelist -> when {
                    userId in whitelist[threadId] -> true
                    "*" in whitelist[threadId] ->
                        !(userId in blacklist || (threadId in blacklist && userId in blacklist[threadId]))

                    else -> false
                }

                userId in whitelist ->
                    !(userId != threadId && threadId in blacklist &&
                            (userId in blacklist[threadId] || "*" in blacklist[threadId]))

                else -> false
            }

            "*" in whitelist -> when {
                userId != threadId && threadId in blacklist -> when {
                    "*" in blacklist[threadId] ->
                        userId in whitelist || (threadId in whitelist && userId in whitelist[threadId])

                    userId in blacklist[threadId] -> false
                    else -> true
                }

                userId in blacklist -> when {
                    userId != threadId && threadId in whitelist -> when {
                        userId in whitelist[threadId] -> true
                        "*" in whitelist[threadId] -> true
                        else -> false
                    }

                    else -> false
                }

                else -> true
            }

            userId != threadId && threadId in whitelist -> when {
                "*" in whitelist[threadId] -> threadId !in blacklist && userId !in blacklist[threadId]
                userId in whitelist[threadId] -> true
                else -> userId in whitelist && (whitelist[userId] as? JsonPrimitive)?.intOrNull == 0
            }

            userId in whitelist && (whitelist[userId] as? JsonPrimitive)?.intOrNull == 0 -> true
            else -> false
        }
    }

    private inline operator fun JsonElement?.contains(key: String) =
        (this as? JsonArray)?.contains(JsonPrimitive(key)) == true
}