package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.getSplittedDouble
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class cEquipService : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        fillReportParam(model as mUODG)

        return getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4
        printPageOrientation = PageOrientation.PORTRAIT

        printMarginLeft = 20
        printMarginRight = 10
        printMarginTop = 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    override fun postReport(sheet: WritableSheet) {
        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val zdtBeg = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, zoneId)    // раньше не бывает :)
        val zdtEnd = ZonedDateTime.now(zoneId)    // на текущий момент
        //gcEnd.add( GregorianCalendar.DAY_OF_MONTH, 1 ); // на всякий случай добавим денёк вперёд

        val alObjectID = ArrayList<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        defineFormats(8, 2, 0)

        var offsY = 0
        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(18)    // Оборудование
        alDim.add(18)    // Наименование ТО
        alDim.add(7)
        alDim.add(8)     // текущая наработка - добавим еще одну цифру
        alDim.add(9)
        alDim.add(8)     // последняя наработка - добавим еще одну цифру
        alDim.add(9)
        alDim.add(8)     // следующая наработка - добавим еще одну цифру

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Наименование ТО", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Перио-дич-ность\n[мото-час]", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Текущая наработка\n[мото-час]", wcfCaptionHC))
        sheet.addCell(Label(5, offsY, "Дата последнего ТО", wcfCaptionHC))
        sheet.addCell(Label(6, offsY, "Наработка к последнему ТО\n[мото-час]", wcfCaptionHC))
        sheet.addCell(Label(7, offsY, "Прогноз даты следую-щего ТО", wcfCaptionHC))
        sheet.addCell(Label(8, offsY, "Наработка к следую-щему ТО [мото-час]", wcfCaptionHC))
        offsY++

        var countNN = 1
        for(objectID in alObjectID) {
            val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectID)

            //--- пропускаем, если нет датчиков оборудования
            val hmSCW = objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]
            if(hmSCW == null || hmSCW.isEmpty()) continue

            val objectCalc = ObjectCalc.calcObject(stm, userConfig, objectConfig, zdtBeg.toEpochSecond().toInt(), zdtEnd.toEpochSecond().toInt())

            //--- пропускаем, если нет данных по работе
            if(objectCalc.tmWorkCalc.isEmpty()) continue

            //--- вывод заголовков групп
            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            sheet.mergeCells(0, offsY, 0, offsY + 2)
            sheet.addCell(Label(1, offsY, objectConfig.name, wcfCellCB))
            sheet.mergeCells(1, offsY, 8, offsY + 2)
            offsY += 4 // +1 пустая строка снизу

            for(portNum in hmSCW.keys) {
                val scw = hmSCW[portNum] as SensorConfigWork
                val wcd = objectCalc.tmWorkCalc[scw.descr] ?: continue

                val alESD = ArrayList<EquipServiceData>()
                var rs = stm.executeQuery(
                    StringBuilder().append(" SELECT id , name , period ").append(" FROM MMS_equip_service_shedule ").append(" WHERE equip_id = ").append(scw.id).append(" ORDER BY name ").toString()
                )
                while(rs.next()) alESD.add(EquipServiceData(rs.getInt(1), rs.getString(2), rs.getDouble(3)))
                rs.close()

                for(esd in alESD) {
                    rs = stm.executeQuery(
                        StringBuilder().append(" SELECT ye , mo , da , work_hour ").append(" FROM MMS_equip_service_history ").append(" WHERE shedule_id = ").append(esd.id).append(" ORDER BY ye DESC , mo DESC , da DESC ").toString()
                    )
                    //--- только одну первую строку (один из вариантов реализации TOP(1))
                    if(rs.next()) {
                        esd.lastWorkTime = ZonedDateTime.of(rs.getInt(1), rs.getInt(2), rs.getInt(3), 0, 0, 0, 0, zoneId)
                        esd.lastWorkValue = rs.getDouble(4)
                    }
                    rs.close()
                }

                for(esd in alESD) {
                    val curWork = scw.begWorkValue + wcd.onTime.toDouble() / 60.0 / 60.0
                    sheet.addCell(Label(1, offsY, scw.descr, wcfCellL))
                    sheet.addCell(Label(2, offsY, esd.name, wcfCellL))
                    sheet.addCell(Label(3, offsY, getSplittedDouble(esd.period, 1).toString(), wcfCellC))
                    sheet.addCell(Label(4, offsY, getSplittedDouble(curWork, 1).toString(), wcfCellC))
                    sheet.addCell(
                        Label(
                            5, offsY, if(esd.lastWorkTime == null) "-" else DateTime_DMY(esd.lastWorkTime!!), wcfCellC
                        )
                    )
                    sheet.addCell(
                        Label(
                            6, offsY, getSplittedDouble(
                                esd.lastWorkValue, 1
                            ).toString(), wcfCellC
                        )
                    )

                    var futureWorkTime: ZonedDateTime? = null
                    if(esd.lastWorkTime != null) {
                        //--- считаем в часах
                        val addHour = esd.period * ((zdtEnd.toEpochSecond() - esd.lastWorkTime!!.toEpochSecond()).toDouble() / 60.0 / 60.0) / (curWork - esd.lastWorkValue)
                        futureWorkTime = esd.lastWorkTime!!.plus(addHour.toLong(), ChronoUnit.HOURS)
                    }
                    sheet.addCell(
                        Label(
                            7, offsY,
                            if(futureWorkTime == null) "-" else DateTime_DMY(futureWorkTime),
                            if(futureWorkTime == null || futureWorkTime.isAfter(zdtEnd)) wcfCellC else wcfCellCBRedStd
                        )
                    )
                    sheet.addCell(
                        Label(
                            8, offsY, if(futureWorkTime == null) "-" else getSplittedDouble(esd.lastWorkValue + esd.period, 1).toString(), wcfCellC
                        )
                    )

                    offsY++
                }
            }
            offsY++    // еще одна пустая строчка снизу
        }
        offsY += 2
        sheet.addCell(
            Label(1, offsY, getPreparedAt(), wcfCellL)
        )
        sheet.mergeCells(1, offsY, 2, offsY)

        //        outReportSignature( sheet, new int[] { 0, 2, 7 }, offsY + 3 );
    }

    private class EquipServiceData(val id: Int, val name: String, val period: Double) {
        var lastWorkTime: ZonedDateTime? = null
        var lastWorkValue = 0.0
    }
}
