package foatto.ts.core_ts

import foatto.core.link.AppAction
import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts_core.app.ICON_NAME_TROUBLE_TYPE_CONNECT
import foatto.ts_core.app.ICON_NAME_TROUBLE_TYPE_ERROR
import foatto.ts_core.app.ICON_NAME_TROUBLE_TYPE_WARNING

class mObject : mAbstract() {

    companion object {
        val TROUBLE_NONE = 0
        val TROUBLE_CONNECT = 1
        val TROUBLE_WARNING = 2
        val TROUBLE_ERROR = 3
    }

    lateinit var columnTroubleType: ColumnComboBox

    lateinit var columnObjectName: ColumnString

    lateinit var columnState: ColumnString
    lateinit var columnDepth: ColumnDouble
    lateinit var columnSpeed: ColumnDouble
    lateinit var columnLoad: ColumnDouble
    lateinit var columnLastDateTime: ColumnDateTimeInt

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "TS_object"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(modelTableName, "user_id", columnUserID, userConfig.userId)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Компания", STRING_COLUMN_WIDTH).apply {
            if (userConfig.isAdmin) {
                selectorAlias = "ts_company"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        columnTroubleType = ColumnComboBox(modelTableName, "_trouble_type", "Тип проблемы", TROUBLE_NONE).apply {
            addChoice(TROUBLE_NONE, "", "")
            addChoice(TROUBLE_CONNECT, "Нет сигнала", "Нет сигнала", ICON_NAME_TROUBLE_TYPE_CONNECT)
            addChoice(TROUBLE_WARNING, "Сложные участки", "Сложные участки", ICON_NAME_TROUBLE_TYPE_WARNING)
            addChoice(TROUBLE_ERROR, "Рекомендуется проверка", "Рекомендуется проверка", ICON_NAME_TROUBLE_TYPE_ERROR)
            tableAlign = TableCellAlign.CENTER
            isVirtual = true
            isEditable = false
        }

        columnObjectName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            //setUnique(true, null); - different clients / users may have objects with the same names
        }

        val columnObjectModel = ColumnString(modelTableName, "model", "Модель", STRING_COLUMN_WIDTH)

        columnState = ColumnString(modelTableName, "_state", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_STATE] ?: "-", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnDepth = ColumnDouble(modelTableName, "_depth", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_DEPTH] ?: "-", 10, 0).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnSpeed = ColumnDouble(modelTableName, "_speed", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_SPEED] ?: "-", 10, 0).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnLoad = ColumnDouble(modelTableName, "_load", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_LOAD] ?: "-", 10, 0).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnLastDateTime = ColumnDateTimeInt(modelTableName, "_last_time", "Время последних данных", true, zoneId).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)

        if (userConfig.isAdmin) {
            alTableGroupColumn.add(columnUserName)
        }
        addTableColumn(columnTroubleType)
        addTableColumn(columnObjectName)
        addTableColumn(columnObjectModel)
        addTableColumn(columnState)
        addTableColumn(columnDepth)
        addTableColumn(columnSpeed)
        addTableColumn(columnLoad)
        addTableColumn(columnLastDateTime)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnUser!!

        if (userConfig.isAdmin) {
            alFormColumn += columnUserName
        }
        alFormColumn += columnTroubleType
        alFormColumn += columnObjectName
        alFormColumn += columnObjectModel
        alFormColumn += columnState
        alFormColumn += columnDepth
        alFormColumn += columnSpeed
        alFormColumn += columnLoad
        alFormColumn += columnLastDateTime

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnObjectName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["ts_company"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(
            ChildData(
                aAlias = "ts_composite_all",
                aColumn = columnId,
                aAction = AppAction.FORM,
                aNewGroup = true,
                aDefaultOperation = true
            )
        )

        alChildData.add(ChildData("ts_show_state", columnId, AppAction.FORM, true))

        alChildData.add(ChildData("ts_setup", columnId, AppAction.FORM, true))

        alChildData.add(ChildData("Графики", "ts_graphic_dsltt", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("Графики", "ts_graphic_depth", columnId, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_speed", columnId, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_load", columnId, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_temperature_in", columnId, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_temperature_out", columnId, AppAction.FORM))

        alChildData.add(ChildData("ts_sensor", columnId, AppAction.TABLE, true))
//        alChildData.add(ChildData("ts_log_session", columnID, AppAction.TABLE))
        alChildData.add(ChildData("ts_data", columnId))
        alChildData.add(ChildData("ts_device", columnId))
        alChildData.add(ChildData("ts_device_command_history", columnId))

        //----------------------------------------------------------------------------------------

        //--- cascade deletion procedure, implemented in cObject.postDelete
        //alDependData.add(new DependData("TS_sensor", "object_id", DependData.DELETE));

        alDependData.add(DependData("TS_device", "object_id", DependData.SET, 0))
        alDependData.add(DependData("TS_device_command_history", "object_id", DependData.SET, 0))
    }

}
