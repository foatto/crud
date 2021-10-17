package foatto.ts.core_ts

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigSetup
import foatto.ts.iTSApplication

class mObjectSetup : mAbstract() {

    lateinit var columnDateTime: ColumnDateTimeInt
    val tmColumnSetup = sortedMapOf<Int, iColumn>()

    override fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- may be null in alias configuration time
        val objectConfig = hmParentData["ts_object"]?.let { objectId ->
            (application as iTSApplication).getObjectConfig(userConfig, objectId)
        }

        //----------------------------------------------------------------------------------------------------------------------

        tableName = FAKE_TABLE_NAME

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id").apply {
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnDateTime = ColumnDateTimeInt(tableName, "_date_time", "Дата и время", true, zoneId).apply {
            isVirtual = true
        }

        objectConfig?.let {
            objectConfig.hmSensorConfig[SensorConfig.SENSOR_SETUP]?.forEach { (_, sc) ->
                val scs = sc as SensorConfigSetup
                val column = when (scs.valueType) {
                    SensorConfigSetup.VALUE_TYPE_NUMBER -> {
                        ColumnString(tableName, "_${scs.id}", scs.descr, STRING_COLUMN_WIDTH)
                    }
                    SensorConfigSetup.VALUE_TYPE_BOOLEAN -> {
                        ColumnBoolean(tableName, "_${scs.id}", scs.descr)
                    }
                    else -> {
                        ColumnString(tableName, "_${scs.id}", scs.descr, STRING_COLUMN_WIDTH)
                    }
                }.apply {
                    isVirtual = true
                    isEditable = false
                }
                tmColumnSetup[scs.showPos] = column
            }
        }
        
        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID)

        alFormColumn.add(columnDateTime)
        tmColumnSetup.forEach { (_, column) ->
            alFormColumn.add(column)
        }
    }
}