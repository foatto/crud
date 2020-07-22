package foatto.mms.core_mms.report

import foatto.app.CoreSpringController
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

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- отдельная обработка перехода от журнала суточных работ/рабочих смен/путёвок/журнала сменных работ
        //int[] arrDT = MMSFunction.getDayShiftWorkParent( dataWorker.alStm.get( 0 ), timeZone, hmParentData, false );

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnReportBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода")
        //columnReportBegDate.setDefaultDate( arrDT[ 0 ], arrDT[ 1 ], arrDT[ 2 ] );
        columnReportBegDate.isVirtual = true
        columnReportEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", "Дата окончания периода")
        //columnReportEndDate.setDefaultDate( arrDT[ 6 ], arrDT[ 7 ], arrDT[ 8 ] );
        columnReportEndDate.isVirtual = true

        initReportCapAndSignature(aliasConfig, userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        //----------------------------------------------------------------------------------------------------------------------

        uodg = UODGSelector()
        uodg.fillColumns(tableName, userConfig, hmParentColumn, alFormHiddenColumn, alFormColumn)

        alFormColumn.add(columnReportBegDate)
        alFormColumn.add(columnReportEndDate)

        addCapAndSignatureColumns()
    }
}
