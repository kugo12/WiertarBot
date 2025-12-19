package pl.kvgx12.wiertarbot.fb.config

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.fb.connector.*
import pl.kvgx12.wiertarbot.fb.services.PermissionDecoderService
import pl.kvgx12.wiertarbot.fb.services.PermissionService

class BeansRegistrar : BeanRegistrarDsl({
    registerBean<FBConnector>()
    registerBean<FBContext>()
    registerBean<FBEventConsumer>()
    registerBean<FBMilestoneTracker>()
    registerBean<FBMessageService>()

    registerBean<PermissionDecoderService>()
    registerBean<PermissionService>()

    registerBean {
        val props = bean<FBProperties>()

        runBlocking {
            Session(loadCookies(props.cookiesFile))
        }
    }
})
