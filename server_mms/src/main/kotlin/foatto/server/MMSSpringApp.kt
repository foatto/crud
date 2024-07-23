package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls
import foatto.server.model.AppModuleConfig
import foatto.server.model.AppRoleConfig
import foatto.server.model.Permission
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
        addDisabledRoles(AppModule.USER, ActionType.FORM_ADD, AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_EDIT, AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_DELETE, AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_EXIT, AppRoleMMS.SUPPORT)

        appModuleConfigs[AppModuleMMS.OBJECT] = AppModuleConfig(
            caption = "Объекты",
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            permissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        put(UserRelationEnum.SELF, mutableSetOf(AppRole.USER))
                        put(UserRelationEnum.WORKER, mutableSetOf(AppRole.USER))
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        put(UserRelationEnum.SELF, mutableSetOf(AppRole.USER))
                        put(UserRelationEnum.WORKER, mutableSetOf(AppRole.USER))
                    },
                ),
                ActionType.FORM_ADD to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_EXIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        put(UserRelationEnum.SELF, mutableSetOf(AppRole.USER))
                        put(UserRelationEnum.WORKER, mutableSetOf(AppRole.USER))
                    },
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )
    }

}
