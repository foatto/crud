package foatto.ts.core_ts

import foatto.core.util.getZoneId
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBinary
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.iTSApplication
import java.util.*

class mData : mAbstract() {

    lateinit var columnDataOnTimeUTC: ColumnDateTimeInt
        private set
    lateinit var columnDataOnTimeLocal: ColumnDateTimeInt
        private set
    lateinit var columnDataBinary: ColumnBinary
        private set

    //--- отдельно выделенные данные по прописанным датчикам
    val tmSensorColumn = TreeMap<Int, ColumnString>()

    //--- прочие данные
    lateinit var columnDataSensorOther: ColumnString
        private set

    val hmSensorPortType = mutableMapOf<Int, Int>()

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val zoneId0 = getZoneId(0)

        //--- может быть null при вызове из "Модули системы"
        val objectId = hmParentData["ts_object"] ?: 0

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "TS_data_$objectId"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "ontime")

        //----------------------------------------------------------------------------------------------------------------------

        columnDataOnTimeUTC = ColumnDateTimeInt(tableName, "_ontime_utc_", "Время (UTC)", true, zoneId0)
        columnDataOnTimeUTC.isVirtual = true
        columnDataOnTimeLocal = ColumnDateTimeInt(tableName, "_ontime_local_", "Время (местное)", true, zoneId)
        columnDataOnTimeLocal.isVirtual = true

        columnDataBinary = ColumnBinary(tableName, "sensor_data")

        //--- соберём все номера портов
        val oc = (application as iTSApplication).getObjectConfig(userConfig, objectId)
        oc.hmSensorConfig.forEach { (sensorType, hmSC) ->
            //--- генерируем виртуальные поля по объявленным портам
            hmSC.keys.forEach { portNum ->
                val cd = ColumnString(tableName, portNum.toString(), portNum.toString(), STRING_COLUMN_WIDTH)
                cd.isVirtual = true
                cd.isSearchable = false
                tmSensorColumn[portNum] = cd
                hmSensorPortType[portNum] = sensorType
            }
        }
        columnDataSensorOther = ColumnString(tableName, "_other_sensor_data", "Прочие данные", STRING_COLUMN_WIDTH)
        columnDataSensorOther.isVirtual = true
        columnDataSensorOther.isSearchable = false

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnDataBinary)

        addTableColumn(columnDataOnTimeUTC)
        addTableColumn(columnDataOnTimeLocal)
        tmSensorColumn.values.forEach { sc -> addTableColumn(sc) }
        addTableColumn(columnDataSensorOther)


        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnDataBinary)

        alFormColumn.add(columnDataOnTimeUTC)
        alFormColumn.add(columnDataOnTimeLocal)
        tmSensorColumn.values.forEach { sc -> alFormColumn.add(sc) }
        alFormColumn.add(columnDataSensorOther)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnID)
        alTableSortDirect.add("DESC")
    }
}