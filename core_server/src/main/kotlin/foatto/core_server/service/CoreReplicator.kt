//package foatto.core_server.service
//
//import foatto.core.link.AppLink
//import foatto.core.link.GetReplicationRequest
//import foatto.core.link.PutReplicationRequest
//import foatto.core.util.AdvancedByteBuffer
//import foatto.sql.CoreAdvancedConnection
//import foatto.sql.SQLDialect
//import foatto.core.util.AdvancedLogger
//import foatto.core.util.readFileToBuffer
//import kotlinx.coroutines.runBlocking
//import java.io.IOException
//
//abstract class CoreReplicator( aConfigFileName: String ) : CoreServiceWorker( aConfigFileName ) {
//
//    companion object {
//
//        private val ROLE_SENDER = 0
//        private val ROLE_RECEIVER = 1
//
//        private val CONFIG_DEST_NAME_ = "dest_name_"
//        private val CONFIG_DEST_IP_ = "dest_ip_"
//        private val CONFIG_DEST_PORT_ = "dest_port_"
//        private val CONFIG_DEST_ROLE_ = "dest_role_"
//
//        private val CONFIG_CYCLE_PAUSE = "cycle_pause"
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    override val isRunOnce: Boolean = false
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private val alDestName = mutableListOf<String>()
//    private val alLink = mutableListOf<AppLink>()
//    private val alDestRole = mutableListOf<Int?>()
//
//    private var cyclePause: Long = 0
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    override fun loadConfig() {
//        super.loadConfig()
//
//        var index = 0
//        while(true) {
//            val destName = hmConfig[ CONFIG_DEST_NAME_ + index ] ?: break
//
//            alDestName.add( destName )
//            //--- репликация - специфическая функция, и чтобы не засорять хранилище сессий,
//            //--- установим sessionID в "как бы служебное" значение 0
//            alLink.add( AppLink( 0 ) )
//            alLink[ index ].addServer( hmConfig[ CONFIG_DEST_IP_ + index ]!!, hmConfig[ CONFIG_DEST_PORT_ + index ]!!.toInt() )
//            //--- если роль репликатора не указана явно, то он выполняет обе роли - и отправителя и получателя
//            val sDestRole = hmConfig[ CONFIG_DEST_ROLE_ + index ]
//            alDestRole.add( sDestRole?.toInt() )
//
//            index++
//        }
//
//        cyclePause = Integer.parseInt(hmConfig[CONFIG_CYCLE_PAUSE])
//    }
//
//    override fun cycle() {
//        //--- лог пришедшей SQL-реплики для разбирательств в случае ошибки
//        val sbSQLLog = StringBuilder()
//        //--- флаг хотя бы одной успешной сработки
//        var isWorked = false
//
//        for( destIndex in alDestName.indices ) {
//            val destName = alDestName[ destIndex ]
//            val destRole = alDestRole[ destIndex ]
//
//            //--- на каждый сервер - отдельный try, чтобы перебирать прочие сервера, пока какие-то из них недоступны
//            try {
//                if( destRole == null || destRole == ROLE_SENDER ) {
//                    val tmFile = alConn[ 0 ].getReplicationList( destName )
//                    if( !tmFile.isEmpty() ) {
//                        isWorked = true
//
//                        //--- нельзя удалить файл из списка, пока не получено подтверждение
//                        val timeKey = tmFile.firstKey()
//                        val alFile = tmFile[ timeKey ]!!
//
//                        val bbIn = AdvancedByteBuffer( CoreAdvancedConnection.START_REPLICATION_SIZE )
//                        for( file in alFile )
//                            readFileToBuffer( file, bbIn, false )
//
//                        bbIn.flip()
//                        val alSQL = mutableListOf<String>()
//                        while( bbIn.hasRemaining() ) {
//                            val sqlCount = bbIn.getInt()
//                            for( i in 0 until sqlCount )
//                                alSQL.add( bbIn.getLongString() )
//                        }
//
//                        val putReplicationRequest = PutReplicationRequest( destName, alDBConfig[ 0 ].name, alConn[ 0 ].dialect.dialect, timeKey, alSQL )
//
//                        runBlocking {
//                            val putReplicationResponse = alLink[ destIndex ].invokePutReplication( putReplicationRequest )
//
//                            //--- окончательно удаляем файл из очереди и его самого
//                            if( timeKey == putReplicationResponse.timeKey ) {
//                                tmFile.remove( timeKey )
//                                for( file in alFile ) file.delete()
//                            }
//                        }
//                    }
//                }
//
//                if( destRole == null || destRole == ROLE_RECEIVER ) {
//                    sbSQLLog.setLength( 0 )
//
//                    //--- что мы успешно получили в прошлый раз?
//                    var prevTimeKey: Long = -1
//                    val rs = alStm[ 0 ].executeQuery( " SELECT time_key FROM SYSTEM_replication_receive WHERE dest_name = '$destName' " )
//                    if( rs.next() ) prevTimeKey = rs.getLong( 1 )
//                    rs.close()
//
//                    val getReplicationRequest = GetReplicationRequest( alDBConfig[ 0 ].name, prevTimeKey )
//
//                    runBlocking {
//                        val getReplicationResponse = alLink[ destIndex ].invokeGetReplication( getReplicationRequest )
//                        val sourDialect = SQLDialect.hmDialect[ getReplicationResponse.dialect ]!!
//                        val timeKey = getReplicationResponse.timeKey
//                        if( timeKey != -1L ) {
//                            isWorked = true
//
//                            getReplicationResponse.alSQL.forEach {
//                                sbSQLLog.append( if( sbSQLLog.isEmpty() ) "" else "\n" ).append( it )
//                                alStm[ 0 ].executeUpdate( CoreAdvancedConnection.convertDialect( it, sourDialect, alConn[ 0 ].dialect ), false )
//                                //                                //--- на время отладки репликатора
//                                //                                AdvancedLogger.debug( sql );
//                            }
//                            //--- и в этой же транзакции запомним имя/номер реплики
//                            if( alStm[ 0 ].executeUpdate( " UPDATE SYSTEM_replication_receive SET time_key = $timeKey WHERE dest_name = '$destName' ", false ) == 0 )
//                                alStm[ 0 ].executeUpdate( " INSERT INTO SYSTEM_replication_receive ( dest_name , time_key ) VALUES ( '$destName' , $timeKey ) ", false )
//                            alConn[ 0 ].commit()
//                        }
//                    }
//                }
//            }
//            catch( ioe: IOException ) {
//                //--- на всякий случай, хотя на этом этапе ещё не было изменений в локальной базе
//                //alConn.get( 0 ).rollback();
//                AdvancedLogger.error( ioe )
//            }
//            catch( t: Throwable ) {
//                t.printStackTrace()
//                //--- вывести/сохранить SQL-запрос, при котором (возможно) получена ошибка
//                AdvancedLogger.error( sbSQLLog )
//                //--- передаём ошибку дальше
//                throw t
//            }
//        }
//        //--- если нам нечего было отправлять и получать на все сервера/со всех серверов,
//        //--- то выдержим паузу
//        if( !isWorked ) {
//            AdvancedLogger.info( "No replication data. Pause ${cyclePause / 1000} sec." )
//            Thread.sleep( cyclePause )
//        }
//    }
//
//}
