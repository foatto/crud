package foatto.mms

import foatto.core.util.AdvancedLogger
import foatto.core_server.service.CoreServiceWorker
import foatto.sql.AdvancedConnection
import kotlin.system.exitProcess

class ClientDBPrepare(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private const val CONFIG_USER_ID = "user_id"

        //----------------------------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "ClientDBPrepare"
                if(args.size == 1) {
                    ClientDBPrepare(args[0]).run()
                    exitCode = 1
                }
                else println("Usage: $serviceWorkerName <ini-file-name>")
            }
            catch(t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }
    }

    //--- список оставляемых пользователей
    private var userIdList = ""

    override val isRunOnce: Boolean
        get() = true

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun loadConfig() {
        super.loadConfig()

        userIdList = hmConfig[CONFIG_USER_ID]!!
    }

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
            alStm.add(conn.createStatement())
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun cycle() {

        val alUserIDDest = userIdList.split( ' ', ',', ';' ).filter { it.isNotEmpty() }.map { it.toInt() }.toMutableList()

        //--- расширяем список оставляемых пользователей (на случай, если там заданы id подразделений/групп пользователей)
        // alUserIDDest.forEach { - даёт ConcurrentModifException, т.к. дополняется на ходу, только через индексы
        for( i in 0 until alUserIDDest.size ) {
            val rs = alStm[0].executeQuery( " SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = ${alUserIDDest[ i ]} " )
            while(rs.next()) alUserIDDest.add(rs.getInt(1))
            rs.close()
        }

        var sUserIDList = ""
        alUserIDDest.distinct().forEach {
            if( sUserIDList.isNotEmpty() ) sUserIDList += " , "
            sUserIDList += it.toString()
        }

        //--- составляем список ID удаляемых объектов

        val alObjectID = mutableListOf<Int>()
        val rs = alStm[0].executeQuery( " SELECT id FROM MMS_object WHERE id <> 0 AND user_id NOT IN ( $sUserIDList ) ")
        while(rs.next()) alObjectID.add(rs.getInt(1))
        rs.close()

        AdvancedLogger.info("load object_id")

        //--- удаляем data-таблицы согласно списка

        for(objectID in alObjectID) {
            alStm[0].executeUpdate(" DROP TABLE MMS_data_$objectID ")
            alConn[0].commit()
        }

        AdvancedLogger.info("MMS_data_XXX")

        //--- чистим объекты

        alStm[0].executeUpdate( " DELETE FROM MMS_object WHERE id <> 0 AND user_id NOT IN ( $sUserIDList ) " )
        alStm[0].executeUpdate(" REINDEX TABLE MMS_object ")
        alConn[0].commit()

        AdvancedLogger.info("MMS_object")

        //--- чистим зоны

        alStm[0].executeUpdate( " DELETE FROM MMS_zone WHERE id <> 0 AND user_id NOT IN ( $sUserIDList ) " )
        alStm[0].executeUpdate(" REINDEX TABLE MMS_zone ")
        alConn[0].commit()

        AdvancedLogger.info("MMS_zone")

        //--- чистим зависимости

        val alDI = mutableListOf<DependInfo>()
        //--- справочники для MMS_object
        alDI.add(DependInfo("MMS_department", "id", "MMS_object", "department_id"))
        alDI.add(DependInfo("MMS_group", "id", "MMS_object", "group_id"))
        //--- зависимости первого порядка от MMS_object
        alDI.add(DependInfo("MMS_sensor", "object_id", "MMS_object", "id"))
        alDI.add(DependInfo("MMS_day_work", "object_id", "MMS_object", "id"))
        alDI.add(DependInfo("MMS_work_shift", "object_id", "MMS_object", "id"))
        alDI.add(DependInfo("MMS_device", "object_id", "MMS_object", "id"))
        alDI.add(DependInfo("MMS_device_command_history", "object_id", "MMS_object", "id"))
        alDI.add(DependInfo("VC_camera", "object_id", "MMS_object", "id"))
        //--- зависимости от MMS_sensor
        alDI.add(DependInfo("MMS_sensor_calibration", "sensor_id", "MMS_sensor", "id"))
        alDI.add(DependInfo("MMS_equip_service_shedule", "equip_id", "MMS_sensor", "id"))
        alDI.add(DependInfo("MMS_equip_service_history", "equip_id", "MMS_sensor", "id"))
        //--- справочники для MMS_work_shift
        alDI.add(DependInfo("MMS_worker", "id", "MMS_work_shift", "worker_id"))
        //--- зависимости от MMS_work_shift
        alDI.add(DependInfo("MMS_work_shift_data", "shift_id", "MMS_work_shift", "id"))
        //--- зависимости от MMS_zone
        alDI.add(DependInfo("MMS_user_zone", "zone_id", "MMS_zone", "id"))
        alDI.add(DependInfo("MMS_object_zone", "zone_id", "MMS_zone", "id"))
        alDI.add(DependInfo("XY_element", "object_id", "MMS_zone", "id"))
        //--- зависимости от MMS_device
        alDI.add(DependInfo("MMS_device_command_history", "device_id", "MMS_device", "id"))

        for(di in alDI) {
            alStm[0].executeUpdate( " DELETE FROM ${di.destTable} WHERE id <> 0 AND ${di.destField} <> 0 AND ${di.destField} NOT IN ( SELECT ${di.sourField} FROM ${di.sourTable} ) " )
            alStm[0].executeUpdate(StringBuilder(" REINDEX TABLE ").append(di.destTable))
            alConn[0].commit()
            AdvancedLogger.info(di.destTable)
        }

        //--- отдельные зависимости в таблицах, не имеющих id-поля
        val alDI2 = mutableListOf<DependInfo>()
        //--- зависимости от XY_element
        alDI2.add(DependInfo("XY_point", "element_id", "XY_element", "id"))
        alDI2.add(DependInfo("XY_property", "element_id", "XY_element", "id"))
        for(di in alDI2) {
            alStm[0].executeUpdate( " DELETE FROM ${di.destTable} WHERE ${di.destField} <> 0 AND ${di.destField} NOT IN ( SELECT ${di.sourField} FROM ${di.sourTable} ) " )
            alStm[0].executeUpdate(StringBuilder(" REINDEX TABLE ").append(di.destTable))
            alConn[0].commit()
            AdvancedLogger.info(di.destTable)
        }

        //--- очистка независимых таблиц

        alStm[0].executeUpdate(" DELETE FROM MMS_service_order ")
        alStm[0].executeUpdate(" REINDEX TABLE MMS_service_order ")
        alConn[0].commit()
        AdvancedLogger.info("MMS_service_order")

        //--- дополнительные зачистки

        alStm[0].executeUpdate(" DELETE FROM MMS_device WHERE object_id = 0 ")
        alStm[0].executeUpdate(" REINDEX TABLE MMS_device ")
        alConn[0].commit()

    }

    private class DependInfo(val destTable: String, val destField: String, val sourTable: String, val sourField: String)
}
