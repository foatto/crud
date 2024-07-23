package foatto.server

import foatto.core.ActionType
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls
import foatto.server.model.AppModuleConfig
import foatto.server.model.AppRoleConfig
import foatto.server.model.SessionData
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.util.concurrent.ConcurrentHashMap

@SpringBootApplication
@EnableWebMvc
class MMSSpringApp {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<MMSSpringApp>(*args)
        }

        private val sessionDataStore = ConcurrentHashMap<Long, SessionData>()

        fun getSessionData(sessionId: Long): SessionData = sessionDataStore.getOrPut(sessionId) { SessionData() }
        fun putSessionData(sessionId: Long, sessionData: SessionData) {
            sessionDataStore[sessionId] = sessionData
        }

        fun removeSessionData(sessionId: Long) {
            sessionDataStore.remove(sessionId)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        addAppModuleUrls()
        initAppRoleConfigs()
        initAppModuleConfigs()
    }

    private fun initAppRoleConfigs() {
        appRoleConfigs[AppRoleMMS.SUPPORT] = AppRoleConfig(
            redirectOnLogon = null,
        )

        appRoleConfigs[AppRoleMMS.USER_FIXED_OBJECTS] = AppRoleConfig(
            redirectOnLogon = null,
        )
        appRoleConfigs[AppRoleMMS.USER_MOBILE_OBJECTS] = AppRoleConfig(
            redirectOnLogon = null,
        )
    }

    private fun initAppModuleConfigs() {
        appModuleConfigs[AppModuleMMS.OBJECT] = AppModuleConfig(
            caption = "Объекты",
            permissions = mapOf(
                ActionType.MODULE_TABLE to setOf(AppRole.ADMIN, AppRoleMMS.SUPPORT, AppRole.USER),
                ActionType.MODULE_FORM to setOf(AppRole.ADMIN, AppRoleMMS.SUPPORT, AppRole.USER),
                ActionType.FORM_ADD to setOf(AppRole.ADMIN, AppRoleMMS.SUPPORT),
                ActionType.FORM_EDIT to setOf(AppRole.ADMIN, AppRoleMMS.SUPPORT),
                ActionType.FORM_DELETE to setOf(AppRole.ADMIN, AppRoleMMS.SUPPORT),
                ActionType.FORM_EXIT to setOf(AppRole.ADMIN, AppRoleMMS.SUPPORT, AppRole.USER),
            )
        )

    }

}
