package foatto.mms.core_mms.device

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.*
import foatto.core_server.app.server.mAbstractUserSelector
import foatto.mms.core_mms.ObjectSelector
import foatto.mms.core_mms.ds.MMSTelematicFunction
import foatto.sql.CoreAdvancedConnection

class mDevice : mAbstractUserSelector() {

    companion object {
        const val MAX_PORT_PER_SENSOR = 4

        //--- 65 устройств по 1000 портов = 65000 портов, что уместится при нумерации от 0 до 65535
        private val MAX_DEVICE_COUNT_PER_OBJECT = 65

        const val CELL_OWNER_UNKNOWN = 0
        const val CELL_OWNER_PLA = 1
        const val CELL_OWNER_CLIENT = 2

        val hmCellOwnerNames = mapOf(
            CELL_OWNER_UNKNOWN to "(неизвестно)",
            CELL_OWNER_PLA to "Петролайн",
            CELL_OWNER_CLIENT to "Клиент",
        )
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

        val cellOperators = mutableSetOf("")
        val rs = conn.executeQuery(
            " SELECT cell_operator , cell_operator_2 FROM $modelTableName"
        )
        while (rs.next()) {
            cellOperators += rs.getString(1).trim()
            cellOperators += rs.getString(2).trim()
        }
        rs.close()
        val sortedCellOperators = cellOperators.toSortedSet()

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserName = addUserSelector(userConfig).apply {
            rowSpan = 3
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnDeviceIndex = ColumnInt(modelTableName, "device_index", "Порядковый номер устройства на объекте", 10, 0).apply {
            minValue = 0
            maxValue = MAX_DEVICE_COUNT_PER_OBJECT - 1
            rowSpan = 3
        }

        val columnDeviceType = ColumnRadioButton(modelTableName, "type", "Тип устройства").apply {
            rowSpan = 3
        }
        MMSTelematicFunction.fillDeviceTypeColumn(columnDeviceType)

        columnSerialNo = ColumnString(modelTableName, "serial_no", "Серийный номер устройства", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            rowSpan = 3
        }

        val columnDeviceCellOwner = ColumnRadioButton(modelTableName, "cell_owner", "Владелец симки", CELL_OWNER_UNKNOWN).apply {
            addChoice(CELL_OWNER_UNKNOWN, hmCellOwnerNames.getOrDefault(CELL_OWNER_UNKNOWN, "-"))
            addChoice(CELL_OWNER_PLA, hmCellOwnerNames.getOrDefault(CELL_OWNER_PLA, "-"))
            addChoice(CELL_OWNER_CLIENT, hmCellOwnerNames.getOrDefault(CELL_OWNER_CLIENT, "-"))
        }
        val columnDeviceCellNum = ColumnString(modelTableName, "cell_num", "Номер телефона", STRING_COLUMN_WIDTH)
        val columnDeviceCellICC = ColumnString(modelTableName, "cell_icc", "ICC сим-карты", STRING_COLUMN_WIDTH)
        val columnDeviceCellOperator = ColumnString(modelTableName, "cell_operator", "Оператор связи", STRING_COLUMN_WIDTH).apply {
            sortedCellOperators.forEach { cellOperatorName ->
                addCombo(cellOperatorName)
            }
        }

        val columnDeviceCellOwner2 = ColumnRadioButton(modelTableName, "cell_owner_2", "Владелец симки 2", CELL_OWNER_UNKNOWN).apply {
            addChoice(CELL_OWNER_UNKNOWN, hmCellOwnerNames.getOrDefault(CELL_OWNER_UNKNOWN, "-"))
            addChoice(CELL_OWNER_PLA, hmCellOwnerNames.getOrDefault(CELL_OWNER_PLA, "-"))
            addChoice(CELL_OWNER_CLIENT, hmCellOwnerNames.getOrDefault(CELL_OWNER_CLIENT, "-"))
        }
        val columnDeviceCellNum2 = ColumnString(modelTableName, "cell_num_2", "Номер телефона 2", STRING_COLUMN_WIDTH)
        val columnDeviceCellICC2 = ColumnString(modelTableName, "cell_icc_2", "ICC сим-карты 2", STRING_COLUMN_WIDTH)
        val columnDeviceCellOperator2 = ColumnString(modelTableName, "cell_operator_2", "Оператор связи 2", STRING_COLUMN_WIDTH).apply {
            sortedCellOperators.forEach { cellOperatorName ->
                addCombo(cellOperatorName)
            }
        }

        val columnDeviceFwVer = ColumnString(modelTableName, "fw_version", "Версия прошивки", 10).apply {
            isEditable = false
            formPinMode = FormPinMode.OFF
            rowSpan = 3
        }

        columnDeviceLastSessionTime = ColumnDateTimeInt(modelTableName, "last_session_time", "Время последней сессии", true, zoneId).apply {
            tableCaption = "Время"
            isEditable = false
        }
        val columnDeviceLastSessionStatusText = ColumnString(modelTableName, "last_session_status", "Статус последней сессии", STRING_COLUMN_WIDTH).apply {
            tableCaption = "Статус"
            isEditable = false
        }
        val columnDeviceLastSessionErrorText = ColumnString(modelTableName, "last_session_error", "Ошибка последней сессии", STRING_COLUMN_WIDTH).apply {
            tableCaption = "Ошибка последней сессии"
            isEditable = false
        }

        val columnDeviceWorkBegin = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Дата начала эксплуатации").apply {
            rowSpan = 3
        }

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
            rowSpan = 3
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

        addTableColumnVertNew(columnDeviceType)
        addTableColumnVertNew(columnSerialNo)
        addTableColumnVertNew(columnDeviceIndex)

        addTableColumnVertNew(columnDeviceCellOwner, columnDeviceCellOwner2, ColumnStatic(modelTableName, ""))
        addTableColumnVertNew(columnDeviceCellNum, columnDeviceCellNum2, ColumnStatic(modelTableName, ""))
        addTableColumnVertNew(columnDeviceCellICC, columnDeviceCellICC2, ColumnStatic(modelTableName, ""))
        addTableColumnVertNew(columnDeviceCellOperator, columnDeviceCellOperator2, ColumnStatic(modelTableName, ""))

        addTableColumnVertNew(columnObjectUserName)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnUser!!

        alFormColumn += columnUserName
        alFormColumn += columnDeviceType
        alFormColumn += columnSerialNo

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
            addedStaticColumnCount = 0,
        )

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumnVertNew(columnDeviceFwVer)
        addTableColumnVertNew(columnDeviceLastSessionTime, columnDeviceLastSessionStatusText, columnDeviceLastSessionErrorText)
        addTableColumnVertNew(columnDeviceWorkBegin)

        alFormColumn += columnDeviceIndex

        alFormColumn += columnDeviceCellOwner
        alFormColumn += columnDeviceCellNum
        alFormColumn += columnDeviceCellICC
        alFormColumn += columnDeviceCellOperator

        alFormColumn += columnDeviceCellOwner2
        alFormColumn += columnDeviceCellNum2
        alFormColumn += columnDeviceCellICC2
        alFormColumn += columnDeviceCellOperator2

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

        addUniqueColumn(columnSerialNo, "")
//--- need to add use empty/zero values of columnObject
//        addUniqueColumn(
//            listOf(
//                UniqueColumnData(os.columnObject),
//                UniqueColumnData(columnDeviceIndex),
//            )
//        )

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
