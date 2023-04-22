package foatto.mms.core_mms

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataAbstractIntValue
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData

class cObject : cStandart() {

    companion object {
        const val PERM_REMOTE_CONTROL: String = "remote_control"
        const val PERM_SCHEME_SENSOR_MOVE: String = "sensor_move"
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа на (дистанционное) управление объектом
        alPermission.add(Pair(PERM_REMOTE_CONTROL, "20 Remote Control"))
        //--- права доступа на перемещение датчиков по схеме объекта
        alPermission.add(Pair(PERM_SCHEME_SENSOR_MOVE, "21 Scheme Sensor Move"))
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val md = model as mObject
        if (column == md.columnObjectName) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            if ((hmColumnData[md.columnDisabled] as DataBoolean).value) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            } else {
                val id = (hmColumnData[model.columnId] as DataInt).intValue

                val rs = conn.executeQuery(" SELECT MAX(ontime) FROM MMS_data_${id} ")
                val lastDataTime = if (rs.next()) {
                    rs.getInt(1)
                } else {
                    0
                }
                rs.close()

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

    //protected open fun getNextID( hmColumnData: HashMap<iColumn,iData> ): Int {
    override fun getNextId(hmColumnData: Map<iColumn, iData>): Int {
        return conn.getNextIntId(arrayOf("MMS_object", "MMS_zone"), arrayOf("id", "id"))
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)
        createDataTable(id)
        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        val mo = model as mObject

        val userID = (hmColumnData[mo.columnUser!!] as DataAbstractIntValue).intValue
        conn.executeUpdate(" UPDATE MMS_day_work SET user_id = $userID WHERE object_id = $id ")

        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)
        deleteDataTable(id)

        conn.executeUpdate(" DELETE FROM MMS_work_shift_data WHERE shift_id IN ( SELECT id FROM MMS_work_shift WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_work_shift WHERE object_id = $id ")

        conn.executeUpdate(" DELETE FROM MMS_sensor_calibration WHERE sensor_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_equip_service_shedule WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_equip_service_history WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) ")
        conn.executeUpdate(" DELETE FROM MMS_sensor WHERE object_id = $id ")
    }

    private fun createDataTable(id: Int) {
        conn.executeUpdate(" CREATE TABLE MMS_data_$id ( ontime ${conn.dialect.integerFieldTypeName} NOT NULL, sensor_data ${conn.dialect.hexFieldTypeName} ) ")
        conn.executeUpdate(conn.dialect.createClusteredIndex + " MMS_data_${id}_ontime ON MMS_data_$id ( ontime ) ")
    }

    private fun deleteDataTable(id: Int) {
        conn.executeUpdate(" DROP TABLE MMS_data_$id ")
    }
}
