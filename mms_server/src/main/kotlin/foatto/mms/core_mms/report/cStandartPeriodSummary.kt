package foatto.mms.core_mms.report

import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.iMMSApplication

abstract class cStandartPeriodSummary : cAbstractPeriodSummary() {

    protected lateinit var alObjectID: MutableList<Int>
    protected lateinit var alObjectConfig: MutableList<ObjectConfig>

    override fun getReport(): String {
        //--- предварительно определим наличие гео-датчика -
        //--- от него зависят кол-во и формат выводимых данных и отчёта
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        alObjectID = mutableListOf()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        alObjectConfig = mutableListOf()
        for (objectID in alObjectID) alObjectConfig.add((application as iMMSApplication).getObjectConfig(userConfig, objectID))

        for(oc in alObjectConfig) {
            if(oc.scg != null) {
                isGlobalUseSpeed = isGlobalUseSpeed or oc.scg!!.isUseSpeed
                isGlobalUseRun = isGlobalUseRun or oc.scg!!.isUseRun
            }
            val hmSCLL = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]
            if (hmSCLL != null && hmSCLL.isNotEmpty()) {
                for (portNum in hmSCLL.keys) {
                    val sca = hmSCLL[portNum] as SensorConfigLiquidLevel
                    isGlobalUsingCalc = isGlobalUsingCalc or sca.isUsingCalc
                }
            }
        }
        return super.getReport()
    }

}
