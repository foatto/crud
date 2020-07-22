package foatto.mms.core_mms.report

import foatto.core.app.xy.XyProjection
import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.secondIntervalToString
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataTime3Int
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*
import kotlin.math.max

class cObjectZone : cMMSReport() {

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        val m = model as mObjectZone

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

        hmReportParam["report_zone"] = (hmColumnData[m.columnReportZone] as DataInt).value

        hmReportParam["report_type"] = (hmColumnData[m.columnReportType] as DataComboBox).value
        hmReportParam["report_group_type"] = (hmColumnData[m.columnReportGroupType] as DataComboBox).value

        return getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4
        printPageOrientation = PageOrientation.LANDSCAPE

        printMarginLeft = 10
        printMarginRight = 10
        printMarginTop = 20
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    override fun postReport(sheet: WritableSheet) {

        //--- черновой/сырой список результатов
        val alDraftResult = calcObjectZone()

        //--- загрузка стартовых параметров
        //--- загрузка стартовых параметров
        //        int reportObjectUser = (Integer) hmReportParam.get( "report_object_user" );
        //        int reportObject = (Integer) hmReportParam.get( "report_object" );
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        //        int reportZone = (Integer) hmReportParam.get( "report_zone" );

        val reportType = hmReportParam["report_type"] as Int
        val reportGroupType = hmReportParam["report_group_type"] as Int

        //--- если отчет получается слишком длинный, то убираем лишние строки (т.к. нет режима вывода только сумм)
        while(alDraftResult.size > java.lang.Short.MAX_VALUE) alDraftResult.removeAt(alDraftResult.size - 1)

        //--- чистовой отсортированный список результатов
        val alSortedResult = mutableListOf<ObjectZoneCalcResult>()
        for(ozcr1 in alDraftResult) {
            val groupName1 = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) ozcr1.oc.name else ozcr1.zoneName
            val detailName1 = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) ozcr1.zoneName else ozcr1.oc.name

