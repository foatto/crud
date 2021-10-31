package foatto.ts.core_ts.device

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.ObjectSelector
import java.time.ZonedDateTime

class mDeviceCommandHistory : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        tableName = "TS_device_command_history"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id", userConfig.userId)

        //----------------------------------------------------------------------------------------------------------------------

        val columnDeviceId = ColumnInt("TS_device", "id")
        val columnDevice = ColumnInt(tableName, "device_id", columnDeviceId)
        val columnDeviceSerialNo = ColumnString("TS_device", "serial_no", "Серийный номер прибора", STRING_COLUMN_WIDTH)

        columnDeviceSerialNo.apply {
            selectorAlias = "ts_device"
            addSelectorColumn(columnDevice, columnDeviceId)
            addSelectorColumn(this)
        }

        val columnCommand = ColumnString(tableName, "command", "Команда", STRING_COLUMN_WIDTH).apply {
            isEditable = false
            formPinMode = FormPinMode.OFF
        }

        val columnCreateTime = ColumnDateTimeInt(tableName, "create_time", "Время создания", true, zoneId).apply {
            isEditable = false
        }

        val columnSendStatus = ColumnBoolean(tableName, "send_status", "Статус отправки", false)

        val columnSendTime = ColumnDateTimeInt(tableName, "send_time", "Время отправки", true, zoneId).apply {
            default = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, zoneId)
            isEditable = false
        }

        //--- вручную добавленное поле для обозначения владельца объекта

        val columnObjectUserName = ColumnComboBox("TS_object", "user_id", "Пользователь")
        columnObjectUserName.addChoice(0, "")
        for ((userID, userName) in UserConfig.hmUserFullNames) {
            columnObjectUserName.addChoice(
                userID,
                if (userID == userConfig.userId || userName.trim().isEmpty()) {
                    ""
                } else {
                    userName
                }
            )
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnDevice)

        addTableColumn(columnDeviceSerialNo)
        addTableColumn(columnCommand)
        addTableColumn(columnCreateTime)
        addTableColumn(columnSendStatus)
        addTableColumn(columnSendTime)

        addTableColumn(columnObjectUserName)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnDevice)

        alFormColumn.add(columnDeviceSerialNo)
        alFormColumn.add(columnCommand)
        alFormColumn.add(columnCreateTime)
        alFormColumn.add(columnSendStatus)
        alFormColumn.add(columnSendTime)

        //----------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = false,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = false,
            addedStaticColumnCount = -1
        )

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnCreateTime)
        alTableSortDirect.add("DESC")

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["ts_device"] = columnDevice
    }
}