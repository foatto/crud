package foatto.mms.core_mms.report

import foatto.mms.core_mms.ObjectConfig
import foatto.mms.iMMSApplication

abstract class cStandartPeriodSummary : cAbstractPeriodSummary() {

    protected lateinit var alobjectId: MutableList<Int>
    protected lateinit var alObjectConfig: MutableList<ObjectConfig>

    override fun getReport(): String {
        //--- предварительно определим наличие гео-датчика -
        //--- от него зависят кол-во и формат выводимых данных и отчёта
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        alobjectId = mutableListOf()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if (reportObject == 0) {
            loadObjectList(conn, userConfig, reportObjectUser, reportDepartment, reportGroup, alobjectId)
        } else {
            alobjectId.add(reportObject)
        }

        alObjectConfig = mutableListOf()
        for (objectId in alobjectId) {
            alObjectConfig.add((application as iMMSApplication).getObjectConfig(userConfig, objectId))
        }

        alObjectConfig.forEach { oc ->
            defineGlobalFlags(oc)
        }
        return super.getReport()
    }

}
