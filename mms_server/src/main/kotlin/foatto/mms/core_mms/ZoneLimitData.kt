package foatto.mms.core_mms

import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

class ZoneLimitData {

    //    public static final int TYPE_LIMIT_ENGINE_ON_BLOCKED    = 26;    // вкл.  двигателя в зоне запрещено
    //    public static final int TYPE_LIMIT_ENGINE_ON_ONLY       = 27;    // вкл.  двигателя вне зоны запрещено
    //    public static final int TYPE_LIMIT_ENGINE_OFF_BLOCKED   = 28;    // выкл. двигателя в зоне запрещено
    //    public static final int TYPE_LIMIT_ENGINE_OFF_ONLY      = 29;    // выкл. двигателя вне зоны запрещено
    //    public static final int TYPE_LIMIT_EQUIP_ON_BLOCKED     = 30;    // вкл.  верх.оборудования в зоне запрещено
    //    public static final int TYPE_LIMIT_EQUIP_ON_ONLY        = 31;    // вкл.  верх.оборудования вне зоны запрещено
    //    public static final int TYPE_LIMIT_EQUIP_OFF_BLOCKED    = 32;    // выкл. верх.оборудования в зоне запрещено
    //    public static final int TYPE_LIMIT_EQUIP_OFF_ONLY       = 33;    // выкл. верх.оборудования вне зоны запрещено
    //    public static final int TYPE_LIMIT_DOOR_OPEN_BLOCKED    = 34;    // открывание дверей в зоне запрещено
    //    public static final int TYPE_LIMIT_DOOR_OPEN_ONLY       = 35;    // открывание дверей вне зоны запрещено
    //    public static final int TYPE_LIMIT_DOOR_CLOSE_BLOCKED   = 36;    // закрывание дверей в зоне запрещено
    //    public static final int TYPE_LIMIT_DOOR_CLOSE_ONLY      = 37;    // закрывание дверей вне зоны запрещено
    //    public static final int TYPE_LIMIT_SEAT_ON_BLOCKED      = 38;    // посадка пассажиров в зоне запрещено
    //    public static final int TYPE_LIMIT_SEAT_ON_ONLY         = 39;    // посадка пассажиров вне зоны запрещено
    //    public static final int TYPE_LIMIT_SEAT_OFF_BLOCKED     = 40;    // высадка пассажиров в зоне запрещено
    //    public static final int TYPE_LIMIT_SEAT_OFF_ONLY        = 41;    // высадка пассажиров вне зоны запрещено
    //    public static final int TYPE_LIMIT_POWER_ON_BLOCKED     = 42;    // включение зажигания в зоне запрещено
    //    public static final int TYPE_LIMIT_POWER_ON_ONLY        = 43;    // включение зажигания вне зоны запрещено
    //    public static final int TYPE_LIMIT_POWER_OFF_BLOCKED    = 44;    // выключение зажигания в зоне запрещено
    //    public static final int TYPE_LIMIT_POWER_OFF_ONLY       = 45;    // выключение зажигания вне зоны запрещено

