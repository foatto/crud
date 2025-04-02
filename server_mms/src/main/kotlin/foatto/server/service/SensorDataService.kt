package foatto.server.service

import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.model.response.table.cell.TableSimpleCell
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getTimeZone
import foatto.core_mms.AppModuleMMS
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.repository.SensorRepository
import jakarta.persistence.EntityManager
import kotlinx.datetime.TimeZone
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class SensorDataService(
    private val entityManager: EntityManager,
    private val sensorRepository: SensorRepository,
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        //!!! попробовать размеры побольше
        private const val PAGE_SIZE_IN_SEC = 900    // 3600  // 10_800 // 21_600 // 43_200 // 86_400
    }

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction, String>> {
        val alColumnInfo = mutableListOf<Pair<String?, String>>()

        alColumnInfo += null to "Время (UTC)"
        alColumnInfo += null to "Время (местное)"
        alColumnInfo += null to "Время 2 (UTC)"
        alColumnInfo += null to "Время 2 (местное)"
        alColumnInfo += null to "Тип"
        alColumnInfo += null to "Значение"
        alColumnInfo += null to "Значение 2"
        alColumnInfo += null to "Значение 3"
        alColumnInfo += null to "Значение 4"

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
        var row = 0

        val parentSensorId = if (action.parentModule == AppModuleMMS.SENSOR) {
            action.parentId ?: return null
        } else {
            return null
        }
        val parentSensorEntity = sensorRepository.findByIdOrNull(parentSensorId) ?: return null

        SensorService.checkAndCreateAggTable(entityManager, parentSensorEntity.id)

        val zoneUTC = getTimeZone(0)
        val zoneLocal = getTimeZone(userConfig.timeOffset)

        var firstTimeUTC = 0
        var lastTimeUTC = 0
        queryNativeSql(
            entityManager,
            """
                SELECT MIN(ontime_0), MAX(ontime_0)
                FROM MMS_agg_$parentSensorId
            """
        ) { rs ->
            if (rs.next()) {
                firstTimeUTC = rs.getInt(1)
                lastTimeUTC = rs.getInt(2)
            }
        }

        val currentPageNo = lastTimeUTC / PAGE_SIZE_IN_SEC - action.pageNo
        val begPageTime = currentPageNo * PAGE_SIZE_IN_SEC
        val endPageTime = begPageTime + PAGE_SIZE_IN_SEC

        queryNativeSql(
            entityManager,
            """
                SELECT ontime_0 , ontime_1 , type_0 , value_0 , value_1 , value_2 , value_3
                FROM MMS_agg_$parentSensorId
                WHERE ontime_0 BETWEEN $begPageTime AND $endPageTime
                ORDER BY ontime_0 DESC
            """
        ) { rs ->
            while (rs.next()) {
                var pos = 1

                val ontime0 = rs.getInt(pos++)
                val ontime1 = rs.getInt(pos++)
                val type0 = rs.getInt(pos++)
                val value0 = rs.getDouble(pos++)
                val value1 = rs.getDouble(pos++)
                val value2 = rs.getDouble(pos++)
                val value3 = rs.getDouble(pos++)

                var col = 0
                alTableCell += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUTC, ontime0)
                )
                alTableCell += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneLocal, ontime0)
                )
                alTableCell += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneUTC, ontime1)
                )
                alTableCell += TableSimpleCell(
                    row = row,
                    col = col++,
                    dataRow = row,
                    name = getDateTimeDMYHMSString(zoneLocal, ontime1)
                )
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = type0.toString())
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = value0.toString())
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = value1.toString())
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = value2.toString())
                alTableCell += TableSimpleCell(row = row, col = col++, dataRow = row, name = value3.toString())

                alTableRowData += TableRowData(
                    formAction = null,
                    rowAction = null,
                    isRowUrlInNewTab = false,
                    gotoAction = null,
                    isGotoUrlInNewTab = true,
                    alPopupData = emptyList(),
                )
                row++
            }
        }

        val pageCount = lastTimeUTC / PAGE_SIZE_IN_SEC - firstTimeUTC / PAGE_SIZE_IN_SEC + 1

        //--- first page
        if (action.pageNo > 0) {
            alPageButton += action.copy(pageNo = 0) to getPageCaption(zoneUTC, lastTimeUTC / PAGE_SIZE_IN_SEC * PAGE_SIZE_IN_SEC)
        }
        //--- empty
        if (action.pageNo > 2) {
            alPageButton += null to "..."
        }
        //--- prev page
        if (action.pageNo > 1) {
            alPageButton += action.copy(pageNo = action.pageNo - 1) to getPageCaption(zoneUTC, (currentPageNo + 1) * PAGE_SIZE_IN_SEC)
        }
        //--- current page
        alPageButton += null to getPageCaption(zoneUTC, currentPageNo * PAGE_SIZE_IN_SEC)
        //--- next page
        if (action.pageNo < pageCount - 2) {
            alPageButton += action.copy(pageNo = action.pageNo + 1) to getPageCaption(zoneUTC, (currentPageNo - 1) * PAGE_SIZE_IN_SEC)
        }
        //--- empty
        if (action.pageNo < pageCount - 3) {
            alPageButton += null to "..."
        }
        //--- last page
        if (action.pageNo < pageCount - 1) {
            alPageButton += action.copy(pageNo = pageCount - 1) to getPageCaption(zoneUTC, firstTimeUTC / PAGE_SIZE_IN_SEC * PAGE_SIZE_IN_SEC)
        }

        return null
    }

     private fun getPageCaption(timeZone: TimeZone, time: Int): String {
        val caption = getDateTimeDMYHMSString(timeZone, time)
        //--- remove last seconds digits
        return caption.substring(0, caption.length - 3)
    }

    override fun getFormCells(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, addEnabled: Boolean, editEnabled: Boolean): List<FormBaseCell> = emptyList()
    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)
    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(ResponseCode.ERROR)
}