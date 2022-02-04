package foatto.mms.core_mms.device

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.ds.MMSHandler
import foatto.sql.CoreAdvancedStatement

class mDeviceCommand : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_device_command"

        //----------------------------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------------------------

        val columnDeviceType = ColumnRadioButton(modelTableName, "type", "Тип устройства")
        MMSHandler.fillDeviceTypeColumn(columnDeviceType)

        val columnName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH)
        columnName.setUnique(true, "")
        val columnDescr = ColumnString(modelTableName, "descr", "Описание", STRING_COLUMN_WIDTH)
        columnDescr.setUnique(true, "")

        val columnCommand = ColumnString(modelTableName, "cmd", "Команда", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        addTableColumn(columnDeviceType)
        addTableColumn(columnName)
        addTableColumn(columnDescr)
        addTableColumn(columnCommand)

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnDeviceType)
        alFormColumn.add(columnName)
        alFormColumn.add(columnDescr)
        alFormColumn.add(columnCommand)

        //----------------------------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDescr)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_device_command_history", columnId))

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_device_command_history", "command_id"))
        alDependData.add(DependData("MMS_device", "cmd_on_id"))
        alDependData.add(DependData("MMS_device", "cmd_off_id"))
    }
}
