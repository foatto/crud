package foatto.shop

import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UniqueColumnData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mWorkHour : mAbstract() {

    lateinit var columnWorker: ColumnRadioButton
    lateinit var columnWorkDate: ColumnDate3Int

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

        //----------------------------------------------------------------------------------------------------------------------

        val alWorkerId = (application as iShopApplication).alWorkHourUserId

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_work_hour"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnWorker = ColumnRadioButton(modelTableName, "worker_id", "Работник").apply {
            alWorkerId.forEach { sUserId ->
                val userId = sUserId.toInt()
                addChoice(userId, UserConfig.hmUserFullNames[userId] ?: "(неизвестный работник)")
            }
            setSavedDefault(userConfig)
        }

        columnWorkDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Дата")

        val columnWorkHour = ColumnDouble(
            aTableName = modelTableName,
            aFieldName = "work_hour",
            aCaption = "Отработано [час]",
            aCols = 10,
            aPrecision = 1,
            aDefaultValue = application.workHourInWorkDay?.toDoubleOrNull() ?: 9.0
        ).apply {
            setEmptyData(0.0, "-")
            minValue = 0.0
            maxValue = 12.0
            tableAlign = TableCellAlign.CENTER
        }

        val columnHourTax = ColumnDouble(
            aTableName = modelTableName,
            aFieldName = "hour_tax",
            aCaption = "Ставка [руб/час]",
            aCols = 10,
            aPrecision = 1,
            aDefaultValue = application.alWorkDayHourTax.firstOrNull()?.toDoubleOrNull() ?: 90.0
        ).apply {
            setEmptyData(0.0, "-")
            minValue = 0.0
//            maxValue = 12.0
            tableAlign = TableCellAlign.CENTER
        }

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(
            listOf(
                UniqueColumnData(columnWorker),
                UniqueColumnData(columnWorkDate),
            )
        )

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        alTableGroupColumn.add(columnWorkDate)

        addTableColumn(columnWorker)
        addTableColumn(columnWorkHour)
        addTableColumn(columnHourTax)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnWorker)
        alFormColumn.add(columnWorkDate)
        alFormColumn.add(columnWorkHour)
        alFormColumn.add(columnHourTax)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnWorkDate, false)
        addTableSort(columnWorker, true)
    }
}