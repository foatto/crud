package foatto.server.service

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getSplittedDouble
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.checkRowPermission
import foatto.server.entity.DayWorkEntity
import foatto.server.getEnabledUserIds
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.DayWorkRepository
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorCalibrationRepository
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class DayWorkService(
    private val entityManager: EntityManager,
    private val dayWorkRepository: DayWorkRepository,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
    private val sensorCalibrationRepository: SensorCalibrationRepository,
    private val calcService: CalcService,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        //        private const val FIELD_USER_ID = "userId"
//        private const val FIELD_OBJECT = "obj"
        private const val FIELD_DAY = "day"

        //        private const val FIELD_OBJECT_ID = "obj.id"
        private const val FIELD_DAY_YE = "day.ye"
        private const val FIELD_DAY_MO = "day.mo"
        private const val FIELD_DAY_DA = "day.da"
        private const val FIELD_OBJECT_NAME = "obj.name"    // отдельное определение для сортировки в Hibernate
//        private const val FIELD_OBJECT_MODEL = "obj.model"  // отдельное определение для сортировки в Hibernate
    }
    /*
            private const val FIELD_COPY_SENSORS = "_copySensors"
     */

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "" // by date group
        alColumnInfo += null to "" // userId
        alColumnInfo += null to "Наименование / Модель / Подразделение / Группа"
        alColumnInfo += null to "Работа оборудования [час]"
        alColumnInfo += null to "Э/энергия [кВт*ч]"
        alColumnInfo += null to "Расход топлива [л]"

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

        val timeZone = getTimeZone(userConfig.timeOffset)

        val pageRequest = getTableSortedPageRequest(
            action,
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_YE),
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_MO),
            Sort.Order(Sort.Direction.DESC, FIELD_DAY_DA),
            Sort.Order(Sort.Direction.ASC, FIELD_OBJECT_NAME),
        )
        val findText = action.findText?.trim() ?: ""

        val enabledUserIds = getEnabledUserIds(action.module, action.type, userConfig.relatedUserIds, userConfig.roles)

        val parentObjectId = if (action.parentModule == AppModuleMMS.OBJECT) {
            action.parentId
        } else {
            null
        }
        val parentObjectEntity = parentObjectId?.let {
            objectRepository.findByIdOrNull(parentObjectId)
        }

        val page: Page<DayWorkEntity> = parentObjectEntity?.let {
            if (findText.isNotEmpty()) {
                dayWorkRepository.findByObjAndUserIdInAndFilter(parentObjectEntity, enabledUserIds, findText, pageRequest)
            } else {
                dayWorkRepository.findByObjAndUserIdIn(parentObjectEntity, enabledUserIds, pageRequest)
            }
        } ?: run {
            if (findText.isNotEmpty()) {
                dayWorkRepository.findByUserIdInAndFilter(enabledUserIds, findText, pageRequest)
            } else {
                dayWorkRepository.findByUserIdIn(enabledUserIds, pageRequest)
            }
        }
        fillTablePageButtons(action, page.totalPages, alPageButton)
        val dayWorkEntities = page.content

        var prevGroupName: String? = null
        for (dayWorkEntity in dayWorkEntities) {

            val objectEntity = dayWorkEntity.obj ?: continue
            val dayEntity = dayWorkEntity.day ?: continue
            val ye = dayEntity.ye ?: continue
            val mo = dayEntity.mo ?: continue
            val da = dayEntity.da ?: continue

            var col = 0

            val rowOwnerShortName = userConfig.shortNames[dayWorkEntity.userId]
            val rowOwnerFullName = userConfig.fullNames[dayWorkEntity.userId]

            val isFormEnabled = checkRowPermission(
                module = action.module,
                actionType = ActionType.MODULE_FORM,
                rowUserRelation = userConfig.relatedUserIds[dayWorkEntity.userId],
                userRoles = userConfig.roles
            )

            val groupName = getDateEntityDMYString(dayEntity)
            if (prevGroupName != groupName) {
                alTableCell += TableSimpleCell(
                    row = row,
                    col = col,
                    colSpan = 6,
                    dataRow = row,
                    name = groupName,
                    backColorType = TableCellBackColorType.GROUP_0,
                    isBoldText = true,
                )
                prevGroupName = groupName
                alTableRowData += TableRowData()
                row++
            }
            alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = "", backColorType = TableCellBackColorType.GROUP_0)

            alTableCell += getTableUserNameCell(
                row = row,
                col = col++,
                userId = userConfig.id,
                rowUserId = dayWorkEntity.userId,
                rowOwnerShortName = rowOwnerShortName,
                rowOwnerFullName = rowOwnerFullName
            )

            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = (objectEntity.name ?: "-") +
                    "\n${objectEntity.model ?: "-"}" +
                    "\n${objectEntity.department?.name ?: "-"}" +
                    "\n${objectEntity.group?.name ?: "-"}"
            )

            val begTime = LocalDateTime(ye, mo, da, 0, 0).toInstant(timeZone).epochSeconds.toInt()
            val endTime = begTime + 86_400

            val byGroupLiquidSum = sortedMapOf<String, SortedMap<String, Double>>()
            val allLiquidSum = sortedMapOf<String, Double>()

            val tmWork = calcService.calcWork(objectEntity, begTime, endTime, byGroupLiquidSum, allLiquidSum)
            val tmEnergo = calcService.calcEnergo(objectEntity, begTime, endTime)
            calcService.calcLiquidUsing(objectEntity, begTime, endTime, byGroupLiquidSum, allLiquidSum)

            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = tmWork.map { (descr, workCalcData) ->
                    "$descr = ${workCalcData.onTime}"
                }.joinToString("\n")
            )
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = tmEnergo.map { (descr, value) ->
                    "$descr = ${getSplittedDouble(value, 1)}"
                }.joinToString("\n")
            )
            alTableCell += TableSimpleCell(
                row = row,
                col = col++,
                dataRow = row,
                name = allLiquidSum.map { (descr, value) ->
                    "$descr = ${getSplittedDouble(value, 1)}"
                }.joinToString("\n")
            )

            val formOpenAction = AppAction(
                type = ActionType.MODULE_FORM,
                module = action.module,
                id = dayWorkEntity.id,
                parentModule = action.parentModule,
                parentId = action.parentId
            )

            val alPopupData = getPopupDatas(
                userConfig = userConfig,
                objectId = objectEntity.id,
                begTime = begTime,
                endTime = endTime,
                isFormEnabled = isFormEnabled,
                formOpenAction = formOpenAction,
            )

            alTableRowData += TableRowData(
                formAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                rowAction = if (isFormEnabled) {
                    formOpenAction
                } else {
                    null
                },
                isRowUrlInNewTab = false,
                gotoAction = null,
                isGotoUrlInNewTab = true,
                alPopupData = alPopupData,
            )

            if (dayWorkEntity.id == action.id) {
                currentRowNo = row
            }

            row++
        }
        return currentRowNo
    }

    private fun getPopupDatas(
        userConfig: ServerUserConfig,
        objectId: Int,
        begTime: Int,
        endTime: Int,
        isFormEnabled: Boolean,
        formOpenAction: AppAction,
    ): List<TablePopupData> {
        val alPopupData = mutableListOf<TablePopupData>()

        if (isFormEnabled) {
            alPopupData += TablePopupData(
                action = formOpenAction,
                text = "Открыть",
                inNewTab = false,
            )
        }

        getTableChartPopupData(userConfig, AppModuleMMS.CHART_LIQUID_LEVEL, objectId, begTime, endTime, alPopupData)
        getTableChartPopupData(userConfig, AppModuleMMS.CHART_ANALOGUE_BY_TYPE, objectId, begTime, endTime, alPopupData)

        getTableReportPopupData(userConfig, AppModuleMMS.REPORT_SUMMARY, objectId, begTime, endTime, alPopupData)

        if (checkAccessPermission(AppModuleMMS.MAP_TRACE, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Карты",
                action = AppAction(
                    type = ActionType.MODULE_MAP,
                    module = AppModuleMMS.MAP_TRACE,
                    id = objectId,
                    begTime = begTime,
                    endTime = endTime,
                ),
                text = appModuleConfigs[AppModuleMMS.MAP_TRACE]?.caption ?: "(неизвестный тип карты)",
                inNewTab = true,
            )
        }
//        if (checkAccessPermission(AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE, userConfig.roles)) {
//            alPopupData += TablePopupData(
//                group = "Контрольные схемы",
//                action = AppAction(
//                    type = ActionType.MODULE_SCHEME,
//                    module = AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE,
//                    id = objectId,
//                ),
//                text = appModuleConfigs[AppModuleMMS.SCHEME_ANALOGUE_INDICATOR_STATE]?.caption ?: "(неизвестный тип схемы)",
//                inNewTab = true,
//            )
//        }
        if (checkAccessPermission(AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Контрольные панели",
                action = AppAction(
                    type = ActionType.MODULE_COMPOSITE,
                    module = AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD,
                    id = objectId,
                ),
                text = appModuleConfigs[AppModuleMMS.COMPOSITE_OBJECT_DASHBOARD]?.caption ?: "(неизвестный тип контрольной панели)",
                inNewTab = true,
            )
        }

        return alPopupData
    }

    private fun getTableChartPopupData(
        userConfig: ServerUserConfig,
        module: String,
        objectId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopupData>
    ) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Графики",
                action = AppAction(
                    type = ActionType.MODULE_CHART,
                    module = module,
                    id = objectId,
                    timeRangeType = 0,
                    begTime = begTime,
                    endTime = endTime,
                    //!!! где-то здесь надо передавать конкретный тип аналогового датчика (пока будем выводить графики по всем аналоговым датчикам сразу)
                ),
                text = appModuleConfigs[module]?.caption ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
    }

    private fun getTableReportPopupData(
        userConfig: ServerUserConfig,
        module: String,
        objectId: Int,
        begTime: Int,
        endTime: Int,
        alPopupData: MutableList<TablePopupData>
    ) {
        if (checkAccessPermission(module, userConfig.roles)) {
            alPopupData += TablePopupData(
                group = "Отчёты",
                action = AppAction(
                    type = ActionType.MODULE_FORM,
                    module = module,
                    id = null,
                    parentModule = AppModuleMMS.OBJECT,
                    parentId = objectId,
                    begTime = begTime,
                    endTime = endTime,
                ),
                text = appModuleConfigs[module]?.caption ?: "(неизвестный тип модуля: '$module')",
                inNewTab = true,
            )
        }
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> {
        TODO("Not yet implemented")
    }

    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> {
        TODO("Not yet implemented")
    }

    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse {
        TODO("Not yet implemented")
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse {
        TODO("Not yet implemented")
    }

}
/*
    lateinit var os: ObjectSelector
        private set

    lateinit var columnDate: ColumnDate3Int
        private set

    lateinit var columnRun: ColumnString
        private set

    lateinit var columnWork: ColumnGrid
        private set

    lateinit var columnEnergo: ColumnGrid
        private set

    lateinit var columnLiquid: ColumnGrid
        private set

    lateinit var columnLevel: ColumnGrid
        private set

    lateinit var columnLevelLiquid: ColumnGrid
        private set

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_day_work"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")
        columnUser = ColumnInt(modelTableName, "user_id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Дата")

        columnRun = ColumnString(modelTableName, "_run", "Пробег [км]", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isSearchable = false
        }
        columnWork = ColumnGrid(modelTableName, "_work", "Работа оборудования [час]").apply {
            isVirtual = true
            isSearchable = false
        }
        columnEnergo = ColumnGrid(modelTableName, "_energo", "Расход/генерация э/энергии").apply {
            isVirtual = true
            isSearchable = false
        }
//        columnGroupSumEnergoName = ColumnString(tableName, "_group_energo_name", "Э/энергия", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumEnergoValue = ColumnString(tableName, "_group_energo_value", "Расход/Генерация", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
        columnLiquid = ColumnGrid(modelTableName, "_liquid_name", "Расход топлива").apply {
            isVirtual = true
            isSearchable = false
        }
//        columnGroupSumLiquidName = ColumnString(tableName, "_group_liquid_name", "Топливо", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
//        columnGroupSumLiquidValue = ColumnString(tableName, "_group_liquid_value", "Расход", STRING_COLUMN_WIDTH).apply {
//            isVirtual = true
//            isSearchable = false
//        }
        columnLevel = ColumnGrid(modelTableName, "_level", "Ёмкость, Нач.остаток, Кон.остаток").apply {
            isVirtual = true
            isSearchable = false
        }
        columnLevelLiquid = ColumnGrid(modelTableName, "_level_liquid_name", "Топливо, Заправка, Слив").apply {
            isVirtual = true
            isSearchable = false
        }

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)

        alTableGroupColumn.add(columnDate)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)

        //----------------------------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = true,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = false,
            addedStaticColumnCount = 0
        )

        //----------------------------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnRun)
        addTableColumn(columnWork)
        addTableColumn(columnEnergo)
        addTableColumn(columnLiquid)
        addTableColumn(columnLevel)
        addTableColumn(columnLevelLiquid)

        alFormColumn.add(columnDate)
        alFormColumn.add(columnRun)
        //alFormColumn.add(columnWork)

        //alFormColumn.add(columnEnergo)
//        alFormColumn.add(columnGroupSumEnergoName)
//        alFormColumn.add(columnGroupSumEnergoValue)
        //alFormColumn.add(columnAllSumEnergo)

        //alFormColumn.add(columnLiquid)
//        alFormColumn.add(columnGroupSumLiquidName)
//        alFormColumn.add(columnGroupSumLiquidValue)
        //alFormColumn.add(columnAllSumLiquid)

        //alFormColumn.add(columnLevel)

        //alFormColumn.add(columnLevelLiquid)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnDate, false)
        addTableSort(os.columnObjectName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        //--- define the user of mobile objects only
        val hsPermissionDW = userConfig.userPermission["mms_day_work"]
        val hsPermissionSW = userConfig.userPermission["mms_shift_work"]
        val isMovingMode = hsPermissionDW != null && hsPermissionDW.contains(cStandart.PERM_ACCESS) &&
            (hsPermissionSW == null || !hsPermissionSW.contains(cStandart.PERM_ACCESS))

        if (isMovingMode) {
            alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM, true, true))
            alChildData.add(ChildData("mms_show_object", columnId, AppAction.FORM))
        }

        MMSFunction.fillChildDataForPeriodicReports(columnId, alChildData)
        MMSFunction.fillChildDataForLiquidIncDecReports(columnId, alChildData, withIncWaybillReport = true, newGroup = false)
        alChildData.add(ChildData("Отчёты", "mms_report_work_detail", columnId, AppAction.FORM))
        MMSFunction.fillChildDataForGeoReports(columnId, alChildData, withMovingDetailReport = true)
        MMSFunction.fillChildDataForEnergoOverReports(columnId, alChildData)
        MMSFunction.fillChildDataForOverReports(columnId, alChildData)
        alChildData.add(ChildData("Отчёты", "mms_report_data_out", columnId, AppAction.FORM))

        MMSFunction.fillAllChildDataForGraphics(columnId, alChildData)

        if (!isMovingMode) {
            alChildData.add(ChildData("mms_show_object", columnId, AppAction.FORM, true))
            alChildData.add(ChildData("mms_show_trace", columnId, AppAction.FORM))
        }
    }

 */