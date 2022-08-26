package foatto.spring.controllers

import foatto.core.app.graphic.GraphicAction
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core.util.AdvancedLogger
import foatto.core.util.BusinessException
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getFilledNumberString
import foatto.core_server.app.AppParameter
import foatto.core_server.app.composite.server.CompositeStartData
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
import foatto.jooq.core.tables.SystemUserProperty
import foatto.jooq.core.tables.SystemUsers
import foatto.spring.CoreSpringApp
import foatto.spring.jpa.repositories.UserRepository
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URLConnection
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.HttpServletResponse
import kotlin.math.min

//--- добавлять у каждого наследника
//@RestController
abstract class CoreAppController : iApplication {

    companion object {

        const val FILE_BASE = "files"

        //--- last enabled time for file access check
        private val chmFileTime = ConcurrentHashMap<String, Int>()

        fun download(response: HttpServletResponse, path: String) {
            val file = File(path)
            val mimeType = URLConnection.guessContentTypeFromName(file.name)

            response.contentType = mimeType
            response.setContentLength(file.length().toInt())
            response.outputStream.write(file.readBytes())
        }
    }

    private enum class DataAccessMethodEnum {
        JDBC,
        JOOQ,
        JPA,
    }

    private val currentDataAccessMethod = DataAccessMethodEnum.JOOQ

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${root_dir}")
    override val rootDirName: String = ""

    @Value("\${temp_dir}")
    override val tempDirName: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${client_alias}")
    override val alClientAlias: Array<String> = emptyArray()

    @Value("\${client_parent_id}")
    override val alClientParentId: Array<String> = emptyArray()

    @Value("\${client_role_id}")
    override val alClientRoleId: Array<String> = emptyArray()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${file_access_period}")
    val fileAccessPeriod: String = "24"  // 24 hour file accessibility by default

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val hmAliasLogDir: MutableMap<String, String>
        get() = CoreSpringApp.hmAliasLogDir

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getFileList(conn: CoreAdvancedConnection, fileId: Int): List<Pair<Int, String>> {
        val alFileStoreData = mutableListOf<Pair<Int, String>>()

        val stm = conn.createStatement()
        val rs = stm.executeQuery(
            """
                SELECT id , name , dir 
                FROM SYSTEM_file_store 
                WHERE file_id = $fileId 
                ORDER BY name 
            """
        )
        while (rs.next()) {
            val id = rs.getInt(1)
            val fileName = rs.getString(2)
            val dirName = rs.getString(3)

            val fileUrl = "/$FILE_BASE/$dirName/$fileName"

            alFileStoreData.add(Pair(id, fileUrl))
            chmFileTime[fileUrl] = getCurrentTimeInt()
        }
        rs.close()
        stm.close()

        return alFileStoreData
    }

    override fun saveFile(stm: CoreAdvancedStatement, fileId: Int, idFromClient: Int, fileName: String) {
        val fileFromClient = File(tempDirName, idFromClient.toString())
        val fileStoreId = stm.getNextIntId("SYSTEM_file_store", "id")

        val dirName = CoreSpringApp.minioProxy?.let { minioProxy ->
            minioProxy.saveFile(
                objectName = fileStoreId.toString(),
                objectStream = fileFromClient.inputStream(),
                objectSize = fileFromClient.length()
            )

            fileStoreId.toString()
        } ?: run {
            //--- найти для него новое местоположение
            val newDirName = getFreeDir(fileName)
            val newFile = File(rootDirName, "$FILE_BASE/$newDirName/$fileName")

            //--- перенести файл в отведённое место
            fileFromClient.renameTo(newFile)

            newDirName
        }
        //--- сохранить запись о файле
        stm.executeUpdate(
            """
                INSERT INTO SYSTEM_file_store ( id , file_id , name , dir ) 
                VALUES ( $fileStoreId , $fileId , '$fileName' , '$dirName' ) 
            """
        )
    }

