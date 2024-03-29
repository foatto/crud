package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedConnection
import java.time.LocalDate

class mDayWork : mAbstractReport() {

    companion object {
        val GROUP_BY_DATE = 0
        val GROUP_BY_OBJECT = 1
    }

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnReportBegDate: ColumnDate3Int
        private set
    lateinit var columnReportEndDate: ColumnDate3Int
        private set
    lateinit var columnReportGroupType: ColumnComboBox
        private set

    lateinit var sros: SummaryReportOptionSelector
        private set

    lateinit var sos: SumOptionSelector
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val arrDT = MMSFunction.getDayWorkParent(conn, hmParentData)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnReportBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Начало периода").apply {
            arrDT?.let {
                default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
            }
            isVirtual = true
        }

        columnReportEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Конец периода").apply {
            arrDT?.let {
                default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
            }
            isVirtual = true
        }

        columnReportGroupType = ColumnComboBox(modelTableName, "object_date_group_type", "Группировка", GROUP_BY_OBJECT).apply {
            addChoice(GROUP_BY_OBJECT, "По объектам")
            addChoice(GROUP_BY_DATE, "По датам")
            isVirtual = true
            setSavedDefault(userConfig)
        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(application, modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        alFormColumn += columnReportBegDate
        alFormColumn += columnReportEndDate
        alFormColumn += columnReportGroupType

        sros = SummaryReportOptionSelector()
        sros.fillColumns(userConfig, modelTableName, alFormColumn)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, modelTableName, alFormColumn)
    }
}
