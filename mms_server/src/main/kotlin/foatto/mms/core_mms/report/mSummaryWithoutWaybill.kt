package foatto.mms.core_mms.report

import foatto.core.link.FormPinMode
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.*
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.MMSFunction
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate
import java.time.LocalTime

class mSummaryWithoutWaybill : mAbstractReport() {

    companion object {
        val TIME_TYPE_DOC = 0
        val TIME_TYPE_FACT = 1
    }

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnReportBegDate: ColumnDate3Int
        private set

    lateinit var columnReportBegTime: ColumnTime3Int
        private set

    lateinit var columnReportEndDate: ColumnDate3Int
        private set

    lateinit var columnReportEndTime: ColumnTime3Int
        private set

    lateinit var columnTimeType: ColumnComboBox
        private set

//    lateinit var sos: SumOptionSelector
//        private set

//----------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val arrDT = MMSFunction.getDayShiftWorkParent( stm, zoneId, hmParentData, false )

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        columnReportBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Начало периода")
            /*if( arrADR != null ) */columnReportBegDate.default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
            columnReportBegDate.isVirtual = true

        columnReportBegTime = ColumnTime3Int(tableName, "beg_ho", "beg_mi", null, "Время начала периода")
            /*if( arrADR != null ) */columnReportBegTime.default = LocalTime.of(arrDT[3], arrDT[4], arrDT[5])
            columnReportBegTime.isVirtual = true
            columnReportBegTime.formPinMode = FormPinMode.ON

        columnReportEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Конец периода")
            /*if( arrADR != null ) */columnReportEndDate.default = LocalDate.of(arrDT[6], arrDT[7], arrDT[8])
            columnReportEndDate.isVirtual = true

        columnReportEndTime = ColumnTime3Int(tableName, "end_ho", "end_mi", null, "Время окончания периода")
            /*if( arrADR != null ) */columnReportEndTime.default = LocalTime.of(arrDT[9], arrDT[10], arrDT[11])
            columnReportEndTime.isVirtual = true
            columnReportEndTime.formPinMode = FormPinMode.ON

        columnTimeType = ColumnComboBox( tableName, "waybill_time_range_type", "Используемое время начала/окончания", 0 )
            columnTimeType.addChoice( TIME_TYPE_DOC, "Заявленное" )
            columnTimeType.addChoice( TIME_TYPE_FACT, "Фактическое" )
            columnTimeType.isVirtual = true
            columnTimeType.setSavedDefault( userConfig )

        initReportCapAndSignature( aliasConfig, userConfig )

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add( columnID!! )

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns( tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn )

        alFormColumn.add( columnReportBegDate )
        alFormColumn.add( columnReportBegTime )
        alFormColumn.add( columnReportEndDate )
        alFormColumn.add( columnReportEndTime )
        alFormColumn.add( columnTimeType )

//        sos = SumOptionSelector()
//        sos!!.fillColumns(userConfig, tableName, alFormColumn)

        addCapAndSignatureColumns()
    }
}
