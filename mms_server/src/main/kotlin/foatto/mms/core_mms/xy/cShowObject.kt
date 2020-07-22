package foatto.mms.core_mms.xy

import foatto.core_server.app.server.data.*
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.mms.core_mms.report.cMMSReport
import java.time.ZonedDateTime

class cShowObject : cShowAbstractObject() {

    override fun loadShowParam(): XyStartData {

        val msa = model as mShowObject
        //--- выборка данных параметров для отчета
        val selectObjectUser = ( hmColumnData[ msa.uodg.columnObjectUser ] as DataComboBox ).value
        val selectObject = ( hmColumnData[ msa.uodg.columnObject ] as DataInt ).value
        val selectDepartment = ( hmColumnData[ msa.uodg.columnDepartment ] as DataInt ).value
        val selectGroup = ( hmColumnData[ msa.uodg.columnGroup ] as DataInt ).value

        val alObjectID = mutableListOf<Int>()
        if( selectObject == 0 ) cMMSReport.loadObjectList( stm, userConfig, selectObjectUser, selectDepartment, selectGroup, alObjectID )
        else alObjectID.add( selectObject )

        val sd = XyStartData()
        for( objectID in alObjectID )
            sd.alStartObjectData.add( XyStartObjectData( objectID ) )
        sd.rangeType = ( hmColumnData[ msa.columnShowRangeType ] as DataRadioButton ).value

        //--- обработка динамических диапазонов - здесь не нужна, будет постоянно производиться при запросах траектории
        if( sd.rangeType == -1 ) {
            sd.begTime = ZonedDateTime.of(
                ( hmColumnData[ msa.columnShowBegDate ] as DataDate3Int ).localDate,
                ( hmColumnData[ msa.columnShowBegTime ] as DataTime3Int ).localTime,
                zoneId).toEpochSecond().toInt()
            sd.endTime = ZonedDateTime.of(
                ( hmColumnData[ msa.columnShowEndDate ] as DataDate3Int ).localDate,
                ( hmColumnData[ msa.columnShowEndTime ] as DataTime3Int ).localTime,
                zoneId).toEpochSecond().toInt()
        }
        showZoneType = ( hmColumnData[ msa.columnShowZoneType ] as DataComboBox ).value

        return sd
    }

}
