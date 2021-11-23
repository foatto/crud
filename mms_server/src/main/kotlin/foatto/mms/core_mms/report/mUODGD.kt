package foatto.mms.core_mms.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstractReport
import foatto.mms.core_mms.UODGSelector
import foatto.sql.CoreAdvancedStatement

class mUODGD : mAbstractReport() {

    lateinit var uodg: UODGSelector
        private set

    lateinit var columnReportBegDate: ColumnDate3Int
        private set
    lateinit var columnReportEndDate: ColumnDate3Int
        private set

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        //int[] arrDT = MMSFunction.getDayShiftWorkParent( dataWorker.alStm.get( 0 ), timeZone, hmParentData, false );

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnReportBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода")
        //columnReportBegDate.setDefaultDate( arrDT[ 0 ], arrDT[ 1 ], arrDT[ 2 ] );
        columnReportBegDate.isVirtual = true
        columnReportEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Дата окончания периода")
        //columnReportEndDate.setDefaultDate( arrDT[ 6 ], arrDT[ 7 ], arrDT[ 8 ] );
        columnReportEndDate.isVirtual = true

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(modelTableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        alFormColumn.add(columnReportBegDate)
        alFormColumn.add(columnReportEndDate)

    }
}
