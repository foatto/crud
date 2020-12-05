package foatto.mms.core_mms

import foatto.core_server.app.server.cStandart
import foatto.mms.core_mms.sensor.config.SensorConfig

class cWorkShiftData : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val isWorkData = aliasConfig.alias == "mms_work_shift_work"
        val isLiquidData = aliasConfig.alias == "mms_work_shift_liquid"

        return super.addSQLWhere(hsTableRenameList) +
            " AND ${renameTableName(hsTableRenameList, model.tableName)}.${(model as mWorkShiftData).columnDataType.getFieldName()} = " +
            "${if (isWorkData) SensorConfig.SENSOR_WORK else if (isLiquidData) SensorConfig.SENSOR_VOLUME_FLOW else 0} "
    }

}
