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
        val MAX_PORT_PER_SENSOR = 4

        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private val MAX_DEVICE_COUNT_PER_OBJECT = 65
    }

    lateinit var columnDeviceIndex: ColumnInt
        private set
    lateinit var columnDevice: ColumnInt
        private set
    lateinit var columnDeviceLastSessionTime: ColumnDateTimeInt
        private set

    lateinit var columnESDCreatingEnabled: ColumnBoolean
        private set
    val alColumnESDGroupName = mutableListOf<ColumnString>()
    val alColumnESDDescrPrefix = mutableListOf<ColumnString>()
    val alColumnESDDescrPostfix = mutableListOf<ColumnString>()

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

        columnDeviceIndex = ColumnInt(tableName, "device_index", "Порядковый номер устройства на объекте", 10, 0).apply {
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

        val columnDeviceOfflineMode = ColumnBoolean(tableName, "offline_mode", "Возможен offline-режим")

        val columnDeviceWorkBegin = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Дата начала эксплуатации")  // есть ещё очень старые приборы

        //--- вручную добавленное поле для обозначения владельца объекта ---

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

        columnESDCreatingEnabled = ColumnBoolean(tableName, "_esd_create_enabled", "Автосоздание датчиков Euro Sens", false).apply {
            isVirtual = true
        }
        (1..MAX_PORT_PER_SENSOR).forEach { si ->
            alColumnESDGroupName += ColumnString(tableName, "_esd_group_name_$si", "Наименование группы датчиков $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnESDCreatingEnabled, true, setOf(1))
            }
            alColumnESDDescrPrefix += ColumnString(tableName, "_esd_descr_prefix_$si", "Префикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnESDCreatingEnabled, true, setOf(1))
            }
            alColumnESDDescrPostfix += ColumnString(tableName, "_esd_descr_postfix_$si", "Постфикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnESDCreatingEnabled, true, setOf(1))
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnID

        addTableColumn(columnDeviceIndex)
        addTableColumn(columnDeviceType)
        addTableColumn(columnDevice)
        addTableColumn(columnDeviceCell)
        addTableColumn(columnDeviceCell2)
        addTableColumn(columnObjectUserName)

        alFormHiddenColumn += columnID

        alFormColumn += columnDeviceIndex
        alFormColumn += columnDeviceType
        alFormColumn += columnDevice
        alFormColumn += columnDeviceCell
        alFormColumn += columnDeviceCell2

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

        addTableColumn(columnDeviceFwVer)
        addTableColumn(columnDeviceLastSessionTime)
        addTableColumn(columnDeviceLastSessionStatusText)
        addTableColumn(columnDeviceLastSessionErrorText)
        addTableColumn(columnDeviceOfflineMode)
        addTableColumn(columnDeviceWorkBegin)

        alFormColumn += columnDeviceFwVer
        alFormColumn += columnDeviceLastSessionTime
        alFormColumn += columnDeviceLastSessionStatusText
        alFormColumn += columnDeviceLastSessionErrorText
        alFormColumn += columnDeviceOfflineMode
        alFormColumn += columnDeviceWorkBegin

        alFormColumn += columnESDCreatingEnabled
        for(si in 0 until MAX_PORT_PER_SENSOR) {
            alFormColumn += alColumnESDGroupName[si]
            alFormColumn += alColumnESDDescrPrefix[si]
            alFormColumn += alColumnESDDescrPostfix[si]
        }

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn += columnDevice
        alTableSortDirect += "ASC"

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData += ChildData("mms_log_session", columnID)
        alChildData += ChildData("mms_device_command_history", columnID)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData += DependData("MMS_device_command_history", "device_id", DependData.DELETE)
    }
}