    //    public static final TreeMap<Integer,int[][]> tmLimitCode = new TreeMap<Integer,int[][]>();
    //    static {
    //        tmLimitCode.put( AutoConfig.SENSOR_ENGINE, new int[][] { { TYPE_LIMIT_ENGINE_ON_BLOCKED, TYPE_LIMIT_ENGINE_ON_ONLY },
    //                                                              { TYPE_LIMIT_ENGINE_OFF_BLOCKED, TYPE_LIMIT_ENGINE_OFF_ONLY } } );
    //        tmLimitCode.put( AutoConfig.SENSOR_EQUIP,  new int[][] { { TYPE_LIMIT_EQUIP_ON_BLOCKED, TYPE_LIMIT_EQUIP_ON_ONLY },
    //                                                              { TYPE_LIMIT_EQUIP_OFF_BLOCKED, TYPE_LIMIT_EQUIP_OFF_ONLY } } );
    //        tmLimitCode.put( AutoConfig.SENSOR_DOOR,   new int[][] { { TYPE_LIMIT_DOOR_OPEN_BLOCKED, TYPE_LIMIT_DOOR_OPEN_ONLY },
    //                                                              { TYPE_LIMIT_DOOR_CLOSE_BLOCKED, TYPE_LIMIT_DOOR_CLOSE_ONLY } } );
    //        tmLimitCode.put( AutoConfig.SENSOR_SEAT,   new int[][] { { TYPE_LIMIT_SEAT_ON_BLOCKED, TYPE_LIMIT_SEAT_ON_ONLY },
    //                                                              { TYPE_LIMIT_SEAT_OFF_BLOCKED, TYPE_LIMIT_SEAT_OFF_ONLY } } );
    //        tmLimitCode.put( AutoConfig.SENSOR_POWER,  new int[][] { { TYPE_LIMIT_POWER_ON_BLOCKED, TYPE_LIMIT_POWER_ON_ONLY },
    //                                                              { TYPE_LIMIT_POWER_OFF_BLOCKED, TYPE_LIMIT_POWER_OFF_ONLY } } );
    //    }

    //--- данные по зоне
    var zoneData: ZoneData? = null

    //    //--- набор ограничений времени действия
    //    public boolean isPeriodical = false;
    //    public ArrayList<Long> alBeg = new ArrayList<>();
    //    public ArrayList<Long> alEnd = new ArrayList<>();

    //--- ограничения по скорости внутри зоны
    var maxSpeed = 0

