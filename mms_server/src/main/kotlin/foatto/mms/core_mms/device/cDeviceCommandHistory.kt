package foatto.mms.core_mms.device

import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.iData

class cDeviceCommandHistory : cStandart() {

    override fun isAddEnabled(): Boolean = super.isAddEnabled() && (getParentId("mms_object") != null || getParentId("mms_device") != null)

    override fun isEditEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean =
        super.isEditEnabled(hmColumnData, id) && (getParentId("mms_object") != null || getParentId("mms_device") != null)

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        val md = model as mDeviceCommandHistory

        (hmColumnData[md.columnEditTime] as DataDateTimeInt).setDateTime(getCurrentTimeInt())

        super.preSave(id, hmColumnData)
    }
}
