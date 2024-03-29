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
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.mms.core_mms.ObjectSelector
import foatto.mms.core_mms.ds.MMSTelematicFunction
import foatto.sql.CoreAdvancedConnection

class mDevice : mAbstractUserSelector() {

    companion object {
        const val MAX_PORT_PER_SENSOR = 4

        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private val MAX_DEVICE_COUNT_PER_OBJECT = 65
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    lateinit var columnDeviceIndex: ColumnInt
        private set
    lateinit var columnSerialNo: ColumnString
        private set
    lateinit var columnDeviceLastSessionTime: ColumnDateTimeInt
        private set

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    lateinit var columnESDCreatingEnabled: ColumnBoolean
        private set
    val alColumnESDGroupName: MutableList<ColumnString> = mutableListOf()
    val alColumnESDDescrPrefix: MutableList<ColumnString> = mutableListOf()
    val alColumnESDDescrPostfix: MutableList<ColumnString> = mutableListOf()

    lateinit var columnEmisCreatingEnabled: ColumnBoolean
        private set
    val alColumnEmisGroupName: MutableList<ColumnString> = mutableListOf()
    val alColumnEmisDescrPrefix: MutableList<ColumnString> = mutableListOf()
    val alColumnEmisDescrPostfix: MutableList<ColumnString> = mutableListOf()

    lateinit var columnUSSCreatingEnabled: ColumnBoolean
        private set
    val alColumnUSSGroupName: MutableList<ColumnString> = mutableListOf()
    val alColumnUSSDescrPrefix: MutableList<ColumnString> = mutableListOf()
    val alColumnUSSDescrPostfix: MutableList<ColumnString> = mutableListOf()

    lateinit var columnMercuryCreatingEnabled: ColumnBoolean
        private set
    val alColumnMercuryGroupName: MutableList<ColumnString> = mutableListOf()
    val alColumnMercuryDescrPrefix: MutableList<ColumnString> = mutableListOf()
    val alColumnMercuryDescrPostfix: MutableList<ColumnString> = mutableListOf()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private lateinit var os: ObjectSelector

    val columnObject: ColumnInt
        get() = os.columnObject

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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

        modelTableName = "MMS_device"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig)

        //----------------------------------------------------------------------------------------------------------------------

        columnDeviceIndex = ColumnInt(modelTableName, "device_index", "Порядковый номер устройства на объекте", 10, 0).apply {
            minValue = 0
            maxValue = MAX_DEVICE_COUNT_PER_OBJECT - 1
        }

        val columnDeviceType = ColumnRadioButton(modelTableName, "type", "Тип устройства")
        MMSTelematicFunction.fillDeviceTypeColumn(columnDeviceType)

