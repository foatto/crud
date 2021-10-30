package foatto.ts.core_ts.device

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnRadioButton
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.ObjectSelector
import foatto.ts.core_ts.ds.TSHandler

class mDevice : mAbstract() {

    companion object {
        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private val MAX_DEVICE_COUNT_PER_OBJECT = 65
    }

    lateinit var columnDeviceIndex: ColumnInt
    lateinit var columnSerialNo: ColumnString
        private set
    lateinit var columnDeviceLastSessionTime: ColumnDateTimeInt
        private set

    lateinit var columnSensorCreatingEnabled: ColumnBoolean
        private set

    private lateinit var os: ObjectSelector

    val columnObject: ColumnInt
        get() = os.columnObject

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "TS_device"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnDeviceIndex = ColumnInt(tableName, "device_index", "Порядковый номер устройства на объекте", 10, 0).apply {
            minValue = 0
            maxValue = MAX_DEVICE_COUNT_PER_OBJECT - 1
        }

        val columnDeviceType = ColumnRadioButton(tableName, "type", "Тип устройства")
        TSHandler.fillDeviceTypeColumn(columnDeviceType)

        columnSerialNo = ColumnString(tableName, "serial_no", "Серийный номер", 10).apply {
            isRequired = true
            setUnique(true, "")
            isEditable = id == 0    // номер устройства задается вручную только один раз - при создании. В дальнейшем редактировать его нельзя.
        }

        val columnDeviceCell = ColumnString(tableName, "cell_num", "Номер телефона", STRING_COLUMN_WIDTH)

        val columnDeviceFwVer = ColumnString(tableName, "fw_version", "Версия прошивки", 10).apply {
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

        columnSensorCreatingEnabled = ColumnBoolean(tableName, "_sensor_create_enabled", "Автосоздание датчиков", false).apply {
            isVirtual = true
        }

        //--- вручную добавленное поле для обозначения владельца а/м ---

        val columnObjectUserName = ColumnComboBox("TS_object", "user_id", "Пользователь").apply {
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

        alTableHiddenColumn += columnID

        addTableColumn(columnDeviceIndex)
        addTableColumn(columnDeviceType)
        addTableColumn(columnSerialNo)
        addTableColumn(columnDeviceCell)
        addTableColumn(columnObjectUserName)

        alFormHiddenColumn += columnID

        alFormColumn += columnDeviceIndex
        alFormColumn += columnDeviceType
        alFormColumn += columnSerialNo
        alFormColumn += columnDeviceCell

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(this, false, true, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1)

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnDeviceFwVer)
        addTableColumn(columnDeviceLastSessionTime)
        addTableColumn(columnDeviceLastSessionStatusText)
        addTableColumn(columnDeviceLastSessionErrorText)

        alFormColumn += columnDeviceFwVer
        alFormColumn += columnDeviceLastSessionTime
        alFormColumn += columnDeviceLastSessionStatusText
        alFormColumn += columnDeviceLastSessionErrorText
        alFormColumn += columnSensorCreatingEnabled

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn += columnSerialNo
        alTableSortDirect += "ASC"

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData += ChildData("ts_log_session", columnID)
        alChildData += ChildData("ts_device_command_history", columnID)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData += DependData("TS_device_command_history", "device_id", DependData.DELETE)
    }
}
