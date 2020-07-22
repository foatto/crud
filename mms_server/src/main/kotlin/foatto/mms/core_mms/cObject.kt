package foatto.mms.core_mms

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataAbstractValue
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData

class cObject : cStandart() {

    companion object {
        const val PERM_REMOTE_CONTROL = "remote_control"
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа на ( дистанционное ) управление объектом
        alPermission.add( Pair( PERM_REMOTE_CONTROL, "20 Remote Control" ) )
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle( rowNo, isNewRow, hmColumnData, column, tci )

        val md = model as mObject
        if( column == md.columnObjectName ) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            if( ( hmColumnData[ md.columnDisabled ] as DataBoolean).value ) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            }
            else {
                val id = ( hmColumnData[ model.columnID!! ] as DataInt).value
                var lastDataTime = 0

                val rs = stm.executeQuery( " SELECT ontime FROM MMS_data_${id} ORDER BY ontime DESC " )
                if( rs.next() )
                    lastDataTime = rs.getInt(1)
                rs.close()

                //--- нет данных больше суток - критично
                if( getCurrentTimeInt() - lastDataTime > 1 * 24 * 60 * 60 ) tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                //--- нет данных больше часа - предупреждение + начинается оповещение по e-mail
                else if( getCurrentTimeInt() - lastDataTime > 1 * 60 * 60 ) tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
                //--- всё нормально
                else tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
            }
        }
    }

    //protected open fun getNextID( hmColumnData: HashMap<iColumn,iData> ): Int {
    override fun getNextID( hmColumnData: Map<iColumn, iData> ): Int {
        return stm.getNextID( arrayOf( "MMS_object", "MMS_zone" ), arrayOf( "id", "id" ) )
    }

    override fun postAdd( id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any> ): String? {
        val postURL = super.postAdd( id, hmColumnData, hmOut )
        createDataTable( id )
        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        val mo = model as mObject

        val userID = ( hmColumnData[ mo.columnUser!! ] as DataAbstractValue ).value
        stm.executeUpdate( " UPDATE MMS_day_work SET user_id = $userID WHERE object_id = $id " )
        stm.executeUpdate( " UPDATE MMS_downtime SET user_id = $userID WHERE object_id = $id " )

        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete( id, hmColumnData )
        deleteDataTable( id )

        stm.executeUpdate( " DELETE FROM MMS_work_shift_data WHERE shift_id IN ( SELECT id FROM MMS_work_shift WHERE object_id = $id ) " )
        stm.executeUpdate( " DELETE FROM MMS_work_shift WHERE object_id = $id " )

        stm.executeUpdate( " DELETE FROM MMS_sensor_calibration WHERE sensor_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) " )
        stm.executeUpdate( " DELETE FROM MMS_equip_service_shedule WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) " )
        stm.executeUpdate( " DELETE FROM MMS_equip_service_history WHERE equip_id IN ( SELECT id FROM MMS_sensor WHERE object_id = $id ) " )
        stm.executeUpdate( " DELETE FROM MMS_sensor WHERE object_id = $id " )
    }

    protected fun createDataTable( id: Int ) {
        val stm = stm

        stm.executeUpdate( " CREATE TABLE MMS_data_$id ( ontime ${stm.dialect.integerFieldTypeName} NOT NULL, " +
                           " sensor_data ${stm.dialect.hexFieldTypeName} ) " )
        stm.executeUpdate( stm.dialect.createClusteredIndex + " MMS_data_${id}_ontime ON MMS_data_$id ( ontime ) " )
    }

    protected fun deleteDataTable( id: Int ) {
        stm.executeUpdate( " DROP TABLE MMS_data_$id " )
    }
}
