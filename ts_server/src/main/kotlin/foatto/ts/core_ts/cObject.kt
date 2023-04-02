package foatto.ts.core_ts

import foatto.core.link.TableCell
import foatto.core.link.TableCellBackColorType
import foatto.core.link.TableCellForeColorType
import foatto.core.link.TableResponse
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataDouble
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.ts.core_ts.calc.ObjectState
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication

class cObject : cStandart() {

    private var parentUserIdFromCompanyId: Int? = null
    private val hmObjectConfigCache = mutableMapOf<Int, ObjectConfig>()
    private val hmObjectStateCache = mutableMapOf<Int, ObjectState>()

    companion object {
        const val PERM_REMOTE_CONTROL = "remote_control"
    }

    override fun definePermission() {
        super.definePermission()

        //--- права доступа на (дистанционное) управление объектом
        alPermission.add(Pair(PERM_REMOTE_CONTROL, "20 Remote Control"))
    }

    override fun getParentId(alias: String?): Int? =
        if (alias == "ts_company") {
            if (parentUserIdFromCompanyId == null) {
                hmParentData[alias]?.let { companyId ->
                    val rs = conn.executeQuery(
                        """
                            SELECT id 
                            FROM SYSTEM_users 
                            WHERE org_type = ${OrgType.ORG_TYPE_WORKER}
                            AND parent_id = $companyId
                        """
                    )
                    if (rs.next()) {
                        parentUserIdFromCompanyId = rs.getInt(1)
                    } else {
                        parentUserIdFromCompanyId = 0
                    }
                    rs.close()
                } ?: run {
                    parentUserIdFromCompanyId = 0
                }
            }
            parentUserIdFromCompanyId
        } else {
            super.getParentId(alias)
        }

    override fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        hmObjectConfigCache.clear()
        hmObjectStateCache.clear()

        return super.getTable(hmOut)
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val id = (hmColumnData[model.columnId] as DataInt).intValue
        val objectConfig = hmObjectConfigCache.getOrPut(id) { (application as iTSApplication).getObjectConfig(userConfig, id) }
        val objectState = hmObjectStateCache.getOrPut(id) { ObjectState.getState(conn, objectConfig) }