    companion object {

        //--- статусы зональных ограничений и связанных с ними нарушениями
        const val TYPE_LIMIT_SPEED = 1    // ограничение по скорости + сохраненные данные по текущему превышению
        const val TYPE_LIMIT_AREA_BLOCKED = 2    // нахождение в зоне запрещено
        const val TYPE_LIMIT_AREA_ONLY = 3    // нахождение вне зоны запрещено
        const val TYPE_LIMIT_PARKING_BLOCKED = 4    // стоянка в зоне запрещена
        const val TYPE_LIMIT_PARKING_ONLY = 5    // стоянка вне зоны запрещена

        //----------------------------------------------------------------------------------------------------------------------

        //    public static String getZoneLimitDescr( int sensorType, int i, int j ) {
        //        SensorDInfo si = SensorDInfo.tmSensorDInfo.get( sensorType );
        //        return new StringBuilder( si.descr ).append( ": " )
        //                         .append( i == 0 ? si.descrOn : si.descrOff ).append( ' ' )
        //                         .append( j == 0 ? " в зоне" : " вне зоны" ).toString();
        //    }
        //
        //    public static void fillZoneLimitComboBox(  ColumnComboBox columnZoneType ) {
        //        for( Integer sensorType : SensorDInfo.tmSensorDInfo.keySet() ) {
        //            for( int i = 0; i < 2; i++ )
        //                for( int j = 0; j < 2; j++ )
        //                    columnZoneType.addChoice( ZoneLimitData.tmLimitCode.get( sensorType )[ i ][ j ],
        //                                              new StringBuilder( getZoneLimitDescr( sensorType, i , j ) ).append( " запрещено" ).toString() );
        //
        //        }
        //    }

        fun getZoneLimit(
            stm: CoreAdvancedStatement,
            userConfig: UserConfig,
            objectConfig: ObjectConfig,
            hmZoneData: Map<Int, ZoneData>,
            zoneType: Int
        ): Map<Int, List<ZoneLimitData>> {

            val hmZoneLimit = mutableMapOf<Int, MutableList<ZoneLimitData>>()

            val hsZonePermission = userConfig.userPermission["mms_zone"]!!
            val hsUserZonePermission = userConfig.userPermission["mms_user_zone"]!!
            val hsObjectZonePermission = userConfig.userPermission["mms_object_zone"]!!
            //HashSet hsWaybillPermission = userConfig.getUserPermission().get( "ft_auto_waybill" );

            //--- взять зоны, привязанные к пользователю
            //                     " PLA_user_zone.is_timed , " )
            //            .append( " PLA_user_zone.beg_ye , PLA_user_zone.beg_mo , PLA_user_zone.beg_da , " )
            //            .append( " PLA_user_zone.end_ye , PLA_user_zone.end_mo , PLA_user_zone.end_da , " )
            //            .append( " PLA_user_zone.beg_ho , PLA_user_zone.beg_mi , PLA_user_zone.end_ho , PLA_user_zone.end_mi , " )
            //            .append( " PLA_user_zone.is_periodical " )
            var sbSQL = " SELECT MMS_user_zone.user_id , MMS_zone.id , MMS_user_zone.zone_type , MMS_user_zone.max_speed " +
                        " FROM MMS_user_zone, MMS_zone " +
                        " WHERE MMS_user_zone.zone_id = MMS_zone.id "
            if( zoneType != 0 ) sbSQL += " AND MMS_user_zone.zone_type = $zoneType "
            var rs = stm.executeQuery( sbSQL )
            while( rs.next() ) {
                val userZoneUserID = rs.getInt( 1 )
                val zoneID = rs.getInt( 2 )

                val zoneData = hmZoneData [zoneID ] ?: continue

                if( cStandart.checkPerm( userConfig, hsUserZonePermission, cStandart.PERM_TABLE, userZoneUserID ) &&
                    cStandart.checkPerm( userConfig, hsZonePermission, cStandart.PERM_TABLE, zoneData.userID ) )

                    loadOneZoneData( zoneData, rs, hmZoneLimit/*, begTime, endTime*/ )
            }
            rs.close()

            //--- взять зоны, привязанные к объекту
            //                     " PLA_auto_zone.is_timed , " )
            //            .append( " PLA_auto_zone.beg_ye , PLA_auto_zone.beg_mo , PLA_auto_zone.beg_da , " )
            //            .append( " PLA_auto_zone.end_ye , PLA_auto_zone.end_mo , PLA_auto_zone.end_da , " )
            //            .append( " PLA_auto_zone.beg_ho , PLA_auto_zone.beg_mi , PLA_auto_zone.end_ho , PLA_auto_zone.end_mi , " )
            //            .append( " PLA_auto_zone.is_periodical " )
            // первое нулевое "поле" - для совместимости с загрузкой user_zone
            sbSQL = " SELECT 0, MMS_zone.id , MMS_object_zone.zone_type , MMS_object_zone.max_speed " +
                    " FROM MMS_object_zone , MMS_zone " +
                " WHERE MMS_object_zone.zone_id = MMS_zone.id AND MMS_object_zone.object_id = ${objectConfig.objectId} "
            if( zoneType != 0 ) sbSQL += " AND MMS_object_zone.zone_type = $zoneType "
            rs = stm.executeQuery( sbSQL )
            while( rs.next() ) {
                val zoneID = rs.getInt( 2 )

                val zoneData = hmZoneData[ zoneID ] ?: continue

                if (cStandart.checkPerm(userConfig, hsObjectZonePermission, cStandart.PERM_TABLE, objectConfig.userId) &&
                    cStandart.checkPerm(userConfig, hsZonePermission, cStandart.PERM_TABLE, zoneData.userID)
                )

                    loadOneZoneData(zoneData, rs, hmZoneLimit/*, begTime, endTime*/)
            }
            rs.close()

            //        if( zoneType == 0 || zoneType == TYPE_LIMIT_AREA_ONLY ) {
            //            //--- взять зоны по путевым листам
            //            ArrayList<Integer> alWaybillID = new ArrayList<Integer>();
            //            ArrayList<Integer> alWaybillRouteID = new ArrayList<Integer>();
            //            ArrayList<Long> alWaybillBeg = new ArrayList<Long>();
            //            sbSQL = new StringBuilder( " SELECT user_id , id , route_id , beg_dt FROM PLA_auto_waybill " );
            //            if( aWaybillID == 0 )
            //                sbSQL.append( " WHERE auto_id = " ).append( autoID )
            //                     .append( " AND beg_dt < '" ).append( StringFunction.DateTime_YMDHMS( endTime ) ).append( "' " )
            //                     .append( " AND end_dt > '" ).append( StringFunction.DateTime_YMDHMS( begTime ) ).append( "' " );
            //            else sbSQL.append( " WHERE id = " ).append( aWaybillID );
            //            rs = stm.executeQuery( sbSQL.toString() );
            //            while( rs.next() ) {
            //                if( ! cStandart.checkPerm( aUserConfig, hsWaybillPermission, cStandart.PERM_TABLE, rs.getInt( 1 ) ) ) continue;
            //                alWaybillID.add( rs.getInt( 2 ) );
            //                alWaybillRouteID.add( rs.getInt( 3 ) );
            //                alWaybillBeg.add( rs.getTimestamp( 4 ).getTime() );
            //            }
            //            rs.close();
            //
            //            for( int i = 0; i < alWaybillID.size(); i++ ) {
            //                int waybillID = alWaybillID.get( i );
            //                int waybillRouteID = alWaybillRouteID.get( i );
            //                long waybillBeg = alWaybillBeg.get( i );
            //
            //                long lastRouteTime = 0;
            //
            //                ArrayList<Integer> alRouteID = new ArrayList<Integer>();
            //                if( waybillRouteID != 0 ) alRouteID.add( waybillRouteID );
            //                String sqlStr = new StringBuilder( " SELECT route_id " )
            //                    .append( " FROM PLA_waybill_route " )
            //                    .append( " WHERE waybill_id = " ).append( waybillID )
            //                    .append( " ORDER BY pos " ).toString();
            //                rs = stm.executeQuery( sqlStr );
            //                while( rs.next() ) alRouteID.add( rs.getInt( 1 ) );
            //                rs.close();
            //
            //                for( Integer routeID : alRouteID )
            //                    lastRouteTime = loadOneRouteZoneData( stm, aUserConfig, hmZoneData,
            //                                                          waybillBeg, lastRouteTime, routeID, hmZoneLimit );
            //            }
            //        }

            return hmZoneLimit
        }

        private fun loadOneZoneData(aZoneData: ZoneData, rs: CoreAdvancedResultSet, hmZoneLimit: MutableMap<Int, MutableList<ZoneLimitData>> /*long begTime, long endTime*/) {
            //--- если графическое представление зоны отсутствует, то не загружаем
            if( aZoneData.polygon == null ) return

            val zld = ZoneLimitData()
            zld.zoneData = aZoneData
            val zoneType = rs.getInt( 3 )
            //        //--- задано ли временнОе ограничение по зоне
            //        if( rs.getInt( 4 ) == 1 ) {
            //            int begYear = rs.getInt( 5 );
            //            int begMonth = rs.getInt( 6 );
            //            int begDay = rs.getInt( 7 );
            //            int endYear = rs.getInt( 8 );
            //            int endMonth = rs.getInt( 9 );
            //            int endDay = rs.getInt( 10 );
            //            int begHour = rs.getInt( 11 );
            //            int begMin  = rs.getInt( 12 );
            //            int endHour = rs.getInt( 13 );
            //            int endMin  = rs.getInt( 14 );
            //
            //            //--- временной диапазон периодический ?
            //            if( rs.getInt( 15 ) == 1 ) {
            //                zld.isPeriodical = true;
            //
            //                GregorianCalendar gc1 = new GregorianCalendar( begYear, begMonth - 1, begDay, begHour, begMin, 0 );
            //                GregorianCalendar gc2 = new GregorianCalendar( begYear, begMonth - 1, begDay, endHour, endMin, 0 );
            //                GregorianCalendar lastDay = new GregorianCalendar( endYear, endMonth - 1, endDay, endHour, endMin, 0 );
            //                do {
            //                    zld.alBeg.add( gc1.getTimeInMillis() );
            //                    zld.alEnd.add( gc2.getTimeInMillis() );
            //
            //                    gc1.add( Calendar.DAY_OF_MONTH, 1 );
            //                    gc2.add( Calendar.DAY_OF_MONTH, 1 );
            //                } while( gc1.before( lastDay ) );
            //            }
            //            else {
            //                zld.alBeg.add( new GregorianCalendar( begYear, begMonth - 1, begDay, begHour, begMin, 0 ).getTimeInMillis() );
            //                zld.alEnd.add( new GregorianCalendar( endYear, endMonth - 1, endDay, endHour, endMin, 0 ).getTimeInMillis() );
            //            }
            //            //--- сразу проверяем зоны с временнЫм ограничением на вхождение в заданный диапазон
            //            boolean isFind = false;
            //            for( int i = 0; i < zld.alBeg.size(); i++ ) {
            //                if( zld.alBeg.get( i ) < endTime && zld.alEnd.get( i ) > begTime ) {
            //                    isFind = true;
            //                    break;
            //                }
            //            }
            //            //--- если не найдено ни одного временного диапазона, пересекающегося с заданным, то в список не добавляем
            //            if( ! isFind ) return;
            //        }
            zld.maxSpeed = rs.getInt( 4 )  // rs.getInt( 16 );

            //--- добавляем информацию по зональному ограничению
            var alZoneLimit: MutableList<ZoneLimitData>? = hmZoneLimit[ zoneType ]
            if( alZoneLimit == null ) {
                alZoneLimit = mutableListOf()
                hmZoneLimit[ zoneType ] = alZoneLimit
            }
            alZoneLimit.add( zld )
        }
    }