    override fun deleteFile(stm: CoreAdvancedStatement, fileId: Int, id: Int?) {
        val sbSQLDiff = id?.let {
            " AND id = $id "
        } ?: ""

        val sbSQL =
            """
                SELECT id , name , dir 
                FROM SYSTEM_file_store 
                WHERE file_id = $fileId 
                $sbSQLDiff 
            """

        val rs = stm.executeQuery(sbSQL)
        while (rs.next()) {
            val storeId = rs.getInt(1)
            val fileName = rs.getString(2)
            val dirName = rs.getString(3)

            CoreSpringApp.minioProxy?.removeFile(
                objectName = storeId.toString()
            ) ?: run {
                val delFile = File(rootDirName, "$FILE_BASE/$dirName/$fileName")
                if (delFile.exists()) {
                    delFile.delete()
                }
            }
        }
        rs.close()

        stm.executeUpdate(
            """
                DELETE FROM SYSTEM_file_store 
                WHERE file_id = $fileId 
                $sbSQLDiff 
            """
        )
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/upload_form_file")
    fun uploadFormFile(
        @RequestParam("form_file_ids")
        arrFormFileId: Array<String>, // со стороны web-клиента ограничение на передачу массива или только строк или только файлов
        @RequestParam("form_file_blobs")
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {

        arrFormFileId.forEachIndexed { i, id ->
            arrFormFileBlob[i].transferTo(File(tempDirName, id))
        }

        return FormFileUploadResponse()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/$FILE_BASE/{dirName}/{fileName}"])
    fun downloadFile(
        response: HttpServletResponse,
        @PathVariable("dirName")
        dirName: String,
        @PathVariable("fileName")
        fileName: String
    ) {
        val fileUrl = "/$FILE_BASE/$dirName/$fileName"

        val fileTime = chmFileTime[fileUrl] ?: 0
        if (getCurrentTimeInt() < fileTime + (fileAccessPeriod.toIntOrNull() ?: 24) * 60 * 60) {

            CoreSpringApp.minioProxy?.loadFileToHttpResponse(
                objectName = dirName,
                fileName = fileName,
                response = response,
            ) ?: run {
                download(response, "$rootDirName$fileUrl")
            }
        } else {
            response.status = 403   // forbidden
        }
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- прописывать у каждого наследника
//    @PostMapping("/api/app")
    open fun app(
        //@RequestBody
        appRequest: AppRequest
    ): AppResponse {
        val appBegTime = getCurrentTimeInt()

        lateinit var appResponse: AppResponse

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(appRequest.sessionId) { ConcurrentHashMap() }

        //--- строка параметров (или только одна команда, зависит от содержимого строки)
        val hmParam = mutableMapOf<String, String>()
        appRequest.action = AppParameter.parseParam(appRequest.action, hmParam)

        //--- набор для накопления выходных параметров.
        //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
        val hmOut = mutableMapOf<String, Any>()
        //--- ссылка на файл, удаляемый после завершения транзакции
        //--- (и, возможно, после успешной контролируемой передачи данных)
        //--- (пока нигде не применяется)
        //File fileForDeleteAfterCommit = null;

        when (appRequest.action) {

            AppAction.LOGON -> {
                val logonRequest = appRequest.logon!!
                val logonResult = checkLogon(conn, logonRequest.login, logonRequest.password, chmSession)

                appResponse = AppResponse(logonResult)

                if (logonResult == ResponseCode.LOGON_SUCCESS || logonResult == ResponseCode.LOGON_SUCCESS_BUT_OLD) {
                    val hmAliasConfig = getAliasConfig(stm, chmSession, hmOut)
                    val userConfig = chmSession[iApplication.USER_CONFIG] as UserConfig

                    appResponse.currentUserName = hmUserFullNames[userConfig.userId] ?: "(неизвестный пользователь)"
                    //--- временно используем List вместо Map, т.к. в Kotlin/JS нет возможности десериализовать Map (а List десериализуется в Array)
                    appResponse.hmUserProperty = loadUserProperies(conn, userConfig.userId).toList().toTypedArray()
                    appResponse.arrMenuData = menuInit(stm, hmAliasConfig, userConfig).toTypedArray()

                    for ((upKey, upValue) in appRequest.logon!!.hmSystemProperties) {
                        userConfig.saveUserProperty(conn, upKey, upValue)
                    }
//                    logQuery( "Logon result: $logonResult" )
                }
            }

            else -> {
                var userConfig: UserConfig? = chmSession[iApplication.USER_CONFIG] as? UserConfig
                when (appRequest.action) {
                    AppAction.GRAPHIC -> {
//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
                        val aliasName = hmParam[AppParameter.ALIAS]!!
                        val graphicStartDataID = hmParam[AppParameter.GRAPHIC_START_DATA]!!

                        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + graphicStartDataID] as GraphicStartData

                        appResponse = AppResponse(
                            code = ResponseCode.GRAPHIC,
                            graphic = GraphicResponse(
                                documentTypeName = aliasName,
                                startParamId = graphicStartDataID,
                                shortTitle = sd.shortTitle.substring(0, min(32000, sd.shortTitle.length)),
                                fullTitle = sd.fullTitle.substring(0, min(32000, sd.fullTitle.length))
                            )
                        )
                    }

                    AppAction.XY -> {
//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
                        val docTypeName = hmParam[AppParameter.ALIAS]
                        val xyStartDataID = hmParam[AppParameter.XY_START_DATA]!!

                        val sd = chmSession[AppParameter.XY_START_DATA + xyStartDataID] as XyStartData
                        appResponse = AppResponse(
                            code = ResponseCode.XY,
                            xy = XyResponse(
                                documentConfig = CoreSpringApp.hmXyDocumentConfig[docTypeName]!!,
                                startParamId = xyStartDataID,
                                shortTitle = sd.shortTitle.substring(0, min(32000, sd.shortTitle.length)),
                                fullTitle = sd.fullTitle.substring(0, min(32000, sd.fullTitle.length)),
                                arrServerActionButton = sd.alServerActionButton.toTypedArray(),
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
                    AppAction.COMPOSITE -> {
                        val compositeStartDataID = hmParam[AppParameter.COMPOSITE_START_DATA]!!

                        appResponse = getCompositeResponse(
                            compositeStartData = chmSession[AppParameter.COMPOSITE_START_DATA + compositeStartDataID] as CompositeStartData
                        )
                    }

                    else -> {
                        val aliasName = hmParam[AppParameter.ALIAS] ?: throw BusinessException("Не указано имя модуля.")

                        val hmAliasConfig = getAliasConfig(stm, chmSession, hmOut)
                        val aliasConfig = hmAliasConfig[aliasName] ?: throw BusinessException("Модуль '$aliasName' не существует.")

                        //--- если класс не требует обязательной аутентификации и нет никакого логина,
                        //--- то подгрузим хотя бы гостевой логин
                        if (!aliasConfig.isAuthorization && userConfig == null) {
                            //--- при отсутствии оного загрузим гостевой логин
                            reloadUserNames(conn)
                            userConfig = UserConfig.getConfig(conn, UserConfig.USER_GUEST, this)
                            hmOut[iApplication.USER_CONFIG] = userConfig // уйдет в сессию
                        }
                        //--- если класс требует обязательную аутентификацию,
                        //--- а юзер не залогинен или имеет гостевой логин, то запросим авторизацию
                        if (aliasConfig.isAuthorization && (userConfig == null || userConfig.userId == UserConfig.USER_GUEST))
                            appResponse = AppResponse(ResponseCode.LOGON_NEED)
                        else {
                            //--- проверим права доступа на класс
                            if (!userConfig!!.userPermission[aliasName]!!.contains(cStandart.PERM_ACCESS))
                                throw BusinessException("Доступ к модулю '${aliasConfig.descr}' не разрешён.")

                            val page = Class.forName(aliasConfig.controlClassName).getConstructor().newInstance() as cStandart
                            page.init(this, conn, stm, chmSession, hmParam, hmAliasConfig, aliasConfig, CoreSpringApp.hmXyDocumentConfig, userConfig)
                            when (appRequest.action) {
                                AppAction.TABLE -> {
                                    if (CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL) logQuery(hmParam)

                                    appResponse = AppResponse(code = ResponseCode.TABLE, table = page.getTable(hmOut))
                                }

                                AppAction.FORM -> {
                                    if (CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL) logQuery(hmParam)

                                    appResponse = AppResponse(code = ResponseCode.FORM, form = page.getForm(hmOut))
                                }

                                AppAction.FIND -> {
                                    if (CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL) logQuery(hmParam)

                                    //--- если сервер вдруг перезагрузили между отдельными командами поиска
                                    //--- (такое бывает редко, только при обновлениях, но тем не менее пользователю обидно),
                                    //--- то сделаем вид что поиска не было :)
                                    val findRedirectURL = page.doFind(appRequest.find, hmOut)
                                    appResponse =
                                        if (findRedirectURL == null) {
                                            AppResponse(code = ResponseCode.TABLE, table = page.getTable(hmOut))
                                        } else {
                                            AppResponse(code = ResponseCode.REDIRECT, redirect = findRedirectURL)
                                        }
                                }

                                else -> {
                                    if (checkLogSkipAliasPrefix(aliasName)) {
                                        if (CoreSpringApp.userLogMode == CoreSpringApp.SYSTEM_LOG_ALL) {
                                            logQuery(hmParam)
                                        }
                                    } else if (CoreSpringApp.userLogMode != CoreSpringApp.SYSTEM_LOG_NONE) {
                                        logQuery(hmParam)
                                    }

                                    val redirectURL = when (appRequest.action) {
                                        AppAction.SAVE, AppAction.ARCHIVE, AppAction.UNARCHIVE -> page.doSave(appRequest.action, appRequest.alFormData!!, hmOut)
                                        AppAction.DELETE -> page.doDelete(appRequest.alFormData!!, hmOut)
                                        else -> throw Throwable("Unknown action = ${appRequest.action}")
                                    }

                                    appResponse = AppResponse(code = ResponseCode.REDIRECT, redirect = redirectURL)
                                }
                            }
                        }
                    }
                }
            }
        }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        //--- обновить данные в сессии только после успешной записи данных
        chmSession.putAll(hmOut)
        //--- после успешного коммита можно и удалить файл, если указан
        //--- (пока нигде не применяется)
        //if( fileForDeleteAfterCommit != null ) fileForDeleteAfterCommit.delete();

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - appBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long App Query = " + (getCurrentTimeInt() - appBegTime))
            AdvancedLogger.error(appRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return appResponse
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- прописывать у каждого наследника
//    @PostMapping("/api/xy")
    open fun xy(
        //@RequestBody
        xyActionRequest: XyActionRequest
    ): XyActionResponse {
        val xyBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(xyActionRequest.sessionId) { ConcurrentHashMap() }

        //--- набор для накопления выходных параметров.
        //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
        val hmOut = mutableMapOf<String, Any>()
        //--- ссылка на файл, удаляемый после завершения транзакции
        //--- (и, возможно, после успешной контролируемой передачи данных)
        //--- (пока нигде не применяется)
        //File fileForDeleteAfterCommit = null;

//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
        val userConfig: UserConfig = chmSession[iApplication.USER_CONFIG] as? UserConfig ?: throw BusinessException("Не найден пользователь в сессии!")
        val docTypeName = xyActionRequest.documentTypeName
        val xyDocConfig = CoreSpringApp.hmXyDocumentConfig[docTypeName]!!

        val doc = Class.forName(xyDocConfig.serverClassName).getConstructor().newInstance() as sdcXyAbstract
        doc.init(this, conn, stm, chmSession, userConfig, xyDocConfig)

        val xyActionResponse =
            when (xyActionRequest.action) {
                XyAction.GET_COORDS -> doc.getCoords(xyActionRequest.startParamId)
                XyAction.GET_ELEMENTS -> doc.getElements(xyActionRequest)
                XyAction.GET_ONE_ELEMENT -> doc.getOneElement(xyActionRequest)
                XyAction.CLICK_ELEMENT -> doc.clickElement(xyActionRequest)
                XyAction.ADD_ELEMENT -> doc.addElement(xyActionRequest, userConfig.userId)
                XyAction.EDIT_ELEMENT_POINT -> doc.editElementPoint(xyActionRequest)
                XyAction.MOVE_ELEMENTS -> doc.moveElements(xyActionRequest)
            }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        //--- обновить данные в сессии только после успешной записи данных
        chmSession.putAll(hmOut)
        //--- после успешного коммита можно и удалить файл, если указан
        //--- (пока нигде не применяется)
        //if( fileForDeleteAfterCommit != null ) fileForDeleteAfterCommit.delete();

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - xyBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Xy Query = " + (getCurrentTimeInt() - xyBegTime))
            AdvancedLogger.error(xyActionRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return xyActionResponse
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- прописывать у каждого наследника
//    @PostMapping("/api/graphic")
    open fun graphic(
        //@RequestBody
        graphicActionRequest: GraphicActionRequest
    ): GraphicActionResponse {
        val graphicBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)
        val stm = conn.createStatement()

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(graphicActionRequest.sessionId) { ConcurrentHashMap() }

        //--- набор для накопления выходных параметров.
        //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
        val hmOut = mutableMapOf<String, Any>()
        //--- ссылка на файл, удаляемый после завершения транзакции
        //--- (и, возможно, после успешной контролируемой передачи данных)
        //--- (пока нигде не применяется)
        //File fileForDeleteAfterCommit = null;

//                        if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam )
        val userConfig: UserConfig = chmSession[iApplication.USER_CONFIG] as? UserConfig ?: throw BusinessException("Не найден пользователь в сессии!")
        val serverDocumentControlClassName = GraphicDocumentConfig.hmConfig[graphicActionRequest.documentTypeName]!!.serverControlClassName
        val doc = Class.forName(serverDocumentControlClassName).getConstructor().newInstance() as sdcAbstractGraphic
        doc.init(this, conn, stm, chmSession, userConfig, graphicActionRequest.documentTypeName)

        val graphicActionResponse =
            when (graphicActionRequest.action) {
                GraphicAction.GET_COORDS -> doc.doGetCoords(graphicActionRequest.startParamId)
                GraphicAction.GET_ELEMENTS -> doc.doGetElements(graphicActionRequest)
            }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

        stm.close()
        conn.close()

        //--- обновить данные в сессии только после успешной записи данных
        chmSession.putAll(hmOut)
        //--- после успешного коммита можно и удалить файл, если указан
        //--- (пока нигде не применяется)
        //if( fileForDeleteAfterCommit != null ) fileForDeleteAfterCommit.delete();

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - graphicBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Graphic Query = " + (getCurrentTimeInt() - graphicBegTime))
            AdvancedLogger.error(graphicActionRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return graphicActionResponse
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open fun getCompositeResponse(compositeStartData: CompositeStartData): AppResponse = AppResponse(ResponseCode.COMPOSITE)

    protected open fun checkLogSkipAliasPrefix(alias: String): Boolean = false

    //--- для перекрытия классами-наследниками
    protected abstract fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData>

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun addMenu(hmAliasConfig: Map<String, AliasConfig>, hmAliasPerm: Map<String, Set<String>>, alMenu: MutableList<MenuData>, alias: String, isTableMenu: Boolean) {
        if (checkMenuPermission(hmAliasConfig, hmAliasPerm, alias))
            alMenu.add(if (isTableMenu) createTableMenu(hmAliasConfig, alias) else createFormMenu(hmAliasConfig, alias))
    }

    protected fun addSeparator(alMenu: MutableList<MenuData>) {
        alMenu.add(MenuData("", ""))
    }

    protected fun checkMenuPermission(hmAliasConfig: Map<String, AliasConfig>, hmAliasPerm: Map<String, Set<String>>, alias: String): Boolean {
        val ac = hmAliasConfig[alias]
        val hsPerm = hmAliasPerm[alias]
        return ac != null && hsPerm != null && hsPerm.contains(cStandart.PERM_ACCESS)
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getFreeDir(fileName: String): String {
        var i = 0
        NEXT_DIR@
        while (true) {
            //--- отдельная папка для каждого файла - не лучший вариант
            //val newDirName = getRandomInt().toString()
            val newDirName = getFilledNumberString(i, 8)
            val newDir = File(rootDirName, "$FILE_BASE/$newDirName")
            newDir.mkdirs()
            val file = File(newDir, fileName)
            if (file.exists()) {
                i++
                continue@NEXT_DIR
            }
            return newDirName
        }
    }

    private fun checkLogon(conn: CoreAdvancedConnection, aLogin: String, aPassword: String, chmSession: ConcurrentHashMap<String, Any>): ResponseCode {
        val stm = conn.createStatement()

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
                " FROM SYSTEM_users WHERE org_type <> ${OrgType.ORG_TYPE_DIVISION} AND login = '$aLogin' "
        )
        if (rs.next()) {
            userID = rs.getInt(1)
            isDisabled = rs.getInt(2) != 0
            userPassword = rs.getString(3)
            atCount = rs.getInt(4)
            atDay = ZonedDateTime.of(rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), 0, 0, ZoneId.systemDefault()).plus(CoreSpringApp.LOGON_LOCK_TIMEOUT.toLong(), ChronoUnit.MINUTES)
            pwdDay = ZonedDateTime.of(rs.getInt(10), rs.getInt(11), rs.getInt(12), 0, 0, 0, 0, ZoneId.systemDefault()).plus(CoreSpringApp.PASSWORD_LIFE_TIME.toLong(), ChronoUnit.MONTHS)
        }
        rs.close()

        //--- если нет такого юзера
        if (userID == 0) return ResponseCode.LOGON_FAILED
        //--- если пользователь заблокирован администратором
        if (isDisabled) return ResponseCode.LOGON_ADMIN_BLOCKED
        //--- если было много неудачных попыток за установленное время
        if (atCount >= CoreSpringApp.MAX_LOGON_ATTEMPT && toDay.isBefore(atDay)) return ResponseCode.LOGON_SYSTEM_BLOCKED
        //--- пароль неправильный
        if (aPassword != userPassword) {
            setAttemptData(stm, true, toDay, userID)
            return ResponseCode.LOGON_FAILED
        }
        //--- пароль правильный, сбрасываем счетчик
        setAttemptData(stm, false, toDay, userID)
        //--- отдельно сохраняем сохраняем прочие user-info
        //--- (т.к. setAttemptData может быть использован в различных местах без последующего saveUserInfo)
        //saveUserInfo( userID )

        //--- исключение из правил: сразу же записываем в сессию информацию по успешно залогиненному пользователю,
        //--- т.к. эта инфа понадобится в той же команде (для выдачи меню и т.п.)
        reloadUserNames(conn)
        chmSession[iApplication.USER_CONFIG] = UserConfig.getConfig(conn, userID, this)

        stm.close()
        //--- проверяем просроченность пароля
        return if (toDay.isAfter(pwdDay)) {
            ResponseCode.LOGON_SUCCESS_BUT_OLD
        } else {
            ResponseCode.LOGON_SUCCESS
        }
    }

    private fun setAttemptData(stm: CoreAdvancedStatement, incCount: Boolean, toDay: ZonedDateTime, userID: Int) {
        stm.executeUpdate(
            " UPDATE SYSTEM_users SET at_count = ${if (incCount) "at_count + 1" else "0"} , " +
                " at_ye = ${toDay.year} , at_mo = ${toDay.monthValue} , at_da = ${toDay.dayOfMonth} , " +
                " at_ho = ${toDay.hour} , at_mi = ${toDay.minute} WHERE id = $userID "
        )
    }

//    private fun saveUserInfo( userID: Int ) {
//        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
//        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
//        jdbcTemplate.update( " UPDATE SYSTEM_users SET last_ip = '${( selectionKey!!.channel() as SocketChannel ).socket().inetAddress}' WHERE id = $userID " )
//    }

    private fun logQuery(hmParam: Map<String, String>) {
//        if( userLogMode == SYSTEM_LOG_NONE ) return
//        logQuery( getAppParam( hmParam ) )
    }

    private fun logQuery(appParam: CharSequence) {
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

    private fun getAliasConfig(stm: CoreAdvancedStatement, chmSession: ConcurrentHashMap<String, Any>, hmOut: MutableMap<String, Any>): Map<String, AliasConfig> {
        //--- вытаскиваем (или загружаем при необходимости) aliasConfig
        var hmAliasConfig: Map<String, AliasConfig>? = chmSession[CoreSpringApp.ALIAS_CONFIG] as? Map<String, AliasConfig>
        if (hmAliasConfig == null) {
            hmAliasConfig = AliasConfig.getConfig(stm)
            hmOut[CoreSpringApp.ALIAS_CONFIG] = hmAliasConfig
        }
        return hmAliasConfig
    }

    private fun createTableMenu(hmAliasConfig: Map<String, AliasConfig>, alias: String): MenuData {
        val ac = hmAliasConfig[alias]
        return MenuData("${AppParameter.ALIAS}=$alias&${AppParameter.ACTION}=${AppAction.TABLE}", ac?.descr ?: "")
    }

    private fun createFormMenu(hmAliasConfig: Map<String, AliasConfig>, alias: String): MenuData {
        return MenuData("${AppParameter.ALIAS}=$alias&${AppParameter.ACTION}=${AppAction.FORM}&${AppParameter.ID}=0", hmAliasConfig[alias]!!.descr)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override var hmUserFullNames = mapOf<Int, String>()
    override var hmUserShortNames = mapOf<Int, String>()

    @Autowired
    private lateinit var userRepository: UserRepository

    override fun reloadUserNames(conn: CoreAdvancedConnection) {
        val hmFullName = mutableMapOf<Int, String>()
        val hmShortName = mutableMapOf<Int, String>()

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC -> {
                val stm = conn.createStatement()
                val rs = stm.executeQuery(" SELECT id , full_name , short_name FROM SYSTEM_users WHERE id <> 0 ")
                while (rs.next()) {
                    val id = rs.getInt(1)
                    hmFullName[id] = rs.getString(2).trim()
                    hmShortName[id] = rs.getString(3).trim()
                }
                rs.close()
                stm.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SystemUsers.SYSTEM_USERS.ID,
                    SystemUsers.SYSTEM_USERS.FULL_NAME,
                    SystemUsers.SYSTEM_USERS.SHORT_NAME,
                ).from(SystemUsers.SYSTEM_USERS)
                    .where(SystemUsers.SYSTEM_USERS.ID.notEqual(0))
                    .fetch()
                result.forEach { record3 ->
                    record3.getValue(SystemUsers.SYSTEM_USERS.ID)?.let { id ->
                        hmFullName[id] = record3.getValue(SystemUsers.SYSTEM_USERS.FULL_NAME)?.trim() ?: ""
                        hmShortName[id] = record3.getValue(SystemUsers.SYSTEM_USERS.SHORT_NAME)?.trim() ?: ""
                    }
                }
            }

            DataAccessMethodEnum.JPA -> {
                val userEntities = userRepository.findAll()
                userEntities.forEach { userEntity ->
                    if (userEntity.id != 0) {
                        hmFullName[userEntity.id] = userEntity.fullName
                        hmShortName[userEntity.id] = userEntity.shortName
                    }
                }
            }
        }

        hmUserFullNames = hmFullName.toMap()
        hmUserShortNames = hmShortName.toMap()
    }

    override fun loadUserProperies(conn: CoreAdvancedConnection, userId: Int): MutableMap<String, String> {
        val hmUserProperty = mutableMapOf<String, String>()

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val stm = conn.createStatement()
                val rs = stm.executeQuery(" SELECT property_name , property_value FROM SYSTEM_user_property WHERE user_id = $userId ")
                while (rs.next()) {
                    hmUserProperty[rs.getString(1).trim()] = rs.getString(2).trim()
                }
                rs.close()
                stm.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SystemUserProperty.SYSTEM_USER_PROPERTY.PROPERTY_NAME,
                    SystemUserProperty.SYSTEM_USER_PROPERTY.PROPERTY_VALUE,
                ).from(SystemUserProperty.SYSTEM_USER_PROPERTY)
                    .where(SystemUserProperty.SYSTEM_USER_PROPERTY.USER_ID.equal(userId))
                    .fetch()
                result.forEach { record2 ->
                    val name = record2.getValue(SystemUserProperty.SYSTEM_USER_PROPERTY.PROPERTY_NAME)
                    val value = record2.getValue(SystemUserProperty.SYSTEM_USER_PROPERTY.PROPERTY_VALUE)
                    if (name != null && value != null) {
                        hmUserProperty[name] = value
                    }
                }
            }
        }

        return hmUserProperty
    }

//    override fun getUserDTO(userId: Int): UserDTO {
//        val userEntity = userRepository.findByIdOrNull(userId) ?: "User not exist for user_id = $userId".let {
//            AdvancedLogger.error(it)
//            throw Exception(it)
//        }
//
//        return UserDTO(
//            id = userEntity.id,
//            parentId = userEntity.parentId,
//            userId = userEntity.userId,
//            isDisabled = userEntity.isDisabled != 0,
//            orgType = userEntity.orgType,
//            login = userEntity.login,
//            password = userEntity.password,
//            fullName = userEntity.fullName,
//            shortName = userEntity.shortName,
//            atCount = userEntity.atCount,
//            lastLoginDateTime = arrayOf(
//                userEntity.lastLoginDateTime.ye,
//                userEntity.lastLoginDateTime.mo,
//                userEntity.lastLoginDateTime.da,
//                userEntity.lastLoginDateTime.ho,
//                userEntity.lastLoginDateTime.mi,
//                0
//            ),
//            passwordLastChangeDate = arrayOf(
//                userEntity.passwordLastChangeDate.ye,
//                userEntity.passwordLastChangeDate.mo,
//                userEntity.passwordLastChangeDate.da,
//                0,
//                0,
//                0
//            ),
//            eMail = userEntity.eMail,
//            contactInfo = userEntity.contactInfo,
//            fileId = userEntity.fileId,
//            lastIP = userEntity.lastIP,
//        )
//    }
//
}

