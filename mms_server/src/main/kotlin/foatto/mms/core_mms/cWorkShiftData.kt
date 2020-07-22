package foatto.mms.core_mms

import foatto.core_server.app.server.cStandart
import foatto.mms.core_mms.sensor.SensorConfig

class cWorkShiftData : cStandart() {

    override fun addSQLWhere( hsTableRenameList: Set<String> ): String {
        val isWorkData = aliasConfig.alias == "mms_work_shift_work"
        val isLiquidData = aliasConfig.alias == "mms_work_shift_liquid"

        return super.addSQLWhere( hsTableRenameList ) +
            " AND ${renameTableName(hsTableRenameList, model.tableName)}.${(model as mWorkShiftData).columnDataType.getFieldName()} = " +
                 "${if(isWorkData) SensorConfig.SENSOR_WORK else if(isLiquidData) SensorConfig.SENSOR_VOLUME_FLOW else 0} "
    }

    //    public void getTable( AdvancedByteBuffer bbOut, HashMap<String,Object> hmOut ) throws Throwable {
    //        //--- может быть null при вызове из "Модули системы"
    //        Integer objectID = hmParentData.get( "mms_object" );
    ////!!! непонятно накой нужно???
    ////        if( objectID == null ) objectID = 0;
    //        if( objectID != null ) oc = ObjectConfig.getObjectConfig( dataWorker.alStm.get( 0 ), userConfig, objectID );
    //
    //        super.getTable( bbOut, hmOut );
    //    }
    //
    //    public void getForm( AdvancedByteBuffer bbOut, HashMap<String,Object> hmOut ) throws Throwable {
    //        //--- может быть null при вызове из "Модули системы"
    //        Integer objectID = hmParentData.get( "mms_object" );
    ////!!! непонятно накой нужно???
    ////        if( objectID == null ) objectID = 0;
    //        if( objectID != null ) oc = ObjectConfig.getObjectConfig( dataWorker.alStm.get( 0 ), userConfig, objectID );
    //
    //        super.getForm( bbOut, hmOut );
    //    }
    //
    //    protected void fillHeader( String selectorID, boolean withAnchors,
    //                              ArrayList<String> alURL, ArrayList<String> alText, HashMap<String,Object> hmOut ) throws Throwable {
    //        StringBuilder sb = new StringBuilder( aliasConfig.getDescr() ).append( ": " );
    //        if( oc == null ) sb.append( "(нет информации)" );
    //        else {
    //            StringBuilder sbDG = new StringBuilder();
    //            if( ! oc.departmentName.isEmpty() )
    //                sbDG.append( oc.departmentName );
    //            if( ! oc.groupName.isEmpty() )
    //                sbDG.append( sbDG.length() == 0 ? "" : ", " )
    //                    .append( oc.groupName );
    //
    //            sb.append( oc.name );
    //            if( sbDG.length() > 0 )
    //                sb.append( " (" ).append( sbDG ).append( ')' );
    //        }
    //
    //        alURL.add( "" );
    //        alText.add( sb.toString() );
    //    }
}
