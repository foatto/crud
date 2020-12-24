package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getZoneId
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZonedDateTime
import java.util.*

class cDataOut : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        fillReportParam(model as mOP)

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
        //--- отчёт защищать не надо, пусть делают что хотят
        printKeyW = 0.0
        printKeyH = 0.0
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        val reportObject = hmReportParam["report_object"] as Int

        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportBegHour = hmReportParam["report_beg_hour"] as Int
        val reportBegMinute = hmReportParam["report_beg_minute"] as Int

        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int
        val reportEndHour = hmReportParam["report_end_hour"] as Int
        val reportEndMinute = hmReportParam["report_end_minute"] as Int

        val begTime = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, reportBegHour, reportBegMinute, 0, 0, zoneId).toEpochSecond().toInt()
        val endTime = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, reportEndHour, reportEndMinute, 0, 0, zoneId).toEpochSecond().toInt()

        val zoneId0 = getZoneId(0)

        //--- загрузка конфигурации объекта
        val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, reportObject)
        //--- соберём все номера портов
        val tmSensorPortType = TreeMap<Int, Int>()
        for((sensorType,hmSC) in objectConfig.hmSensorConfig) {
            for(portNum in hmSC.keys) tmSensorPortType[portNum] = sensorType
        }
        //--- отдельно доберём geo-датчик, если есть
        if(objectConfig.scg != null) tmSensorPortType[SensorConfig.GEO_PORT_NUM] = SensorConfig.SENSOR_GEO

        //--- связка номер порта - позиция в отчёте
        val tmPortIndex = TreeMap<Int, Int>()
        var portIndex = 0
        for(portNum in tmSensorPortType.keys) tmPortIndex[portNum] = portIndex++

        //--- единоразово загрузим данные по всем датчикам объекта
        val ( alRawTime, alRawData ) = ObjectCalc.loadAllSensorData(stm, objectConfig, begTime, endTime)

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)
        offsY = fillReportHeader(objectConfig, sheet, offsY)

        //offsY = Math.max( offsY, outReportCap( sheet, 7, 0 ) + 1 );

        //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(9)    // Время UTC в две строки
        for(i in 0 until tmSensorPortType.size) alDim.add((140 - 5 - 9) / tmSensorPortType.size)

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Время UTC", wcfCaptionHC))
        for(portNum in tmPortIndex.keys) sheet.addCell(Label(2 + tmPortIndex[portNum]!!, offsY, Integer.toString(portNum), wcfCaptionHC))
        offsY++

        var countNN = 1
        for(pos in alRawTime.indices) {
            val curTime = alRawTime[pos]
            //--- данные до запрашиваемого диапазона (расширенные для сглаживания)
            //--- в данном случае не интересны и их можно пропустить
            if(curTime < begTime) continue
            //--- данные после запрашиваемого диапазона (расширенные для сглаживания)
            //--- в данном случае не интересны и можно прекращать обработку
            if(curTime > endTime) break

            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(1, offsY, DateTime_YMDHMS(zoneId0, alRawTime[pos]), wcfCellC))

            val bb = alRawData[pos]
            while(bb.hasRemaining()) {
                val (portNum, dataSize) = AbstractObjectStateCalc.getSensorPortNumAndDataSize(bb)
                //--- по каждому номеру порта - составляем визуальное представление значения
                val sensorValue = AbstractObjectStateCalc.getSensorString(tmSensorPortType[portNum], dataSize, bb)
                //--- выводим только определённые/прописанные порты
                if(tmSensorPortType.containsKey(portNum)) sheet.addCell(Label(2 + tmPortIndex[portNum]!!, offsY, sensorValue, wcfCellC))
            }
            offsY++
        }

        offsY += 2
        sheet.addCell(
            Label(0, offsY, getPreparedAt(), wcfCellL)
        )
        sheet.mergeCells(0, offsY, 3, offsY)

        //outReportSignature( sheet, new int[] { 0, 2, 7 }, offsY + 3 );
    }
}
