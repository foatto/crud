package foatto.mms.core_mms.device

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.core_server.ds.AbstractTelematicHandler
import foatto.mms.core_mms.ds.GalileoHandler
import foatto.mms.core_mms.sensor.config.SensorConfig

class cDevice : cStandart() {

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val md = model as mDevice
        if (column == md.columnDevice) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            val lastSessionTime = (hmColumnData[md.columnDeviceLastSessionTime] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()
            //--- раскраска номера контроллера в зависимости от времени последнего входа в систему
            val curTime = getCurrentTimeInt()

            tci.foreColor = if (lastSessionTime == 0) {
                TABLE_CELL_FORE_COLOR_DISABLED
            } else if (curTime - lastSessionTime > 7 * 24 * 60 * 60) {
                TABLE_CELL_FORE_COLOR_CRITICAL
            } else if (curTime - lastSessionTime > 1 * 24 * 60 * 60) {
                TABLE_CELL_FORE_COLOR_WARNING
            } else {
                TABLE_CELL_FORE_COLOR_NORMAL
            }
        }
    }

    //--- при создании записи id = controller_id, задаваемый вручную
    override fun getNextID(hmColumnData: Map<iColumn, iData>): Int = (hmColumnData[(model as mDevice).columnDevice] as DataInt).intValue

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val m = model as mDevice

        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue
        if (objectId != 0) {
            val deviceIndex = (hmColumnData[m.columnDeviceIndex] as DataInt).intValue

            val isESDCreate = (hmColumnData[m.columnESDCreatingEnabled] as DataBoolean).value
            if (isESDCreate) {
                val alESCGroupName = m.alColumnESDGroupName.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alESCDescrPrefix = m.alColumnESDDescrPrefix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alESCDescrPostfix = m.alColumnESDDescrPostfix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                createESDSensors(objectId, deviceIndex, alESCGroupName, alESCDescrPrefix, alESCDescrPostfix)
            }
        }

//        clearOldCameraInfo(hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        val m = model as mDevice

        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue
        if (objectId != 0) {
            val deviceIndex = (hmColumnData[m.columnDeviceIndex] as DataInt).intValue

            val isESDCreate = (hmColumnData[m.columnESDCreatingEnabled] as DataBoolean).value
            if (isESDCreate) {
                val alESCGroupName = m.alColumnESDGroupName.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alESCDescrPrefix = m.alColumnESDDescrPrefix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                val alESCDescrPostfix = m.alColumnESDDescrPostfix.map { column ->
                    (hmColumnData[column] as DataString).text
                }
                createESDSensors(objectId, deviceIndex, alESCGroupName, alESCDescrPrefix, alESCDescrPostfix)
            }
        }

//        clearOldCameraInfo(hmColumnData)

        return postURL
    }

//    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
//        super.postDelete(id, hmColumnData)
//
//        clearOldCameraInfo(hmColumnData)
//    }

    //----------------------------------------------------------------------------------------------------------------------------------------

