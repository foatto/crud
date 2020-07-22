//package foatto.core_server.service;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.CommonFunction;
//
//import java.util.ArrayList;
//import java.util.StringTokenizer;
//
//public abstract class CoreDataTransfer extends CoreServiceWorker {
//
//    private static final String CONFIG_SQL_FILE = "sql_file";
//
//    private static final int TYPE_UNKNOWN = 0;
//    private static final int TYPE_INT = 1;
//    private static final int TYPE_DOUBLE = 2;
//    private static final int TYPE_STRING = 3;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    private String sqlFileName = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    protected CoreDataTransfer( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        sqlFileName = hmConfig.get( CONFIG_SQL_FILE );
//    }
//
//    protected boolean isRunOnce() { return true; }
//
//    protected void cycle() throws Throwable {
//        ArrayList<String> alSQL = CommonFunction.loadTextFile( sqlFileName, "Cp1251", null, true );
//
//        String tableName = null;
//        StringBuilder sbSQL = new StringBuilder();
//        ArrayList<String> alFieldName = new ArrayList<>();
//        ArrayList<Integer> alFieldType = new ArrayList<>();
//
//        for( String rawSQL : alSQL ) {
//            AdvancedLogger.debug( rawSQL );
//
//            //--- убираем возможный комментарий до конца строки
//            int commentPos = rawSQL.indexOf( "--" );
//            String sql = commentPos == -1 ? rawSQL : rawSQL.substring( 0, commentPos );
//            if( sql.trim().isEmpty() ) continue;
//
//            //--- распознаём SQL-директиву
//            StringTokenizer st = new StringTokenizer( sql, " ()," );
//
//            String word1 = st.nextToken();
//            String word2 = st.hasMoreTokens() ? st.nextToken() : null;
//
//            //--- создание таблицы или индекса
//            if( word1.equalsIgnoreCase( "CREATE" ) ) {
//                //--- ещё не закончилась обработка предыдущей таблицы
//                if( tableName != null )
//                    throw new Throwable( new StringBuilder(
//                        "Previous CREATE TABLE not finished: " ).append( sql ).toString() );
//
//                if( word2.equalsIgnoreCase( "TABLE" ) ) {
//                    tableName = st.nextToken();
//                    sbSQL.append( sql );
//                }
//                //--- создание индекса - одна команда в одну строку
//                else if( word2.equalsIgnoreCase( "INDEX" ) || word2.equalsIgnoreCase( "CLUSTERED" ) ) {
//
//                    //--- множество таблиц с нумерацией в своём имени
//                    if( sql.contains( "#" ) ) {
//                        ArrayList<Integer> alID = getTableID( rawSQL, commentPos );
//
//                        for( Integer id : alID ) {
//                            alStm.get( 1 ).executeUpdate( sql.replace( "#", id.toString() ) );
//                            alConn.get( 1 ).commit();
//                        }
//                    }
//                    else {
//                        alStm.get( 1 ).executeUpdate( sql );
//                        alConn.get( 1 ).commit();
//                    }
//                }
//                else throw new Throwable( new StringBuilder(
//                    "Unknown CREATE command: " ).append( sql ).toString() );
//            }
//            //--- завершение описания таблицы ";" (без сгрызённой парсером закрывающей скобки)
//            else if( word1.equals( ";" ) ) {
//                if( tableName == null )
//                    throw new Throwable( new StringBuilder(
//                        "Undefined table finishing: " ).append( sql ).toString() );
//
//                sbSQL.append( sql );
//
//                StringBuilder sbFieldName = new StringBuilder();
//                for( String fieldName : alFieldName )
//                    sbFieldName.append( sbFieldName.length() == 0 ? "" : " , " )
//                               .append( fieldName );
//
//                //--- множество таблиц с нумерацией в своём имени
//                if( tableName.contains( "#" ) ) {
//                    ArrayList<Integer> alID = getTableID( rawSQL, commentPos );
//
//                    for( Integer id : alID )
//                        transferOneTable( sbSQL.toString().replace( "#", id.toString() ),
//                                          tableName.replace( "#", id.toString() ),
//                                          sbFieldName, alFieldType );
//                }
//                else transferOneTable( sbSQL, tableName, sbFieldName, alFieldType );
//
//                AdvancedLogger.info( new StringBuilder( "--- Data transfer for table " ).append( tableName )
//                                               .append( " completed. ---" ) );
//
//                tableName = null;
//                sbSQL.setLength( 0 );
//                alFieldName.clear();
//                alFieldType.clear();
//            }
//            //--- PRIMARY KEY внутри описания таблицы
//            else if( word1.equalsIgnoreCase( "PRIMARY" ) ) {
//                if( tableName == null )
//                    throw new Throwable( new StringBuilder(
//                        "Primary key for undefined table: " ).append( sql ).toString() );
//
//                sbSQL.append( sql );
//            }
//            //--- описания полей внутри таблицы
//            else {
//                if( tableName == null )
//                    throw new Throwable( new StringBuilder(
//                        "Primary key for undefined table: " ).append( sql ).toString() );
//
//                int fieldType =
//                    word2.equalsIgnoreCase( "INT" ) || word2.equalsIgnoreCase( "INTEGER" ) ||
//                    word2.equalsIgnoreCase( "BIGINT" ) ? TYPE_INT :
//                    word2.equalsIgnoreCase( "FLOAT" ) || word2.equalsIgnoreCase( "FLOAT8" ) ||
//                                                          word2.equalsIgnoreCase( "REAL" ) ? TYPE_DOUBLE :
//                    word2.equalsIgnoreCase( "VARCHAR" ) || word2.equalsIgnoreCase( "TEXT" ) ? TYPE_STRING :
//                                                                                              TYPE_UNKNOWN;
//                if( fieldType == TYPE_UNKNOWN )
//                    throw new Throwable( new StringBuilder(
//                        "Unknown field type: " ).append( sql ).toString() );
//
//                sbSQL.append( sql );
//                alFieldName.add( word1 );
//                alFieldType.add( fieldType );
//            }
//        }
//    }
//
//    private ArrayList<Integer> getTableID( String sql, int commentPos ) throws Throwable {
//        if( commentPos == -1 )
//            throw new Throwable( new StringBuilder(
//                "ID-table not defined: " ).append( sql ).toString() );
//
//        StringTokenizer stTableID = new StringTokenizer( sql.substring( commentPos + 2 ), " " );
//        //--- критическая ошибка - должно быть как минимум два слова, разделённых пробелами/запятыми
//        if( stTableID.countTokens() < 2 )
//            throw new Throwable( new StringBuilder(
//                "Too few tokens for ID-Table: " ).append( sql ).toString() );
//
//        String idTableName = stTableID.nextToken();
//        String idFieldName = stTableID.nextToken();
//
//        ArrayList<Integer> alID = new ArrayList<>();
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT " ).append( idFieldName ).append( " FROM " ).append( idTableName )
//            .append( " WHERE " ).append( idFieldName ).append( " <> 0 " ) );
//        while( rs.next() ) alID.add( rs.getInt( 1 ) );
//        rs.close();
//
//        return alID;
//    }
//
//    private void transferOneTable( CharSequence sbSQL, CharSequence tableName, CharSequence sbFieldName,
//                                   ArrayList<Integer> alFieldType ) {
//
//        //--- создаём таблицу
//        alStm.get( 1 ).executeUpdate( sbSQL );
//        alConn.get( 1 ).commit();
//
//        //--- копируем данные
//
//        CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT " ).append( sbFieldName ).append( " FROM " ).append( tableName ) );
//        while( rs.next() ) {
//            StringBuilder sbFieldValue = new StringBuilder();
//            int pos = 1;
//            for( int fieldType : alFieldType )
//                sbFieldValue.append( sbFieldValue.length() == 0 ? "" : " , " )
//                        .append( fieldType == TYPE_STRING ? "'" : "" )
//                        .append( fieldType == TYPE_INT ?    rs.getLong( pos++ ) :
//                                 fieldType == TYPE_DOUBLE ? rs.getDouble( pos++ ) :
//                                                            rs.getString( pos++ ) )
//                        .append( fieldType == TYPE_STRING ? "'" : "" );
//            alStm.get( 1 ).executeUpdate( new StringBuilder(
//                " INSERT INTO " ).append( tableName ).append( " ( " ).append( sbFieldName )
//                    .append( " ) VALUES ( " ).append( sbFieldValue ).append( " ); " ) );
//        }
//        rs.close();
//        alConn.get( 1 ).commit();
//    }
//}
