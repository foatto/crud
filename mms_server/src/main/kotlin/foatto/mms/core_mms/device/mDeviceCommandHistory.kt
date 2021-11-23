package foatto.mms.core_mms.device

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
import foatto.mms.core_mms.ObjectSelector
import foatto.sql.CoreAdvancedStatement
import java.time.ZonedDateTime

class mDeviceCommandHistory : mAbstract() {

    lateinit var columnEditTime: ColumnDateTimeInt
        private set

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------------------------

        var parentObjectId = hmParentData["mms_object"]
        val parentDeviceID = hmParentData["mms_device"]

        //--- если переход из списка контроллеров, то подгрузим соответствующий объект ( если прикреплён )
        if (parentObjectId == null && parentDeviceID != null) {
            val rs = stm.executeQuery(" SELECT object_id FROM MMS_device WHERE id = $parentDeviceID ")
            rs.next()
            parentObjectId = rs.getInt(1)
            rs.close()

            hmParentData["mms_object"] = parentObjectId
        }

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_device_command_history"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")
        columnUser = ColumnInt(modelTableName, "user_id", userConfig.userId)

        //----------------------------------------------------------------------------------------------------------------------

        val columnDevice = ColumnComboBox(modelTableName, "device_id", "Номер устройства")
        if (parentDeviceID != null) {
            columnDevice.defaultValue = parentDeviceID
            columnDevice.addChoice(parentDeviceID, parentDeviceID.toString())
        } else if (parentObjectId != null) {
            val rs = stm.executeQuery(" SELECT id FROM MMS_device WHERE object_id = $parentObjectId ORDER BY device_index ")
            //--- первая строка станет дефолтным значение
            if (rs.next()) {
                val deviceID = rs.getInt(1)
                columnDevice.defaultValue = deviceID
                columnDevice.addChoice(deviceID, deviceID.toString())
            }
            while (rs.next()) {
                val deviceID = rs.getInt(1)
                columnDevice.addChoice(deviceID, deviceID.toString())
            }
            rs.close()
        }
        //--- хоть что-нибудь, чтобы ошибок не было
        else {
            columnDevice.defaultValue = 0
            columnDevice.addChoice(0, "0")
        }

        val columnCommandID = ColumnInt("MMS_device_command", "id")
        val columnCommand = ColumnInt(modelTableName, "command_id", columnCommandID)
        val columnCommandName = ColumnString("MMS_device_command", "name", "Наименование", STRING_COLUMN_WIDTH)
        columnCommandName.isRequired = true
        columnCommandName.formPinMode = FormPinMode.OFF
        val columnCommandDescr = ColumnString("MMS_device_command", "descr", "Описание", STRING_COLUMN_WIDTH)
        val columnCommandCommand = ColumnString("MMS_device_command", "cmd", "Команда", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnCommandName.selectorAlias = "mms_device_command"
        columnCommandName.addSelectorColumn(columnCommand, columnCommandID)
        columnCommandName.addSelectorColumn(columnCommandName)
        columnCommandName.addSelectorColumn(columnCommandDescr)
        columnCommandName.addSelectorColumn(columnCommandCommand)

        columnEditTime = ColumnDateTimeInt(modelTableName, "edit_time", "Время последнего редактирования", true, zoneId)
        columnEditTime.isEditable = false
        columnEditTime.formPinMode = FormPinMode.OFF

        val columnForSend = ColumnBoolean(modelTableName, "for_send", "Отправить команду", true)
        columnForSend.formPinMode = FormPinMode.OFF

        val columnSendTime = ColumnDateTimeInt(modelTableName, "send_time", "Время отправки", true, zoneId)
        columnSendTime.default = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, zoneId)
        columnSendTime.isEditable = false

        //--- вручную добавленное поле для обозначения владельца а/м ---

        val columnObjectUserName = ColumnComboBox("MMS_object", "user_id", "Пользователь")
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
        alTableHiddenColumn.add(columnCommand)

        addTableColumn(columnDevice)
        addTableColumn(columnObjectUserName)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnCommand)

        alFormColumn.add(columnDevice)

        //----------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns(this, false, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnCommandName)
        addTableColumn(columnCommandDescr)
        addTableColumn(columnCommandCommand)
        addTableColumn(columnEditTime)
        addTableColumn(columnForSend)
        addTableColumn(columnSendTime)

        alFormColumn.add(columnCommandName)
        alFormColumn.add(columnCommandDescr)
        alFormColumn.add(columnCommandCommand)
        alFormColumn.add(columnEditTime)
        alFormColumn.add(columnForSend)
        alFormColumn.add(columnSendTime)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnEditTime)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["mms_device"] = columnDevice
        hmParentColumn["mms_device_command"] = columnCommand
    }
}
