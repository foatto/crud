package foatto.mms.core_mms

import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyPolygon
import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.xy.server.document.sdcXyMap
import foatto.mms.core_mms.xy.server.document.sdcMMSMap
import java.nio.ByteOrder

class ZoneData( val id: Int, val userID: Int, val name: String, val descr: String, val outerID: String ) {

    //--- как набор координат и для определения isContain
    var polygon: XyPolygon? = null
    //--- режим read-only для зон
    //--- ( заполняется при выдаче списка зон для конкретного пользователя )
    var hmUserRO = mutableMapOf<Int, Boolean>()

    companion object {
        fun getZoneData(stm: CoreAdvancedStatement, userConfig: UserConfig, zoneID: Int ): Map<Int, ZoneData> {
            val hmAllZoneData = reloadZoneData( stm )

            val hsZonePermission = userConfig.userPermission[ "mms_zone" ]
            val hmZoneData = mutableMapOf<Int, ZoneData>()

            if( hsZonePermission != null ) {
                if( zoneID == 0 ) {
                    for( ( curZoneID, zd ) in hmAllZoneData ) {
                        if( cStandart.checkPerm( userConfig, hsZonePermission, cStandart.PERM_TABLE, zd.userID ) ) {
                            zd.hmUserRO[ userConfig.userId ] = !cStandart.checkPerm( userConfig, hsZonePermission, cStandart.PERM_EDIT, zd.userID )
                            hmZoneData[ curZoneID ] = zd
                        }
                    }
                }
                else {
                    val zd = hmAllZoneData[ zoneID ]!!
                    if( cStandart.checkPerm( userConfig, hsZonePermission, cStandart.PERM_TABLE, zd.userID ) ) {
                        zd.hmUserRO[ userConfig.userId ] = !cStandart.checkPerm( userConfig, hsZonePermission, cStandart.PERM_EDIT, zd.userID )
                        hmZoneData[ zoneID ] = zd
                    }
                }
            }
            return hmZoneData
        }

        private fun reloadZoneData( stm: CoreAdvancedStatement): Map<Int, ZoneData> {
            val hmAllZoneData = mutableMapOf<Int, ZoneData>()

            //--- сначала загружаем в черновой список
            //--- ( т.к. зоны без точек в рабочий список попадать не должны, а определится это попозже )
            val hmZoneDataDraft = mutableMapOf<Int, ZoneData>()
            var rs = stm.executeQuery( " SELECT id , user_id , name , descr , outer_id FROM MMS_zone WHERE id <> 0 " )
            while( rs.next() ) {
                val id = rs.getInt( 1 )
                val userID = rs.getInt( 2 )
                val name = rs.getString( 3 )
                val descr = rs.getString( 4 )
                val outerID = rs.getString( 5 )

                hmZoneDataDraft[id] = ZoneData( id, userID, name, descr, outerID )
            }
            rs.close()

            //--- загрузим точки по зонам
            for( zoneObjectID in hmZoneDataDraft.keys ) {
                rs = stm.executeQuery( " SELECT id , prj_x1 , prj_y1 , point_data FROM XY_element WHERE type_name = '${sdcMMSMap.ELEMENT_TYPE_ZONE}' AND object_id = $zoneObjectID " )
                rs.next()
                val zoneElementID = rs.getInt( 1 )
                val prjX = rs.getInt( 2 )
                val prjY = rs.getInt( 3 )
                val bbPoint = rs.getByteBuffer( 4, ByteOrder.BIG_ENDIAN )
                rs.close()

                val polygon = XyPolygon()
                //--- ( аналог см. в XyCoreElement.load )
                while( bbPoint.hasRemaining() ) polygon.alPoint.add(XyPoint(prjX + bbPoint.getInt(), prjY + bbPoint.getInt()))
                //--- если кол-во точек равно максимальной вместимости поля point_data,
                //--- то возможно что в XY_point лежат ещё точки
                if( polygon.alPoint.size >= stm.dialect.binaryFieldMaxSize / sdcXyMap.POINT_SIZE_IN_BIN ) {
                    rs = stm.executeQuery( " SELECT prj_x , prj_y FROM XY_point WHERE element_id = $zoneElementID ORDER BY sort_id " )
                    while( rs.next() ) polygon.alPoint.add(XyPoint(prjX + rs.getInt(1), prjY + rs.getInt(2)))
                    rs.close()
                }
                //--- в чистовой список заносим только зоны с ненулевым кол-вом точек
                if( polygon.alPoint.size > 0 ) {
                    val zd = hmZoneDataDraft[ zoneObjectID ]!!
                    zd.polygon = polygon
                    hmAllZoneData[ zoneObjectID ] = zd
                }
            }

            return hmAllZoneData
        }
    }
}
