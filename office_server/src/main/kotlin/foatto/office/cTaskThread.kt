package foatto.office

import foatto.core.app.ICON_NAME_PRINT
import foatto.core.link.AppAction
import foatto.core.link.ServerActionButton
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData

class cTaskThread : cStandart() {

    override fun fillHeader(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>, hmOut: MutableMap<String, Any>) {
        val parentTaskID: Int = getParentId("office_task_out") ?: getParentId("office_task_in") ?: getParentId("office_task_out_archive") ?: getParentId("office_task_in_archive")!!

        val rs = conn.executeQuery(
            if (withAnchors) {
                " SELECT subj FROM OFFICE_task WHERE id = $parentTaskID "
            } else {
                " SELECT ${conn.getPreLimit(1)} message FROM OFFICE_task_thread " +
                    " WHERE task_id = $parentTaskID ${conn.getMidLimit(1)} " +
                    " ORDER BY ye DESC, mo DESC, da DESC, ho DESC, mi DESC " +
                    " ${conn.getPostLimit(1)} "
            }
        )

        alPath.add(
            Pair(
                "",
                if (rs.next()) {
                    (if (withAnchors) "Тема поручения:" else "Последнее сообщение: ") + rs.getString(1)
                } else {
                    aliasConfig.descr
                }
            )
        )

        rs.close()
    }

    override fun generateColumnDataBeforeFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mtt = model as mTaskThread

        val ds = hmColumnData[mtt.columnTaskThreadMessage] as DataString

        var s = ""
        ds.text.split("\n").forEach {
            if (!it.startsWith(">")) {
                s += (if (s.isEmpty()) "" else "\n") + it
            }
        }

        ds.text = s
    }

    override fun getServerAction(): MutableList<ServerActionButton> {
        val alSAB = super.getServerAction()

        alSAB.add(
            ServerActionButton(
                caption = "Распечатать",
                tooltip = "Распечатать",
                icon = ICON_NAME_PRINT,
                url = getParamURL("office_report_task_thread", AppAction.FORM, null, 0, hmParentData, null, ""),
                inNewWindow = true,
                isForWideScreenOnly = false,
            )
        )

        return alSAB
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.preSave(id, hmColumnData)

        val mtt = model as mTaskThread
        conn.executeUpdate(
            """
                UPDATE OFFICE_task 
                SET last_update = ${getCurrentTimeInt()} 
                WHERE id = ${(hmColumnData[mtt.columnTask] as DataInt).intValue} 
            """
        )
    }
}
