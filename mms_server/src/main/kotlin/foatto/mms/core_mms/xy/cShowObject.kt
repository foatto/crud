package foatto.mms.core_mms.xy

import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.DataTime3Int
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.mms.core_mms.report.cMMSReport
import java.time.ZonedDateTime

class cShowObject : cShowAbstractObject() {

    override fun loadShowParam(): XyStartData {

        val msa = model as mShowObject
        //--- выборка данных параметров для отчета
        val selectObjectUser = (hmColumnData[msa.uodg.columnObjectUser] as DataComboBox).intValue
        val selectObject = (hmColumnData[msa.uodg.columnObject] as DataInt).intValue
        val selectDepartment = (hmColumnData[msa.uodg.columnDepartment] as DataInt).intValue
        val selectGroup = (hmColumnData[msa.uodg.columnGroup] as DataInt).intValue

        val alobjectId = mutableListOf<Int>()
        if (selectObject == 0) {
            cMMSReport.loadObjectList(conn, userConfig, selectObjectUser, selectDepartment, selectGroup, alobjectId)
        } else {
            alobjectId.add(selectObject)
        }

        val sd = XyStartData()
        for (objectId in alobjectId)
            sd.alStartObjectData.add(XyStartObjectData(objectId))
        sd.rangeType = (hmColumnData[msa.columnShowRangeType] as DataRadioButton).intValue

        //--- обработка динамических диапазонов - здесь не нужна, будет постоянно производиться при запросах траектории
        if (sd.rangeType == -1) {
            sd.begTime = ZonedDateTime.of(
                (hmColumnData[msa.columnShowBegDate] as DataDate3Int).localDate,
                (hmColumnData[msa.columnShowBegTime] as DataTime3Int).localTime,
                zoneId
            ).toEpochSecond().toInt()
            sd.endTime = ZonedDateTime.of(
                (hmColumnData[msa.columnShowEndDate] as DataDate3Int).localDate,
                (hmColumnData[msa.columnShowEndTime] as DataTime3Int).localTime,
                zoneId
            ).toEpochSecond().toInt()
        }
        showZoneType = (hmColumnData[msa.columnShowZoneType] as DataComboBox).intValue

        return sd
    }

}
