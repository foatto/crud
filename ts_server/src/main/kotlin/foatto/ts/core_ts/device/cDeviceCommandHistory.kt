package foatto.ts.core_ts.device

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection
import foatto.ts_core.app.CommandStatusCode

class cDeviceCommandHistory : cStandart() {

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mdch = model as mDeviceCommandHistory

        if ((hmColumnData[mdch.columnStatus] as DataComboBox).intValue == CommandStatusCode.DELETED) {
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
        }
    }

    companion object {

        fun addDeviceCommand(
            conn: CoreAdvancedConnection,
            userId: Int,
            deviceId: Int,
            objectId: Int,
            command: String,
        ) {
            conn.executeUpdate(
                """
                    INSERT INTO TS_device_command_history ( id , 
                        user_id , device_id , object_id , 
                        command , create_time , 
                        send_status , send_time ) VALUES ( 
                        ${conn.getNextIntId("TS_device_command_history", "id")} , 
                        $userId , $deviceId , $objectId , 
                        '$command' , ${getCurrentTimeInt()} , 
                        ${CommandStatusCode.NOT_SENDED} , 0 )  
                """
            )
        }
    }

}