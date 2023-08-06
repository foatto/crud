package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.mms.core_mms.device.mDevice.Companion.hmCellOwnerNames
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

class cDeviceCellInfo : cMMSReport() {

    override fun isFormAutoClick(): Boolean = true

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

//        fillReportParam(model as mDeviceCellInfo)

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
        defineFormats(8, 2, 0)

        //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)        // "N п/п"
        alDim.add(8)        // Номер контроллера
        alDim.add(33)       // Владелец объекта
        alDim.add(45)       // Наименование объекта
        alDim.add(11)       // Принадлежность сим-карты
        alDim.add(14)       // Номер сим-карты
        alDim.add(16)       // ICC сим-карты
        alDim.add(8)        // Оператор связи

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        var offsY = 0

        sheet.addCell(Label(2, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Номер контроллера", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Владелец объекта", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Наименование объекта", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Принадлежность сим-карты", wcfCaptionHC))
        sheet.addCell(Label(5, offsY, "Номер сим-карты", wcfCaptionHC))
        sheet.addCell(Label(6, offsY, "ICC сим-карты", wcfCaptionHC))
        sheet.addCell(Label(7, offsY, "Оператор связи", wcfCaptionHC))
        offsY++

        var countNN = 1
        val rs = conn.executeQuery(
            """
                SELECT MMS_device.serial_no , MMS_object.user_id , MMS_object.name , 
                MMS_device.cell_owner , MMS_device.cell_num , MMS_device.cell_icc , MMS_device.cell_operator ,
                MMS_device.cell_owner_2 , MMS_device.cell_num_2 , MMS_device.cell_icc_2 , MMS_device.cell_operator_2
                FROM MMS_device , MMS_object
                WHERE MMS_device.id <> 0 
                AND MMS_device.object_id = MMS_object.id  
                ORDER BY serial_no
            """
        )
        while (rs.next()) {
            var fieldIndex = 1
            val serialNo = rs.getString(fieldIndex++)
            val objectUserId = rs.getInt(fieldIndex++)
            val objectName = rs.getString(fieldIndex++)
            val cellOwner = rs.getInt(fieldIndex++)
            val cellNum = rs.getString(fieldIndex++)
            val cellICC = rs.getString(fieldIndex++)
            val cellOperator = rs.getString(fieldIndex++)
            val cellOwner2 = rs.getInt(fieldIndex++)
            val cellNum2 = rs.getString(fieldIndex++)
            val cellICC2 = rs.getString(fieldIndex++)
            val cellOperator2 = rs.getString(fieldIndex++)

            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(1, offsY, serialNo, wcfCellC))
            sheet.addCell(Label(2, offsY, getRecordUserName(objectUserId), wcfCellC))
            sheet.addCell(Label(3, offsY, objectName, wcfCellC))
            sheet.addCell(Label(4, offsY,
                    hmCellOwnerNames.getOrDefault(cellOwner, "-") + "\n" +
                    hmCellOwnerNames.getOrDefault(cellOwner2, "-"),
                    wcfCellC
                )
            )
            sheet.addCell(Label(5, offsY, cellNum + "\n" + cellNum2, wcfCellC))
            sheet.addCell(Label(6, offsY, cellICC + "\n" + cellICC2, wcfCellC))
            sheet.addCell(Label(7, offsY, cellOperator + "\n" + cellOperator2, wcfCellC))

            offsY++
        }
        rs.close()

        offsY += 2
        sheet.addCell(
            Label(0, offsY, getPreparedAt(), wcfCellL)
        )
        sheet.mergeCells(0, offsY, 3, offsY)

    }
}
