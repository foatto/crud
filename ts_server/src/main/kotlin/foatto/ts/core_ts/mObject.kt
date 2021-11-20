package foatto.ts.core_ts

import foatto.core.link.AppAction
import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.sensor.config.SensorConfig

class mObject : mAbstract() {

    lateinit var columnObjectName: ColumnString

    lateinit var columnState: ColumnString
    lateinit var columnDepth: ColumnDouble
    lateinit var columnSpeed: ColumnDouble
    lateinit var columnLoad: ColumnDouble
    lateinit var columnLastDateTime: ColumnDateTimeInt

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "TS_object"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(tableName, "user_id", columnUserID, userConfig.userId)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец", STRING_COLUMN_WIDTH).apply {
            if (userConfig.isAdmin) {
                selectorAlias = "ts_client"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        columnObjectName = ColumnString(tableName, "name", "Наименование", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            //setUnique(true, null); - different clients / users may have objects with the same names
        }

        val columnObjectModel = ColumnString(tableName, "model", "Модель", STRING_COLUMN_WIDTH)

        columnState = ColumnString(tableName, "_state", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_STATE] ?: "-", STRING_COLUMN_WIDTH).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnDepth = ColumnDouble(tableName, "_depth", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_DEPTH] ?: "-", 10, 0).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnSpeed = ColumnDouble(tableName, "_speed", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_SPEED] ?: "-", 10, 0).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnLoad = ColumnDouble(tableName, "_load", SensorConfig.hmSensorDescr[SensorConfig.SENSOR_LOAD] ?: "-", 10, 0).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }
        columnLastDateTime = ColumnDateTimeInt(tableName, "_last_time", "Время последних данных", true, zoneId).apply {
            isVirtual = true
            isEditable = false
            tableAlign = TableCellAlign.CENTER
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnUser!!)

        if (userConfig.isAdmin) {
            alTableGroupColumn.add(columnUserName)
        }
        addTableColumn(columnObjectName)
        addTableColumn(columnObjectModel)
        addTableColumn(columnState)
        addTableColumn(columnDepth)
        addTableColumn(columnSpeed)
        addTableColumn(columnLoad)
        addTableColumn(columnLastDateTime)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnUser!!)

        if (userConfig.isAdmin) {
            alFormColumn.add(columnUserName)
        }
        alFormColumn.add(columnObjectName)
        alFormColumn.add(columnObjectModel)
        alFormColumn.add(columnState)
        alFormColumn.add(columnDepth)
        alFormColumn.add(columnSpeed)
        alFormColumn.add(columnLoad)
        alFormColumn.add(columnLastDateTime)

        //----------------------------------------------------------------------------------------------------------------------

        alTableSortColumn.add(columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["ts_client"] = columnUser!!

        //----------------------------------------------------------------------------------------------------------------------

//        TSFunction.fillAllChildDataForGraphics(columnID, alChildData)

        alChildData.add(ChildData("ts_show_state", columnID, AppAction.FORM, true))

        alChildData.add(ChildData("ts_setup", columnID, AppAction.FORM, true))

        alChildData.add(ChildData("Графики", "ts_graphic_dsltt", columnID, AppAction.FORM, true))
        alChildData.add(ChildData("Графики", "ts_graphic_depth", columnID, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_speed", columnID, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_load", columnID, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_temperature_in", columnID, AppAction.FORM))
        alChildData.add(ChildData("Графики", "ts_graphic_temperature_out", columnID, AppAction.FORM))

        alChildData.add(ChildData("ts_sensor", columnID, AppAction.TABLE, true))
//        alChildData.add(ChildData("ts_log_session", columnID, AppAction.TABLE))
        alChildData.add(ChildData("ts_data", columnID))
        alChildData.add(ChildData("ts_device", columnID))
        alChildData.add(ChildData("ts_device_command_history", columnID))

        //----------------------------------------------------------------------------------------

        //--- cascade deletion procedure, implemented in cObject.postDelete
        //alDependData.add(new DependData("TS_sensor", "object_id", DependData.DELETE));

        alDependData.add(DependData("TS_device", "object_id", DependData.SET, 0))
        alDependData.add(DependData("TS_device_command_history", "object_id", DependData.SET, 0))
    }

}