    //    public static long loadOneRouteZoneData( Statement stm, UserConfig aUserConfig, HashMap<Integer,ZoneData> hmZoneData,
    //                                             long waybillBeg, long lastRouteTime, int routeID,
    //                                             HashMap<Integer,ArrayList<ZoneLimitData>> hmZoneLimit ) throws Throwable {
    //        HashSet hsZonePermission = aUserConfig.getUserPermission().get( "ft_zone" );
    //
    //        long curRouteTime = 0;
    //
    //        RouteZoneData rzd = RouteZoneData.getRouteZoneData( stm, routeID );
    //        if( rzd != null )
    //            for( int j = 0; j < rzd.alZoneID.size(); j++ ) {
    //                int zoneID = rzd.alZoneID.get( j );
    //
    //                ZoneData zoneData = hmZoneData.get( zoneID );
    //                if( zoneData == null || zoneData.gp == null ) continue;
    //
    //                if( cStandart.checkPerm( aUserConfig, hsZonePermission, cStandart.PERM_TABLE, zoneData.userID ) ) {
    //
    //                    ZoneLimitData zld = new ZoneLimitData();
    //                    zld.zoneData = zoneData;
    //                    zld.isReadOnly = ! cStandart.checkPerm( aUserConfig, hsZonePermission, cStandart.PERM_EDIT, zoneData.userID );
    //                    int zoneType = TYPE_LIMIT_AREA_ONLY;
    //
    //                    long curBeg = waybillBeg + lastRouteTime + rzd.alBeg.get( j );
    //                    long curEnd = waybillBeg + lastRouteTime + rzd.alEnd.get( j );
    //                    zld.alBeg.add( curBeg );
    //                    zld.alEnd.add( curEnd );
    //
    //                    curRouteTime = curEnd;
    //
    //                    //--- добавляем информацию по зональному ограничению
    //                    ArrayList<ZoneLimitData> alZoneLimit = hmZoneLimit.get( zoneType );
    //                    if( alZoneLimit == null ) {
    //                        alZoneLimit = new ArrayList<ZoneLimitData>();
    //                        hmZoneLimit.put( zoneType, alZoneLimit );
    //                    }
    //                    alZoneLimit.add( zld );
    //                }
    //            }
    //        return curRouteTime;
    //    }
}
