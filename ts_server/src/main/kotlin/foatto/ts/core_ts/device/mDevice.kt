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
import foatto.sql.CoreAdvancedConnection
import foatto.ts.core_ts.ObjectSelector
import foatto.ts.core_ts.ds.TSHandler

class mDevice : mAbstract() {

    companion object {
        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private val MAX_DEVICE_COUNT_PER_OBJECT = 65
    }

    lateinit var columnDeviceIsLocked: ColumnBoolean
    lateinit var columnDeviceIndex: ColumnInt
    lateinit var columnDeviceType: ColumnRadioButton
    lateinit var columnSerialNo: ColumnString
        private set
    lateinit var columnDeviceLastSessionTime: ColumnDateTimeInt
        private set

    lateinit var columnSensorCreatingEnabled: ColumnBoolean
        private set

    private lateinit var os: ObjectSelector

    val columnObject: ColumnInt
        get() = os.columnObject

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "TS_device"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnDeviceIsLocked = ColumnBoolean(modelTableName, "is_locked", "Блокирован")

        columnDeviceIndex = ColumnInt(modelTableName, "device_index", "Порядковый номер устройства на объекте", 10, 0).apply {
            minValue = 0
            maxValue = MAX_DEVICE_COUNT_PER_OBJECT - 1
        }

        columnDeviceType = ColumnRadioButton(modelTableName, "type", "Тип устройства")
        TSHandler.fillDeviceTypeColumn(columnDeviceType)

        columnSerialNo = ColumnString(modelTableName, "serial_no", "Серийный номер", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        val columnDeviceCell = ColumnString(modelTableName, "cell_num", "Номер телефона", STRING_COLUMN_WIDTH)

        val columnDeviceFwVer = ColumnString(modelTableName, "fw_version", "Версия прошивки", 10).apply {
            isEditable = false
            formPinMode = FormPinMode.OFF
        }

        val columnDeviceImei = ColumnString(modelTableName, "imei", "IMEI", 10).apply {
            isEditable = false
        }

        columnDeviceLastSessionTime = ColumnDateTimeInt(modelTableName, "last_session_time", "Время последней сессии", true, zoneId).apply {
            isEditable = false
        }
        val columnDeviceLastSessionStatusText = ColumnString(modelTableName, "last_session_status", "Статус последней сессии", STRING_COLUMN_WIDTH).apply {
            isEditable = false
        }
        val columnDeviceLastSessionErrorText = ColumnString(modelTableName, "last_session_error", "Ошибка последней сессии", STRING_COLUMN_WIDTH).apply {
            isEditable = false
        }

        columnSensorCreatingEnabled = ColumnBoolean(modelTableName, "_sensor_create_enabled", "Автосоздание датчиков", id == 0).apply {
            isVirtual = true
        }

        //--- вручную добавленное поле для обозначения владельца объекта ---

        val columnObjectUserName = ColumnComboBox("TS_object", "user_id", "Пользователь").apply {
            addChoice(0, "")
            application.hmUserFullNames.forEach { (userID, userName) ->
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

        alTableHiddenColumn += columnId

        addTableColumn(columnDeviceIsLocked)
        addTableColumn(columnDeviceIndex)
        addTableColumn(columnDeviceType)
        addTableColumn(columnSerialNo)
        addTableColumn(columnDeviceCell)
        addTableColumn(columnObjectUserName)

        alFormHiddenColumn += columnId

        alFormColumn += columnDeviceIsLocked
        alFormColumn += columnDeviceType
        alFormColumn += columnSerialNo
        alFormColumn += columnDeviceCell
        alFormColumn += columnDeviceIndex

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
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

        //--- возможны совпадающие серийные номера разных поколений приборов
        //addUniqueColumn(columnSerialNo, "")
//        addUniqueColumn(
//            listOf(
//                UniqueColumnData(os.columnObject),
//                UniqueColumnData(columnDeviceIndex),
//            )
//        )

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnDeviceFwVer)
        addTableColumn(columnDeviceImei)
        addTableColumn(columnDeviceLastSessionTime)
        addTableColumn(columnDeviceLastSessionStatusText)
        addTableColumn(columnDeviceLastSessionErrorText)

        alFormColumn += columnDeviceFwVer
        alFormColumn += columnDeviceImei
        alFormColumn += columnDeviceLastSessionTime
        alFormColumn += columnDeviceLastSessionStatusText
        alFormColumn += columnDeviceLastSessionErrorText
        alFormColumn += columnSensorCreatingEnabled

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnSerialNo, true)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData += ChildData("ts_log_session", columnId)
        alChildData += ChildData("ts_device_command_history", columnId)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData += DependData("TS_device_command_history", "device_id", DependData.DELETE)
    }
}
