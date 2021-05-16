package foatto.core_server.app.xy.server.document

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.link.XyDocumentConfig
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getZoneId
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectParsedData
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

abstract class sdcXyAbstract {

    companion object {

        val BITMAP = "bitmap"
        val ICON = "icon"
        val MARKER = "marker"
        val POLY = "poly"
        val TEXT = "text"
        val TRACE = "trace"
    }

    protected lateinit var application: iApplication
    protected lateinit var conn: CoreAdvancedConnection
    protected lateinit var stm: CoreAdvancedStatement
    protected lateinit var chmSession: ConcurrentHashMap<String, Any>
    protected lateinit var userConfig: UserConfig
    protected lateinit var documentConfig: XyDocumentConfig

    protected lateinit var zoneId: ZoneId

    //--- таблица преобразований бизнес-алиасов в типы графических элементов
    protected val hmInAliasElementType = mutableMapOf<String, Array<String>>()

    //--- таблица преобразований типов графических элементов в бизнес-алиасы
    protected val hmOutElementTypeAlias = mutableMapOf<String, String>() //??? пока никак не используется...

    open fun init(
        aApplication: iApplication,
        aConn: CoreAdvancedConnection,
        aStm: CoreAdvancedStatement,
        aChmSession: ConcurrentHashMap<String, Any>,
        aUserConfig: UserConfig,
        aDocumentConfig: XyDocumentConfig
    ) {

        application = aApplication
        conn = aConn
        stm = aStm
        chmSession = aChmSession
        //--- получить конфигурацию по подключенному пользователю
        userConfig = aUserConfig
        //--- получить конфигурацию
        documentConfig = aDocumentConfig

        zoneId = getZoneId(userConfig.getUserProperty(UP_TIME_OFFSET)?.toIntOrNull())
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    abstract fun getCoords(startParamID: String): XyActionResponse

    abstract fun getElements(xyActionRequest: XyActionRequest): XyActionResponse

    abstract fun getOneElement(xyActionRequest: XyActionRequest): XyActionResponse

    abstract fun clickElement(xyActionRequest: XyActionRequest): XyActionResponse

    abstract fun addElement(xyActionRequest: XyActionRequest, userID: Int): XyActionResponse

    abstract fun editElementPoint(xyActionRequest: XyActionRequest): XyActionResponse

    abstract fun moveElements(xyActionRequest: XyActionRequest): XyActionResponse

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- разбор стартовых параметров
    protected fun parseObjectParam(isStartObjectOnly: Boolean, sd: XyStartData, hsReadOnlyObject: MutableSet<Int>): List<XyStartObjectParsedData> {

        val alObjectParamData = mutableListOf<XyStartObjectParsedData>()

        for(sod in sd.alStartObjectData) {
            //--- обрабатываем только параметры с заданным префиксом
            if(isStartObjectOnly && !sod.isStart) continue

            //--- собираем список объектов "только для чтения/просмотра"
            if(sod.isReadOnly) hsReadOnlyObject.add(sod.objectID)

            //--- разбор параметров
            val objectParamData = XyStartObjectParsedData(sod.objectID)

            //--- список типов элементов для данного бизнес-объекта
            //--- (их может быть несколько, например, траектория а/м -
            //--- это сама траектория, стоянки, превышения и проч.)
            hmInAliasElementType[sod.typeName]?.let {
                objectParamData.hsType.addAll(it)
            }

            //--- пройдемся по временному интервалу
            if(sod.isTimed) {
                if(sd.rangeType == -1) {
                    objectParamData.begTime = sd.begTime
                    objectParamData.endTime = sd.endTime
                } else {
                    objectParamData.endTime = getCurrentTimeInt()
                    objectParamData.begTime = objectParamData.endTime - sd.rangeType
                }
            }
            alObjectParamData.add(objectParamData)
        }
        return alObjectParamData
    }

    protected abstract fun loadDynamicElements(scale: Int, objectParamData: XyStartObjectParsedData, alElement: MutableList<XyElement>)

}

//    protected static final String SELF_LINK_TABLE_PREFIX = "SELF_LINK_";
//
//    protected byte[] doCopyElement( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable {
//        return doAddElement( alConn, hmParam );
//    }
//
//    protected byte[] doRotateElement( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable {
//        int[] arrElementKey = cemXyElement.parseElementKey( (String) hmParam.get( XyParameter.ROTATE_ELEMENT_KEY ) );
//        int objectID = arrElementKey[ 0 ];
//        int elementID = arrElementKey[ 1 ];
//        Double rotateDegree = (Double) hmParam.get( XyParameter.ROTATE_DEGREE );
//
//        int[] arrX = (int[]) hmParam.get( XyParameter.ROTATE_X_POINTS );
//        int[] arrY = (int[]) hmParam.get( XyParameter.ROTATE_Y_POINTS );
//
//        semXyElement.putPoints( alConn, 0, objectID, elementID, arrScale, arrX, arrY );
//
//        Statement stm = DBFunction.createStatement( alConn[ objectID != 0 ? 0 : 1 ] );
//        if( rotateDegree != null ) {
//            if( DBFunction.executeUpdate( stm, new StringBuilder( " UPDATE XY_property " )
//                                   .append( " SET v_double = " ).append( rotateDegree )
//                                   .append( " WHERE element_id = " ).append( elementID )
//                                   .append( " AND name = '" ).append( XyProperty.ROTATE_DEGREE ).append( "' " ).toString(), objectID != 0 ) == 0 ) {
//
//                DBFunction.executeUpdate( stm, new StringBuilder( " INSERT INTO XY_property " )
//                    .append( " ( element_id , name , v_double ) VALUES ( " ).append( elementID ).append( " , '" )
//                    .append( XyProperty.ROTATE_DEGREE ).append( "' , " ).append( rotateDegree ).append( " ) " ).toString(), objectID != 0 );
//            }
//        }
//        //--- удаление устаревших тайл-файлов топографии
//        if( objectID == 0 ) {
//            HashSet<Integer> hsID = new HashSet<Integer>();
//            hsID.add( elementID );
//            deleteTiles( stm, hsID );
//        }
//        stm.close();
//        return null;
//    }
//
//    protected byte[] doEditElementText( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable {
//        int[] arrElementKey = cemXyElement.parseElementKey( (String) hmParam.get( XyParameter.EDIT_ELEMENT_KEY ) );
//        int objectID = arrElementKey[ 0 ];
//        int elementID = arrElementKey[ 1 ];
//
//        String textName = null;
//        String textValue = null;
//
//        if( hmParam.get( XyParameter.EDIT_TOOL_TIP ) != null ) {
//            textName = XyProperty.TOOL_TIP_TEXT;
//            textValue = (String) hmParam.get( XyParameter.EDIT_TOOL_TIP );
//        }
//        else if( hmParam.get( XyParameter.EDIT_TEXT ) != null ) {
//            textName = XyProperty.TEXT;
//            textValue = (String) hmParam.get( XyParameter.EDIT_TEXT );
//        }
//
//        Statement stm = DBFunction.createStatement( alConn[ objectID != 0 ? 0 : 1 ] );
//        if( DBFunction.executeUpdate( stm, new StringBuilder( " UPDATE XY_property " )
//                               .append( " SET v_string = '" ).append( textValue ).append( "' " )
//                               .append( " WHERE element_id = " ).append( elementID )
//                               .append( " AND name = '" ).append( textName ).append( "' " ).toString(), objectID != 0 ) == 0 ) {
//
//            DBFunction.executeUpdate( stm, new StringBuilder( " INSERT INTO XY_property " )
//                .append( " ( element_id , name , v_string ) VALUES ( " ).append( elementID ).append( " , '" )
//                .append( textName ).append( "' , '" ).append( textValue ).append( "' ) " ).toString(), objectID != 0 );
//        }
//        //--- удаление устаревших тайл-файлов топографии
//        if( objectID == 0 ) {
//            HashSet<Integer> hsID = new HashSet<Integer>();
//            hsID.add( elementID );
//            deleteTiles( stm, hsID );
//        }
//        stm.close();
//        return null;
//    }
//
//    protected byte[] doDeleteElement( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable {
//        HashSet<String> hsElementKey = (HashSet<String>) hmParam.get( XyParameter.DELETE_ELEMENT_KEY );
//
//        //--- подготовка списка элементов
//        HashMap<Integer,HashSet<Integer>> hmObjectElement = new HashMap<Integer,HashSet<Integer>>();
//        for( String elementKey : hsElementKey ) {
//            int[] arrElementKey = cemXyElement.parseElementKey( elementKey );
//            HashSet<Integer> hsElement = hmObjectElement.get( arrElementKey[ 0 ] );
//            if( hsElement == null ) {
//                hsElement = new HashSet<Integer>();
//                hmObjectElement.put( arrElementKey[ 0 ], hsElement );
//            }
//            hsElement.add( arrElementKey[ 1 ] );
//        }
//
//        Statement[] alStm = { DBFunction.createStatement( alConn.get( 0 ) ), DBFunction.createStatement( alConn.get( 1 ) ) };
//
//        for( Integer objectID : hmObjectElement.keySet() ) {
//            HashSet<Integer> hsElement = hmObjectElement.get( objectID );
//            StringBuilder sbElementID = StringFunction.getStringFromSet( hsElement, " , " );
//
//            StringBuilder sb = new StringBuilder( " DELETE FROM XY_property " ).append( " WHERE element_id IN ( " ).append( sbElementID ).append( " ) " );
//            DBFunction.executeUpdate( alStm[ objectID != 0 ? 0 : 1 ], sb.toString(), objectID != 0 );
//
//            sb = new StringBuilder( " DELETE FROM XY_point " ).append( " WHERE element_id IN ( " ).append( sbElementID ).append( " ) " );
//            DBFunction.executeUpdate( alStm[ objectID != 0 ? 0 : 1 ], sb.toString(), objectID != 0 );
//
//            //--- удаление устаревших тайл-файлов топографии до удаления самих элементов (иначе prj_-координаты потеряем)
//            if( objectID == 0 ) deleteTiles( alStm.get( 1 ), hsElement );
//
//            sb = new StringBuilder( " DELETE FROM XY_element " ).append( " WHERE id IN ( " ).append( sbElementID ).append( " ) " );
//            DBFunction.executeUpdate( alStm[ objectID != 0 ? 0 : 1 ], sb.toString(), objectID != 0 );
//        }
//        for( Statement stm : alStm ) stm.close();
//
////--- пока не надо
////        //--- почистить стартовые параметры для удаленных бизнес-элементов
////        ArrayList<String> alObjectParamOld = (ArrayList<String>) hmParam.get( XyParameter.OBJECT_PARAM );
////        ArrayList<String> alObjectParamNew = new ArrayList<String>();
////        for( int i = 0; i < alObjectParamOld.size(); i++ ) {
////            String objectParam = alObjectParamOld.get( i );
////            StringTokenizer st = new StringTokenizer( objectParam, " ," );
////            st.nextToken(); // paramType
////            st.nextToken(); // objectAlias
////            int objectID = Integer.parseInt( st.nextToken() );
////            //--- если данного стартового объекта нет в списке удаляемых, добавим его в новый/возвращаемый список
////            if( hmObjectElement.get( objectID ) == null ) alObjectParamNew.add( objectParam );
////        }
////
////        //--- выгрузка ответа в выходной поток байтов
////        ByteArrayOutputStream baos = new ByteArrayOutputStream();
////        DataOutputStream dos = new DataOutputStream( baos );
////        dos.writeInt( alObjectParamNew.size() );
////        for( int i = 0; i < alObjectParamNew.size(); i++ ) dos.writeUTF( alObjectParamNew.get( i ) );
////        dos.flush();
////        byte[] arrByte = baos.toByteArray();
////        dos.close();
////
////        return arrByte;
//        return null;
//    }
//
//    protected byte[] doChangeTypeOfElement( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable {
//        Integer newElementTypeID = (Integer) hmParam.get( XyParameter.CHANGE_TYPE_NEW_ID );
//        HashSet<Integer> hsElementID = (HashSet<Integer>) hmParam.get( XyParameter.CHANGE_TYPE_ELEMENT_ID );
//
//        StringBuilder sbID = new StringBuilder();
//        for( Integer id : hsElementID ) sbID.append( sbID.length() == 0 ? "" : " , " ).append( id );
//
//        Statement stm = DBFunction.createStatement( alConn.get( 1 ) );
//        DBFunction.executeUpdate( stm, new StringBuilder( " UPDATE XY_element SET element_type_id = " ).append( newElementTypeID )
//                           .append( " WHERE id IN ( " ).append( sbID ).append( " ) " ).toString(), false );
//
//        //--- удаление устаревших тайл-файлов топографии
//        deleteTiles( stm, hsElementID );
//        stm.close();
//        return null;
//    }
//
//    //--- добавление/удаление объектов в/из родительский объект зависит от конкретного подкласса
//    protected abstract byte[] doLinkToParentObject( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable;
//    protected abstract byte[] doUnlinkFromParentObject( Connection[] alConn, HashMap<String,Object> hmParam ) throws Throwable;
//
