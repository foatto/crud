package foatto.ts.core_ts.device

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData
import foatto.core_server.ds.AbstractTelematicHandler
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigSetup

class cDevice : cStandart() {

    companion object {
        val CLEANING_DEPTH_SHOW_POS = 0
        val DRIVE_LOAD_RESTRICT_SHOW_POS = 3
        val PARKING_DEPTH_SHOW_POS = 4
    }

    private var oldobjectId = 0

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val md = model as mDevice
        if (column == md.columnSerialNo) {
            tci.foreColorType = TableCellForeColorType.DEFINED

            val lastSessionTime = (hmColumnData[md.columnDeviceLastSessionTime] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()
            //--- раскраска номера контроллера в зависимости от времени последнего входа в систему
            val curTime = getCurrentTimeInt()

            if (lastSessionTime == 0) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            }
            else if (curTime - lastSessionTime > 7 * 24 * 60 * 60) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
            }
            else if (curTime - lastSessionTime > 1 * 24 * 60 * 60) {
                tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
            }
            else {
                tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
            }
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        if (id != 0) {
            val rs = stm.executeQuery(" SELECT object_id FROM TS_device WHERE id = $id ")
            if (rs.next()) {
                oldobjectId = rs.getInt(1)
            }
            rs.close()
        }

        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val newobjectId = (hmColumnData[(model as mDevice).columnObject] as DataInt).intValue
        val deviceIndex = (hmColumnData[(model as mDevice).columnDeviceIndex] as DataInt).intValue
        if (newobjectId != 0) {
            createSensors(newobjectId, deviceIndex)
        }

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        val newobjectId = (hmColumnData[(model as mDevice).columnObject] as DataInt).intValue
        val deviceIndex = (hmColumnData[(model as mDevice).columnDeviceIndex] as DataInt).intValue
        createSensors(newobjectId, deviceIndex)

        return postURL
    }

    //--- temporary for MVP
    private fun createSensors(objectId: Int, deviceIndex: Int) {
        val rs = stm.executeQuery(" SELECT COUNT( id ) FROM TS_sensor WHERE object_id = $objectId ")
        val isDefitionsExists = rs.next() && rs.getInt(1) > 0
        rs.close()

        if(!isDefitionsExists) {
            //--- state sensor
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Код текущего состояния установки УДС' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 0} , ${SensorConfig.SENSOR_STATE}
                    )
                """
            )
            //--- analogue sensors
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Глубина [м]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 1} , ${SensorConfig.SENSOR_DEPTH} ,
                        -1 , 1000000 , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        0, 2000, 0, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Скорость спуска [м/ч]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 2} , ${SensorConfig.SENSOR_SPEED} ,
                        -1 , 1000 , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        0, 1000, 0, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Нагрузка на привод [%]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 3} , ${SensorConfig.SENSOR_LOAD} ,
                        -1 , 151 , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        0, 150, 0, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Дата/время следующей чистки' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 4} , ${SensorConfig.SENSOR_NEXT_CLEAN_DATETIME} ,
                        -1 , ${Int.MAX_VALUE} , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        0, ${Int.MAX_VALUE}, 0, 0
                    )
                """
            )
            //--- setup/config sensors
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Глубина очистки [м]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 5} , ${SensorConfig.SENSOR_SETUP} ,
                        $CLEANING_DEPTH_SHOW_POS , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Период очистки [час]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 6} , ${SensorConfig.SENSOR_SETUP} ,
                        1 , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Скорость очистки [м/ч]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 7} , ${SensorConfig.SENSOR_SETUP} ,
                        2 , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Уровень сигнала [%]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 8} , ${SensorConfig.SENSOR_SIGNAL_LEVEL} ,
                        -1 , 101 , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        0, 100, 0, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Ограничение нагрузки на привод [%]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 10} , ${SensorConfig.SENSOR_SETUP} ,
                        $DRIVE_LOAD_RESTRICT_SHOW_POS , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Глубина парковки скребка [м]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 12} , ${SensorConfig.SENSOR_SETUP} ,
                        $PARKING_DEPTH_SHOW_POS , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Количество попыток прохода препятствия' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 13} , ${SensorConfig.SENSOR_SETUP} ,
                        6 , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Запуск ЭЦН' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 14} , ${SensorConfig.SENSOR_SETUP} ,
                        7 , ${SensorConfigSetup.VALUE_TYPE_BOOLEAN}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Пауза между проходами препятствия [сек]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 15} , ${SensorConfig.SENSOR_SETUP} ,
                        5 , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Текущая температура внутри станции УДС (реле №1) [˚С]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 16} , ${SensorConfig.SENSOR_TEMPERATURE_IN} ,
                        -200 , 1000 , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        -100, 200, 0, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        ignore_min_sensor , ignore_max_sensor , smooth_method , smooth_time ,
                        analog_min_view , analog_max_view , analog_min_limit , analog_max_limit                                          
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Текущая температура снаружи (реле №2) [˚С]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 17} , ${SensorConfig.SENSOR_TEMPERATURE_OUT} ,
                        -200 , 1000 , ${SensorConfig.SMOOTH_METOD_MEDIAN} , 0 ,
                        -100, 200, 0, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Уровень температуры внутри [˚С]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 18} , ${SensorConfig.SENSOR_SETUP} ,
                        8 , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )
            stm.executeUpdate(
                """
                    INSERT INTO TS_sensor( 
                        id , object_id , name , group_name , descr , port_num , sensor_type ,
                        show_pos , value_type , prec                
                    ) VALUES ( 
                        ${stm.getNextID("TS_sensor", "id")} , $objectId ,  '' , '' , 'Уровень температуры снаружи [˚С]' , 
                        ${deviceIndex * AbstractTelematicHandler.MAX_PORT_PER_DEVICE + 19} , ${SensorConfig.SENSOR_SETUP} ,
                        9 , ${SensorConfigSetup.VALUE_TYPE_NUMBER}, 0
                    )
                """
            )

        }
    }

}

