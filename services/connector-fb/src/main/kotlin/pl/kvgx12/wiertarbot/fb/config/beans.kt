package pl.kvgx12.wiertarbot.fb.config

import kotlinx.coroutines.runBlocking
import org.springframework.context.support.beans
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.connector.connectorBeans
import pl.kvgx12.wiertarbot.fb.commands.delegatedCommandsBeans
import pl.kvgx12.wiertarbot.fb.connector.*
import pl.kvgx12.wiertarbot.fb.services.PermissionDecoderService
import pl.kvgx12.wiertarbot.fb.services.PermissionService

fun beans() = beans {
    bean<FBConnector>()
    bean<FBContext>()
    bean<FBEventConsumer>()
    bean<FBMilestoneTracker>()
    bean<FBMessageService>()

    bean<PermissionDecoderService>()
    bean<PermissionService>()

    bean {
        val props = ref<FBProperties>()

        runBlocking {
            Session(loadCookies(props.cookiesFile))
        }
    }

    connectorBeans()
    delegatedCommandsBeans()
}
