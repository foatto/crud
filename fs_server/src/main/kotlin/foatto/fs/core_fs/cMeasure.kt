package foatto.fs.core_fs

import foatto.app.CoreSpringApp
import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getSplittedDouble
import foatto.core.util.getSplittedLong
import foatto.core.util.getZoneId
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.ColumnStatic
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.fs.core_fs.calc.MeasureCalc
import foatto.fs.core_fs.calc.getMeasureFile

class cMeasure : cStandart() {

    private val zoneId0 = getZoneId( 0 )

    //--- понятно что будет регулироваться правами доступа, но лишняя предосторожность не помешает
    override fun isAddEnabled(): Boolean = false

//--- тормозить будет
//    //--- перекрывается наследниками для генерации данных в момент загрузки записей ДО фильтров поиска и страничной разбивки
//    override fun generateColumnDataBeforeFilter( hmColumnData: HashMap<iColumn, iData> ) {
//        generateColumnData( hmColumnData )
//    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        generateColumnData( hmColumnData )
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        generateColumnData( hmColumnData )
    }

    override fun getTableColumnStyle( rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle( rowNo, isNewRow, hmColumnData, column, tci )

        if( column is ColumnStatic ) return

        val md = model as mMeasure
        when( column ) {
            md.columnMeasureCurSize -> {
                val curSize = ( hmColumnData[ md.columnMeasureCurSize ] as DataInt ).value
                val allSize = ( hmColumnData[ md.columnMeasureAllSize ] as DataInt ).value

                if( curSize != allSize ) {
                    tci.foreColorType = TableCellForeColorType.DEFINED
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                    tci.fontStyle = 1
                }
            }
            md.columnMeasureError -> {
                tci.foreColorType = TableCellForeColorType.DEFINED
                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                tci.fontStyle = 1
            }
        }
    }

    override fun postDelete( id: Int, hmColumnData: Map<iColumn, iData> ) {
        super.postDelete( id, hmColumnData )
        getMeasureFile( CoreSpringApp.rootDirName, id ).delete()
    }

    private fun generateColumnData( hmColumnData: Map<iColumn, iData> ) {
        val mM = model as mMeasure

        //--- withDataParsing = true -> разбираем весь файл с измерением, чтобы вывести максимум данных
        val mc = MeasureCalc( CoreSpringApp.rootDirName, ( hmColumnData[ mM.columnID!! ] as DataInt ).value, true )

        ( hmColumnData[ mM.columnMeasureSensorCount ] as DataInt ).value = mc.alSensor.size

        for( sensorData in mc.alSensor ) {
            ( hmColumnData[ mM.columnMeasureSensorInfo1 ] as DataString).text += "${sensorData.typeDescr} [${sensorData.dimDescr}]\n"
            ( hmColumnData[ mM.columnMeasureSensorInfo2 ] as DataString).text += "${DateTime_YMDHMS( zoneId0, sensorData.minTime)} -> ${DateTime_YMDHMS( zoneId0, sensorData.maxTime )}\n"
            ( hmColumnData[ mM.columnMeasureSensorInfo3 ] as DataString).text += "Min: ${getSplittedDouble( sensorData.minValue, 1 )} Max: ${getSplittedDouble( sensorData.maxValue, 1 )}\n"
            var calibInfo = ""
            sensorData.alCalibration.forEach { ( sensorValue, realValue ) -> calibInfo += "${getSplittedLong( sensorValue.toLong() )} -> ${getSplittedDouble( realValue, 1 )}, " }
            if( calibInfo.isEmpty() ) calibInfo = "-"
            ( hmColumnData[ mM.columnMeasureSensorInfo4 ] as DataString).text += calibInfo + "\n"
        }

        ( hmColumnData[ mM.columnMeasureError ] as DataString).text = mc.errorDescr
    }

}
