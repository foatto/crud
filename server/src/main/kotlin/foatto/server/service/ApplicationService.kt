package foatto.server.service

import foatto.core.ActionType
import foatto.core.AppModule
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.ServerActionButton
import foatto.core.model.response.TitleData
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormButtonKey
import foatto.core.model.response.form.FormResponse
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormFileData
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.model.response.table.AddActionButton
import foatto.core.model.response.table.ClientActionButton
import foatto.core.model.response.table.TableResponse
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableButtonCellData
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getDateDMYString
import foatto.core.util.getDateTimeDMYHMSString
import foatto.server.AppRole
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.entity.DateEntity
import foatto.server.entity.DateTimeEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.sql.CoreAdvancedConnection
import foatto.server.sql.SpringConnection
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.sql.ResultSet
import java.sql.Statement

abstract class ApplicationService(
    private val fileStoreService: FileStoreService,
) {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    @Value("\${temp_dir}")
    private val tempDirName: String = ""

    @Value("\${file_access_period}")
    private val fileAccessPeriod: String = "24"  // 24 hour file accessibility by default

    companion object {

        fun withConnection(entityManager: EntityManager, func: (conn: CoreAdvancedConnection) -> Unit) {
            entityManager.unwrap(Session::class.java).let { session ->
                session.doWork { conn ->
                    func(SpringConnection(conn))
                }
            }
        }

        fun queryNativeSql(entityManager: EntityManager, sql: String, func: (rs: ResultSet) -> Unit) {
            entityManager.unwrap(Session::class.java).let { session ->
                session.doWork { conn ->
                    var stm: Statement? = null
                    var rs: ResultSet? = null
                    try {
                        stm = conn.createStatement()
                        rs = stm.executeQuery(sql.trimIndent())
                        func(rs)
                    } catch (t: Throwable) {
                        println("sql = '$sql'")
                        t.printStackTrace()
                    } finally {
                        rs?.close()
                        stm?.close()
                    }
                }
            }
        }

        fun executeNativeSql(entityManager: EntityManager, vararg sqls: String): Int {
            var result = 0

            entityManager.unwrap(Session::class.java).let { session ->
                session.doWork { conn ->
                    var stm: Statement? = null
                    var currentSql: String? = null
                    try {
                        stm = conn.createStatement()
                        sqls.forEach { sql ->
                            currentSql = sql
                            result += stm.executeUpdate(sql.trimIndent())
                        }
                    } catch (t: Throwable) {
                        println("currentSql = '$currentSql'")
                        t.printStackTrace()
                    } finally {
                        stm?.close()
                    }
                }
            }
            return result
        }
    }

    fun app(
        sessionId: Long,
        action: AppAction,
    ): AppResponse {
        val actionModule = action.module

        val sessionData = SpringApp.getSessionData(sessionId) ?: return AppResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return AppResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return AppResponse(ResponseCode.LOGON_NEED)
        }

        val moduleConfig = appModuleConfigs[actionModule] ?: return AppResponse(ResponseCode.LOGON_NEED)

        return when (action.type) {
            ActionType.MODULE_TABLE -> {
                getTableResponse(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                )
            }

            ActionType.MODULE_FORM -> {
                getFormResponse(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                )
            }

            else -> {
                doOtherActionType(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                )
            }
        }
    }

    private fun getTableResponse(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse {
        val tableCaption = moduleConfig.caption
        val columnCaptions = getTableColumnCaptions(action, userConfig)

        val alTableCell = mutableListOf<TableBaseCell>()
        val alTableRowData = mutableListOf<TableRowData>()
        val alPageButton = mutableListOf<Pair<AppAction?, String>>()

        val currentRowNo = fillTableGridData(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
            alTableCell = alTableCell,
            alTableRowData = alTableRowData,
            alPageButton = alPageButton,
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_TABLE,
            table = TableResponse(
                tabCaption = tableCaption,
                headerData = getTableHeaderData(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig,
                ),
                findText = action.findText ?: "",
                alAddActionButton = if (checkFormAddPermission(action.module, userConfig.roles)) {
                    listOf(
                        AddActionButton(
                            text = "Добавить",
                            tooltip = "Добавить",
                            icon = IconName.ADD_ITEM,
                            action = getTableAddAction(
                                action = action,
                                parentModule = action.parentModule,
                                parentId = action.parentId,
                            ),
                        )
                    )
                } else {
                    emptyList()
                },
                alServerActionButton = getTableServerActionButtons(),
                alClientActionButton = getTableClientActionButtons(),
                alColumnCaption = columnCaptions,
                alTableCell = alTableCell,
                alTableRowData = alTableRowData,
                selectedRowNo = currentRowNo,
                alPageButton = alPageButton,
            )
        )
    }

    protected open fun getTableHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): HeaderData = HeaderData(
        titles = listOf(
            TitleData(
                action = null,
                text = if (action.isSelectorMode) {
                    "Выбор: "
                } else {
                    ""
                } + moduleConfig.caption,
                isBold = true,
            ),
        ),
        rows = emptyList()
    )

    protected open fun getTableServerActionButtons(): List<ServerActionButton> = emptyList()

    protected open fun getTableClientActionButtons(): List<ClientActionButton> = emptyList()

    protected abstract fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction?, String>>

    protected fun getTableColumnCaptionActions(action: AppAction, alColumnInfo: List<Pair<String?, String>>): List<Pair<AppAction, String>> =
        alColumnInfo.map { (fieldName, fieldCaption) ->
            action.copy(
                sortName = fieldName,
                isSortAsc = if (fieldName == action.sortName) {
                    !action.isSortAsc
                } else {
                    true
                }
            ) to fieldCaption
        }

    protected abstract fun fillTableGridData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        alTableCell: MutableList<TableBaseCell>,
        alTableRowData: MutableList<TableRowData>,
        alPageButton: MutableList<Pair<AppAction?, String>>,
    ): Int?

    protected fun getTableSortedPageRequest(
        action: AppAction,
        vararg orders: Sort.Order,
    ): PageRequest {
        var pageSize = appModuleConfigs[action.module]?.pageSize ?: 0
        if (pageSize < 1) {
            pageSize = Int.MAX_VALUE
        }

        return PageRequest.of(action.pageNo, pageSize, Sort.by(*orders))
    }

    protected fun getTableSelectorButtonCell(row: Int, col: Int, selectorAction: AppAction): TableBaseCell = TableButtonCell(
        row = row,
        col = col,
        dataRow = row,
        values = listOf(
            TableButtonCellData(
                name = IconName.SELECT,
                action = selectorAction,
            ),
        ),
    )

    protected fun getTableUserNameCell(
        row: Int,
        col: Int,
        userId: Int,
        rowUserId: Int?,
        rowOwnerShortName: String?,
        rowOwnerFullName: String?,
    ): TableBaseCell = TableSimpleCell(
        row = row,
        col = col,
        dataRow = row,
        name = if (rowUserId == null) {
            "-"
        } else if (rowUserId == 0) {
            "-"
        } else if (rowUserId == userId) {
            ""
        } else if (!rowOwnerShortName.isNullOrEmpty()) {
            rowOwnerShortName
        } else {
            rowOwnerFullName ?: "(неизвестно)"
        }
    )

    protected fun getTableFileButtonCellData(fileId: Int?): List<TableButtonCellData> = fileId?.let {
        fileStoreService.getFileList(fileId, fileAccessPeriod.toIntOrNull() ?: 24).map { ref ->
            TableButtonCellData(
                name = "${fileStoreService.getFileName(ref)} [${((fileStoreService.getFileSize(ref) ?: 0) / 1000) + 1} Кб]",
                action = AppAction(type = ActionType.FILE, url = fileStoreService.getFileRefUrl(ref)),
                inNewTab = true,
            )
        }
    } ?: emptyList()

    protected open fun getTableAddAction(action: AppAction, parentModule: String?, parentId: Int?): AppAction =
        AppAction(
            type = ActionType.MODULE_FORM,
            module = action.module,
            parentModule = parentModule,
            parentId = parentId,
        )

    /*
        protected fun jvmInstantToString(currentUserTimeZone: TimeZone, instant: Instant?): String = instant?.let {
            getDateTimeDMYHMSString(currentUserTimeZone, instant.epochSecond.toInt())
        } ?: "-"
     */

    protected fun fillTablePageButtons(
        action: AppAction,
        pageCount: Int,
        alPageButton: MutableList<Pair<AppAction?, String>>,
    ) {
        val pageNo = action.pageNo
        //--- first page
        if (pageNo > 0) {
            alPageButton += getPageButton(action, 0)
        }
        //--- empty
        if (pageNo > 2) {
            alPageButton += null to "..."
        }
        //--- prev page
        if (pageNo > 1) {
            alPageButton += getPageButton(action, pageNo - 1)
        }

        //--- current page
        alPageButton += null to "${pageNo + 1}"

        //--- next page
        if (pageNo < pageCount - 2) {
            alPageButton += getPageButton(action, pageNo + 1)
        }
        //--- empty
        if (pageNo < pageCount - 3) {
            alPageButton += null to "..."
        }
        //--- last page
        if (pageNo < pageCount - 1) {
            alPageButton += getPageButton(action, pageCount - 1)
        }
    }

    private fun getPageButton(action: AppAction, pageNo: Int): Pair<AppAction?, String> =
        action.copy(pageNo = pageNo) to "${pageNo + 1}"

    private fun getFormResponse(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse {
        val (addEnabled, editEnabled, deleteEnabled) = getFormActionPermissions(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )
        val alFormCell = getFormCells(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
            addEnabled = addEnabled,
            editEnabled = editEnabled,
        )

        val alFormButton = getFormButtons(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
            addEnabled = addEnabled,
            editEnabled = editEnabled,
            deleteEnabled = deleteEnabled,
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_FORM,
            form = FormResponse(
                id = action.id,
                tabCaption = moduleConfig.caption,
                headerData = getFormHeaderData(
                    action = action,
                    userConfig = userConfig,
                    moduleConfig = moduleConfig
                ),
                alFormColumn = emptyList(),
                alFormCell = alFormCell,
                alFormButton = alFormButton,
                prevAction = action.prevAction,
            )
        )
    }

    protected open fun getFormHeaderData(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): HeaderData = HeaderData(
        titles = listOf(
            TitleData(
                action = null,
                text = moduleConfig.caption,
                isBold = true,
            ),
        ),
        rows = emptyList()
    )

    protected abstract fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
    ): List<FormBaseCell>

    protected fun fillFormUserCells(
        fieldUserId: String,
        fieldOwnerFullName: String,
        userId: Int?,
        userConfig: ServerUserConfig,
        changeEnabled: Boolean,
        formCells: MutableList<FormBaseCell>,
    ) {
        formCells += FormSimpleCell(
            name = fieldUserId,
            caption = "",
            isEditable = false,
            value = userId.toString(),
        )
        formCells += FormSimpleCell(
            name = fieldOwnerFullName,
            caption = "Владелец",
            isEditable = false,
            value = if (userId == null || userId == 0) {
                "-"
            } else {
                userConfig.fullNames[userId] ?: "(неизвестно)"
            },
            selectorAction = if (changeEnabled) {
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = AppModule.USER,
                    isSelectorMode = true,
                    selectorPath = mapOf(
                        UserService.FIELD_ID to fieldUserId,
                        UserService.FIELD_FULL_NAME to fieldOwnerFullName,
                    ),
                    selectorClear = mapOf(
                        fieldUserId to "0",
                        fieldOwnerFullName to "",
                    ),
                )
            } else {
                null
            },
        )
    }

    protected fun getFormFileCellData(fileId: Int?): List<FormFileData> = fileId?.let {
        fileStoreService.getFileList(fileId, fileAccessPeriod.toIntOrNull() ?: 24).map { ref ->
            FormFileData(
                id = fileStoreService.getFileStoreData(ref)?.id ?: -1,
                ref = ref,
                action = AppAction(type = ActionType.FILE, url = fileStoreService.getFileRefUrl(ref)),
                name = "${fileStoreService.getFileName(ref)} [${((fileStoreService.getFileSize(ref) ?: 0) / 1000) + 1} Кб]",
            )
        }
    } ?: emptyList()

    protected open fun getFormButtons(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
        deleteEnabled: Boolean,
    ): List<FormButton> {

        val alFormButton = mutableListOf<FormButton>()

        val id = action.id

        if (id == null && addEnabled || id != null && editEnabled) {
            alFormButton += FormButton(
                actionType = id?.let { ActionType.FORM_EDIT } ?: ActionType.FORM_ADD,
                withNewData = true,
                name = IconName.SAVE,
                key = FormButtonKey.SAVE,
            )
        }

        if (id != null && deleteEnabled) {
            alFormButton += FormButton(
                actionType = ActionType.FORM_DELETE,
                withNewData = false,
                name = IconName.DELETE,
            )
        }

        alFormButton += FormButton(
            actionType = ActionType.FORM_EXIT,
            withNewData = false,
            name = IconName.EXIT,
            key = FormButtonKey.EXIT,
        )

        return alFormButton
    }

    protected open fun doOtherActionType(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): AppResponse = AppResponse(ResponseCode.LOGON_NEED)

    protected abstract fun getFormActionPermissions(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): Triple<Boolean, Boolean, Boolean>

    open fun formAction(
        sessionId: Long,
        action: AppAction,
        formActionData: Map<String, FormActionData>,
    ): FormActionResponse {
        val sessionData = SpringApp.getSessionData(sessionId) ?: return FormActionResponse(ResponseCode.ERROR)
        val userConfig = sessionData.serverUserConfig ?: return FormActionResponse(ResponseCode.ERROR)
        val moduleConfig = appModuleConfigs[action.module] ?: return FormActionResponse(ResponseCode.ERROR)

        val (addEnabled, editEnabled, deleteEnabled) = getFormActionPermissions(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )

        return when (action.type) {
            ActionType.FORM_ADD -> {
                if (addEnabled) {
                    formActionSave(
                        action = action,
                        userConfig = userConfig,
                        moduleConfig = moduleConfig,
                        formActionData = formActionData
                    )
                } else {
                    FormActionResponse(ResponseCode.ERROR)
                }
            }

            ActionType.FORM_EDIT -> {
                if (editEnabled) {
                    formActionSave(
                        action = action,
                        userConfig = userConfig,
                        moduleConfig = moduleConfig,
                        formActionData = formActionData
                    )
                } else {
                    FormActionResponse(ResponseCode.ERROR)
                }
            }

            ActionType.FORM_DELETE -> {
                if (deleteEnabled) {
                    action.id?.let { id ->
                        formActionDelete(userConfig.id, id)
                    } ?: FormActionResponse(ResponseCode.ERROR)
                } else {
                    FormActionResponse(ResponseCode.ERROR)
                }
            }

            else -> {
                formActionExit(action)
            }
        }
    }

    protected abstract fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): FormActionResponse

    protected abstract fun formActionDelete(userId: Int, id: Int): FormActionResponse

    protected open fun formActionExit(action: AppAction): FormActionResponse = FormActionResponse(ResponseCode.OK)

    protected fun isAdminOnly(userConfig: ServerUserConfig): Boolean = userConfig.roles.size == 1 && userConfig.roles.contains(AppRole.ADMIN)

    protected fun formActionSaveFiles(formActionData: FormActionData): Int {
        val fileId = formActionData.fileId ?: fileStoreService.getNextFileId()

        formActionData.addFiles?.forEach { (fromClientId, fileName) ->
            fileStoreService.addFile(fileId, fromClientId, fileName)
        }

        formActionData.fileRemovedIds?.forEach { idForDelete ->
            fileStoreService.deleteFile(fileId, idForDelete)
        }

        return fileId
    }

//--- date/time helper functions

    protected fun getDateEntityDMYString(dateEntity: DateEntity?): String =
        dateEntity?.let { de ->
            if (de.ye != null && de.mo != null && de.da != null) {
                getDateDMYString(de.ye, de.mo, de.da)
            } else {
                "-"
            }
        } ?: "-"

    protected fun getDateTimeEntityDMYString(dateTimeEntity: DateTimeEntity?): String =
        dateTimeEntity?.let { dte ->
            if (dte.ye != null && dte.mo != null && dte.da != null && dte.ho != null && dte.mi != null && dte.se != null) {
                getDateTimeDMYHMSString(dte.ye, dte.mo, dte.da, dte.ho, dte.mi, dte.se)
            } else {
                "-"
            }
        } ?: "-"

}