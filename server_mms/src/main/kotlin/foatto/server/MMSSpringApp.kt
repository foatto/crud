package foatto.server

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.model.response.MenuData
import foatto.core_mms.AppModuleMMS
import foatto.core_mms.addAppModuleUrls
import foatto.server.model.AppModuleConfig
import foatto.server.model.AppRoleConfig
import foatto.server.model.Permission
import foatto.server.util.AdvancedLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener

@SpringBootApplication(
    scanBasePackages = ["foatto.server"],
)
class MMSSpringApp : SpringApp() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<MMSSpringApp>(*args)
        }
    }

    @Value("\${log_dir}")
    val logDir: String = ""

    @Value("\${log_options}")
    val logOptions: String = ""

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()

        AdvancedLogger.init(logDir, logOptions.contains("error"), logOptions.contains("info"), logOptions.contains("debug"))
        AdvancedLogger.info("==================== MMSSpringApp started ====================")

        addAppModuleUrls()
        //--- строго после addAppModuleUrls
        initAppRoleConfigs()
        initAppModuleConfigs()
        initMenuInit()
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
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
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

        appModuleConfigs[AppModuleMMS.OBJECT_LIST] = AppModuleConfig(
            caption = "Список объектов",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.DEPARTMENT] = AppModuleConfig(
            caption = "Подразделения",
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.GROUP] = AppModuleConfig(
            caption = "Группы",
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_EDIT to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.FORM_DELETE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                    },
                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.SENSOR] = AppModuleConfig(
            caption = "Датчики",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.MODULE_FORM to Permission(
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
            ),
        )
        appModuleConfigs[AppModuleMMS.SENSOR_CALIBRATION] = AppModuleConfig(
            caption = "Тарировка датчика",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
                ActionType.MODULE_FORM to Permission(
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
            ),
        )

        appModuleConfigs[AppModuleMMS.OBJECT_DATA] = AppModuleConfig(
            caption = "Данные по объекту",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
//            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
//            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
//                ActionType.MODULE_FORM to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_EDIT to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_DELETE to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
            ),
        )
        appModuleConfigs[AppModuleMMS.SENSOR_DATA] = AppModuleConfig(
            caption = "Данные по датчику",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
//            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
//            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
                    disabledRoles = getRoleAllPermissions(AppRole.USER),
                ),
//                ActionType.MODULE_FORM to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_EDIT to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
//                ActionType.FORM_DELETE to Permission(
//                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
//                    disabledRoles = getRoleAllPermissions(AppRole.USER),
//                ),
            ),
        )

        appModuleConfigs[AppModuleMMS.DEVICE] = AppModuleConfig(
            caption = "Контроллеры",
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN),
            disabledAccessRoles = mutableSetOf(AppRole.USER),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
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

        appModuleConfigs[AppModuleMMS.DAY_WORK] = AppModuleConfig(
            caption = "Журнал суточных работ",
            pageSize = AppModuleConfig.DEFAULT_PAGE_SIZE,
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN),
            disabledFormAddRoles = mutableSetOf(AppRole.USER),
            rowPermissions = mutableMapOf(
                ActionType.MODULE_TABLE to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN).apply {
                        getOrPut(UserRelationEnum.SELF) { mutableSetOf() } += AppRole.USER
                        getOrPut(UserRelationEnum.WORKER) { mutableSetOf() } += AppRole.USER
                    },
                ),
                ActionType.MODULE_FORM to Permission(
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
            ),
        )

        appModuleConfigs[AppModuleMMS.CHART_ANALOGUE_BY_TYPE] = AppModuleConfig(
            caption = "Графики по типам датчиков",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.CHART_LIQUID_LEVEL] = AppModuleConfig(
            caption = "Графики уровня топлива",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_FIXED_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.MAP_TRACE] = AppModuleConfig(
            caption = "Траектория на карте",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRoleMMS.USER_MOBILE_OBJECTS),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE] = AppModuleConfig(
            caption = "Аналоговый индикатор датчика объекта",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.SCHEME_COUNTER_INDICATOR_STATE] = AppModuleConfig(
            caption = "Индикатор счётного датчика объекта",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.SCHEME_WORK_INDICATOR_STATE] = AppModuleConfig(
            caption = "Индикатор датчика работы объекта",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD] = AppModuleConfig(
            caption = "Контрольная панель объекта: все датчики",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )
        appModuleConfigs[AppModuleMMS.COMPOSITE_OBJECT_LIST_DASHBOARD] = AppModuleConfig(
            caption = "Контрольная панель объектов: все датчики",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
        )

        appModuleConfigs[AppModuleMMS.REPORT_SUMMARY] = AppModuleConfig(
            caption = "Суммарный отчёт",
            enabledAccessRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledAccessRoles = mutableSetOf(),
            enabledFormAddRoles = mutableSetOf(AppRole.ADMIN, AppRole.USER),
            disabledFormAddRoles = mutableSetOf(),
            rowPermissions = mutableMapOf(
////                ActionType.MODULE_TABLE to Permission(
////                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
////                    disabledRoles = getRoleAllPermissions(AppRole.USER),
////                ),
                ActionType.MODULE_FORM to Permission(
                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN, AppRole.USER),
//                    disabledRoles = getRoleAllPermissions(),
                ),
////                ActionType.FORM_EDIT to Permission(
////                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
////                    disabledRoles = getRoleAllPermissions(AppRole.USER),
////                ),
////                ActionType.FORM_DELETE to Permission(
////                    enabledRoles = getRoleAllPermissions(AppRole.ADMIN),
////                    disabledRoles = getRoleAllPermissions(AppRole.USER),
////                ),
            ),
        )

    }

    private fun initMenuInit() {
        menuInit = { serverUserConfig ->
            val alMenu = mutableListOf<MenuData>()

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.OBJECT, ActionType.MODULE_TABLE, null, serverUserConfig, this)
                addMenuItem(AppModuleMMS.DAY_WORK, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData("Учёт", null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.COMPOSITE_OBJECT_LIST_DASHBOARD, ActionType.MODULE_COMPOSITE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData("Контроль", null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.DEPARTMENT, ActionType.MODULE_TABLE, null, serverUserConfig, this)
                addMenuItem(AppModuleMMS.GROUP, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData("Справочники", null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModuleMMS.DEVICE, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData("Контроллеры", null, this)
                }
            }

            mutableListOf<MenuData>().apply {

                addMenuItem(AppModule.USER_PROPERTY_EDIT, ActionType.MODULE_FORM, serverUserConfig.id, serverUserConfig, this)
                addMenuItem(AppModule.USER, ActionType.MODULE_TABLE, null, serverUserConfig, this)

                if (size > 0) {
                    alMenu += MenuData("Система", null, this)
                }
            }

            alMenu
        }
    }

}
