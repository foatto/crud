package foatto.mms.core_mms.report

import foatto.core.link.FormPinMode
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstractReport
import java.time.LocalDate
import java.time.LocalTime

open class mP : mAbstractReport() {

    lateinit var columnReportBegDate: ColumnDate3Int
        protected set
    lateinit var columnReportBegTime: ColumnTime3Int
        protected set
    lateinit var columnReportEndDate: ColumnDate3Int
        protected set
    lateinit var columnReportEndTime: ColumnTime3Int
        protected set

    protected fun initReportPeriod(arrDT: Array<Int>?) {
        columnReportBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Дата начала периода")
        if (arrDT != null) columnReportBegDate.default = LocalDate.of(arrDT[0], arrDT[1], arrDT[2])
        columnReportBegDate.isVirtual = true
        columnReportBegTime = ColumnTime3Int(modelTableName, "beg_ho", "beg_mi", null, "Время начала периода")
        if (arrDT != null) columnReportBegTime.default = LocalTime.of(arrDT[3], arrDT[4], arrDT[5])
        columnReportBegTime.isVirtual = true
        columnReportBegTime.formPinMode = FormPinMode.ON
        columnReportEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", "Дата окончания периода")
        if (arrDT != null) columnReportEndDate.default = LocalDate.of(arrDT[6], arrDT[7], arrDT[8])
        columnReportEndDate.isVirtual = true
        columnReportEndTime = ColumnTime3Int(modelTableName, "end_ho", "end_mi", null, "Время окончания периода")
        if (arrDT != null) columnReportEndTime.default = LocalTime.of(arrDT[9], arrDT[10], arrDT[11])
        columnReportEndTime.isVirtual = true
        columnReportEndTime.formPinMode = FormPinMode.ON
    }

    protected fun addReportPeriodFormColumns() {
        alFormColumn.add(columnReportBegDate)
        alFormColumn.add(columnReportBegTime)
        alFormColumn.add(columnReportEndDate)
        alFormColumn.add(columnReportEndTime)
    }
}
