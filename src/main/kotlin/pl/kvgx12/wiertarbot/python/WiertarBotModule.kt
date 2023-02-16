package pl.kvgx12.wiertarbot.python

import jep.python.PyObject
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import pl.kvgx12.wiertarbot.repositories.MessageCountMilestoneRepository
import pl.kvgx12.wiertarbot.repositories.PermissionRepository

@Suppress("FunctionName")
sealed interface WiertarBotModule {
    fun init_repositories(
        permissionRepository: PermissionRepository,
        fbMessageRepository: FBMessageRepository,
        milestoneRepository: MessageCountMilestoneRepository
    )

    fun init_dispatcher(): PyObject
}