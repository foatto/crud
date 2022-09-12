package foatto.mms.core_mms

import foatto.core_server.app.server.cStandart
import foatto.mms.core_mms.sensor.config.SensorConfig

class cWorkShiftData : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val isWorkData = aliasConfig.name == "mms_work_shift_work"
        val isLiquidData = aliasConfig.name == "mms_work_shift_liquid"

        return super.addSQLWhere(hsTableRenameList) +
            " AND ${renameTableName(hsTableRenameList, model.modelTableName)}.${(model as mWorkShiftData).columnDataType.getFieldName()} = " +
            "${if (isWorkData) SensorConfig.SENSOR_WORK else if (isLiquidData) SensorConfig.SENSOR_LIQUID_USING else 0} "
    }

}