        columnSerialNo = ColumnString(modelTableName, "serial_no", "Серийный номер устройства", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        val columnDeviceCell = ColumnString(modelTableName, "cell_num", "Номер телефона", STRING_COLUMN_WIDTH)
        val columnDeviceCell2 = ColumnString(modelTableName, "cell_num_2", "Номер телефона 2", STRING_COLUMN_WIDTH)

        val columnDeviceFwVer = ColumnString(modelTableName, "fw_version", "Версия прошивки", 10).apply {
            isEditable = false
            formPinMode = FormPinMode.OFF
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

        val columnDeviceWorkBegin = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Дата начала эксплуатации")  // есть ещё очень старые приборы

        //--- вручную добавленное поле для обозначения владельца объекта ---

        val columnObjectUserName = ColumnComboBox("MMS_object", "user_id", "Пользователь").apply {
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

        columnESDCreatingEnabled = ColumnBoolean(modelTableName, "_esd_create_enabled", "Автосоздание датчиков Euro Sens", false).apply {
            isVirtual = true
        }
        (1..MAX_PORT_PER_SENSOR).forEach { si ->
            alColumnESDGroupName += ColumnString(modelTableName, "_esd_group_name_$si", "Наименование группы датчиков $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnESDCreatingEnabled, true, setOf(1))
            }
            alColumnESDDescrPrefix += ColumnString(modelTableName, "_esd_descr_prefix_$si", "Префикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnESDCreatingEnabled, true, setOf(1))
            }
            alColumnESDDescrPostfix += ColumnString(modelTableName, "_esd_descr_postfix_$si", "Постфикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnESDCreatingEnabled, true, setOf(1))
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnEmisCreatingEnabled = ColumnBoolean(modelTableName, "_emis_create_enabled", "Автосоздание датчиков Эмис", false).apply {
            isVirtual = true
        }
        (1..MAX_PORT_PER_SENSOR).forEach { si ->
            alColumnEmisGroupName += ColumnString(modelTableName, "_emis_group_name_$si", "Наименование группы датчиков $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnEmisCreatingEnabled, true, setOf(1))
            }
            alColumnEmisDescrPrefix += ColumnString(modelTableName, "_emis_descr_prefix_$si", "Префикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnEmisCreatingEnabled, true, setOf(1))
            }
            alColumnEmisDescrPostfix += ColumnString(modelTableName, "_emis_descr_postfix_$si", "Постфикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnEmisCreatingEnabled, true, setOf(1))
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnUSSCreatingEnabled = ColumnBoolean(modelTableName, "_uss_create_enabled", "Автосоздание датчиков УСС", false).apply {
            isVirtual = true
        }
        (1..MAX_PORT_PER_SENSOR).forEach { si ->
            alColumnUSSGroupName += ColumnString(modelTableName, "_uss_group_name_$si", "Наименование группы датчиков $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnUSSCreatingEnabled, true, setOf(1))
            }
            alColumnUSSDescrPrefix += ColumnString(modelTableName, "_uss_descr_prefix_$si", "Префикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnUSSCreatingEnabled, true, setOf(1))
            }
            alColumnUSSDescrPostfix += ColumnString(modelTableName, "_uss_descr_postfix_$si", "Постфикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnUSSCreatingEnabled, true, setOf(1))
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnMercuryCreatingEnabled = ColumnBoolean(modelTableName, "_mercury_create_enabled", "Автосоздание датчиков Меркурий", false).apply {
            isVirtual = true
        }
        (1..MAX_PORT_PER_SENSOR).forEach { si ->
            alColumnMercuryGroupName += ColumnString(modelTableName, "_mercury_group_name_$si", "Наименование группы датчиков $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnMercuryCreatingEnabled, true, setOf(1))
            }
            alColumnMercuryDescrPrefix += ColumnString(modelTableName, "_mercury_descr_prefix_$si", "Префикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnMercuryCreatingEnabled, true, setOf(1))
            }
            alColumnMercuryDescrPostfix += ColumnString(modelTableName, "_mercury_descr_postfix_$si", "Постфикс наименования датчика $si", STRING_COLUMN_WIDTH).apply {
                isVirtual = true
                addFormVisible(columnMercuryCreatingEnabled, true, setOf(1))
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId
        alTableHiddenColumn += columnUser!!

        addTableColumn(columnDeviceType)
        addTableColumn(columnSerialNo)
        addTableColumn(columnDeviceCell)
        addTableColumn(columnDeviceCell2)
        addTableColumn(columnObjectUserName)
        addTableColumn(columnDeviceIndex)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnUser!!

        alFormColumn += columnUserName
        alFormColumn += columnDeviceType
        alFormColumn += columnSerialNo
        alFormColumn += columnDeviceCell
        alFormColumn += columnDeviceCell2
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

        addUniqueColumn(columnSerialNo, "")
//--- need to add use empty/zero values of columnObject
//        addUniqueColumn(
//            listOf(
//                UniqueColumnData(os.columnObject),
//                UniqueColumnData(columnDeviceIndex),
//            )
//        )

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnDeviceFwVer)
        addTableColumn(columnDeviceLastSessionTime)
        addTableColumn(columnDeviceLastSessionStatusText)
        addTableColumn(columnDeviceLastSessionErrorText)
        addTableColumn(columnDeviceWorkBegin)

        alFormColumn += columnDeviceFwVer
        alFormColumn += columnDeviceLastSessionTime
        alFormColumn += columnDeviceLastSessionStatusText
        alFormColumn += columnDeviceLastSessionErrorText
        alFormColumn += columnDeviceWorkBegin

        alFormColumn += columnESDCreatingEnabled
        for (si in 0 until MAX_PORT_PER_SENSOR) {
            alFormColumn += alColumnESDGroupName[si]
            alFormColumn += alColumnESDDescrPrefix[si]
            alFormColumn += alColumnESDDescrPostfix[si]
        }

        alFormColumn += columnEmisCreatingEnabled
        for (si in 0 until MAX_PORT_PER_SENSOR) {
            alFormColumn += alColumnEmisGroupName[si]
            alFormColumn += alColumnEmisDescrPrefix[si]
            alFormColumn += alColumnEmisDescrPostfix[si]
        }

        alFormColumn += columnUSSCreatingEnabled
        for (si in 0 until MAX_PORT_PER_SENSOR) {
            alFormColumn += alColumnUSSGroupName[si]
            alFormColumn += alColumnUSSDescrPrefix[si]
            alFormColumn += alColumnUSSDescrPostfix[si]
        }

        alFormColumn += columnMercuryCreatingEnabled
        for (si in 0 until MAX_PORT_PER_SENSOR) {
            alFormColumn += alColumnMercuryGroupName[si]
            alFormColumn += alColumnMercuryDescrPrefix[si]
            alFormColumn += alColumnMercuryDescrPostfix[si]
        }

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnSerialNo, true)

        //----------------------------------------------------------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------------------------

        alChildData += ChildData("mms_log_session", columnId)
        alChildData += ChildData("mms_device_command_history", columnId)

        //----------------------------------------------------------------------------------------------------------------------------------------

        alDependData += DependData("MMS_device_command_history", "device_id", DependData.DELETE)
    }
}
