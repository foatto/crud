package foatto.server.service

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableBooleanCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.server.AppRole
import foatto.server.OrgType
import foatto.server.appRoleConfigs
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DateEntity
import foatto.server.entity.DateTimeEntity
import foatto.server.entity.UserEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.FileStoreRepository
import foatto.server.repository.UserRepository
import foatto.server.util.encodePassword
import org.springframework.data.domain.Page
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val fileStoreRepository: FileStoreRepository,
) : ApplicationService() {

    override fun getTableHeader(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): List<Pair<AppAction?, String>> {
        val headers = mutableListOf<Pair<AppAction?, String>>()

        var parentId = action.parentId
        while (parentId != null && parentId != 0) {
            userRepository.findByIdOrNull(parentId)?.let { userEntity ->
                val pid = userEntity.parentId
                headers.add(0, action.copy(parentId = pid) to (userEntity.fullName ?: "(неизвестно)"))
                parentId = pid
            } ?: run {
                parentId = null
            }
        }
        headers.add(0, action.copy(parentId = 0) to moduleConfig.caption)

        return headers
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // userId
        alColumnInfo += null to "" // orgType
        alColumnInfo += FIELD_IS_DISABLED to "Заблокирован"
        alColumnInfo += FIELD_LOGIN to "Логин"
        alColumnInfo += FIELD_SHORT_NAME to "Краткое имя"
        alColumnInfo += FIELD_FULL_NAME to "Полное имя"

        if (isAdminOnly(userConfig)) {
            alColumnInfo += FIELD_ROLES to "Роли"
        }
        alColumnInfo += FIELD_EMAIL to "E-mail"
        alColumnInfo += FIELD_CONTACT_INFO to "Контактная информация"
        if (isAdminOnly(userConfig)) {
            alColumnInfo += FIELD_FILE to "Файлы"
        }
        alColumnInfo += FIELD_TIME_OFFSET to "Time Offset"
        if (isAdminOnly(userConfig)) {
            alColumnInfo += FIELD_LAST_IP to "Last IP"
        }

        return getTableColumnCaptionActions(
            action = action,
            alColumnInfo = alColumnInfo,
        )
    }

    override fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        alTableCell: MutableList<TableBaseCell>,
        alTableRowData: MutableList<TableRowData>,
        alPageButton: MutableList<Pair<AppAction?, String>>,
    ): Int? {

        var currentRowNo: Int? = null
        var row = 0

        val pageRequest = getTableSortedPageRequest(action, FIELD_FULL_NAME)
        val findText = action.findText?.trim() ?: ""

        //--- hierarchical table/data - parent id is existing always
        val parentId = if (action.parentModule == AppModule.USER) {
            action.parentId ?: 0
        } else {
            0
        }

        val enabledUserIds = userConfig.relatedUserIds.filterValues { userRelation ->
            checkRowPermission(action.module, action.type, userRelation, userConfig.roles)
        }.map { (userId, _) ->
            userId
        }

        val page: Page<UserEntity> = if (findText.isNotEmpty()) {
            userRepository.findByParentIdAndUserIdInAndFilter(parentId, enabledUserIds, findText, pageRequest)
        } else {
            userRepository.findByParentIdAndUserIdIn(parentId, enabledUserIds, pageRequest)
        }
        fillTablePageButtons(action, page.totalPages, alPageButton)
        val userEntities = page.content

        for (userEntity in userEntities) {
            var col = 0

            val rowOwnerShortName = userConfig.shortNames[userEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[userEntity.userId]

            val isFormEnabled = checkRowPermission(action.module, ActionType.MODULE_FORM, userConfig.relatedUserIds[userEntity.userId], userConfig.roles)

            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = if (userEntity.userId == 0) {
                    "-"
                } else if (userEntity.userId == userConfig.id) {
                    ""
                } else if (!rowOwnerShortName.isNullOrEmpty()) {
                    rowOwnerShortName
                } else {
                    rowOwnerFullName ?: "(неизвестно)"
                }
            )
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = when (userEntity.orgType) {
                    OrgType.ORG_TYPE_DIVISION -> IconName.DIVISION
                    OrgType.ORG_TYPE_BOSS -> IconName.BOSS
                    OrgType.ORG_TYPE_WORKER -> IconName.WORKER
                    else -> "-"
                }
            )
            alTableCell += TableBooleanCell(row = row, col = col++, dataRow = row, value = userEntity.isDisabled ?: false)
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.login ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.shortName ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.fullName ?: "-")
            if (isAdminOnly(userConfig)) {
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.roles.joinToString())
            }
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.eMail ?: "-")
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.contactInfo ?: "-")
            if (isAdminOnly(userConfig)) {
                alTableCell += TableButtonCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    values = getTableFileButtonCellData(fileStoreRepository, userEntity.fileId),
                )
            }
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.timeOffset?.toString() ?: "-")
            if (isAdminOnly(userConfig)) {
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.lastIP ?: "-")
            }

            val formAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = userEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val alPopupData = mutableListOf<TablePopupData>()

            if (isFormEnabled) {
                alPopupData += TablePopupData(
                    action = formAction,
                    text = "Открыть",
                    inNewTab = false,
                )
            }

            alTableRowData.add(
                TableRowData(
                    formAction = if (isFormEnabled) {
                        formAction
                    } else {
                        null
                    },
                    rowAction = if (userEntity.orgType == OrgType.ORG_TYPE_DIVISION) {
                        AppAction(type = ActionType.MODULE_TABLE, module = action.module, parentModule = action.module, parentId = userEntity.id)
                    } else if (isFormEnabled) {
                        formAction
                    } else {
                        null
                    },
                    isRowUrlInNewTab = false,
                    gotoAction = null,
                    isGotoUrlInNewTab = true,
                    alPopupData = alPopupData,
                )
            )

            if (userEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    override fun getTableAddAction(action: AppAction, parentModule: String?, parentId: Int?): AppAction =
        AppAction(
            type = ActionType.MODULE_FORM,
            module = action.module,
            parentModule = parentModule,
            parentId = parentId,
        )

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val userEntity = id?.let {
            //--- TODO: ошибка об отсутствии такой записи
            userRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)
        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[userEntity?.userId], userConfig.roles)
        val changeEnabled = action.id?.let { editEnabled } ?: addEnabled

        val nonVisibleForDivision = FormCellVisibility(
            name = FIELD_ORG_TYPE,
            state = false,
            values = setOf(OrgType.ORG_TYPE_DIVISION.toString()),
        )

        formCells += FormSimpleCell(
            name = FIELD_PARENT_ID,
            caption = "",
            isEditable = false,
            isAutoFocus = false,
            value = userEntity?.parentId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_PARENT_FULL_NAME,
            caption = "Верхнее подразделение",
            isEditable = false,
            isAutoFocus = false,
            value = userConfig.fullNames[userEntity?.parentId] ?: "(неизвестно)",
            selectorAction = AppAction(
                type = ActionType.MODULE_TABLE,
                module = AppModule.USER,
                isSelectorMode = true,
                selectorPath = mapOf(
                    FIELD_USER_ID to FIELD_PARENT_ID,
                    FIELD_FULL_NAME to FIELD_PARENT_FULL_NAME,
                ),
                selectorClear = mapOf(
                    FIELD_PARENT_ID to "null",
                    FIELD_PARENT_FULL_NAME to "",
                ),
            ),
        )
        formCells += FormSimpleCell(
            name = FIELD_USER_ID,
            caption = "",
            isEditable = false,
            isAutoFocus = false,
            value = userEntity?.userId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_OWNER_FULL_NAME,
            caption = "Владелец",
            isEditable = false,
            isAutoFocus = false,
            value = userConfig.fullNames[userEntity?.userId] ?: "(неизвестно)",
            selectorAction = AppAction(
                type = ActionType.MODULE_TABLE,
                module = AppModule.USER,
                isSelectorMode = true,
                selectorPath = mapOf(
                    FIELD_USER_ID to FIELD_USER_ID,
                    FIELD_FULL_NAME to FIELD_OWNER_FULL_NAME,
                ),
                selectorClear = mapOf(
                    FIELD_USER_ID to "null",
                    FIELD_OWNER_FULL_NAME to "",
                ),
            ),
        )
        formCells += FormComboCell(
            name = FIELD_ORG_TYPE,
            caption = "Тип",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = when (userEntity?.orgType) {
                OrgType.ORG_TYPE_DIVISION -> "Подразделение"
                OrgType.ORG_TYPE_BOSS -> "Руководитель"
                OrgType.ORG_TYPE_WORKER -> "Работник"
                else -> "(неизвестно)"
            },
            values = listOf(
                OrgType.ORG_TYPE_DIVISION.toString() to "Подразделение",
                OrgType.ORG_TYPE_BOSS.toString() to "Руководитель",
                OrgType.ORG_TYPE_WORKER.toString() to "Работник",
            ),
        )
        formCells += FormBooleanCell(
            name = FIELD_IS_DISABLED,
            caption = "Заблокирован",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.isDisabled ?: false,
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_FULL_NAME,
            caption = "Полное имя",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.fullName ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_SHORT_NAME,
            caption = "Краткое имя",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.shortName ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_LOGIN,
            caption = "Логин",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.login ?: "",
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_PASSWORD,
            caption = "Пароль",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.password ?: "",
            isPassword = true,
            visibility = nonVisibleForDivision,
        )
        appRoleConfigs.map { (role, _) ->
            role
        }.sorted().forEach { role ->
            formCells += FormBooleanCell(
                name = FIELD_ROLES + "_" + role,
                caption = "Роль $role",
                isEditable = changeEnabled,
                isAutoFocus = false,
                value = userEntity?.roles?.contains(role) ?: false,
                visibility = nonVisibleForDivision,
            )
        }
        formCells += FormSimpleCell(
            name = FIELD_TIME_OFFSET,
            caption = "Сдвиг часового пояса [сек]",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.timeOffset?.toString() ?: "10800",
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_EMAIL,
            caption = "E-mail",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.eMail ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CONTACT_INFO,
            caption = "Контактная информация",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.contactInfo ?: "",
            rows = 5,
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файлы",
            isEditable = changeEnabled,
            isAutoFocus = false,
            fileId = userEntity?.fileId,
            files = getFormFileCellData(fileStoreRepository, userEntity?.fileId)
        )
        formCells += FormSimpleCell(
            name = FIELD_AT_COUNT,
            caption = "Кол-во попыток входа",
            isEditable = changeEnabled,
            isAutoFocus = false,
            value = userEntity?.atCount?.toString() ?: "",
            visibility = nonVisibleForDivision,
        )
        formCells += FormDateTimeCell(
            name = FIELD_LAST_LOGIN,
            caption = "Дата/время последнего входа",
            isEditable = false,
            isAutoFocus = false,
            value = userEntity?.lastLoginDateTime?.let { dt ->
                listOf(dt.da.toString(), dt.mo.toString(), dt.ye.toString(), dt.ho.toString(), dt.mi.toString(), dt.se.toString())
            } ?: listOf("", "", "", "", "", ""),
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_LAST_IP,
            caption = "Last IP",
            isEditable = false,
            isAutoFocus = false,
            value = userEntity?.lastIP ?: "",
            visibility = nonVisibleForDivision,
        )
        formCells += FormDateTimeCell(
            name = FIELD_PASSWORD_LAST_CHANGE,
            caption = "Дата/время последней смены пароля",
            isEditable = false,
            isAutoFocus = false,
            value = userEntity?.passwordLastChangeDate?.let { d ->
                listOf(d.da.toString(), d.mo.toString(), d.ye.toString())
            } ?: listOf("", "", ""),
            visibility = nonVisibleForDivision,
        )

        return formCells
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): Map<String, String>? {
        val id = action.id

        //TODO: общий Map со всеми ошибками
        val fullName = formActionData[FIELD_FULL_NAME]?.stringValue ?: return mapOf(FIELD_FULL_NAME to "Не введёно полное имя")
        if (fullName.isBlank()) {
            return mapOf(FIELD_FULL_NAME to "Не введёно полное имя")
        }
        if (userRepository.findByFullName(fullName).any { ue -> ue.id != id }) {
            return mapOf(FIELD_FULL_NAME to "Такое полное имя уже существует")
        }

        val login = formActionData[FIELD_LOGIN]?.stringValue ?: return mapOf(FIELD_LOGIN to "Не введён логин")
        if (login.isBlank()) {
            return mapOf(FIELD_LOGIN to "Не введён логин")
        }
        if (userRepository.findByLogin(login).any { ue -> ue.id != id }) {
            return mapOf(FIELD_LOGIN to "Такой логин уже существует")
        }

        val password = formActionData[FIELD_PASSWORD]?.stringValue ?: return mapOf(FIELD_PASSWORD to "Не введён пароль")
        if (password.isBlank()) {
            return mapOf(FIELD_PASSWORD to "Не введён пароль")
        }
        val encodedPassword: String = id?.let {
            userRepository.findByIdOrNull(id)?.let { userEntity ->
                //--- шифруем пароль только если он поменялся
                //--- (в прочих случаях в этой поле лежит хэш-код текущего пароля и второй раз перешифровывать его не надо)
                if (password != userEntity.password) {
                    encodePassword(password)
                } else {
                    password
                }
            } ?: password
        } ?: password

        val roles = appRoleConfigs.map { (role, _) ->
            role
        }.filter { role ->
            formActionData[FIELD_ROLES + "_" + role]?.booleanValue == true
        }.toSet()
        val lastLoginDateTime = formActionData[FIELD_LAST_LOGIN]?.alDateTimeValue ?: listOf("1", "1", "2000", "0", "0", "0")
        val passwordLastChangeDate = formActionData[FIELD_PASSWORD_LAST_CHANGE]?.alDateTimeValue ?: listOf("1", "1", "2000")

        val recordId = id ?: getNextId()
        val userEntity = UserEntity(
            id = recordId,
            parentId = formActionData[FIELD_PARENT_ID]?.stringValue?.toIntOrNull() ?: 0,
            userId = formActionData[FIELD_USER_ID]?.stringValue?.toIntOrNull() ?: 0,
            orgType = formActionData[FIELD_ORG_TYPE]?.stringValue?.toIntOrNull() ?: OrgType.ORG_TYPE_DIVISION,
            isDisabled = formActionData[FIELD_IS_DISABLED]?.booleanValue == true,
            fullName = fullName,
            shortName = formActionData[FIELD_SHORT_NAME]?.stringValue,
            login = login,
            password = encodedPassword,
            roles = roles,
            timeOffset = formActionData[FIELD_TIME_OFFSET]?.stringValue?.toIntOrNull() ?: 0,
            eMail = formActionData[FIELD_EMAIL]?.stringValue,
            contactInfo = formActionData[FIELD_CONTACT_INFO]?.stringValue,
            fileId = formActionData[FIELD_FILE]?.let { fad ->
                formActionSaveFiles(fileStoreRepository, fad)
            },
            atCount = formActionData[FIELD_AT_COUNT]?.stringValue?.toIntOrNull() ?: 0,
            lastLoginDateTime = DateTimeEntity(
                ye = lastLoginDateTime[2].toIntOrNull(),
                mo = lastLoginDateTime[1].toIntOrNull(),
                da = lastLoginDateTime[0].toIntOrNull(),
                ho = lastLoginDateTime[3].toIntOrNull(),
                mi = lastLoginDateTime[4].toIntOrNull(),
                se = lastLoginDateTime[5].toIntOrNull(),
            ),
            lastIP = formActionData[FIELD_LAST_IP]?.stringValue,
            passwordLastChangeDate = DateEntity(
                ye = passwordLastChangeDate[2].toIntOrNull(),
                mo = passwordLastChangeDate[1].toIntOrNull(),
                da = passwordLastChangeDate[0].toIntOrNull(),
            ),
        )
        userRepository.saveAndFlush(userEntity)

        return null
    }

    override fun formActionDelete(userId: Int, id: Int): Map<String, String>? {
        userRepository.deleteById(id)
        return null
    }

    override fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean> {
        val id = action.id

        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)

        val userEntity = id?.let {
            userRepository.findByIdOrNull(id) ?: return Triple(addEnabled, false, false)
        }

        val editEnabled = checkRowPermission(action.module, ActionType.FORM_EDIT, userConfig.relatedUserIds[userEntity?.userId], userConfig.roles)
        val deleteEnabled = checkRowPermission(action.module, ActionType.FORM_DELETE, userConfig.relatedUserIds[userEntity?.userId], userConfig.roles)

        return Triple(addEnabled, editEnabled, deleteEnabled)
    }

    override fun checkExistingId(nextId: Int): Boolean = userRepository.existsById(nextId)

    private fun isAdminOnly(userConfig: ServerUserConfig) = userConfig.roles.size == 1 && userConfig.roles.contains(AppRole.ADMIN)

    companion object {
        private const val FIELD_PARENT_ID = "parentId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ORG_TYPE = "orgType"

        private const val FIELD_IS_DISABLED = "isDisabled"
        private const val FIELD_FULL_NAME = "fullName"
        private const val FIELD_SHORT_NAME = "shortName"
        private const val FIELD_LOGIN = "login"
        private const val FIELD_PASSWORD = "password"
        private const val FIELD_ROLES = "roles"

        private const val FIELD_TIME_OFFSET = "timeOffset"
        private const val FIELD_EMAIL = "eMail"
        private const val FIELD_CONTACT_INFO = "contactInfo"
        private const val FIELD_FILE = "fileId"

        private const val FIELD_AT_COUNT = "atCount"
        private const val FIELD_LAST_LOGIN = "lastLoginDateTime"
        private const val FIELD_LAST_IP = "lastIP"
        private const val FIELD_PASSWORD_LAST_CHANGE = "passwordLastChangeDate"

        private const val FIELD_OWNER_FULL_NAME = "ownerFullName"   // псевдополе для селектора
        private const val FIELD_PARENT_FULL_NAME = "parentFullName"   // псевдополе для селектора
    }
}