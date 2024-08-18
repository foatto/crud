package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls
import foatto.server.model.AppModuleConfig
import foatto.server.model.AppRoleConfig
import foatto.server.model.Permission
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication(
    scanBasePackages = ["foatto.server"],
)
@EnableWebMvc
class MMSSpringApp : SpringApp() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<MMSSpringApp>(*args)
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
        //--- add disabled permissions for SUPPORT role to USER module
        appModuleConfigs[AppModule.USER]?.disabledFormAddRoles?.add(AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_EDIT, AppRoleMMS.SUPPORT)
        addDisabledRoles(AppModule.USER, ActionType.FORM_DELETE, AppRoleMMS.SUPPORT)

        appModuleConfigs[AppModuleMMS.OBJECT] = AppModuleConfig(
            caption = "Объекты",
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
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
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
            ),
        )
    }

}