//    private fun clearOldCameraInfo(hmColumnData: Map<iColumn, iData>) {
//        val md = model as mDevice
//
//        val deviceID = (hmColumnData[md.columnDevice] as DataInt).intValue
//        val objectId = (hmColumnData[md.columnObject] as DataInt).intValue
//
//        stm.executeUpdate(" DELETE FROM VC_camera WHERE name = '$deviceID' AND object_id <> $objectId ")
//    }

    private fun createESDSensors(
        objectId: Int,
        deviceIndex: Int,
        alEuroSensGroupName: List<String>,
        alEuroSensDescrPrefix: List<String>,
        alEuroSensDescrPostfix: List<String>,
    ) {
        for (si in 0 until mDevice.MAX_PORT_PER_SENSOR) {
            val groupName = alEuroSensGroupName[si].trim()
            val descrPrefix = alEuroSensDescrPrefix[si].trim()
            val descrPostfix = alEuroSensDescrPostfix[si].trim()

            if (descrPrefix.isNotEmpty() || descrPostfix.isNotEmpty()) {
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da 
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Состояние расходомера $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_STATUS + si} , 
                            ${SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE} , 0 , 0 , 2000 , 1 , 1 
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , is_absolute_count , liquid_name            
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Расходомер $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_VOLUME + si} , 
                            ${SensorConfig.SENSOR_LIQUID_USING} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 , 1 , ''                            
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                            analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Скорость потока $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_FLOW + si} , 
                            ${SensorConfig.SENSOR_VOLUME_FLOW} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 ,                             
                            0 , 0 , 0 , 0                              
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , is_absolute_count , liquid_name            
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Расходомер камеры подачи $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_CAMERA_VOLUME + si} , 
                            ${SensorConfig.SENSOR_LIQUID_USING} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 , 1 , ''                            
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                            analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Скорость потока камеры подачи $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_CAMERA_FLOW + si} , 
                            ${SensorConfig.SENSOR_VOLUME_FLOW} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 ,                             
                            0 , 0 , 0 , 0                              
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                            analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Температура камеры подачи $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_CAMERA_TEMPERATURE + si} , 
                            ${SensorConfig.SENSOR_TEMPERATURE} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 ,                             
                            0 , 0 , 0 , 0                              
                        )
                    """
                )

                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , is_absolute_count , liquid_name            
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Расходомер камеры обратки $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_REVERSE_CAMERA_VOLUME + si} , 
                            ${SensorConfig.SENSOR_LIQUID_USING} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 , 1 , ''                            
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                            analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Скорость потока камеры обратки $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_REVERSE_CAMERA_FLOW + si} , 
                            ${SensorConfig.SENSOR_VOLUME_FLOW} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 ,                             
                            0 , 0 , 0 , 0                              
                        )
                    """
                )
                stm.executeUpdate(
                    """
                        INSERT INTO MMS_sensor( 
                            id , object_id , name , group_name , descr , 
                            port_num , 
                            sensor_type , cmd_on_id , cmd_off_id , beg_ye , beg_mo , beg_da ,
                            smooth_method , smooth_time , ignore_min_sensor , ignore_max_sensor , 
                            analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                
                        ) VALUES ( 
                            ${stm.getNextID("MMS_sensor", "id")} , $objectId ,  '' , '$groupName' , '$descrPrefix Температура камеры обратки $descrPostfix' , 
                            ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + GalileoHandler.PORT_NUM_ESD_REVERSE_CAMERA_TEMPERATURE + si} , 
                            ${SensorConfig.SENSOR_TEMPERATURE} , 0, 0 , 2000 , 1 , 1 ,
                            0 , 0 , 0 , 0 ,                             
                            0 , 0 , 0 , 0                              
                        )
                    """
                )
            }
        }

/*
-- дискретные датчики (времени работы оборудования и сигналов)
    bound_value         INT,    -- граничное значение (<= bound_value - логический 0, выше - логический 1
    active_value        INT,    -- активное/рабочее значение
    min_on_time         INT,    -- минимальное (учитываемое) время работы
    min_off_time        INT,    -- минимальное (учитываемое) время простоя
    beg_work_value      FLOAT8, -- наработка на момент установки датчика
    cmd_on_id           INT,    -- команда на включение
    cmd_off_id          INT,    -- команда на отключение
    signal_on           VARCHAR( 250 ), -- сигналы, разрешающие включение
    signal_off          VARCHAR( 250 ), -- сигналы, разрешающие отключение
    -- применяется только для показаний электросчётчиков
    energo_phase        INT NOT NULL DEFAULT(0),
    -- применяется только для уровнемеров
    container_type              INT NOT NULL DEFAULT(1),    -- тип ёмкости
    analog_using_min_len        INT,    -- минимальная продолжительность расхода
    analog_is_using_calc        INT,    -- использовать ли расчётный расход топлива за период заправок/сливов
    analog_detect_inc           FLOAT8,  -- скорость увеличения уровня (топлива) для детектора заправки
    analog_detect_inc_min_diff  FLOAT8,  -- минимально учитываемый объём заправки
    analog_detect_inc_min_len   INT,    -- минимально учитываемая продолжительность заправки
    analog_inc_add_time_before  INT,    -- добавить время до заправки
    analog_inc_add_time_after   INT,    -- добавить время после заправки
    analog_detect_dec           FLOAT8,  -- скорость уменьшения уровня (топлива) для детектора слива
    analog_detect_dec_min_diff  FLOAT8,  -- минимально учитываемый объём слива
    analog_detect_dec_min_len   INT,    -- минимально учитываемая продолжительность слива
    analog_dec_add_time_before  INT,    -- добавить время до заправки
    analog_dec_add_time_after   INT,    -- добавить время после заправки
-- общее для гео и дискретных датчиков
    liquid_norm         FLOAT8,  -- норматив расхода рабочей жидкости/топлива

 */

    }

}
