package foatto.mms.core_mms.report

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement
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
    lateinit var columnIsCompactReport: ColumnBoolean
        private set

    lateinit var sos: SumOptionSelector
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val arrADR = MMSFunction.getDayWorkParent(stm, hmParentData)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnReportBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Начало периода")
        if(arrADR != null) columnReportBegDate.default = LocalDate.of(arrADR[0], arrADR[1], arrADR[2])
        columnReportBegDate.isVirtual = true

        columnReportEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Конец периода")
        if(arrADR != null) columnReportEndDate.default = LocalDate.of(arrADR[0], arrADR[1], arrADR[2])
        columnReportEndDate.isVirtual = true

        columnReportGroupType = ColumnComboBox(tableName, "object_date_group_type", "Группировка", GROUP_BY_OBJECT)
        columnReportGroupType.addChoice(GROUP_BY_OBJECT, "По объектам")
        columnReportGroupType.addChoice(GROUP_BY_DATE, "По датам")
        columnReportGroupType.isVirtual = true
        columnReportGroupType.setSavedDefault(userConfig)

        columnIsCompactReport = ColumnBoolean(tableName, "is_compact", "Компактный вид", false)
        columnIsCompactReport.isVirtual = true
        columnIsCompactReport.setSavedDefault(userConfig)

        initReportCapAndSignature(aliasConfig, userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        alFormColumn.add(columnReportBegDate)
        alFormColumn.add(columnReportEndDate)
        alFormColumn.add(columnReportGroupType)
        alFormColumn.add(columnIsCompactReport)

        sos = SumOptionSelector()
        sos.fillColumns(userConfig, tableName, alFormColumn)

        addCapAndSignatureColumns()
    }
}
