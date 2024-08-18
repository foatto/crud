package foatto.server.service

import foatto.core.ActionType
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.FormResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.ServerActionButton
import foatto.core.model.response.TableResponse
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormButtonKey
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormFileData
import foatto.core.model.response.table.AddActionButton
import foatto.core.model.response.table.ClientActionButton
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableButtonCellData
import foatto.core.util.getFilledNumberString
import foatto.core.util.getRandomInt
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkFormAddPermission
import foatto.server.controller.FileController
import foatto.server.entity.FileStoreEntity
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.FileStoreRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import java.io.File

abstract class ApplicationService {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    @Value("\${temp_dir}")
    private val tempDirName: String = ""

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

    protected fun getTableResponse(
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
                alHeader = getTableHeader(
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

    protected open fun getTableHeader(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): List<Pair<AppAction?, String>> = listOf(null to moduleConfig.caption)

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
        defaultFieldName: String,
        defaultDirection: Sort.Direction = Sort.Direction.ASC,
    ): PageRequest {
        val sort = action.sortName?.let { sortName ->
            Sort.by(if (action.isSortAsc) Sort.Direction.ASC else Sort.Direction.DESC, sortName)
        } ?: run {
            Sort.by(defaultDirection, defaultFieldName)
        }

        var pageSize = appModuleConfigs[action.module]?.pageSize ?: 0
        if (pageSize < 1) {
            pageSize = Int.MAX_VALUE
        }

        return PageRequest.of(action.pageNo, pageSize, sort)
    }

    protected fun getTableFileButtonCellData(
        fileStoreRepository: FileStoreRepository,
        fileId: Int?,
    ): List<TableButtonCellData> = fileId?.let {
        getFileList(fileStoreRepository, fileId).map { (_, url) ->
            TableButtonCellData(
                name = url.substringAfterLast('/'),
                action = AppAction(type = ActionType.FILE, url = url),
                inNewTab = true,
            )
        }
    } ?: emptyList()

    protected abstract fun getTableAddAction(action: AppAction, parentModule: String?, parentId: Int?): AppAction

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
        val formCaption = moduleConfig.caption

        val alFormCell = getFormCells(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )
        val alFormButton = getFormButtons(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_FORM,
            form = FormResponse(
                id = action.id,
                tabCaption = formCaption,
                alHeader = listOf(null to formCaption),
                alFormColumn = emptyList(),
                alFormCell = alFormCell,
                alFormButton = alFormButton,
                prevAction = action.prevAction,
            )
        )
    }

    protected abstract fun getFormCells(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): List<FormBaseCell>

    protected fun getFormFileCellData(
        fileStoreRepository: FileStoreRepository,
        fileId: Int?,
    ): List<FormFileData> = fileId?.let {
        getFileList(fileStoreRepository, fileId).map { (recordId, url) ->
            FormFileData(
                id = recordId,
                action = AppAction(type = ActionType.FILE, url = url),
                name = url.substringAfterLast('/'),
            )
        }
    } ?: emptyList()

    protected open fun getFormButtons(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
    ): List<FormButton> {

        val alFormButton = mutableListOf<FormButton>()

        val id = action.id

        val (addEnabled, editEnabled, deleteEnabled) = getFormActionPermissions(
            action = action,
            userConfig = userConfig,
            moduleConfig = moduleConfig,
        )

        if (id == null && addEnabled || id != null && editEnabled) {
            alFormButton += FormButton(
                actionType = id?.let { ActionType.FORM_EDIT } ?: ActionType.FORM_ADD,
                withNewData = true,
                caption = "Сохранить",
                iconName = IconName.SAVE,
                key = FormButtonKey.SAVE,
            )
        }

        if (id != null && deleteEnabled) {
            alFormButton += FormButton(
                actionType = ActionType.FORM_DELETE,
                withNewData = false,
                caption = "Удалить",
                iconName = IconName.DELETE,
            )
        }

        alFormButton += FormButton(
            actionType = ActionType.FORM_EXIT,
            withNewData = false,
            caption = "Выйти",
            iconName = IconName.EXIT,
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

    protected fun getNextId(): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (checkExistingId(nextId)) {
                continue
            }
            return nextId
        }
    }

    protected abstract fun checkExistingId(nextId: Int): Boolean

