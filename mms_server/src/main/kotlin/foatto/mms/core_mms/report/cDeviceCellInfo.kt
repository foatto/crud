package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.mms.core_mms.device.mDevice.Companion.hmCellOwnerNames
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

class cDeviceCellInfo : cMMSReport() {

    override fun isFormAutoClick(): Boolean = true

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, hmFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

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

        //--- установка размеров заголовков (отчёт неограниченной ширины)
        val alDim = mutableListOf<Int>()
        alDim.add(5)        // "N п/п"
        alDim.add(8)        // Номер контроллера
        alDim.add(33)       // Владелец объекта
        alDim.add(40)       // Наименование объекта

        alDim.add(16)       // IMEI модема - 1
        alDim.add(12)       // Принадлежность сим-карты - 1
        alDim.add(15)       // Номер сим-карты - 1
        alDim.add(16)       // ICC сим-карты - 1
        alDim.add(8)        // Оператор связи - 1

        alDim.add(16)       // IMEI модема - 2
        alDim.add(12)       // Принадлежность сим-карты - 2
        alDim.add(15)       // Номер сим-карты - 2
        alDim.add(16)       // ICC сим-карты - 2
        alDim.add(8)        // Оператор связи - 2

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        var offsY = 0

        sheet.addCell(Label(2, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++

        //--- вывод заголовка
        var offsX = 0
        sheet.addCell(Label(offsX++, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Номер контроллера", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Владелец объекта", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Наименование объекта", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "IMEI модема 1", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Принадлежность сим-карты 1", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Номер сим-карты 1", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "ICC сим-карты 1", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Оператор связи 1", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "IMEI модема 2", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Принадлежность сим-карты 2", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Номер сим-карты 2", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "ICC сим-карты 2", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Оператор связи 2", wcfCaptionHC))
        offsY++

        var countNN = 1
        val rs = conn.executeQuery(
            """
                SELECT MMS_device.serial_no , MMS_object.user_id , MMS_object.name , 
                MMS_device.cell_imei ,   MMS_device.cell_owner ,   MMS_device.cell_num ,   MMS_device.cell_icc ,   MMS_device.cell_operator ,
                MMS_device.cell_imei_2 , MMS_device.cell_owner_2 , MMS_device.cell_num_2 , MMS_device.cell_icc_2 , MMS_device.cell_operator_2
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

            val cellImei = rs.getString(fieldIndex++)
            val cellOwner = rs.getInt(fieldIndex++)
            val cellNum = rs.getString(fieldIndex++)
            val cellICC = rs.getString(fieldIndex++)
            val cellOperator = rs.getString(fieldIndex++)

            val cellImei2 = rs.getString(fieldIndex++)
            val cellOwner2 = rs.getInt(fieldIndex++)
            val cellNum2 = rs.getString(fieldIndex++)
            val cellICC2 = rs.getString(fieldIndex++)
            val cellOperator2 = rs.getString(fieldIndex++)

            offsX = 0

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(offsX++, offsY, serialNo, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, getRecordUserName(objectUserId), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, objectName, wcfCellC))

            sheet.addCell(Label(offsX++, offsY, cellImei, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, hmCellOwnerNames.getOrDefault(cellOwner, "-"), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, cellNum, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, cellICC, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, cellOperator, wcfCellC))

            sheet.addCell(Label(offsX++, offsY, cellImei2, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, hmCellOwnerNames.getOrDefault(cellOwner2, "-"), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, cellNum2, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, cellICC2, wcfCellC))
            sheet.addCell(Label(offsX++, offsY, cellOperator2, wcfCellC))

            offsY++
        }
        rs.close()

        offsY += 2
        sheet.addCell(
            Label(0, offsY, getPreparedAt(), wcfCellL)
        )
        sheet.mergeCells(0, offsY, 2, offsY)

    }
}
