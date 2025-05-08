package foatto.server.service

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormCellVisibility
import foatto.core.model.response.form.FormDateTimeCellMode
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
import foatto.core.util.getDateTimeYMDHMSInts
import foatto.core.util.getTimeZone
import foatto.server.OrgType
import foatto.server.appRoleConfigs
import foatto.server.checkFormAddPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DateEntity
import foatto.server.entity.DateTimeEntity
import foatto.server.entity.UserEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.UserRepository
import foatto.server.util.encodePassword
import foatto.server.util.getNextId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        const val FIELD_ID: String = "id"
        private const val FIELD_PARENT_ID = "parentId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ORG_TYPE = "orgType"

        private const val FIELD_IS_DISABLED = "isDisabled"
        const val FIELD_FULL_NAME: String = "fullName"
        private const val FIELD_SHORT_NAME = "shortName"
        private const val FIELD_LOGIN = "login"
        private const val FIELD_PASSWORD = "password"
        private const val FIELD_ROLES = "roles"

        private const val FIELD_TIME_OFFSET = "timeOffset"
        private const val FIELD_EMAIL = "eMail"
        private const val FIELD_CONTACT_INFO = "contactInfo"
        private const val FIELD_USE_THOUSANDS_DIVIDER = "useThousandsDivider"
        private const val FIELD_DECIMAL_SEPARATOR = "decimalSeparator"
        private const val FIELD_FILE = "fileId"

        private const val FIELD_AT_COUNT = "atCount"
        private const val FIELD_LAST_LOGIN = "lastLoginDateTime"
        private const val FIELD_LAST_IP = "lastIP"
        private const val FIELD_PASSWORD_LAST_CHANGE = "passwordLastChangeDate"

        private const val FIELD_OWNER_FULL_NAME = "ownerFullName"   // псевдополе для селектора
        private const val FIELD_PARENT_FULL_NAME = "parentFullName"   // псевдополе для селектора
    }

    override fun getTableHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig
    ): HeaderData {
        val titles = mutableListOf<TitleData>()

        var parentId = if (action.parentModule == action.module) {
            action.parentId
        } else {
            null
        }
        val currentParentId = parentId

        while (parentId != null && parentId != 0) {
            userRepository.findByIdOrNull(parentId)?.let { userEntity ->
                titles.add(
                    index = 0,
                    element = TitleData(
                        action = if (parentId == currentParentId) {
                            null
                        } else {
                            action.copy(parentId = userEntity.id)
                        },
                        text = userEntity.fullName ?: "(неизвестно)",
                        isBold = parentId == currentParentId,
                    )
                )
                parentId = userEntity.parentId
            } ?: run {
                parentId = null
            }
        }
        titles.add(
            0,
            TitleData(
                action = if (titles.isEmpty()) {
                    null
                } else {
                    action.copy(parentId = 0)
                },
                text = if (action.isSelectorMode) {
                    "Выбор: "
                } else {
                    ""
                } + moduleConfig.caption,
                isBold = true,
            )
        )

        return HeaderData(
            titles = titles,
            rows = emptyList(),
        )
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        if (action.isSelectorMode) {
            alColumnInfo += null to "" // selector button
        }
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
            alColumnInfo += null to "Файлы"
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

        val pageRequest = getTableSortedPageRequest(action, Sort.Order(Sort.Direction.ASC, FIELD_ORG_TYPE), Sort.Order(Sort.Direction.ASC, FIELD_FULL_NAME))
        val findText = action.findText?.trim() ?: ""

        //--- hierarchical table/data - parent id is existing always
        val parentId = if (action.parentModule == action.module) {
            action.parentId ?: 0
        } else {
            0
        }

        val enabledUserIds = getEnabledUserIds(
            module = action.module,
            actionType = action.type,
            relatedUserIds = userConfig.relatedUserIds,
            roles = userConfig.roles,
        )

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

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[userEntity.userId],
                userRoles = userConfig.roles
            )

            val selectorAction = AppAction(
                type = ActionType.FORM_SELECTOR,
                selectorData = mapOf(
                    FIELD_ID to userEntity.id.toString(),
                    FIELD_FULL_NAME to (userEntity.fullName ?: "(неизвестно)"),
                ),
            )

            if (action.isSelectorMode) {
                alTableCell += getTableSelectorButtonCell(row = row, col = col++, selectorAction = selectorAction)
            }
            alTableCell += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = userEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
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
            alTableCell += if (userEntity.orgType == OrgType.ORG_TYPE_DIVISION) {
                TableSimpleCell(row = row, col = col++, dataRow = row, name = "")
            } else {
                TableBooleanCell(row = row, col = col++, dataRow = row, value = userEntity.isDisabled ?: false)
            }
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
                    values = getTableFileButtonCellData(userEntity.fileId),
                )
            }
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.timeOffset?.toString() ?: "-")
            if (isAdminOnly(userConfig)) {
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = userEntity.lastIP?.removePrefix("/") ?: "-")
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

            alTableRowData += TableRowData(
                formAction = if (isFormEnabled) {
                    formAction
                } else {
                    null
                },
                rowAction = if (userEntity.orgType == OrgType.ORG_TYPE_DIVISION) {
                    action.copy(parentModule = action.module, parentId = userEntity.id)
                } else if (action.isSelectorMode) {
                    AppAction(
                        type = ActionType.FORM_SELECTOR,
                        selectorData = mapOf(
                            FIELD_ID to userEntity.id.toString(),
                            FIELD_FULL_NAME to (userEntity.fullName ?: "(неизвестно)"),
                        ),
                    )
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

            if (userEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    override fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
    ): List<FormBaseCell> {
        val formCells = mutableListOf<FormBaseCell>()

        val id = action.id

        val userEntity = id?.let {
            userRepository.findByIdOrNull(id) ?: return emptyList()
        }

        val parentId = userEntity?.let {
            userEntity.parentId
        } ?: action.parentId

        //--- в отличие от прочих модулей, запись пользователя создаётся не "на себя", а с "общим" пользователем по умолчанию
        val userId = userEntity?.let {
            userEntity.userId
        } ?: 0

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
            value = parentId.toString(),
        )
        formCells += FormSimpleCell(
            name = FIELD_PARENT_FULL_NAME,
            caption = "Верхнее подразделение",
            isEditable = false,
            value = if (parentId == null || parentId == 0) {
                "-"
            } else {
                userConfig.fullNames[parentId] ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModule.USER,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        FIELD_ID to FIELD_PARENT_ID,
                        FIELD_FULL_NAME to FIELD_PARENT_FULL_NAME,
                    ),
                    selectorClear = mapOf(
                        FIELD_PARENT_ID to "0",
                        FIELD_PARENT_FULL_NAME to "",
                    ),
                )
            } else {
                null
            },
        )
        fillFormUserCells(
            fieldUserId = FIELD_USER_ID,
            fieldOwnerFullName = FIELD_OWNER_FULL_NAME,
            userId = userId,
            userConfig = userConfig,
            changeEnabled = changeEnabled,
            formCells = formCells,
        )
        formCells += FormComboCell(
            name = FIELD_ORG_TYPE,
            caption = "Тип",
            isEditable = changeEnabled,
            value = userEntity?.orgType?.toString() ?: OrgType.ORG_TYPE_WORKER.toString(),
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
            value = userEntity?.isDisabled ?: false,
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_FULL_NAME,
            caption = "Полное имя",
            isEditable = changeEnabled,
            value = userEntity?.fullName ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_SHORT_NAME,
            caption = "Краткое имя",
            isEditable = changeEnabled,
            value = userEntity?.shortName ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_LOGIN,
            caption = "Логин",
            isEditable = changeEnabled,
            value = userEntity?.login ?: "",
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_PASSWORD,
            caption = "Пароль",
            isEditable = changeEnabled,
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
                value = userEntity?.roles?.contains(role) ?: false,
                visibility = nonVisibleForDivision,
            )
        }
        formCells += FormSimpleCell(
            name = FIELD_TIME_OFFSET,
            caption = "Сдвиг часового пояса [сек]",
            isEditable = changeEnabled,
            value = (userEntity?.timeOffset ?: TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds).toString(),
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_EMAIL,
            caption = "E-mail",
            isEditable = changeEnabled,
            value = userEntity?.eMail ?: "",
        )
        formCells += FormSimpleCell(
            name = FIELD_CONTACT_INFO,
            caption = "Контактная информация",
            isEditable = changeEnabled,
            value = userEntity?.contactInfo ?: "",
            rows = 5,
        )
        formCells += FormBooleanCell(
            name = FIELD_USE_THOUSANDS_DIVIDER,
            caption = "Разделять тысячи пробелами",
            isEditable = changeEnabled,
            value = userEntity?.useThousandsDivider ?: true,
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_DECIMAL_SEPARATOR,
            caption = "Разделить дробной части",
            isEditable = changeEnabled,
            value = userEntity?.decimalSeparator ?: ".",
        )
        formCells += FormFileCell(
            name = FIELD_FILE,
            caption = "Файлы",
            isEditable = changeEnabled,
            fileId = userEntity?.fileId,
            files = getFormFileCellData(userEntity?.fileId)
        )
        formCells += FormSimpleCell(
            name = FIELD_AT_COUNT,
            caption = "Кол-во попыток входа",
            isEditable = changeEnabled,
            value = userEntity?.atCount?.toString() ?: "",
            visibility = nonVisibleForDivision,
        )
        formCells += FormDateTimeCell(
            name = FIELD_LAST_LOGIN,
            caption = "Дата/время последнего входа",
            isEditable = false,
            mode = FormDateTimeCellMode.DMYHMS,
            value = userEntity?.lastLoginDateTime?.let { dt ->
                LocalDateTime(
                    year = dt.ye ?: 2000,
                    monthNumber = dt.mo ?: 1,
                    dayOfMonth = dt.da ?: 1,
                    hour = dt.ho ?: 0,
                    minute = dt.mi ?: 0,
                    second = dt.se ?: 0
                ).toInstant(getTimeZone(userConfig.timeOffset)).epochSeconds.toInt()
            },
            visibility = nonVisibleForDivision,
        )
        formCells += FormSimpleCell(
            name = FIELD_LAST_IP,
            caption = "Last IP",
            isEditable = false,
            value = userEntity?.lastIP ?: "",
            visibility = nonVisibleForDivision,
        )
        formCells += FormDateTimeCell(
            name = FIELD_PASSWORD_LAST_CHANGE,
            caption = "Дата/время последней смены пароля",
            isEditable = false,
            mode = FormDateTimeCellMode.DMY,
            value = userEntity?.passwordLastChangeDate?.let { dt ->
                LocalDateTime(
                    year = dt.ye ?: 2000,
                    monthNumber = dt.mo ?: 1,
                    dayOfMonth = dt.da ?: 1,
                    hour = 0,
                    minute = 0,
                    second = 0
                ).toInstant(getTimeZone(userConfig.timeOffset)).epochSeconds.toInt()
            },
            visibility = nonVisibleForDivision,
        )

        return formCells
    }

    override fun getFormButtons(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean, deleteEnabled: Boolean): List<FormButton> {
        val id = action.id

        val isExistsDepencies: Boolean = if (deleteEnabled) {
            id?.let {
                userRepository.findByParentId(id).isNotEmpty()
            } ?: false
        } else {
            false
        }

        return super.getFormButtons(action, userConfig, moduleConfig, addEnabled, editEnabled, deleteEnabled && !isExistsDepencies)
    }

    override fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse {
        val id = action.id

        val fullName = formActionData[FIELD_FULL_NAME]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_FULL_NAME to "Не введёно полное имя"))
        if (fullName.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_FULL_NAME to "Не введёно полное имя"))
        }
        if (userRepository.findByFullName(fullName).any { ue -> ue.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_FULL_NAME to "Такое полное имя уже существует"))
        }

        val login = formActionData[FIELD_LOGIN]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_LOGIN to "Не введён логин"))
        if (login.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_LOGIN to "Не введён логин"))
        }
        if (userRepository.findByLogin(login).any { ue -> ue.id != id }) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_LOGIN to "Такой логин уже существует"))
        }

        val password = formActionData[FIELD_PASSWORD]?.stringValue?.trim() ?: return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_PASSWORD to "Не введён пароль"))
        if (password.isEmpty()) {
            return FormActionResponse(responseCode = ResponseCode.ERROR, errors = mapOf(FIELD_PASSWORD to "Не введён пароль"))
        }
        val encodedPassword: String = id?.let {
            userRepository.findByIdOrNull(id)?.let { userEntity ->
                //--- шифруем пароль только если он поменялся
                //--- (в прочих случаях в этой поле лежит хэш-код текущего пароля и второй раз перешифровывать его не надо)
                if (userEntity.password == password) {
                    password
                } else {
                    encodePassword(password)
                }
            } ?: encodePassword(password)
        } ?: encodePassword(password)

        val roles = appRoleConfigs.map { (role, _) ->
            role
        }.filter { role ->
            formActionData[FIELD_ROLES + "_" + role]?.booleanValue == true
        }.toSet()
        val lastLoginDateTime = getDateTimeYMDHMSInts(getTimeZone(userConfig.timeOffset), formActionData[FIELD_LAST_LOGIN]?.dateTimeValue ?: 0)
        val passwordLastChangeDate = getDateTimeYMDHMSInts(getTimeZone(userConfig.timeOffset), formActionData[FIELD_PASSWORD_LAST_CHANGE]?.dateTimeValue ?: 0)

        val recordId = id ?: getNextId { nextId -> userRepository.existsById(nextId) }
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
            timeOffset = formActionData[FIELD_TIME_OFFSET]?.stringValue?.toIntOrNull() ?: TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds,
            eMail = formActionData[FIELD_EMAIL]?.stringValue,
            contactInfo = formActionData[FIELD_CONTACT_INFO]?.stringValue,
            useThousandsDivider = formActionData[FIELD_USE_THOUSANDS_DIVIDER]?.booleanValue ?: true,
            decimalSeparator = formActionData[FIELD_DECIMAL_SEPARATOR]?.stringValue ?: ".",
            fileId = formActionData[FIELD_FILE]?.let { fad ->
                formActionSaveFiles(fad)
            },
            atCount = formActionData[FIELD_AT_COUNT]?.stringValue?.toIntOrNull() ?: 0,
            lastLoginDateTime = DateTimeEntity(
                ye = lastLoginDateTime[0],
                mo = lastLoginDateTime[1],
                da = lastLoginDateTime[2],
                ho = lastLoginDateTime[3],
                mi = lastLoginDateTime[4],
                se = lastLoginDateTime[5],
            ),
            lastIP = formActionData[FIELD_LAST_IP]?.stringValue,
            passwordLastChangeDate = DateEntity(
                ye = passwordLastChangeDate[0],
                mo = passwordLastChangeDate[1],
                da = passwordLastChangeDate[2],
            ),
        )
        userRepository.saveAndFlush(userEntity)

        return FormActionResponse(responseCode = ResponseCode.OK)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        userRepository.findByIdOrNull(id)?.let { userEntity ->
            fileStoreService.deleteFile(userEntity.fileId, null)
        }
        userRepository.deleteById(id)
        return FormActionResponse(responseCode = ResponseCode.OK)
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

}