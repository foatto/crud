package foatto.mms.core_mms.report

import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSplittedDouble
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

abstract class cAbstractPeriodSummary : cMMSReport() {

    protected var isCompactReport = false

    protected var isGlobalUseSpeed = false
    protected var isGlobalUseRun = false
    protected var isGlobalUsingCalc = false

    override fun getReport(): String {
        //--- предварительно определим вид отчёта
        isCompactReport = hmReportParam["report_is_compact"] as Boolean

        return super.getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4

        if(isCompactReport) {
            printPageOrientation = PageOrientation.PORTRAIT

            printMarginLeft = 20
            printMarginRight = 10
            printMarginTop = 10
            printMarginBottom = 10
        }
        else {
            printPageOrientation = if(isGlobalUseSpeed) PageOrientation.LANDSCAPE else PageOrientation.PORTRAIT

            printMarginLeft = if(isGlobalUseSpeed) 10 else 20
            printMarginRight = 10
            printMarginTop = if(isGlobalUseSpeed) 20 else 10
            printMarginBottom = 10
        }
        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    protected fun defineSummaryReportHeaders(sheet: WritableSheet, aOffsY: Int, columnCaption: String): Int {
        var offsY = aOffsY
        val alDim = ArrayList<Int>()
        if(isCompactReport) {
            offsY = Math.max(offsY, outReportCap(sheet, 5, 0) + 1)
            offsY++

            //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
            //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
            alDim.add(5)    // "N п/п"
            alDim.add(24)    // Объект/Дата
            alDim.add(7)    // Пробег [км]
            alDim.add(20)    // Оборудование
            alDim.add(7)    // Время работы [час]
            alDim.add(20)    // Топливо
            alDim.add(7)    // Расход [л]
        }
        else {
            offsY = Math.max(offsY, outReportCap(sheet, if(isGlobalUseSpeed) 8 else if(isGlobalUsingCalc) 5 else 4, 0) + 1)
            offsY++

            //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
            //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
            alDim.add(5)    // "N п/п"
            alDim.add(if(isGlobalUseSpeed) 54 else if(isGlobalUsingCalc) 31 else 40)    // Наименование
            //--- далее в зависимости от опций и наличия гео-датчика данные могут выводиться
            //--- в от 5 до 9 столбцов с одинаковой шириной
            for(i in 0 until if(isGlobalUseSpeed) 9 else if(isGlobalUsingCalc) 6 else 5) alDim.add(9)
        }
        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        //--- после установки ширин столбцов выводим их заголовки
        if(isCompactReport) {
            //--- вывод заголовка
            sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
            sheet.addCell(Label(1, offsY, columnCaption, wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Пробег [км]", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Оборудование", wcfCaptionHC))
            sheet.addCell(Label(4, offsY, "Время работы [час]", wcfCaptionHC))
            sheet.addCell(Label(5, offsY, "Топливо", wcfCaptionHC))
            sheet.addCell(Label(6, offsY, "Расход [л]", wcfCaptionHC))
            offsY++
        }
        return offsY
    }

    protected fun addGroupTitle(sheet: WritableSheet, aOffsY: Int, title: String): Int {
        var offsY = aOffsY
        if(isCompactReport) {
            offsY++    // отодвинуться от предыдущей строки
            sheet.addCell(Label(1, offsY, title, wcfCellCBStdYellow))
            sheet.mergeCells(1, offsY, 6, offsY)
            offsY += 2 // +1 пустая строка снизу
        }
        else {
            sheet.addCell(Label(1, offsY, title, wcfCellCB))
            sheet.mergeCells(1, offsY, if(isGlobalUseSpeed) 10 else if(isGlobalUsingCalc) 7 else 6, offsY + 2)
            offsY += 4 // +1 пустая строка снизу
        }
        return offsY
    }

    protected open fun outRow(
        sheet: WritableSheet, aOffsY: Int, objectConfig: ObjectConfig, objectCalc: ObjectCalc
    ): Int {
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
                    for(llcd in objectCalc.tmLiquidLevelCalc.values) {
                        //--- если в группе у этого уровнемера есть гео-датчик, и нет оборудования и генераторов/электросчётчиков,
                        //--- то считаем средний расход в движении по нему
                        if(llcd.gcd != null && llcd.tmWorkCalc.isEmpty() && llcd.tmEnergoCalc.isEmpty()) sumUsingTotal = (sumUsingTotal ?: 0.0) + llcd.usingTotal
                    }
                    sbAvgUsingTotal.append(
                        if(sumUsingTotal == null || sumUsingTotal < 0 || objectCalc.gcd!!.run == 0.0) '-'
                        else getSplittedDouble(100.0 * sumUsingTotal / objectCalc.gcd!!.run, 1)
                    )
                }
                //--- при наличии движения и пробега посчитаем средний расход в движении
                if(objectConfig.scg!!.isUseSpeed && objectConfig.scg!!.isUseRun) {
                    //--- нужен именно объект, чтобы поймать null
                    var sumUsingMoving: Double? = null
                    for(llcd in objectCalc.tmLiquidLevelCalc.values) {
                        //--- если в группе у этого уровнемера есть гео-датчик, и нет оборудования и генераторов/электросчётчиков,
                        //--- то считаем средний расход в движении по нему
                        if(llcd.gcd != null && llcd.tmWorkCalc.isEmpty() && llcd.tmEnergoCalc.isEmpty()) sumUsingMoving = (sumUsingMoving ?: 0.0) + llcd.usingMoving
                    }
                    sbAvgUsingMoving.append(
                        if(sumUsingMoving == null || sumUsingMoving < 0 || objectCalc.gcd!!.run == 0.0) '-'
                        else getSplittedDouble(100.0 * sumUsingMoving / objectCalc.gcd!!.run, 1)
                    )
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
                if(objectConfig.scg!!.isUseRun) {
                    sheet.addCell(Label(offsX, offsY, "Общий сред. расх. [л/100км]", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                    offsX++
                }
                if(objectConfig.scg!!.isUseSpeed && objectConfig.scg!!.isUseRun) {
                    sheet.addCell(Label(offsX, offsY, "Сред. расх. в движ. [л/100км]", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                    offsX++
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
                if(objectConfig.scg!!.isUseRun) sheet.addCell(Label(offsX++, offsY, sbAvgUsingTotal.toString(), wcfCellC))
                if(objectConfig.scg!!.isUseSpeed && objectConfig.scg!!.isUseRun) sheet.addCell(Label(offsX++, offsY, sbAvgUsingMoving.toString(), wcfCellC))
                offsY += 2
            }

            //--- отчёт по датчикам оборудования
            if(!objectCalc.tmWorkCalc.isEmpty()) {
                //--- дополним стандартный расчёт средним расходом
                val tmAvgUsingTotal = TreeMap<String, String>()
                val tmAvgUsingMoving = TreeMap<String, String>()
                val tmAvgUsingParking = TreeMap<String, String>()
                for((workDescr,wcd) in objectCalc.tmWorkCalc) {
                    var sumUsingTotal: Double? = null
                    var sumUsingMoving: Double? = null
                    var sumUsingParking: Double? = null
                    for(llcd in objectCalc.tmLiquidLevelCalc.values) {
                        //--- если в группе у этого уровнемера есть датчик работы оборудования, он один и "этот самый",
                        //--- и при этом нет геодатчика и электросчётчика (т.е. генераторов), то считаем средний расход
                        if(llcd.tmWorkCalc.size == 1 && llcd.tmWorkCalc[workDescr] != null && llcd.gcd == null && llcd.tmEnergoCalc.isEmpty()) {

                            sumUsingTotal = (sumUsingTotal ?: 0.0) + llcd.usingTotal
                            if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                                sumUsingMoving = (sumUsingMoving ?: 0.0) + llcd.usingMoving
                                sumUsingParking = (sumUsingParking ?: 0.0) + llcd.usingParking
                            }
                        }
                    }
                    tmAvgUsingTotal[workDescr] = if(wcd.onTime == 0 || sumUsingTotal == null || sumUsingTotal < 0) "-"
                    else getSplittedDouble(sumUsingTotal / (wcd.onTime.toDouble() / 60.0 / 60.0), 1).toString()
                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        tmAvgUsingMoving[workDescr] = if(wcd.onMovingTime == 0 || sumUsingMoving == null || sumUsingMoving < 0) "-"
                        else getSplittedDouble(sumUsingMoving / (wcd.onMovingTime.toDouble() / 60.0 / 60.0), 1).toString()
                        tmAvgUsingParking[workDescr] = if(wcd.onParkingTime == 0 || sumUsingParking == null || sumUsingParking < 0) "-"
                        else getSplittedDouble(sumUsingParking / (wcd.onParkingTime.toDouble() / 60.0 / 60.0), 1).toString()
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
                    sheet.addCell(Label(5, offsY, "Средний расход [л/час]", wcfCaptionHC))
                    sheet.mergeCells(5, offsY, 7, offsY)
                    sheet.addCell(Label(5, offsY + 1, "общее", wcfCaptionHC))
                    sheet.addCell(Label(6, offsY + 1, "в движении", wcfCaptionHC))
                    sheet.addCell(Label(7, offsY + 1, "на стоянках", wcfCaptionHC))
                    offsY += 2
                }
                else {
                    sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
                    sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
                    sheet.addCell(Label(3, offsY, "Средний расход [л/час]", wcfCaptionHC))
                    offsY++
                }
                for((workDescr,wcd) in objectCalc.tmWorkCalc) {

                    sheet.addCell(Label(1, offsY, workDescr, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        sheet.addCell(Label(3, offsY, getSplittedDouble(wcd.onMovingTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                        sheet.addCell(Label(4, offsY, getSplittedDouble(wcd.onParkingTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                    }
                    sheet.addCell(Label(if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) 5 else 3, offsY, tmAvgUsingTotal[workDescr], wcfCellC))
                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        sheet.addCell(Label(6, offsY, tmAvgUsingMoving[workDescr], wcfCellC))
                        sheet.addCell(Label(7, offsY, tmAvgUsingParking[workDescr], wcfCellC))
                    }
                    offsY++
                }
                offsY++
            }

            //--- отчёт по датчикам уровня жидкости
            if(!objectCalc.tmLiquidLevelCalc.isEmpty()) {
                //--- используется ли вообще usingCalc
                var isUsingCalc = false
                for(llcd in objectCalc.tmLiquidLevelCalc.values) {
                    if(llcd.usingCalc != null) {
                        isUsingCalc = true
                        break
                    }
                }

                if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                    sheet.addCell(Label(1, offsY, "Наименование ёмкости", wcfCaptionHC))
                    sheet.mergeCells(1, offsY, 1, offsY + 1)
                    sheet.addCell(Label(2, offsY, "Остаток на начало периода [л]", wcfCaptionHC))
                    sheet.mergeCells(2, offsY, 2, offsY + 1)
                    sheet.addCell(Label(3, offsY, "Остаток на конец периода [л]", wcfCaptionHC))
                    sheet.mergeCells(3, offsY, 3, offsY + 1)

                    sheet.addCell(Label(4, offsY, "Заправка [л]", wcfCaptionHC))
                    sheet.mergeCells(4, offsY, 4, offsY + 1)

                    sheet.addCell(Label(5, offsY, "Слив [л]", wcfCaptionHC))
                    sheet.mergeCells(5, offsY, 5, offsY + 1)

                    sheet.addCell(Label(6, offsY, "Расход [л]", wcfCaptionHC))
                    sheet.mergeCells(6, offsY, if(objectCalc.sbLiquidLevelUsingCalc.length == 0) 8 else 9, offsY)

                    sheet.addCell(Label(6, offsY + 1, "общий", wcfCaptionHC))
                    sheet.addCell(Label(7, offsY + 1, "в движении", wcfCaptionHC))
                    sheet.addCell(Label(8, offsY + 1, "на стоянках", wcfCaptionHC))
                    if(isUsingCalc) sheet.addCell(Label(9, offsY + 1, "расчётный", wcfCaptionHC))
                    offsY += 2
                }
                else {
                    sheet.addCell(Label(1, offsY, "Наименование ёмкости", wcfCaptionHC))
                    sheet.addCell(Label(2, offsY, "Остаток на начало периода [л]", wcfCaptionHC))
                    sheet.addCell(Label(3, offsY, "Остаток на конец периода [л]", wcfCaptionHC))
                    sheet.addCell(Label(4, offsY, "Заправка [л]", wcfCaptionHC))
                    sheet.addCell(Label(5, offsY, "Слив [л]", wcfCaptionHC))
                    sheet.addCell(Label(6, offsY, "Расход [л]", wcfCaptionHC))
                    if(isUsingCalc) sheet.addCell(Label(7, offsY, "В т.ч. расчётный расход [л]", wcfCaptionHC))
                    offsY++
                }
                for((liquidName,llcd) in objectCalc.tmLiquidLevelCalc) {

                    sheet.addCell(Label(1, offsY, liquidName, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(llcd.begLevel, ObjectCalc.getPrecision(llcd.begLevel)).toString(), wcfCellC))
                    sheet.addCell(Label(3, offsY, getSplittedDouble(llcd.endLevel, ObjectCalc.getPrecision(llcd.endLevel)).toString(), wcfCellC))
                    sheet.addCell(Label(4, offsY, getSplittedDouble(llcd.incTotal, ObjectCalc.getPrecision(llcd.incTotal)).toString(), wcfCellC))
                    sheet.addCell(Label(5, offsY, getSplittedDouble(llcd.decTotal, ObjectCalc.getPrecision(llcd.decTotal)).toString(), wcfCellC))
                    sheet.addCell(Label(6, offsY, getSplittedDouble(llcd.usingTotal, ObjectCalc.getPrecision(llcd.usingTotal)).toString(), wcfCellC))

                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        sheet.addCell(Label(7, offsY, getSplittedDouble(llcd.usingMoving, ObjectCalc.getPrecision(llcd.usingMoving)).toString(), wcfCellC))
                        sheet.addCell(Label(8, offsY, getSplittedDouble(llcd.usingParking, ObjectCalc.getPrecision(llcd.usingParking)).toString(), wcfCellC))
                        if(isUsingCalc) sheet.addCell(
                            Label(
                                9, offsY, if(llcd.usingCalc == null) "-"
                                else getSplittedDouble(llcd.usingCalc!!, ObjectCalc.getPrecision(llcd.usingCalc!!)).toString(), wcfCellC
                            )
                        )
                    }
                    else {
                        if(isUsingCalc) sheet.addCell(
                            Label(
                                7, offsY, if(llcd.usingCalc == null) "-"
                                else getSplittedDouble(llcd.usingCalc!!, ObjectCalc.getPrecision(llcd.usingCalc!!)).toString(), wcfCellC
                            )
                        )
                    }
                    offsY++
                }
                //                offsY++;

                //--- вывод суммы по каждой суммовой группе
                for((sumGroup,llcdSum) in objectCalc.tmLiquidLevelGroupSum) {

                    sheet.addCell(
                        Label(
                            1, offsY, if(sumGroup.isEmpty()) "ИТОГО:"
                            else StringBuilder("ИТОГО по '").append(sumGroup).append("':").toString(), wcfCellRBStdYellow
                        )
                    )

                    sheet.addCell(
                        Label(
                            2, offsY, getSplittedDouble(
                                llcdSum.begLevel, ObjectCalc.getPrecision(llcdSum.begLevel)
                            ).toString(), wcfCellCBStdYellow
                        )
                    )
                    sheet.addCell(
                        Label(
                            3, offsY, getSplittedDouble(
                                llcdSum.endLevel, ObjectCalc.getPrecision(llcdSum.endLevel)
                            ).toString(), wcfCellCBStdYellow
                        )
                    )

                    sheet.addCell(
                        Label(
                            4, offsY, getSplittedDouble(
                                llcdSum.incTotal, ObjectCalc.getPrecision(llcdSum.incTotal)
                            ).toString(), wcfCellCBStdYellow
                        )
                    )

                    sheet.addCell(
                        Label(
                            5, offsY, getSplittedDouble(
                                llcdSum.decTotal, ObjectCalc.getPrecision(llcdSum.decTotal)
                            ).toString(), wcfCellCBStdYellow
                        )
                    )

                    sheet.addCell(
                        Label(
                            6, offsY, getSplittedDouble(
                                llcdSum.usingTotal, ObjectCalc.getPrecision(llcdSum.usingTotal)
                            ).toString(), wcfCellCBStdYellow
                        )
                    )

                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
                        sheet.addCell(
                            Label(
                                7, offsY, getSplittedDouble(
                                    llcdSum.usingMoving, ObjectCalc.getPrecision(llcdSum.usingMoving)
                                ).toString(), wcfCellCBStdYellow
                            )
                        )
                        sheet.addCell(
                            Label(
                                8, offsY, getSplittedDouble(
                                    llcdSum.usingParking, ObjectCalc.getPrecision(llcdSum.usingParking)
                                ).toString(), wcfCellCBStdYellow
                            )
                        )
                        if(llcdSum.usingCalc != null) sheet.addCell(
                            Label(
                                9, offsY, getSplittedDouble(
                                    llcdSum.usingCalc!!, ObjectCalc.getPrecision(llcdSum.usingCalc!!)
                                ).toString(), wcfCellCBStdYellow
                            )
                        )
                    }
                    else {
                        if(llcdSum.usingCalc != null) sheet.addCell(
                            Label(
                                7, offsY, getSplittedDouble(
                                    llcdSum.usingCalc!!, ObjectCalc.getPrecision(llcdSum.usingCalc!!)
                                ).toString(), wcfCellCBStdYellow
                            )
                        )
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
                for((liquidName,lucd) in objectCalc.tmLiquidUsingCalc) {

                    sheet.addCell(Label(1, offsY, liquidName, wcfCellC))
                    sheet.addCell(
                        Label(
                            2, offsY, getSplittedDouble(lucd.usingTotal, ObjectCalc.getPrecision(lucd.usingTotal)).toString(), wcfCellC
                        )
                    )
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
                    for(llcd in objectCalc.tmLiquidLevelCalc.values) {
                        //--- если в группе у этого уровнемера есть электросчётчик, он один и "этот самый",
                        //--- и при этом нет геодатчика и оборудования, то считаем средний расход
                        if(llcd.tmEnergoCalc.size == 1 && llcd.tmEnergoCalc[energoDescr] != null && llcd.gcd == null && llcd.tmWorkCalc.isEmpty())

                            sumUsing = (sumUsing ?: 0.0) + llcd.usingTotal
                    }
                    //--- выводим в кВт*ч
                    tmEnergoCalcAvgUsing[energoDescr] = if(e == 0 || sumUsing == null || sumUsing < 0) "-"
                    else getSplittedDouble(sumUsing / (e!! / 1000.0), 1).toString()
                }

                sheet.addCell(Label(1, offsY, "Наименование счётчика", wcfCaptionHC))
                sheet.addCell(Label(2, offsY, "Электро энергия [кВт*ч]", wcfCaptionHC))
                sheet.addCell(Label(3, offsY, "Средний расход [л/кВт*ч]", wcfCaptionHC))
                offsY++
                for(energoDescr in objectCalc.tmEnergoCalc.keys) {
                    sheet.addCell(Label(1, offsY, energoDescr, wcfCellC))
                    //--- выводим в кВт*ч
                    sheet.addCell(
                        Label(
                            2, offsY, getSplittedDouble(objectCalc.tmEnergoCalc[energoDescr]!! / 1000.0, 3).toString(), wcfCellC
                        )
                    )
                    sheet.addCell(Label(3, offsY, tmEnergoCalcAvgUsing[energoDescr], wcfCellC))
                    offsY++
                }
                offsY++
            }

            //--- вывод суммы по каждой суммовой группе
            for(sumGroup in objectCalc.tmEnergoGroupSum.keys) {
                val eSum = objectCalc.tmEnergoGroupSum[sumGroup]

                sheet.addCell(
                    Label(
                        1, offsY, if(sumGroup.isEmpty()) "ИТОГО:" else StringBuilder("ИТОГО по '").append(sumGroup).append("':").toString(), wcfCellRBStdYellow
                    )
                )

                sheet.addCell(Label(2, offsY, getSplittedDouble(eSum!! / 1000.0, 3).toString(), wcfCellCBStdYellow))
                offsY++
            }

            offsY++    // еще одна пустая строчка снизу
        }
        return offsY
    }

    protected fun outPeriodSum(sheet: WritableSheet, aOffsY: Int, periodSumCollector: SumCollector): Int {
        var offsY = aOffsY
        val sbLiquidUsingName = StringBuilder()
        val sbLiquidUsingTotal = StringBuilder()
        val sbLiquidUsingInMove = StringBuilder()
        val sbLiquidUsingInParking = StringBuilder()

        val sbEnergoName = StringBuilder()
        val sbEnergoValue = StringBuilder()

        ObjectCalc.fillLiquidUsingString(
            periodSumCollector.sumUser.tmLiquidUsingCalc, sbLiquidUsingName, sbLiquidUsingTotal, sbLiquidUsingInMove, sbLiquidUsingInParking
        )

        ObjectCalc.fillEnergoString(periodSumCollector.sumUser.tmEnergoCalc, sbEnergoName, sbEnergoValue)

        sheet.addCell(Label(1, offsY, "ИТОГО за период:", wcfCellCBStdYellow))
        if(isCompactReport) {
            sheet.addCell(Label(5, offsY, sbLiquidUsingName.toString(), wcfCellCBStdYellow))
            sheet.addCell(Label(6, offsY, sbLiquidUsingTotal.toString(), wcfCellCBStdYellow))
            offsY++
            //--- в компактном режиме электроэнергия не выводится
        }
        else {
            offsY++
            for((liquidDescr,lucd) in periodSumCollector.sumUser.tmLiquidUsingCalc) {
                sheet.addCell(Label(1, offsY, liquidDescr, wcfCellCBStdYellow))
                sheet.addCell(Label(2, offsY, getSplittedDouble(lucd.usingTotal, ObjectCalc.getPrecision(lucd.usingTotal)).toString(), wcfCellCBStdYellow))
                if(isGlobalUseSpeed) {
                    sheet.addCell(Label(3, offsY, getSplittedDouble(lucd.usingMoving, ObjectCalc.getPrecision(lucd.usingMoving)).toString(), wcfCellC))
                    sheet.addCell(Label(4, offsY, getSplittedDouble(lucd.usingParking, ObjectCalc.getPrecision(lucd.usingParking)).toString(), wcfCellC))
                }
                offsY++
            }
            offsY++
            for(energoDescr in periodSumCollector.sumUser.tmEnergoCalc.keys) {
                sheet.addCell(Label(1, offsY, energoDescr, wcfCellCBStdYellow))
                sheet.addCell(Label(2, offsY, getSplittedDouble(periodSumCollector.sumUser.tmEnergoCalc[energoDescr]!! / 1000.0, 3).toString(), wcfCellCBStdYellow))
                offsY++
            }
            offsY += 2
        }
        return offsY
    }

    protected fun outSumData(sheet: WritableSheet, aOffsY: Int, sumData: SumData, isBold: Boolean): Int {
        var offsY = aOffsY
        val sbGeoName = StringBuilder()
        val sbGeoRun = StringBuilder()
        val sbGeoMovingTime = StringBuilder()
        val sbGeoParkingTime = StringBuilder()
        val sbGeoParkingCount = StringBuilder()

        val sbWorkName = StringBuilder()
        val sbWorkTotal = StringBuilder()
        val sbWorkMoving = StringBuilder()
        val sbWorkParking = StringBuilder()

        val sbLiquidUsingName = StringBuilder()
        val sbLiquidUsingTotal = StringBuilder()
        val sbLiquidUsingInMove = StringBuilder()
        val sbLiquidUsingInParking = StringBuilder()

        val sbEnergoName = StringBuilder()
        val sbEnergoValue = StringBuilder()

        //--- сумма пробегов, времени и моточасов имеет смысл только в разрезе конкретной единицы оборудования
        if(!isBold) {
            ObjectCalc.fillGeoString(
                sumData.gcd, zoneId, sbGeoName, sbGeoRun, StringBuilder(), StringBuilder(), StringBuilder(), sbGeoMovingTime, sbGeoParkingTime, sbGeoParkingCount
            )
            ObjectCalc.fillWorkString(sumData.tmWorkCalc, sbWorkName, sbWorkTotal, sbWorkMoving, sbWorkParking)
        }
        ObjectCalc.fillLiquidUsingString(sumData.tmLiquidUsingCalc, sbLiquidUsingName, sbLiquidUsingTotal, sbLiquidUsingInMove, sbLiquidUsingInParking)
        ObjectCalc.fillEnergoString(sumData.tmEnergoCalc, sbEnergoName, sbEnergoValue)

        if(isCompactReport) {
            if(!isBold) {
                sheet.addCell(Label(2, offsY, sbGeoRun.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(3, offsY, sbWorkName.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(4, offsY, sbWorkTotal.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
            }
            sheet.addCell(Label(5, offsY, sbLiquidUsingName.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
            sheet.addCell(Label(6, offsY, sbLiquidUsingTotal.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
            offsY++
        }
        else {
            //-- отчёт по гео-датчику - определяется по наличию данных о пробеге,
            //-- т.к. наименование датчика могут забыть ввести
            //-- (sbGeoRun.length() > 0 тоже не показатель, т.к. всегда равен какому-нибудь числу)
            if((isGlobalUseSpeed || isGlobalUseRun) && sumData.gcd.descr != null && !isBold) {
                var offsX = 1
                sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                offsX++
                if(isGlobalUseRun) {
                    sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                    offsX++
                }
                if(isGlobalUseSpeed) {
                    sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX + 1, offsY)
                    sheet.addCell(Label(offsX, offsY + 1, "в движении", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 1, offsY + 1, "на стоянках", wcfCaptionHC))
                    sheet.addCell(Label(offsX + 2, offsY, "Кол-во стоянок", wcfCaptionHC))
                    sheet.mergeCells(offsX + 2, offsY, offsX + 2, offsY + 1)
                }
                offsY += 2

                offsX = 1
                sheet.addCell(Label(offsX++, offsY, sbGeoName.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                if(isGlobalUseRun) sheet.addCell(Label(offsX++, offsY, sbGeoRun.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                if(isGlobalUseSpeed) {
                    sheet.addCell(Label(offsX++, offsY, sbGeoMovingTime.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, sbGeoParkingTime.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, sbGeoParkingCount.toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                }
                offsY++
            }

            //--- отчёт по датчикам оборудования  - определяется по наличию данных о моточасах,
            //--- т.к. наименование датчика могут забыть ввести
            if(sbWorkTotal.isNotBlank()) {
                if(isGlobalUseSpeed) {
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
                for((workDescr,wcd) in sumData.tmWorkCalc) {
                    sheet.addCell(Label(1, offsY, workDescr, if(isBold) wcfCellCBStdYellow else wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    if(isGlobalUseSpeed) {
                        sheet.addCell(Label(3, offsY, getSplittedDouble(wcd.onMovingTime.toDouble() / 60.0 / 60.0, 1).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                        sheet.addCell(Label(4, offsY, getSplittedDouble(wcd.onParkingTime.toDouble() / 60.0 / 60.0, 1).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    }
                    offsY++
                }
                offsY++
            }

            //--- отчёт по совместному расходу жидкости - определяется по наличию данных об общем расходе,
            //--- т.к. наименование жидкости могут забыть ввести
            if(sbLiquidUsingTotal.length > 0) {
                if(isGlobalUseSpeed) {
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
                for((liquidDescr,lucd) in sumData.tmLiquidUsingCalc) {
                    sheet.addCell(Label(1, offsY, liquidDescr, if(isBold) wcfCellCBStdYellow else wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(lucd.usingTotal, ObjectCalc.getPrecision(lucd.usingTotal)).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    if(isGlobalUseSpeed) {
                        sheet.addCell(Label(3, offsY, getSplittedDouble(lucd.usingMoving, ObjectCalc.getPrecision(lucd.usingTotal)).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                        sheet.addCell(Label(4, offsY, getSplittedDouble(lucd.usingParking, ObjectCalc.getPrecision(lucd.usingTotal)).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    }
                    offsY++
                }
                offsY++
            }

            //--- отчёт по электросчётчикам  - определяется по наличию данных об электроэнергии,
            //--- т.к. наименование датчика могут забыть ввести
            if(sbEnergoValue.isNotBlank()) {
                sheet.addCell(Label(1, offsY, "Электросчётчик", wcfCaptionHC))
                sheet.addCell(Label(2, offsY, "Электроэнергия [кВт*час]", wcfCaptionHC))
                offsY++
                for((energoDescr,energoData) in sumData.tmEnergoCalc) {
                    sheet.addCell(Label(1, offsY, energoDescr, if(isBold) wcfCellCBStdYellow else wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(energoData / 1000.0, 3).toString(), if(isBold) wcfCellCBStdYellow else wcfCellC))
                    offsY++
                }
                offsY++
            }

            offsY++    // еще одна пустая строчка снизу
        }
        return offsY
    }

    protected fun outReportTrail(sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        if(isCompactReport) {
            offsY++
            sheet.addCell(Label(5, offsY, getPreparedAt(), wcfCellL))
            sheet.mergeCells(5, offsY, 6, offsY)

            outReportSignature(sheet, intArrayOf(0, 3, 5), offsY + 3)
        }
        else {
            sheet.addCell(Label(if(isGlobalUseSpeed) 8 else if(isGlobalUsingCalc) 5 else 4, offsY,getPreparedAt(), wcfCellL))
            sheet.mergeCells(
                if(isGlobalUseSpeed) 8 else if(isGlobalUsingCalc) 5 else 4, offsY, if(isGlobalUseSpeed) 10 else if(isGlobalUsingCalc) 7 else 6, offsY
            )

            outReportSignature(
                sheet, if(isGlobalUseSpeed) intArrayOf(0, 3, 6)
                else intArrayOf(0, 2, 4), offsY + 3
            )
        }
        return offsY
    }

}
