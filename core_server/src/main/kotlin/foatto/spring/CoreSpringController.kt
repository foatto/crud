package foatto.spring

import foatto.core.app.graphic.GraphicAction
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.BusinessException
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.readFileToBuffer
import foatto.core.util.separateUnixPath
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.server.document.sdcXyAbstract
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import foatto.sql.SQLDialect
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLConnection
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.HttpServletResponse

//--- добавлять у каждого наследника
//@RestController
abstract class CoreSpringController : iApplication {

    protected fun download(response: HttpServletResponse, path: String) {
        val file = File(path)
        val mimeType = URLConnection.guessContentTypeFromName(file.name)

        response.contentType = mimeType
        response.setContentLength(file.length().toInt())
        response.outputStream.write(file.readBytes())
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- прописывать у каждого наследника
//    @PostMapping("/api/app")
//    @Transactional
    open fun app(
        //authentication: Authentication,
        //@RequestBody
        appRequest: AppRequest
        //@CookieValue("SESSION") sessionId: String
    ): AppResponse {
        val appBegTime = getCurrentTimeInt()

        lateinit var appResponse: AppResponse

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut( appRequest.sessionID ) { ConcurrentHashMap() }

        //--- строка параметров (или только одна команда, зависит от содержимого строки)
        val hmParam = mutableMapOf<String, String>()
        appRequest.action = AppParameter.parseParam( appRequest.action, hmParam )

        //--- набор для накопления выходных параметров.
        //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
        val hmOut = mutableMapOf<String, Any>()
        //--- ссылка на файл, удаляемый после завершения транзакции
        //--- (и, возможно, после успешной контролируемой передачи данных)
        //--- (пока нигде не применяется)
        //File fileForDeleteAfterCommit = null;

        when( appRequest.action ) {

            AppAction.LOGON -> {
                val logonRequest = appRequest.logon!!
                val logonResult = checkLogon( stm, logonRequest.login, logonRequest.password, chmSession )

                appResponse = AppResponse( logonResult )

                if( logonResult == ResponseCode.LOGON_SUCCESS || logonResult == ResponseCode.LOGON_SUCCESS_BUT_OLD ) {
                    val hmAliasConfig = getAliasConfig( stm, chmSession, hmOut )
                    val userConfig = chmSession[ CoreSpringApp.USER_CONFIG ] as UserConfig

                    //--- временно используем List вместо Map, т.к. в Kotlin/JS нет возможности десериализовать Map (а List десериализуется в Array)
                    appResponse.hmUserProperty = userConfig.userProperty.toList()
                    appResponse.alMenuData = menuInit( stm, hmAliasConfig, userConfig )

                    for( ( upKey, upValue ) in appRequest.logon!!.hmSystemProperties ) {
                        //println( "$upKey = $upValue" )
                        userConfig.saveUserProperty( stm, upKey, upValue )
                    }

//                    logQuery( "Logon result: $logonResult" )
                }
            }
            else -> {
                var userConfig: UserConfig? = chmSession[ CoreSpringApp.USER_CONFIG ] as? UserConfig
                when( appRequest.action ) {
                    AppAction.GRAPHIC -> {
//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
                        val aliasName = hmParam[ AppParameter.ALIAS ]!!
                        val graphicStartDataID = hmParam[ AppParameter.GRAPHIC_START_DATA ]!!

                        val sd = chmSession[ AppParameter.GRAPHIC_START_DATA + graphicStartDataID ] as GraphicStartData

                        appResponse = AppResponse(
                            code = ResponseCode.GRAPHIC,
                            graphic = GraphicResponse(
                                documentTypeName = aliasName,
                                startParamID = graphicStartDataID,
                                shortTitle = sd.shortTitle,
                                fullTitle = sd.sbTitle.substring(0, Math.min(32000, sd.sbTitle.length))
                            )
                        )
                    }
                    AppAction.XY -> {
//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
                        val docTypeName = hmParam[ AppParameter.ALIAS ]
                        val xyStartDataID = hmParam[ AppParameter.XY_START_DATA ]!!

                        val sd = chmSession[ AppParameter.XY_START_DATA + xyStartDataID ] as XyStartData

                        appResponse = AppResponse(
                            code = ResponseCode.XY,
                            xy = XyResponse(
                                documentConfig = CoreSpringApp.hmXyDocumentConfig[ docTypeName ]!!,
                                startParamID = xyStartDataID,
                                shortTitle = sd.shortTitle,
                                fullTitle = sd.sbTitle.substring(0, Math.min(32000, sd.sbTitle.length)),
                                parentObjectID = sd.parentObjectID,
                                parentObjectInfo = sd.parentObjectInfo
                            )
                        )

                    }
//                    AppAction.VIDEO -> {
//                    if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
//                    val aliasName = hmParam[ AppParameter.ALIAS ]
//                    val vcStartDataID = hmParam[ VideoParameter.VIDEO_START_DATA ]!!
//
//                    val sd = chmSession!![ VideoParameter.VIDEO_START_DATA + vcStartDataID ] as VideoStartData
//
//                    bbOut.putByte( if( sd.rangeType == -1 ) ResponseCode.VIDEO_ONLINE else ResponseCode.VIDEO_ARCHIVE )
//                    bbOut.putShortString( vcStartDataID )
//                    bbOut.putShortString( sd.sbTitle.substring( 0, Math.min( 32000, sd.sbTitle.length ) ) )
//                    }
//                    AppAction.VIDEO_ACTION -> {
//                    if( userConfig == null ) throw BusinessException( BUSINESS_EXCEPTION_MESSAGE )
//
//                    if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
//
//                    val doc = sdcVideo()
//                    doc.init( dataServer, dataWorker, chmSession, userConfig )
//                    withCompression = doc.doAction( bbIn, bbOut )
//                    }
                    else ->{
                        val aliasName = hmParam[ AppParameter.ALIAS ] ?: throw BusinessException( "Не указано имя модуля." )

                        val hmAliasConfig = getAliasConfig( stm, chmSession, hmOut )
                        val aliasConfig = hmAliasConfig[ aliasName ] ?: throw BusinessException( "Модуль '$aliasName' не существует." )

                        //--- если класс не требует обязательной аутентификации и нет никакого логина,
                        //--- то подгрузим хотя бы гостевой логин
                        if( !aliasConfig.isAuthorization && userConfig == null ) {
                            //--- при отсутствии оного загрузим гостевой логин
                            userConfig = UserConfig.getConfig( stm, UserConfig.USER_GUEST )
                            hmOut[ CoreSpringApp.USER_CONFIG ] = userConfig // уйдет в сессию
                        }
                        //--- если класс требует обязательную аутентификацию,
                        //--- а юзер не залогинен или имеет гостевой логин, то запросим авторизацию
                        if( aliasConfig.isAuthorization && ( userConfig == null || userConfig.userID == UserConfig.USER_GUEST ) )
                            appResponse = AppResponse( ResponseCode.LOGON_NEED )
                        else {
                            //--- проверим права доступа на класс
                            if( !userConfig!!.userPermission[ aliasName ]!!.contains( cStandart.PERM_ACCESS ) )
                                throw BusinessException( "Доступ к модулю '${aliasConfig.descr}' не разрешён." )

                            val page = Class.forName( aliasConfig.controlClassName ).getConstructor().newInstance() as cStandart
                            page.init(this, stm, chmSession, hmParam, hmAliasConfig, aliasConfig, CoreSpringApp.hmXyDocumentConfig, userConfig)
                            when( appRequest.action ) {
                                AppAction.TABLE -> {
                                    if( CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL ) logQuery( hmParam )

                                    appResponse = AppResponse( code = ResponseCode.TABLE, table = page.getTable( hmOut ) )
                                }
                                AppAction.FORM -> {
                                    if( CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL ) logQuery( hmParam )

                                    appResponse = AppResponse( code = ResponseCode.FORM, form = page.getForm( hmOut ) )
                                }
                                AppAction.FIND  -> {
                                    if( CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL ) logQuery( hmParam )

                                    //--- если сервер вдруг перезагрузили между отдельными командами поиска
                                    //--- (такое бывает редко, только при обновлениях, но тем не менее пользователю обидно),
                                    //--- то сделаем вид что поиска не было :)
                                    val findRedirectURL = page.doFind( appRequest.find!!, hmOut )
                                    appResponse =
                                    if( findRedirectURL == null )
                                         AppResponse( code = ResponseCode.TABLE, table = page.getTable( hmOut ) )
                                    else
                                        AppResponse( code = ResponseCode.REDIRECT, redirect = findRedirectURL )
                                }
                                else -> {
                                    //--- пропускаем модули с синими вёдрами
                                    //if( "ru".equals( aliasName ) ) {}
                                    //--- пропускаем логи отчётов и показов картографии
                                    //else
                                    if( checkLogSkipAliasPrefix( aliasName ) ) {
                                        if( CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL ) logQuery( hmParam )
                                    }
                                    else if( CoreSpringApp.userLogMode != CoreSpringApp.SYSTEM_LOG_NONE ) logQuery( hmParam )

                                    val redirectURL = when( appRequest.action ) {
                                        AppAction.SAVE, AppAction.ARCHIVE, AppAction.UNARCHIVE -> page.doSave(appRequest.action, appRequest.alFormData!!, hmOut)
                                        AppAction.DELETE -> page.doDelete( appRequest.alFormData!!, hmOut )
                                        else -> throw Throwable( "Unknown action = ${appRequest.action}" )
                                    }

                                    appResponse = AppResponse( code = ResponseCode.REDIRECT, redirect = redirectURL )
                                }
                            }
                        }
                    }
                }
            }
        }

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- обновить данные в сессии только после успешной записи данных
        chmSession.putAll( hmOut )
        //--- после успешного коммита можно и удалить файл, если указан
        //--- (пока нигде не применяется)
        //if( fileForDeleteAfterCommit != null ) fileForDeleteAfterCommit.delete();

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - appBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long App Query = " + ( getCurrentTimeInt() - appBegTime ) )
            AdvancedLogger.error( appRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return appResponse
    }

//--- прописывать у каждого наследника
//    @PostMapping("/api/graphic")
//    @Transactional
    open fun graphic(
        //authentication: Authentication,
        //@RequestBody
        graphicActionRequest: GraphicActionRequest
        //@CookieValue("SESSION") sessionId: String
    ): GraphicActionResponse {
        val graphicBegTime = getCurrentTimeInt()

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut( graphicActionRequest.sessionID ) { ConcurrentHashMap() }

        //--- набор для накопления выходных параметров.
        //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
        val hmOut = mutableMapOf<String, Any>()
        //--- ссылка на файл, удаляемый после завершения транзакции
        //--- (и, возможно, после успешной контролируемой передачи данных)
        //--- (пока нигде не применяется)
        //File fileForDeleteAfterCommit = null;

//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
        val userConfig: UserConfig = chmSession[ CoreSpringApp.USER_CONFIG ] as? UserConfig ?: throw BusinessException( CoreSpringApp.BUSINESS_EXCEPTION_MESSAGE )
        val serverDocumentControlClassName = GraphicDocumentConfig.hmConfig[ graphicActionRequest.documentTypeName ]!!.serverControlClassName
        val doc = Class.forName( serverDocumentControlClassName ).getConstructor().newInstance() as sdcAbstractGraphic
        doc.init( this, stm, chmSession, userConfig, graphicActionRequest.documentTypeName )

        val graphicActionResponse =
            when( graphicActionRequest.action ) {
                GraphicAction.GET_COORDS -> doc.doGetCoords( graphicActionRequest.startParamID )
                GraphicAction.GET_ELEMENTS -> doc.doGetElements( graphicActionRequest )
            }

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- обновить данные в сессии только после успешной записи данных
        chmSession.putAll( hmOut )
        //--- после успешного коммита можно и удалить файл, если указан
        //--- (пока нигде не применяется)
        //if( fileForDeleteAfterCommit != null ) fileForDeleteAfterCommit.delete();

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - graphicBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Graphic Query = " + ( getCurrentTimeInt() - graphicBegTime ) )
            AdvancedLogger.error( graphicActionRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return graphicActionResponse
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- прописывать у каждого наследника
//    @PostMapping("/api/xy")
//    @Transactional
    open fun xy(
        //authentication: Authentication,
        //@RequestBody
        xyActionRequest: XyActionRequest
        //@CookieValue("SESSION") sessionId: String
    ): XyActionResponse {
        val xyBegTime = getCurrentTimeInt()

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut( xyActionRequest.sessionID ) { ConcurrentHashMap() }

        //--- набор для накопления выходных параметров.
        //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
        val hmOut = mutableMapOf<String, Any>()
        //--- ссылка на файл, удаляемый после завершения транзакции
        //--- (и, возможно, после успешной контролируемой передачи данных)
        //--- (пока нигде не применяется)
        //File fileForDeleteAfterCommit = null;

//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
        val userConfig: UserConfig = chmSession[ CoreSpringApp.USER_CONFIG ] as? UserConfig ?: throw BusinessException( CoreSpringApp.BUSINESS_EXCEPTION_MESSAGE )
        val docTypeName = xyActionRequest.documentTypeName
        val xyDocConfig = CoreSpringApp.hmXyDocumentConfig[docTypeName]!!

        val doc = Class.forName( xyDocConfig.serverClassName ).getConstructor().newInstance() as sdcXyAbstract
        doc.init( this, stm, chmSession, userConfig, xyDocConfig )

        val xyActionResponse =
            when( xyActionRequest.action ) {
                XyAction.GET_COORDS   -> doc.getCoords( xyActionRequest.startParamID )
                XyAction.GET_ELEMENTS -> doc.getElements( xyActionRequest )
                XyAction.GET_ONE_ELEMENT -> doc.getOneElement( xyActionRequest)
                XyAction.CLICK_ELEMENT -> doc.clickElement( xyActionRequest )
                XyAction.ADD_ELEMENT -> doc.addElement( xyActionRequest, userConfig.userID )
                XyAction.EDIT_ELEMENT_POINT -> doc.editElementPoint( xyActionRequest )
                XyAction.MOVE_ELEMENTS -> doc.moveElements( xyActionRequest )
            }

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- обновить данные в сессии только после успешной записи данных
        chmSession.putAll( hmOut )
        //--- после успешного коммита можно и удалить файл, если указан
        //--- (пока нигде не применяется)
        //if( fileForDeleteAfterCommit != null ) fileForDeleteAfterCommit.delete();

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - xyBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Xy Query = " + ( getCurrentTimeInt() - xyBegTime ) )
            AdvancedLogger.error( xyActionRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return xyActionResponse
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- прописывать у каждого наследника
//    @PostMapping("/api/update")
//    open fun update(
//        //@RequestBody
//        updateRequest: UpdateRequest
//    ): UpdateResponse {
//        val updateBegTime = getCurrentTimeInt()
//
//        val updateDir = File( "${CoreSpringApp.rootDirName}/update" )
//        //--- предварительно загрузим список файлов на обновление в отдельный список,
//        //--- т.к. могут быть вложенные папки и прочая ненужная хрень
//        val hmServerFile = updateDir.listFiles().filter{ it.isFile }.associate { Pair( it.name, it ) }.toMutableMap()
//
//        //--- результирующий список файлов, требующих обновления
//        val alUpdateFile = mutableListOf<File>()
//
//        //--- сверяем пришедший запрос на обновление с серверным списком обновляемых файлов
//        for( ( fileName, fileTime ) in updateRequest.hmFileInfo ) {
//            val serverFile = hmServerFile[ fileName ]
//            //--- если такое файло нашлось
//            if( serverFile != null ) {
//                //--- его время не совпадает с запрашиваемым
//                if( serverFile.lastModified() != fileTime )
//                    alUpdateFile.add( serverFile )
//                //--- в любом случае убираем этот файл из серверного списка - таким образом у нас останется список новых файлов,
//                //--- отсутствовавших в списке запрашиваемых от клиента
//                hmServerFile.remove( fileName )
//            }
//
//        }
//        alUpdateFile.addAll( hmServerFile.values )
//
//        val updateResponse = UpdateResponse( alUpdateFile.associate { Pair( it.name, Pair( it.lastModified(), FileInputStream( it ).readAllBytes() ) ) } )
//
//        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
//        if( System.currentTimeMillis() - updateBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
//            AdvancedLogger.error( "--- Long Update Query = " + ( getCurrentTimeInt() - updateBegTime ) )
//            AdvancedLogger.error( updateRequest.toString() )
//        }
//        //AdvancedLogger.error( "Query time = " + ( getCurrentTimeInt() - appBegTime ) );
//
//        return updateResponse
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- прописывать у каждого наследника
//    @PostMapping("/api/get_file")
    open fun getFile(
        //@RequestBody
        getFileRequest: GetFileRequest
    ): GetFileResponse {
        val getFileBegTime = getCurrentTimeInt()

        val file = File( getFileRequest.altServerDirName ?: CoreSpringApp.rootDirName, getFileRequest.fullFileName )
        val getFileResponse = GetFileResponse( if( file.exists() ) FileInputStream( file ).readAllBytes() else null )
        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - getFileBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Get File Query = " + ( getCurrentTimeInt() - getFileBegTime ) )
            AdvancedLogger.error( getFileRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return getFileResponse
    }

//--- прописывать у каждого наследника
//    @PostMapping("/api/put_file")
    open fun putFile(
        //@RequestBody
        putFileRequest: PutFileRequest
    ): PutFileResponse {
        val putFileBegTime = getCurrentTimeInt()

        val uploadFileName = putFileRequest.fullFileName

        //--- для правильного срабатывания mkdirs надо выделить путь из общего имени файла
        val ( dirName, fileName ) = separateUnixPath( uploadFileName )
        val dir = File( CoreSpringApp.rootDirName, dirName )
        val file = File( dir, fileName )

        dir.mkdirs()
        val fos = FileOutputStream( file )
        fos.write( putFileRequest.fileData )
        fos.close()
        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
        //AdvancedLogger.debug( "FILE: file = $uploadFileName received from ${( selectionKey!!.channel() as SocketChannel ).socket().inetAddress}" )

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - putFileBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Put File Query = " + ( getCurrentTimeInt() - putFileBegTime ) )
            AdvancedLogger.error( putFileRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return PutFileResponse()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- прописывать у каждого наследника
//    @PostMapping("/api/get_replication")
//    @Transactional
    open fun getReplication(
        //@RequestBody
        getReplicationRequest: GetReplicationRequest
    ): GetReplicationResponse {
        val getReplicationBegTime = getCurrentTimeInt()

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

        val getReplicationResponse = GetReplicationResponse( conn.dialect.dialect )

        val tmFile = conn.getReplicationList( getReplicationRequest.destName )
        //--- нельзя удалить файл из списка, пока не получено подтверждение
        var timeKey = if( tmFile.isEmpty() ) -1 else tmFile.firstKey()
        if( timeKey != -1L && timeKey == getReplicationRequest.prevTimeKey ) {
            val alFile = tmFile[ timeKey ]!!
            //--- окончательно удаляем файл из очереди и его самого
            tmFile.remove( timeKey )
            for( file in alFile ) file.delete()

            timeKey = if( tmFile.isEmpty() ) -1 else tmFile.firstKey()
        }
        if( timeKey != -1L ) {
            val alFile = tmFile[ timeKey ]!!
            getReplicationResponse.timeKey = timeKey

            val bbIn = AdvancedByteBuffer( CoreAdvancedConnection.START_REPLICATION_SIZE )
            for( file in alFile )
                readFileToBuffer( file, bbIn, false )

            bbIn.flip()
            while( bbIn.hasRemaining() ) {
                val sqlCount = bbIn.getInt()
                for( i in 0 until sqlCount )
                    getReplicationResponse.alSQL.add( bbIn.getLongString() )
            }
        }

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - getReplicationBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Update Query = " + ( getCurrentTimeInt() - getReplicationBegTime ) )
            AdvancedLogger.error( getReplicationRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return getReplicationResponse
    }

//--- прописывать у каждого наследника
//    @PostMapping("/api/put_replication")
//    @Transactional
    open fun putReplication(
        //@RequestBody
        putReplicationRequest: PutReplicationRequest
    ): PutReplicationResponse {
        val putReplicationBegTime = getCurrentTimeInt()

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

        val destName = putReplicationRequest.destName
        val sourName = putReplicationRequest.sourName
        val sourDialect = putReplicationRequest.sourDialect
        val timeKey = putReplicationRequest.timeKey

        val alReplicationSQL = putReplicationRequest.alSQL.map {
            CoreAdvancedConnection.convertDialect( it, SQLDialect.hmDialect[ sourDialect ]!!, conn.dialect )
        }

        //--- проверка на приём этой реплики в предыдущей сессии связи
        val rs = stm.executeQuery(
            " SELECT 1 FROM SYSTEM_replication_send WHERE dest_name = '$destName' AND sour_name = '$sourName' AND time_key = $timeKey" )
        val isAlReadyReceived = rs.next()
        rs.close()
        //--- такую реплику мы ещё не получали
        if( !isAlReadyReceived ) {
            //--- реплика предназначена этому серверу - работаем как обычно
            if( CoreSpringApp.dbConfig.name == destName ) {
                //--- выполнить реплику, возможно, с последующей перерепликацией:
                //--- если партнёр безымянный, то считаем, что к другим серверам-партнёрам в кластере он уже
                //--- не будет обращаться (т.е. ведёт себя как типовая клиентская программа) -
                //--- поэтому передадим реплику другим именованым партнёрам
                for( sql in alReplicationSQL ) {
                    stm.executeUpdate( sql, sourName.isEmpty() )
                    //                            //--- на время отладки репликатора
                    //                            AdvancedLogger.debug( sql );
                }
            }
            //--- реплика предназначена другому серверу - её надо просто отложить в соответствующую папку
            else if( !alReplicationSQL.isEmpty() ) {
                val bbReplicationData = CoreAdvancedConnection.getReplicationData( alReplicationSQL )
                conn.saveReplication( destName, bbReplicationData )
            }

            //--- и в этой же транзакции запомним имя/номер реплики
            if( stm.executeUpdate(
                " UPDATE SYSTEM_replication_send SET time_key = $timeKey WHERE dest_name = '$destName' AND sour_name = '$sourName' ", false ) == 0 )

                stm.executeUpdate(
                    " INSERT INTO SYSTEM_replication_send ( dest_name , sour_name , time_key ) VALUES ( '$destName' , '$sourName' , $timeKey ) ", false )
        }
        //--- просто ответ
        val putReplicationResponse = PutReplicationResponse( timeKey )

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - putReplicationBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Put Replication Query = " + ( getCurrentTimeInt() - putReplicationBegTime ) )
            AdvancedLogger.error( putReplicationRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return putReplicationResponse
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- прописывать у каждого наследника
//    @PostMapping("/api/save_user_property")
//    @Transactional
    open fun saveUserProperty(
        //@RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        val saveUserPropertyBegTime = getCurrentTimeInt()

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

//        logQuery( hmParam )
        val upName = saveUserPropertyRequest.name
        val upValue = saveUserPropertyRequest.value

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut( saveUserPropertyRequest.sessionID ) { ConcurrentHashMap() }
        val userConfig: UserConfig? = chmSession[ CoreSpringApp.USER_CONFIG ] as? UserConfig

        if( userConfig != null )
            userConfig.saveUserProperty( stm, upName, upValue )
        else
            AdvancedLogger.error( "User config not defined for saved property, name = '$upName', value = '$upValue'." )

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - saveUserPropertyBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Save User Property Query = " + ( getCurrentTimeInt() - saveUserPropertyBegTime ) )
            AdvancedLogger.error( saveUserPropertyRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return SaveUserPropertyResponse()
    }

//--- прописывать у каждого наследника
//    @PostMapping("/api/change_password")
//    @Transactional
    open fun changePassword(
        //@RequestBody
        changePasswordRequest: ChangePasswordRequest
    ): ChangePasswordResponse {
        val changePasswordBegTime = getCurrentTimeInt()

    val conn = AdvancedConnection(CoreSpringApp.dbConfig)
    val stm = conn.createStatement()

//        logQuery( hmParam )

        val newPassword = changePasswordRequest.password
        val toDay = ZonedDateTime.now()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut( changePasswordRequest.sessionID ) { ConcurrentHashMap() }
        val userConfig: UserConfig? = chmSession[ CoreSpringApp.USER_CONFIG ] as? UserConfig

        if( userConfig != null )
            stm.executeUpdate(
                " UPDATE SYSTEM_users SET pwd = '$newPassword' , " +
                " pwd_ye = ${toDay.year} , pwd_mo = ${toDay.monthValue} , pwd_da = ${toDay.dayOfMonth}" +
                " WHERE id = ${userConfig.userID} " )

        //--- зафиксировать любые изменения в базе/
        //--- на самом деле база коммитится самим спрингом, здесь только реплика пишется
        conn.commit()

        stm.close()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - changePasswordBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Change Password Query = " + ( getCurrentTimeInt() - changePasswordBegTime ) )
            AdvancedLogger.error( changePasswordRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return ChangePasswordResponse()
    }

//--- прописывать у каждого наследника
//    @PostMapping("/api/logoff")
    open fun logoff(
        //@RequestBody
        logoffRequest: LogoffRequest
    ): LogoffResponse {
        val logoffBegTime = getCurrentTimeInt()

        CoreSpringApp.chmSessionStore.remove( logoffRequest.sessionID )

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if( getCurrentTimeInt() - logoffBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST ) {
            AdvancedLogger.error( "--- Long Logoff Query = " + ( getCurrentTimeInt() - logoffBegTime ) )
            AdvancedLogger.error( logoffRequest.toString() )
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return LogoffResponse()
    }

//--- прописывать у каждого наследника
//    @PostMapping("/api/upload_form_file")
    open fun uploadFormFile(
//        @RequestParam("form_file_ids")
        arrFormFileId: Array<String>, // со стороны web-клиента ограничение на передачу массива или только строк или только файлов
//        @RequestParam("form_file_blobs")
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {

        arrFormFileId.forEachIndexed { i, id ->
            arrFormFileBlob[ i ].transferTo( File( CoreSpringApp.tempDirName, id ) )
        }

        return FormFileUploadResponse()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun checkLogon( stm: CoreAdvancedStatement, aLogin: String, aPassword: String, chmSession: ConcurrentHashMap<String, Any> ): ResponseCode {
        //--- загрузка данных по активному пользователю с данным логином
        var userID = 0
        var isDisabled = false
        var userPassword: String? = null
        var atCount = 0
        val toDay = ZonedDateTime.now()
        lateinit var atDay: ZonedDateTime
        lateinit var pwdDay: ZonedDateTime
        val rs = stm.executeQuery(
            " SELECT id , is_disabled , pwd , at_count , at_ye , at_mo , at_da , at_ho , at_mi , pwd_ye , pwd_mo , pwd_da " +
            " FROM SYSTEM_users WHERE org_type <> ${OrgType.ORG_TYPE_DIVISION} AND login = '$aLogin' " )
        if( rs.next() ) {
            userID = rs.getInt( 1 )
            isDisabled = rs.getInt( 2 ) != 0
            userPassword = rs.getString( 3 )
            atCount = rs.getInt( 4 )
            atDay = ZonedDateTime.of(rs.getInt( 5 ), rs.getInt( 6 ), rs.getInt( 7 ), rs.getInt( 8 ), rs.getInt( 9 ), 0, 0, ZoneId.systemDefault()).
                    plus(CoreSpringApp.LOGON_LOCK_TIMEOUT.toLong(), ChronoUnit.MINUTES)
            pwdDay = ZonedDateTime.of(rs.getInt( 10 ), rs.getInt( 11 ), rs.getInt( 12 ), 0, 0, 0, 0, ZoneId.systemDefault()).
                    plus(CoreSpringApp.PASSWORD_LIFE_TIME.toLong(), ChronoUnit.MONTHS)
        }
        rs.close()

        //--- если нет такого юзера
        if( userID == 0 ) return ResponseCode.LOGON_FAILED
        //--- если пользователь заблокирован администратором
        if( isDisabled ) return ResponseCode.LOGON_ADMIN_BLOCKED
        //--- если было много неудачных попыток за установленное время
        if( atCount >= CoreSpringApp.MAX_LOGON_ATTEMPT && toDay.isBefore( atDay ) ) return ResponseCode.LOGON_SYSTEM_BLOCKED
        //--- пароль неправильный
        if( aPassword != userPassword ) {
            setAttemptData( stm, true, toDay, userID )
            return ResponseCode.LOGON_FAILED
        }
        //--- пароль правильный, сбрасываем счетчик
        setAttemptData( stm, false, toDay, userID )
        //--- отдельно сохраняем сохраняем прочие user-info
        //--- (т.к. setAttemptData может быть использован в различных местах без последующего saveUserInfo)
        //saveUserInfo( userID )

        //--- исключение из правил: сразу же записываем в сессию информацию по успешно залогиненному пользователю,
        //--- т.к. эта инфа понадобится в той же команде (для выдачи меню и т.п.)
        chmSession[ CoreSpringApp.USER_CONFIG ] = UserConfig.getConfig( stm, userID )

        //--- проверяем просроченность пароля
        return if( toDay.isAfter( pwdDay ) ) ResponseCode.LOGON_SUCCESS_BUT_OLD else ResponseCode.LOGON_SUCCESS
    }

    private fun setAttemptData( stm: CoreAdvancedStatement, incCount: Boolean, toDay: ZonedDateTime, userID: Int ) {
        stm.executeUpdate(
            " UPDATE SYSTEM_users SET at_count = ${if(incCount) "at_count + 1" else "0"} , " +
            " at_ye = ${toDay.year} , at_mo = ${toDay.monthValue} , at_da = ${toDay.dayOfMonth} , " +
            " at_ho = ${toDay.hour} , at_mi = ${toDay.minute} WHERE id = $userID " )
    }

//    private fun saveUserInfo( userID: Int ) {
//        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
//        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
//        jdbcTemplate.update( " UPDATE SYSTEM_users SET last_ip = '${( selectionKey!!.channel() as SocketChannel ).socket().inetAddress}' WHERE id = $userID " )
//    }

    private fun logQuery( hmParam: Map<String, String> ) {
//        if( userLogMode == SYSTEM_LOG_NONE ) return
//        logQuery( getAppParam( hmParam ) )
    }

    private fun logQuery( appParam: CharSequence ) {
//        if( userLogMode == SYSTEM_LOG_NONE ) return
//
//        val uc = chmSession!![ AbstractAppServer.USER_CONFIG ] as UserConfig
//        //--- какое д.б. имя лог-файла для текущего дня и часа
//        //--- собственно обработка
//        val logTime = DateTime_YMDHMS( timeZone, System.currentTimeMillis() )
//        val curLogFileName = logTime.substring( 0, 13 ).replace( '.', '-' ).replace( ' ', '-' )
//
//        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
//        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
//        val out = getFileWriter( File( dirUserLog, curLogFileName ), "UTF-8", true )
//        //--- собственно вывод
//        out.write( "${uc.userID} $logTime ${( selectionKey!!.channel() as SocketChannel ).socket().inetAddress} ${uc.userFullName} $appParam" )
//        out.newLine()
//        out.flush()
//        out.close()
    }

    protected open fun checkLogSkipAliasPrefix( alias: String ): Boolean = false

//    private fun getAppParam( hmParam: HashMap<String, String> ): StringBuilder {
//        val sbAppParam = StringBuilder()
//        for( key in hmParam.keys ) {
//            if( !hsSkipKeyWords.contains( key ) && !key.startsWith( AppParameter.REFERER ) && !key.startsWith( AppParameter.SELECTOR ) &&
//                !key.startsWith( AppParameter.FORM_DATA ) && !key.startsWith( GraphicParameter.GRAPHIC_START_DATA ) && !key.startsWith( XyParameter.XY_START_DATA ) ) {
//
//                sbAppParam.append( if( sbAppParam.isEmpty() ) "" else sLineSeparator ).append( key ).append( " = " ).append( hmParam[ key ] )
//            }
//        }
//        return sbAppParam
//    }
//
//    private fun checkUpdateAPK( updateFileName: String, updateFileTime: Long, bbOut: AdvancedByteBuffer ) {
//
//        val file = File( StringBuilder( dataServer.rootDirName ).append( "/update_apk/" ).append( updateFileName ).toString() )
//        if( file.exists() && file.lastModified() != updateFileTime ) {
//            bbOut.putLong( file.lastModified() )
//            readFileToBuffer( file, bbOut, false )
//        }
//        else bbOut.putLong( 0 )
//    }

    private fun getAliasConfig( stm: CoreAdvancedStatement, chmSession: ConcurrentHashMap<String, Any>, hmOut: MutableMap<String, Any> ): Map<String, AliasConfig> {
        //--- вытаскиваем (или загружаем при необходимости) aliasConfig
        var hmAliasConfig: Map<String, AliasConfig>? = chmSession[ CoreSpringApp.ALIAS_CONFIG ] as? Map<String, AliasConfig>
        if( hmAliasConfig == null ) {
            hmAliasConfig = AliasConfig.getConfig( stm )
            hmOut[ CoreSpringApp.ALIAS_CONFIG ] = hmAliasConfig
        }
        return hmAliasConfig
    }

    //--- для перекрытия классами-наследниками
    protected abstract fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig ): List<MenuData>

    protected fun addMenu( hmAliasConfig: Map<String, AliasConfig>, hmAliasPerm: Map<String, Set<String>>, alMenu: MutableList<MenuData>, alias: String, isTableMenu: Boolean ) {
        if( checkMenuPermission( hmAliasConfig, hmAliasPerm, alias ) )
            alMenu.add( if( isTableMenu ) createTableMenu( hmAliasConfig, alias ) else createFormMenu( hmAliasConfig, alias ) )
    }

    protected fun addSeparator( alMenu: MutableList<MenuData> ) {
        alMenu.add( MenuData( "", "" ) )
    }

    protected fun checkMenuPermission( hmAliasConfig: Map<String, AliasConfig>, hmAliasPerm: Map<String, Set<String>>, alias: String ): Boolean {
        val ac = hmAliasConfig[ alias ]
        val hsPerm = hmAliasPerm[ alias ]
        return ac != null && hsPerm != null && hsPerm.contains( cStandart.PERM_ACCESS )
    }

    private fun createTableMenu( hmAliasConfig: Map<String, AliasConfig>, alias: String ): MenuData {
        val ac = hmAliasConfig[ alias ]
        return MenuData( "${AppParameter.ALIAS}=$alias&${AppParameter.ACTION}=${AppAction.TABLE}", ac?.descr ?: "" )
    }

    private fun createFormMenu( hmAliasConfig: Map<String, AliasConfig>, alias: String ): MenuData {
        return MenuData( "${AppParameter.ALIAS}=$alias&${AppParameter.ACTION}=${AppAction.FORM}&${AppParameter.ID}=0", hmAliasConfig[ alias ]!!.descr )
    }
}

