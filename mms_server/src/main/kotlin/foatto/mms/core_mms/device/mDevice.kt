package foatto.mms.core_mms.device

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.ObjectSelector
import foatto.mms.core_mms.ds.MMSHandler
import foatto.sql.CoreAdvancedStatement

class mDevice : mAbstract() {

    companion object {
        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private val MAX_DEVICE_COUNT_PER_OBJECT = 65
    }

    lateinit var columnDevice: ColumnInt
        private set
    lateinit var columnDeviceLastSessionTime: ColumnDateTimeInt
        private set

    private lateinit var os: ObjectSelector

    val columnObject: ColumnInt
        get() = os.columnObject

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_device"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnDeviceIndex = ColumnInt(tableName, "device_index", "Порядковый номер устройства на объекте", 10, 0).apply {
            minValue = 0
            maxValue = MAX_DEVICE_COUNT_PER_OBJECT - 1
        }

        val columnDeviceType = ColumnRadioButton(tableName, "type", "Тип устройства")
        MMSHandler.fillDeviceTypeColumn(columnDeviceType)

        columnDevice = ColumnInt(tableName, "device_id", "Номер устройства", 10).apply {
            isRequired = true
            setUnique(true, "")
            isEditable = id == 0    // номер устройства задается вручную только один раз - при создании. В дальнейшем редактировать его нельзя.
        }

        val columnDeviceCell = ColumnString(tableName, "cell_num", "Номер телефона", STRING_COLUMN_WIDTH)
        val columnDeviceCell2 = ColumnString(tableName, "cell_num_2", "Номер телефона 2", STRING_COLUMN_WIDTH)

        val columnDeviceFWVer = ColumnInt(tableName, "fw_version", "Версия прошивки", 10).apply {
            isEditable = false
            formPinMode = FormPinMode.OFF
        }

        columnDeviceLastSessionTime = ColumnDateTimeInt(tableName, "last_session_time", "Время последней сессии", true, zoneId).apply {
            isEditable = false
        }
        val columnDeviceLastSessionStatusText = ColumnString(tableName, "last_session_status", "Статус последней сессии", STRING_COLUMN_WIDTH).apply {
            isEditable = false
        }
        val columnDeviceLastSessionErrorText = ColumnString(tableName, "last_session_error", "Ошибка последней сессии", STRING_COLUMN_WIDTH).apply {
            isEditable = false
        }

        val columnDeviceOfflineMode = ColumnBoolean(tableName, "offline_mode", "Возможен offline-режим")

        val columnDeviceWorkBegin = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала эксплуатации")  // есть ещё очень старые приборы

        //--- вручную добавленное поле для обозначения владельца а/м ---

        val columnObjectUserName = ColumnComboBox("MMS_object", "user_id", "Пользователь").apply {
            addChoice(0, "")
            UserConfig.hmUserFullNames.forEach { (userID, userName) ->
                addChoice(
                    value = userID,
                    tableDescr = if (userID == userConfig.userId || userName.isBlank()) {
                        ""
                    } else {
                        userName
                    }
                )
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)

        addTableColumn(columnDeviceIndex)
        addTableColumn(columnDeviceType)
        addTableColumn(columnDevice)
        addTableColumn(columnDeviceCell)
        addTableColumn(columnDeviceCell2)
        addTableColumn(columnObjectUserName)

        alFormHiddenColumn.add(columnID)

        alFormColumn.add(columnDeviceIndex)
        alFormColumn.add(columnDeviceType)
        alFormColumn.add(columnDevice)
        alFormColumn.add(columnDeviceCell)
        alFormColumn.add(columnDeviceCell2)

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(this, false, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnDeviceFWVer)
        addTableColumn(columnDeviceLastSessionTime)
        addTableColumn(columnDeviceLastSessionStatusText)
        addTableColumn(columnDeviceLastSessionErrorText)
        addTableColumn(columnDeviceOfflineMode)
        addTableColumn(columnDeviceWorkBegin)

        alFormColumn.add(columnDeviceFWVer)
        alFormColumn.add(columnDeviceLastSessionTime)
        alFormColumn.add(columnDeviceLastSessionStatusText)
        alFormColumn.add(columnDeviceLastSessionErrorText)
        alFormColumn.add(columnDeviceOfflineMode)
        alFormColumn.add(columnDeviceWorkBegin)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDevice)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_log_session", columnID))
        alChildData.add(ChildData("mms_device_command_history", columnID))

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_device_command_history", "device_id", DependData.DELETE))
    }
}
