package foatto.shop.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstractReport
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate

class mWorkHour : mAbstractReport() {

    lateinit var columnMonth: ColumnInt
    lateinit var columnYear: ColumnInt

    override fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val now = LocalDate.now()

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = FAKE_TABLE_NAME    //"SHOP_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnMonth = ColumnInt(modelTableName, "_month", "Месяц", 10, now.monthValue).apply {
            isVirtual = true
            minValue = 1
            maxValue = 12
            setSavedDefault(userConfig)
        }

        columnYear = ColumnInt(modelTableName, "_year", "Год", 10, now.year).apply {
            isVirtual = true
            minValue = 2022
            setSavedDefault(userConfig)
        }

        //----------------------------------------------------------------------------------------------------------------------

        //        initReportCapAndSignature(  aliasConfig, userConfig  );

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnMonth)
        alFormColumn.add(columnYear)
    }
}