package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataTime3Int
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.WorkCalcData
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cSummaryWithoutWaybill : cStandartPeriodSummary() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if( returnURL != null ) return returnURL

        val m = model as mSummaryWithoutWaybill

        //--- выборка данных параметров для отчета
        fillReportParam(m.uodg)

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val begTime = (hmColumnData[m.columnReportBegTime] as DataTime3Int).localTime
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate
        val endTime = (hmColumnData[m.columnReportEndTime] as DataTime3Int).localTime

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth
        hmReportParam["report_beg_hour"] = begTime.hour
        hmReportParam["report_beg_minute"] = begTime.minute

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth
        hmReportParam["report_end_hour"] = endTime.hour
        hmReportParam["report_end_minute"] = endTime.minute

        hmReportParam.put( "report_time_type", (hmColumnData[ m.columnTimeType ] as DataComboBox ).value )

        hmReportParam.put( "report_is_compact", false  )

        return getReport()
    }

    override fun postReport( sheet: WritableSheet ) {

        //--- загрузка стартовых параметров
        //        int reportObjectUser = ( Integer ) hmReportParam.get(  "report_object_user"  );
        //        int reportObject = ( Integer ) hmReportParam.get(  "report_object"  );
        val reportDepartment = hmReportParam[ "report_department" ] as Int
        val reportGroup = hmReportParam[ "report_group" ] as Int

        val reportTimeType = hmReportParam[ "report_time_type" ] as Int

        val (begTime, endTime) = getBegEndTimeFromParam()

        defineFormats( 8, 2, 0 )
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader( reportDepartment, reportGroup, sheet, 1, offsY )

        offsY = defineSummaryReportHeaders( sheet, offsY, "Объект" )

        val allSumCollector = SumCollector()
        var countNN = 1
        for( objectIndex in alObjectID.indices ) {
            val objectConfig = alObjectConfig[ objectIndex ]
            val objectCalc = ObjectCalc.calcObject( stm, userConfig, objectConfig, begTime, endTime )

            //--- расчёт по путёвкам для последующего вычитания
            val alWSD = ArrayList<WorkShiftData>()

            val begDTField = if( reportTimeType == mSummaryWithoutWaybill.TIME_TYPE_DOC ) "beg_dt" else "beg_dt_fact"
            val endDTField = if( reportTimeType == mSummaryWithoutWaybill.TIME_TYPE_DOC ) "end_dt" else "end_dt_fact"

            val sql = " SELECT $begDTField , $endDTField " +
                      " FROM MMS_work_shift " +
                      " WHERE object_id = ${alObjectID[ objectIndex ]} " +
                      //--- пока будем брать путевки, только полностью входящие в требуемый диапазон
                      " AND $begDTField >= $begTime " +
                      " AND $endDTField <= $endTime "

            val rs = stm.executeQuery( sql )
            while( rs.next() ) alWSD.add( WorkShiftData( rs.getInt( 1 ), rs.getInt( 2 ) ) )
            rs.close()

            for( wsd in alWSD ) {
                val wsdCalc = ObjectCalc.calcObject( stm, userConfig, objectConfig, wsd.begTime, wsd.endTime )

                if( objectCalc.gcd != null && wsdCalc.gcd != null ) {
                    objectCalc.gcd!!.run -= wsdCalc.gcd!!.run
                    objectCalc.gcd!!.movingTime -= wsdCalc.gcd!!.movingTime
                    objectCalc.gcd!!.parkingCount -= wsdCalc.gcd!!.parkingCount
                    objectCalc.gcd!!.parkingTime -= wsdCalc.gcd!!.parkingTime
                }

                for( workName in wsdCalc.tmWorkCalc.keys ) {
                    val wcd = wsdCalc.tmWorkCalc[ workName ]!!
                    val wcdObject: WorkCalcData = objectCalc.tmWorkCalc[ workName ]!!

                    wcdObject.onTime -= wcd.onTime
                    wcdObject.onMovingTime -= wcd.onMovingTime
                    wcdObject.onParkingTime -= wcd.onParkingTime
                }

                for( liquidName in objectCalc.tmLiquidUsingCalc.keys ) {
                    val lucd = objectCalc.tmLiquidUsingCalc[ liquidName ]!!
                    val lucdObject = objectCalc.tmLiquidUsingCalc[ liquidName ]!!

                    lucdObject.usingMoving -= lucd.usingMoving
                    lucdObject.usingParking -= lucd.usingParking
                    lucdObject.usingTotal -= lucd.usingTotal
                }

                for( energoName in objectCalc.tmEnergoCalc.keys ) {
                    val e = objectCalc.tmEnergoCalc[ energoName ]!!
                    val eSum = objectCalc.tmEnergoCalc[ energoName ]!!

                    objectCalc.tmEnergoCalc[ energoName ] = eSum - e
                }
            }

            allSumCollector.add( null, 0, objectCalc )

            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell( Label( 0, offsY, (countNN++).toString(), wcfNN ) )
            offsY = addGroupTitle( sheet, offsY, objectConfig.name )

            offsY = outRow( sheet, offsY, objectConfig, objectCalc )
        }

        if( isCompactReport ) {
            sheet.addCell( Label( 0, offsY, "ИТОГО общее", wcfCellCBStdYellow ) )
            sheet.mergeCells( 0, offsY, 6, offsY )
            offsY += 2
        }
        else {
            sheet.addCell( Label( 0, offsY, "ИТОГО общее", wcfCellCBStdYellow ) )
            sheet.mergeCells( 0, offsY, if( isGlobalUseSpeed ) 10 else if( isGlobalUsingCalc ) 7 else 6, offsY + 2 )
            offsY += 4
        }
        offsY = outSumData( sheet, offsY, allSumCollector.sumUser, false )

        outReportTrail( sheet, offsY )
    }

    override fun outRow(sheet: WritableSheet, aOffsY: Int, objectConfig: ObjectConfig, objectCalc: ObjectCalc): Int {
        var offsY = aOffsY
        if(isCompactReport) {
            sheet.addCell(Label(2, offsY, objectCalc.sbGeoRun.toString(), wcfCellC))
            sheet.addCell(Label(3, offsY, objectCalc.sbWorkName.toString(), wcfCellC))
            sheet.addCell(Label(4, offsY, objectCalc.sbWorkTotal.toString(), wcfCellC))
            sheet.addCell(Label(5, offsY, objectCalc.sbLiquidUsingName.toString(), wcfCellC))
            sheet.addCell(Label(6, offsY, objectCalc.sbLiquidUsingTotal.toString(), wcfCellC))
            offsY++
        }
        else {
            //--- отчёт по гео-датчику
            if(objectConfig.scg != null && (objectConfig.scg!!.isUseSpeed || objectConfig.scg!!.isUseRun)) {
                //--- дополним стандартный расчёт средними расходами
                val sbAvgUsingTotal = StringBuilder()
                val sbAvgUsingMoving = StringBuilder()
                //--- при наличии пробега посчитаем общий средний расход
                if(objectConfig.scg!!.isUseRun) {
                    //--- нужен именно объект, чтобы поймать null
                    var sumUsingTotal: Double? = null
                    for(llcdDescr in objectCalc.tmLiquidLevelCalc.keys) {
                        val llcd = objectCalc.tmLiquidLevelCalc[llcdDescr]!!
                        //--- если в группе у этого уровнемера есть гео-датчик, и нет оборудования и генераторов/электросчётчиков,
                        //--- то считаем средний расход в движении по нему
                        if( llcd.gcd != null && llcd.tmWorkCalc.isEmpty() && llcd.tmEnergoCalc.isEmpty() )
                            sumUsingTotal = ( sumUsingTotal ?: 0.0 ) + llcd.usingTotal
                    }
                    sbAvgUsingTotal.append( if( sumUsingTotal == null || sumUsingTotal < 0 || objectCalc.gcd!!.run == 0.0 ) '-'
                    else getSplittedDouble(100.0 * sumUsingTotal / objectCalc.gcd!!.run, 1 ) )
                }
                //--- при наличии движения и пробега посчитаем средний расход в движении
                if(objectConfig.scg!!.isUseSpeed && objectConfig.scg!!.isUseRun) {
                    //--- нужен именно объект, чтобы поймать null
                    var sumUsingMoving: Double? = null
                    for(llcdDescr in objectCalc.tmLiquidLevelCalc.keys) {
                        val llcd = objectCalc.tmLiquidLevelCalc[llcdDescr]!!
                        //--- если в группе у этого уровнемера есть гео-датчик, и нет оборудования и генераторов/электросчётчиков,
                        //--- то считаем средний расход в движении по нему
                        if(llcd.gcd != null && llcd.tmWorkCalc.isEmpty() && llcd.tmEnergoCalc.isEmpty())
                            sumUsingMoving = ( sumUsingMoving ?: 0.0 ) + llcd.usingMoving
                    }
                    sbAvgUsingMoving.append(if(sumUsingMoving == null || sumUsingMoving < 0 || objectCalc.gcd!!.run == 0.0) '-'
                    else getSplittedDouble(100.0 * sumUsingMoving / objectCalc.gcd!!.run, 1))
                }

                var offsX = 1
                sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                offsX++
                if(objectConfig.scg!!.isUseRun) {
                    sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                    offsX++
                }
                if(objectConfig.scg!!.isUseSpeed) {
                    sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX + 4, offsY)
                    sheet.addCell(Label(offsX, offsY + 1, "выезда", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 1, offsY + 1, "заезда", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 2, offsY + 1, "в пути", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 3, offsY + 1, "в движении", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 4, offsY + 1, "на стоянках", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 5, offsY, "Кол-во стоянок", wcfCaptionHC))
                    sheet.mergeCells(offsX + 5, offsY, offsX + 5, offsY + 1)
                    offsX += 6
                }
                offsY += 2

                offsX = 1
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoName.toString(), wcfCellC))
                if(objectConfig.scg!!.isUseRun) sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoRun.toString(), wcfCellC))
                if(objectConfig.scg!!.isUseSpeed) {
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoOutTime.toString(), wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoInTime.toString(), wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoWayTime.toString(), wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoMovingTime.toString(), wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoParkingTime.toString(), wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoParkingCount.toString(), wcfCellC))
                }
                offsY += 2
            }

            //--- отчёт по датчикам оборудования
            if(!objectCalc.tmWorkCalc.isEmpty()) {
                //--- дополним стандартный расчёт средним расходом
                val tmAvgUsingTotal = TreeMap<String, String>()
                val tmAvgUsingMoving = TreeMap<String, String>()
                val tmAvgUsingParking = TreeMap<String, String>()
                for(workDescr in objectCalc.tmWorkCalc.keys) {
                    val wcd = objectCalc.tmWorkCalc[workDescr]!!
                    var sumUsingTotal: Double? = null
                    var sumUsingMoving: Double? = null
                    var sumUsingParking: Double? = null
                    for(llcdDescr in objectCalc.tmLiquidLevelCalc.keys) {
                        val llcd = objectCalc.tmLiquidLevelCalc[llcdDescr]!!
                        //--- если в группе у этого уровнемера есть датчик работы оборудования, он один и "этот самый",
                        //--- и при этом нет геодатчика и электросчётчика (т.е. генераторов), то считаем средний расход
                        if(llcd.tmWorkCalc.size == 1 && llcd.tmWorkCalc[workDescr] != null && llcd.gcd == null && llcd.tmEnergoCalc.isEmpty()) {

                            sumUsingTotal = ( sumUsingTotal ?: 0.0 ) + llcd.usingTotal
                            if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                                sumUsingMoving = ( sumUsingMoving ?: 0.0 ) + llcd.usingMoving
                                sumUsingParking = ( sumUsingParking ?: 0.0 ) + llcd.usingParking
                            }
                        }
                    }
                    tmAvgUsingTotal.put(workDescr, if(wcd.onTime == 0 || sumUsingTotal == null || sumUsingTotal < 0) "-"
                    else getSplittedDouble(sumUsingTotal / (wcd.onTime.toDouble() / 60.0 / 60.0), 1).toString())
                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        tmAvgUsingMoving.put(workDescr, if(wcd.onMovingTime == 0 || sumUsingMoving == null || sumUsingMoving < 0) "-"
                        else getSplittedDouble(sumUsingMoving / (wcd.onMovingTime.toDouble() / 60.0 / 60.0), 1).toString())
                        tmAvgUsingParking.put(workDescr, if(wcd.onParkingTime == 0 || sumUsingParking == null || sumUsingParking < 0) "-"
                        else getSplittedDouble(sumUsingParking / (wcd.onParkingTime.toDouble() / 60.0 / 60.0), 1).toString())
                    }
                }

                if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                    sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
                    sheet.mergeCells(1, offsY, 1, offsY + 1)
                    sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
                    sheet.mergeCells(2, offsY, 4, offsY)
                    sheet.addCell(Label(2, offsY + 1, "общее", wcfCaptionHC))
                    sheet.addCell(Label(3, offsY + 1, "в движении", wcfCaptionHC))
                    sheet.addCell(Label(4, offsY + 1, "на стоянках", wcfCaptionHC))
                    offsY += 2
                }
                else {
                    sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
                    sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
                    offsY++
                }
                for(workDescr in objectCalc.tmWorkCalc.keys) {
                    val wcd = objectCalc.tmWorkCalc[workDescr]!!

                    sheet.addCell(Label(1, offsY, workDescr, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        sheet.addCell(Label(3, offsY, getSplittedDouble(wcd.onMovingTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                        sheet.addCell(Label(4, offsY, getSplittedDouble(wcd.onParkingTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                    }
                    offsY++
                }
                offsY++
            }

            //--- отчёт по совместному расходу жидкости
            if(!objectCalc.tmLiquidUsingCalc.isEmpty()) {
                if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                    sheet.addCell(Label(1, offsY, "Расход по видам топлива", wcfCaptionHC))
                    sheet.mergeCells(1, offsY, 1, offsY + 1)
                    sheet.addCell(Label(2, offsY, "Расход [л]", wcfCaptionHC))
                    sheet.mergeCells(2, offsY, 4, offsY)
                    sheet.addCell(Label(2, offsY + 1, "общий", wcfCaptionHC))
                    sheet.addCell(Label(3, offsY + 1, "в движении", wcfCaptionHC))
                    sheet.addCell(Label(4, offsY + 1, "на стоянках", wcfCaptionHC))
                    offsY += 2
                }
                else {
                    sheet.addCell(Label(1, offsY, "Расход по видам топлива", wcfCaptionHC))
                    sheet.addCell(Label(2, offsY, "Расход [л]", wcfCaptionHC))
                    offsY++
                }
                for(liquidName in objectCalc.tmLiquidUsingCalc.keys) {
                    val lucd = objectCalc.tmLiquidUsingCalc[liquidName]!!

                    sheet.addCell(Label(1, offsY, liquidName, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(lucd.usingTotal, ObjectCalc.getPrecision(lucd.usingTotal)).toString(), wcfCellC))
                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        sheet.addCell(Label(3, offsY, getSplittedDouble(lucd.usingMoving, ObjectCalc.getPrecision(lucd.usingMoving)).toString(), wcfCellC))
                        sheet.addCell(Label(4, offsY, getSplittedDouble(lucd.usingParking, ObjectCalc.getPrecision(lucd.usingParking)).toString(), wcfCellC))
                    }
                    offsY++
                }
                offsY++
            }

            //--- отчёт по расходу/выработке электроэнергии
            if(!objectCalc.tmEnergoCalc.isEmpty()) {
                //--- дополним стандартный расчёт средним расходом
                val tmEnergoCalcAvgUsing = TreeMap<String, String>()
                for(energoDescr in objectCalc.tmEnergoCalc.keys) {
                    val e = objectCalc.tmEnergoCalc[energoDescr]
                    var sumUsing: Double? = null
                    for(llcdDescr in objectCalc.tmLiquidLevelCalc.keys) {
                        val llcd = objectCalc.tmLiquidLevelCalc[llcdDescr]!!
                        //--- если в группе у этого уровнемера есть электросчётчик, он один и "этот самый",
                        //--- и при этом нет геодатчика и оборудования, то считаем средний расход
                        if(llcd.tmEnergoCalc.size == 1 && llcd.tmEnergoCalc[energoDescr] != null && llcd.gcd == null && llcd.tmWorkCalc.isEmpty())

                            sumUsing = ( sumUsing ?: 0.0 ) + llcd.usingTotal
                    }
                    //--- выводим в кВт*ч
                    tmEnergoCalcAvgUsing.put(energoDescr, if(e == 0 || sumUsing == null || sumUsing < 0) "-"
                    else getSplittedDouble(sumUsing / (e!! / 1000.0), 1).toString())
                }

                sheet.addCell(Label(1, offsY, "Наименование счётчика", wcfCaptionHC))
                sheet.addCell(Label(2, offsY, "Электро энергия [кВт*ч]", wcfCaptionHC))
                sheet.addCell(Label(3, offsY, "Средний расход [л/кВт*ч]", wcfCaptionHC))
                offsY++
                for(energoDescr in objectCalc.tmEnergoCalc.keys) {
                    sheet.addCell(Label(1, offsY, energoDescr, wcfCellC))
                    //--- выводим в кВт*ч
                    sheet.addCell(Label(2, offsY, getSplittedDouble(objectCalc.tmEnergoCalc[energoDescr]!! / 1000.0, 3).toString(), wcfCellC))
                    sheet.addCell(Label(3, offsY, tmEnergoCalcAvgUsing[energoDescr], wcfCellC))
                    offsY++
                }
                offsY++
            }

            //--- вывод суммы по каждой суммовой группе
            for(sumGroup in objectCalc.tmEnergoGroupSum.keys) {
                val eSum = objectCalc.tmEnergoGroupSum[sumGroup]!!

                sheet.addCell(Label(1, offsY, if(sumGroup.isEmpty()) "ИТОГО:" else StringBuilder("ИТОГО по '").append(sumGroup).append("':").toString(), wcfCellRBStdYellow))

                sheet.addCell(Label(2, offsY, getSplittedDouble(eSum / 1000.0, 3).toString(), wcfCellCBStdYellow))
                offsY++
            }

            offsY++    // еще одна пустая строчка снизу
        }
        return offsY
    }

    private class WorkShiftData constructor( val begTime: Int, val endTime: Int )

}