    //TODO: возвращать соответствующие коды/тексты ошибок
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

        val errors = when (action.type) {
            ActionType.FORM_ADD -> {
                if (addEnabled) {
                    formActionSave(
                        action = action,
                        userConfig = userConfig,
                        moduleConfig = moduleConfig,
                        formActionData = formActionData
                    )
                } else {
                    return FormActionResponse(ResponseCode.ERROR)
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
                    return FormActionResponse(ResponseCode.ERROR)
                }
            }

            ActionType.FORM_DELETE -> {
                if (deleteEnabled) {
                    formActionDelete(userConfig.id, action.id ?: return FormActionResponse(ResponseCode.ERROR))
                } else {
                    return FormActionResponse(ResponseCode.ERROR)
                }
            }

            else -> {
                null
            }
        }
        return if (errors == null) {
            FormActionResponse(ResponseCode.OK)
        } else {
            FormActionResponse(responseCode = ResponseCode.ERROR, errors = errors)
        }
    }

    protected abstract fun formActionSave(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>
    ): Map<String, String>?

    protected abstract fun formActionDelete(userId: Int, id: Int): Map<String, String>?

    protected fun formActionSaveFiles(fileStoreRepository: FileStoreRepository, formActionData: FormActionData): Int {
        val fileId = formActionData.fileId ?: getNextFileId(fileStoreRepository)

        formActionData.addFiles?.forEach { (fromClientId, fileName) ->
            addFile(fileStoreRepository, fileId, fromClientId, fileName)
        }

        formActionData.fileRemovedIds?.forEach { idForDelete ->
            deleteFile(fileStoreRepository, fileId, idForDelete)
        }

        return fileId
    }

    private fun addFile(fileStoreRepository: FileStoreRepository, fileId: Int, fromClientId: String, fileName: String) {
        val fileFromClient = File(tempDirName, fromClientId)

        val dirName = getFreeDir(fileName)
        val newFile = File(rootDirName, FileController.getFileUrl(dirName, fileName))

        fileFromClient.renameTo(newFile)

        val fileStoreEntity = FileStoreEntity(
            id = getNextFileStoreId(fileStoreRepository),
            fileId = fileId,
            name = fileName,
            dir = dirName,
        )
        fileStoreRepository.saveAndFlush(fileStoreEntity)
    }

    private fun deleteFile(fileStoreRepository: FileStoreRepository, fileId: Int, fileStoreId: Int?) {
        val fileStoreEntities = fileStoreId?.let {
            fileStoreRepository.findByIdOrNull(fileStoreId)?.let { fse ->
                listOf(fse)
            } ?: emptyList()
        } ?: run {
            fileStoreRepository.findByFileId(fileId)
        }

        for (fileStoreEntity in fileStoreEntities) {
            val deleteFile = File(rootDirName, FileController.getFileUrl(fileStoreEntity.dir, fileStoreEntity.name))
            if (deleteFile.exists()) {
                deleteFile.delete()
            }
        }

        fileStoreRepository.deleteAllInBatch(fileStoreEntities)
    }

    private fun getFileList(fileStoreRepository: FileStoreRepository, fileId: Int): List<Pair<Int, String>> =
        fileStoreRepository.findByFileIdOrderByName(fileId).map { fileStoreEntity ->
            val fileUrl = FileController.getFileUrl(fileStoreEntity.dir, fileStoreEntity.name)

            FileController.chmFileTime[fileUrl] = System.currentTimeMillis()

            Pair(fileStoreEntity.id, fileUrl)
        }

    private fun getNextFileId(fileStoreRepository: FileStoreRepository): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (fileStoreRepository.existsByFileId(nextId)) {
                continue
            }
            return nextId
        }
    }

    private fun getNextFileStoreId(fileStoreRepository: FileStoreRepository): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (fileStoreRepository.existsById(nextId)) {
                continue
            }
            return nextId
        }
    }

    private fun getFreeDir(fileName: String): String {
        var i = 0
        while (true) {
            val newDirName = getFilledNumberString(i, 8)
            val newDir = File(rootDirName, "${FileController.FILE_BASE}/$newDirName")
            newDir.mkdirs()
            val file = File(newDir, fileName)
            if (!file.exists()) {
                return newDirName
            }
            i++
        }
    }
}