            var pos = 0
            while(pos < alSortedResult.size) {
                val ozcr2 = alSortedResult[pos]
                val groupName2 = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) ozcr2.oc.name else ozcr2.zoneName
                val detailName2 = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) ozcr2.zoneName else ozcr2.oc.name

                val groupSortResult = groupName1.compareTo(groupName2)
                if(groupSortResult < 0) break
                if(groupSortResult == 0 && (if(reportType == mObjectZone.TYPE_DETAIL) (ozcr1.begTime - ozcr2.begTime).toInt()
                    else detailName1.compareTo(detailName2)) <= 0
                ) break
                pos++
            }
            if(reportType == mObjectZone.TYPE_DETAIL) alSortedResult.add(pos, ozcr1)
            else {
                //--- позиция для вставки в конце списка
                if(pos >= alSortedResult.size) alSortedResult.add(pos, ObjectZoneCalcResult(ozcr1))
                else {
                    val ozcr2 = alSortedResult[pos]
                    val groupName2 = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) ozcr2.oc!!.name else ozcr2.zoneName
                    val detailName2 = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) ozcr2.zoneName else ozcr2.oc!!.name
                    if(groupName1 == groupName2 && detailName1 == detailName2) ozcr2.add(ozcr1)
                    else alSortedResult.add(pos, ObjectZoneCalcResult(ozcr1))
                }
            }
        }

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = max(offsY, outReportCap(sheet, 4, 0) + 1)

        //--- установка размеров заголовков (общая ширина = 90 для А4 портрет и 140 для А4 ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(if(reportType == mObjectZone.TYPE_DETAIL) 29 else 34)    // "Объект/Геозона"
        if(reportType == mObjectZone.TYPE_DETAIL) {
            alDim.add(9)    // "Въезд" - дата и время в две строки как в сравнительном отчёте
            alDim.add(9)    // "Выезд" - дата и время в две строки как в сравнительном отчёте
        }
        else alDim.add(5)     // "Кол-во вх./вых."
        alDim.add(9)         // "Продолжительность"
        alDim.add(7)         // "Пробег [км]"
        alDim.add(if(reportType == mObjectZone.TYPE_DETAIL) 29 else 33)         // "Оборудование"
        alDim.add(7)         // "Время работы [час]"
        alDim.add(if(reportType == mObjectZone.TYPE_DETAIL) 29 else 33)         // "Топливо"
        alDim.add(7)         // "Расход [л]"

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        var offsX = 0
        sheet.addCell(Label(offsX++, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(
            Label(
                offsX++, offsY, if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) "Геозона" else "Объект", wcfCaptionHC
            )
        )
        if(reportType == mObjectZone.TYPE_DETAIL) {
            sheet.addCell(Label(offsX++, offsY, "Въезд", wcfCaptionHC))
            sheet.addCell(Label(offsX++, offsY, "Выезд", wcfCaptionHC))
        }
        else sheet.addCell(Label(offsX++, offsY, "Кол-во вх./вых.", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Продолжительность", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Пробег [км]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Оборудование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Время работы [час]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Топливо", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Расход [л]", wcfCaptionHC))

        offsY++

        var countNN = 1
        var lastGroupName = ""

        for(cr in alSortedResult) {
            offsX = 0

            val curGroupName = if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) cr.oc!!.name else cr.zoneName
            if(lastGroupName != curGroupName) {
                sheet.addCell(Label(1, offsY, curGroupName, wcfCellCB))
                sheet.mergeCells(1, offsY, if(reportType == mObjectZone.TYPE_DETAIL) 9 else 8, offsY + 2)
                offsY += 3

                lastGroupName = curGroupName
            }

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(
                Label(
                    offsX++, offsY, if(reportGroupType == mObjectZone.GROUP_BY_OBJECT) cr.zoneName
                    else cr.oc.name, wcfCellC
                )
            )
            if(reportType == mObjectZone.TYPE_DETAIL) {
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, cr.begTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, cr.endTime), wcfCellC))
            }
            else sheet.addCell(Label(offsX++, offsY, cr.count.toString(), wcfCellR))

            sheet.addCell(Label(offsX++, offsY, secondIntervalToString(cr.begTime, cr.endTime), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, getSplittedDouble(cr.calc!!.gcd!!.run, 1).toString(), wcfCellR))

            val sbWorkName = StringBuilder()
            val sbWorkTotal = StringBuilder()
            val sbLiquidUsingName = StringBuilder()
            val sbLiquidUsingTotal = StringBuilder()

            ObjectCalc.fillWorkString(cr.calc!!.tmWorkCalc, sbWorkName, sbWorkTotal, StringBuilder(), StringBuilder())
            ObjectCalc.fillLiquidUsingString(
                cr.calc!!.tmLiquidUsingCalc, sbLiquidUsingName, sbLiquidUsingTotal, StringBuilder(), StringBuilder()
            )

            sheet.addCell(Label(offsX++, offsY, sbWorkName.toString(), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, sbWorkTotal.toString(), wcfCellR))
            sheet.addCell(Label(offsX++, offsY, sbLiquidUsingName.toString(), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, sbLiquidUsingTotal.toString(), wcfCellR))

            offsY++
        }
        offsY++

        //offsY += 2;
        sheet.addCell(Label(if(reportType == mObjectZone.TYPE_DETAIL) 8 else 7, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells( 3, offsY, 4, offsY );

        outReportSignature(sheet, intArrayOf(0, 4, 7), offsY + 3)
    }

    private fun calcObjectZone(): MutableList<ObjectZoneCalcResult> {

        val alResult = mutableListOf<ObjectZoneCalcResult>()

        //--- загрузка стартовых параметров
        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        val (begTime, endTime) = getBegEndTimeFromParam()

        val alObjectID = ArrayList<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        //--- загрузить данные по зонам
        val hmZoneData = ZoneData.getZoneData(stm, userConfig, reportZone)

        for(objectID in alObjectID) {
            val oc = ObjectConfig.getObjectConfig(stm, userConfig, objectID)
            //--- гео-датчик не прописан
            if(oc.scg == null) continue

            //--- список активных/обрабатываемых зон
            val alZoneCalc = ArrayList<ObjectZoneCalcResult>()

            //--- грузим данные за период
            val pair = ObjectCalc.loadAllSensorData(stm, oc, begTime, endTime)
            val alRawTime = pair.component1()
            val alRawData = pair.component2()

            var curTime = 0
            for(timeIndex in alRawTime.indices) {
                curTime = alRawTime[timeIndex]
                //--- данные до запрашиваемого диапазона (расширенные для сглаживания)
                //--- в данном случае не интересны и их можно пропустить
                if(curTime < begTime) continue
                //--- данные после запрашиваемого диапазона (расширенные для сглаживания)
                //--- в данном случае не интересны и можно прекращать обработку
                if(curTime > endTime) {
                    //--- точка за краем диапазона нам не нужна, возвращаем предыдущую точку
                    if(timeIndex > 0) curTime = alRawTime[timeIndex - 1]
                    break
                }

                val gd = AbstractObjectStateCalc.getGeoData(oc, alRawData[timeIndex]) ?: continue
                //--- самих геоданных может и не оказаться

                val pixPoint = XyProjection.wgs_pix(gd.wgs)

                //--- составление списка зон для данной точки (работаем с name, а не ID, для удобства последующей группировки/сортировки)
                val hsZoneName = HashSet<String>()
                for( zd in hmZoneData.values ) {
                    if(zd.polygon!!.isContains(pixPoint)) hsZoneName.add(zd.name)
                }

                //--- по каждой обрабатываемой сейчас зоне для данного автомобиля
                var i = 0
                while(i < alZoneCalc.size) {
                    val cr = alZoneCalc[i]
                    //--- а/м все еще в этой зоне
                    if(hsZoneName.contains(cr.zoneName)) {
                        hsZoneName.remove(cr.zoneName)   // убрать обработанную зону из списка
                        i++
                    }
                    else {
                        cr.endTime = curTime
                        cr.calc = ObjectCalc.calcObject(stm, userConfig, oc, cr.begTime, cr.endTime)

                        alResult.add(cr)
                        alZoneCalc.removeAt(i)
                    }//--- а/м вышел из этой зоны
                }
                //--- обработка оставшихся в списке новых зон
                for(zoneName in hsZoneName) alZoneCalc.add(ObjectZoneCalcResult(oc, zoneName, curTime))
            }

            //--- "завершить" зоны, реальный выход из которых находится за пределами заданного временного периода
            //--- по каждой обрабатываемой сейчас зоне для данного автомобиля
            for(cr in alZoneCalc) {
                cr.endTime = curTime
                cr.calc = ObjectCalc.calcObject(stm, userConfig, oc, cr.begTime, cr.endTime)

                alResult.add(cr)
            }
        }

        return alResult
    }

    private class ObjectZoneCalcResult {
        var oc: ObjectConfig
        var zoneName: String

        var begTime = 0
        var endTime = 0

        var calc: ObjectCalc? = null

        var count = 0

        constructor(aOc: ObjectConfig, aZoneName: String, aBegTime: Int) {
            oc = aOc
            zoneName = aZoneName
            begTime = aBegTime
        }

        //--- конструктор для суммарного отчёта
        constructor(cr: ObjectZoneCalcResult) {
            oc = cr.oc
            zoneName = cr.zoneName

            begTime = 0
            endTime = cr.endTime - cr.begTime

            calc = cr.calc

            count = 1
        }

        fun add(cr: ObjectZoneCalcResult) {
            endTime += cr.endTime - cr.begTime  // суммируется общее время/продолжительность

            calc!!.gcd!!.run += cr.calc!!.gcd!!.run

            for((name,wcdNew) in cr.calc!!.tmWorkCalc) {
                val wcdSum = calc!!.tmWorkCalc[name]

                if(wcdSum == null) calc!!.tmWorkCalc[name] = wcdNew
                else wcdSum.onTime += wcdNew.onTime
            }

            for((name,lucdNew) in cr.calc!!.tmLiquidUsingCalc) {
                val lucdSum = calc!!.tmLiquidUsingCalc[name]

                if(lucdSum == null) calc!!.tmLiquidUsingCalc[name] = lucdNew
                else lucdSum.usingTotal += lucdNew.usingTotal
            }

            count++
        }
    }
}
