package foatto.mms.core_mms.device

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData

class cDevice : cStandart() {

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle( rowNo, isNewRow, hmColumnData, column, tci )

        val md = model as mDevice
        if( column == md.columnDevice ) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            val lastSessionTime = ( hmColumnData[ md.columnDeviceLastSessionTime ] as DataDateTimeInt ).zonedDateTime.toEpochSecond().toInt()
            //--- раскраска номера контроллера в зависимости от времени последнего входа в систему
            val curTime = getCurrentTimeInt()

            if( lastSessionTime == 0 ) tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            else if( curTime - lastSessionTime > 7 * 24 * 60 * 60 ) tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
            else if( curTime - lastSessionTime > 1 * 24 * 60 * 60 ) tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
            else tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
        }
    }

    //--- при создании записи id = controller_id, задаваемый вручную
    override fun getNextID(hmColumnData: Map<iColumn, iData>): Int = ( hmColumnData[ ( model as mDevice ).columnDevice ] as DataInt ).intValue

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd( id, hmColumnData, hmOut )

        clearOldCameraInfo( hmColumnData )

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        clearOldCameraInfo( hmColumnData )

        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete( id, hmColumnData )

        clearOldCameraInfo( hmColumnData )
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    private fun clearOldCameraInfo( hmColumnData: Map<iColumn, iData> ) {
        val md = model as mDevice

        val deviceID = ( hmColumnData[ md.columnDevice ] as DataInt ).intValue
        val objectID = ( hmColumnData[ md.columnObject ] as DataInt ).intValue

        stm.executeUpdate( " DELETE FROM VC_camera WHERE name = '$deviceID' AND object_id <> $objectID " )
    }
}