        val md = model as mObject
        when (column) {
            md.columnState -> {
                objectState.tmStateValue.values.firstOrNull()?.let { stateCode ->
                    SensorConfigState.hmStateInfo[stateCode]?.brightColor
                }?.let { brightColor ->
                    tci.backColorType = TableCellBackColorType.DEFINED
                    tci.backColor = brightColor
                }
            }

            md.columnLastDateTime -> {
                tci.foreColorType = TableCellForeColorType.DEFINED

//            if( ( hmColumnData[ md.columnDisabled ] as DataBoolean).value ) {
//                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
//            }
//            else {
                val lastDataTime = objectState.lastDateTime ?: 0

                //--- нет данных больше суток - критично
                if (getCurrentTimeInt() - lastDataTime > 1 * 24 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                }
                //--- нет данных больше часа - предупреждение + начинается оповещение по e-mail
                else if (getCurrentTimeInt() - lastDataTime > 1 * 60 * 60) {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
                }
                //--- всё нормально
                else {
                    tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
                }
            }
        }
    }

    override fun generateTableColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        super.generateTableColumnDataAfterFilter(hmColumnData)

        generateColumnData((hmColumnData[model.columnId] as DataInt).intValue, hmColumnData)
    }

    override fun postProcessFormColumnDataFromFormData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.postProcessFormColumnDataFromFormData(id, hmColumnData)

        //--- заменим company-id на реальный user-id
        val dataUser = hmColumnData[model.columnUser!!] as DataInt
        val rs = conn.executeQuery(
            """
                SELECT id FROM SYSTEM_users 
                WHERE parent_id = ${dataUser.intValue}
                AND org_type = ${OrgType.ORG_TYPE_WORKER}
            """
        )
        //--- да, в этом поле лежит company-id, меняем его на user-id
        if (rs.next()) {
            dataUser.intValue = rs.getInt(1)
        }
        //--- иначе там лежит уже готовый/правильный user-id, ничего не трогаем

        rs.close()
    }

    override fun getCalculatedFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.getCalculatedFormColumnData(id, hmColumnData)

        generateColumnData(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)
        createDataTable(id)
        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)
        deleteDataTable(id)

        conn.executeUpdate(" DELETE FROM TS_sensor_calibration WHERE sensor_id IN ( SELECT id FROM TS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM TS_sensor WHERE object_id = $id ")
    }

    private fun createDataTable(id: Int) {
        conn.executeUpdate(" CREATE TABLE TS_data_$id ( ontime ${conn.dialect.integerFieldTypeName} NOT NULL, sensor_data ${conn.dialect.hexFieldTypeName} ) ")
        conn.executeUpdate(conn.dialect.createClusteredIndex + " TS_data_${id}_ontime ON TS_data_$id ( ontime ) ")
    }

    private fun deleteDataTable(id: Int) {
        conn.executeUpdate(" DROP TABLE TS_data_$id ")
    }

    private fun generateColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        val m = model as mObject

        val objectConfig = hmObjectConfigCache.getOrPut(id) { (application as iTSApplication).getObjectConfig(userConfig, id) }
        val objectState = hmObjectStateCache.getOrPut(id) { ObjectState.getState(conn, objectConfig) }

        val curState = objectState.tmStateValue.values.firstOrNull()

        (hmColumnData[m.columnTroubleType] as DataComboBox).intValue = when (curState) {
            null -> {
                mObject.TROUBLE_CONNECT
            }

            SensorConfigState.STATE_UNPASS_DOWN,
            SensorConfigState.STATE_UNPASS_UP,
            SensorConfigState.STATE_WIRE_RUNOUT,
            SensorConfigState.STATE_DRIVE_PROTECT,
            SensorConfigState.STATE_STOPPED_BY_SERVER,
            SensorConfigState.STATE_BLOCKED_BY_SERVER -> {
                mObject.TROUBLE_ERROR
            }

            SensorConfigState.STATE_UNPASS_UP_1_METER_DOWN,
            SensorConfigState.STATE_UNPASS_DOWN_PAUSE,
            SensorConfigState.STATE_UNPASS_DOWN_1_METER_UP,
            SensorConfigState.STATE_UNPASS_UP_1_METER_DOWN_,
            SensorConfigState.STATE_UNPASS_DOWN_PAUSE_,
            SensorConfigState.STATE_UNPASS_DOWN_1_METER_UP_ -> {
                mObject.TROUBLE_WARNING
            }

            else -> {
                if (getCurrentTimeInt() - (objectState.lastDateTime ?: 0) > 3600) {
                    mObject.TROUBLE_CONNECT
                } else {
                    mObject.TROUBLE_NONE
                }
            }
        }

        (hmColumnData[m.columnLastDateTime] as DataDateTimeInt).setDateTime(objectState.lastDateTime ?: 0)

        (hmColumnData[m.columnState] as DataString).text = curState?.let { stateCode ->
            SensorConfigState.hmStateInfo[stateCode]?.descr ?: "(неизвестный код состояния: $stateCode)"
        } ?: "(нет данных)"

        (hmColumnData[m.columnDepth] as DataDouble).doubleValue = objectState.tmDepthValue.values.firstOrNull() ?: 0.0
        (hmColumnData[m.columnSpeed] as DataDouble).doubleValue = objectState.tmSpeedValue.values.firstOrNull() ?: 0.0
        (hmColumnData[m.columnLoad] as DataDouble).doubleValue = objectState.tmLoadValue.values.firstOrNull() ?: 0.0
        (hmColumnData[m.columnSignalLevel] as DataDouble).doubleValue = objectState.tmSignalLevel.values.firstOrNull() ?: 0.0
    }

}
