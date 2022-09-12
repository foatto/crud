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
import foatto.core.util.encodePassword
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
import foatto.core_server.app.server.UserRelation
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.server.document.sdcXyAbstract
import foatto.jooq.core.tables.references.SYSTEM_ALIAS
import foatto.jooq.core.tables.references.SYSTEM_PERMISSION
import foatto.jooq.core.tables.references.SYSTEM_ROLE
import foatto.jooq.core.tables.references.SYSTEM_ROLE_PERMISSION
import foatto.jooq.core.tables.references.SYSTEM_USERS
import foatto.jooq.core.tables.references.SYSTEM_USER_PROPERTY
import foatto.jooq.core.tables.references.SYSTEM_USER_ROLE
import foatto.spring.CoreSpringApp
import foatto.spring.jpa.repositories.UserRepository
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

        //!!! предопределенные roleID - вывести в application.yaml
        //private val ROLE_GUEST = -1
        private val ROLE_ADMIN = -2
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

        val rs = conn.executeQuery(
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

        return alFileStoreData
    }

    override fun saveFile(conn: CoreAdvancedConnection, fileId: Int, idFromClient: Int, fileName: String) {
        val fileFromClient = File(tempDirName, idFromClient.toString())
        val fileStoreId = conn.getNextIntId("SYSTEM_file_store", "id")

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
        conn.executeUpdate(
            """
                INSERT INTO SYSTEM_file_store ( id , file_id , name , dir ) 
                VALUES ( $fileStoreId , $fileId , '$fileName' , '$dirName' ) 
            """
        )
    }

    override fun deleteFile(conn: CoreAdvancedConnection, fileId: Int, id: Int?) {
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

        val rs = conn.executeQuery(sbSQL)
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

        conn.executeUpdate(
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
                    //--- вытаскиваем (или загружаем при необходимости) aliasConfig
                    val hmAliasConfigs = chmSession.getOrPut(CoreSpringApp.ALIAS_CONFIG) { getAliasConfig(conn) } as Map<String, AliasConfig>
                    hmOut[CoreSpringApp.ALIAS_CONFIG] = hmAliasConfigs

                    val userConfig = chmSession[iApplication.USER_CONFIG] as UserConfig

                    appResponse.currentUserName = hmUserFullNames[userConfig.userId] ?: "(неизвестный пользователь)"
                    //--- временно используем List вместо Map, т.к. в Kotlin/JS нет возможности десериализовать Map (а List десериализуется в Array)
                    appResponse.hmUserProperty = loadUserProperies(conn, userConfig.userId).toList().toTypedArray()
                    appResponse.arrMenuData = menuInit(conn, hmAliasConfigs, userConfig).toTypedArray()

                    for ((upKey, upValue) in appRequest.logon!!.hmSystemProperties) {
                        saveUserProperty(conn = conn, userId = null, userConfig = userConfig, upName = upKey, upValue = upValue)
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
                        val aliasName = hmParam[AppParameter.ALIAS] ?: throw BusinessException("Module name is not defined.")

                        //--- вытаскиваем (или загружаем при необходимости) aliasConfig
                        val hmAliasConfigs = chmSession.getOrPut(CoreSpringApp.ALIAS_CONFIG) { getAliasConfig(conn) } as Map<String, AliasConfig>
                        hmOut[CoreSpringApp.ALIAS_CONFIG] = hmAliasConfigs

                        val aliasConfig = hmAliasConfigs[aliasName] ?: throw BusinessException("The module '$aliasName' is not exist.")

                        //--- если класс не требует обязательной аутентификации и нет никакого логина,
                        //--- то подгрузим хотя бы гостевой логин
                        if (!aliasConfig.isAuthorization && userConfig == null) {
                            //--- при отсутствии оного загрузим гостевой логин
                            userConfig = getUserConfig(conn, UserConfig.USER_GUEST)
                            hmOut[iApplication.USER_CONFIG] = userConfig // уйдет в сессию
                        }
                        //--- если класс требует обязательную аутентификацию,
                        //--- а юзер не залогинен или имеет гостевой логин, то запросим авторизацию
                        if (aliasConfig.isAuthorization && (userConfig == null || userConfig.userId == UserConfig.USER_GUEST))
                            appResponse = AppResponse(ResponseCode.LOGON_NEED)
                        else {
                            //--- проверим права доступа на класс
                            if (!userConfig!!.userPermission[aliasName]!!.contains(cStandart.PERM_ACCESS)) {
                                throw BusinessException("Доступ к модулю '${aliasConfig.descr}' не разрешён.")
                            }

                            val page = Class.forName(aliasConfig.controlClassName).getConstructor().newInstance() as cStandart
                            page.init(this, conn, chmSession, hmParam, hmAliasConfigs, aliasConfig, CoreSpringApp.hmXyDocumentConfig, userConfig)
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
                                    //--- (такое бывает редко, только при обновлениях, тем не менее, пользователю обидно),
                                    //--- то сделаем вид что поиска не было:)
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
        doc.init(this, conn, chmSession, userConfig, xyDocConfig)

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
        doc.init(this, conn, chmSession, userConfig, graphicActionRequest.documentTypeName)

        val graphicActionResponse =
            when (graphicActionRequest.action) {
                GraphicAction.GET_COORDS -> doc.doGetCoords(graphicActionRequest.startParamId)
                GraphicAction.GET_ELEMENTS -> doc.doGetElements(graphicActionRequest)
            }

        //--- зафиксировать любые изменения в базе/
        conn.commit()

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

    @PostMapping("/api/save_user_property")
    fun saveUserProperty(
        @RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        val saveUserPropertyBegTime = getCurrentTimeInt()

        val conn = AdvancedConnection(CoreSpringApp.dbConfig)

//        logQuery( hmParam )
        val upName = saveUserPropertyRequest.name
        val upValue = saveUserPropertyRequest.value

        //--- загрузка/создании сессии
        val chmSession = CoreSpringApp.chmSessionStore.getOrPut(saveUserPropertyRequest.sessionId) { ConcurrentHashMap() }
        val userConfig: UserConfig? = chmSession[iApplication.USER_CONFIG] as? UserConfig

        if (userConfig != null) {
            saveUserProperty(conn = conn, userId = null, userConfig = userConfig, upName = upName, upValue = upValue)
        } else {
            AdvancedLogger.error("User config not defined for saved property, name = '$upName', value = '$upValue'.")
        }

        //--- зафиксировать любые изменения в базе/
        conn.commit()
        conn.close()

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - saveUserPropertyBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Save User Property Query = " + (getCurrentTimeInt() - saveUserPropertyBegTime))
            AdvancedLogger.error(saveUserPropertyRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return SaveUserPropertyResponse()
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
    protected abstract fun menuInit(conn: CoreAdvancedConnection, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData>

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
        //--- загрузка данных по активному пользователю с данным логином
        var userID = 0
        var isDisabled = false
        var userPassword: String? = null
        var atCount = 0
        val toDay = ZonedDateTime.now()
        lateinit var atDay: ZonedDateTime
        lateinit var pwdDay: ZonedDateTime
        val rs = conn.executeQuery(
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
            setAttemptData(conn, true, toDay, userID)
            return ResponseCode.LOGON_FAILED
        }
        //--- пароль правильный, сбрасываем счетчик
        setAttemptData(conn, false, toDay, userID)
        //--- отдельно сохраняем сохраняем прочие user-info
        //--- (т.к. setAttemptData может быть использован в различных местах без последующего saveUserInfo)
        //saveUserInfo( userID )

        //--- исключение из правил: сразу же записываем в сессию информацию по успешно залогиненному пользователю,
        //--- т.к. эта инфа понадобится в той же команде (для выдачи меню и т.п.)
        chmSession[iApplication.USER_CONFIG] = getUserConfig(conn, userID)

        //--- проверяем просроченность пароля
        return if (toDay.isAfter(pwdDay)) {
            ResponseCode.LOGON_SUCCESS_BUT_OLD
        } else {
            ResponseCode.LOGON_SUCCESS
        }
    }

    private fun setAttemptData(conn: CoreAdvancedConnection, incCount: Boolean, toDay: ZonedDateTime, userID: Int) {
        conn.executeUpdate(
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

    private fun createTableMenu(hmAliasConfig: Map<String, AliasConfig>, alias: String): MenuData {
        val ac = hmAliasConfig[alias]
        return MenuData("${AppParameter.ALIAS}=$alias&${AppParameter.ACTION}=${AppAction.TABLE}", ac?.descr ?: "")
    }

    private fun createFormMenu(hmAliasConfig: Map<String, AliasConfig>, alias: String): MenuData {
        return MenuData("${AppParameter.ALIAS}=$alias&${AppParameter.ACTION}=${AppAction.FORM}&${AppParameter.ID}=0", hmAliasConfig[alias]!!.descr)
    }

//--- User Config --------------------------------------------------------------------------------------------------------------------------------------------------------

    override var hmUserFullNames = mapOf<Int, String>()
    override var hmUserShortNames = mapOf<Int, String>()

    @Autowired
    private lateinit var userRepository: UserRepository

    override fun loadUserIdList(conn: CoreAdvancedConnection, parentId: Int, orgType: Int): Set<Int> {
        val hsUserId = mutableSetOf<Int>()

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val rs = conn.executeQuery(" SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = $parentId AND org_type = $orgType ")
                while (rs.next()) {
                    hsUserId += rs.getInt(1)
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SYSTEM_USERS.ID,
                ).from(SYSTEM_USERS)
                    .where(SYSTEM_USERS.ID.notEqual(0))
                    .and(SYSTEM_USERS.PARENT_ID.equal(parentId))
                    .and(SYSTEM_USERS.ORG_TYPE.equal(orgType))
                    .fetch()
                result.forEach { record1 ->
                    record1.getValue(SYSTEM_USERS.ID)?.let { userId ->
                        hsUserId += userId
                    }
                }
            }
        }

        return hsUserId
    }

    override fun getUserConfig(conn: CoreAdvancedConnection, userId: Int): UserConfig {
        val (isAdmin, isCleanAdmin) = loadAdminRoles(conn, userId)
        lateinit var userConfig: UserConfig

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val rs = conn.executeQuery(" SELECT parent_id , org_type FROM SYSTEM_users WHERE id = $userId ")
                rs.next()
                userConfig = UserConfig(
                    userId = userId,
                    parentId = rs.getInt(1),
                    orgType = rs.getInt(2),
                    isAdmin = isAdmin,
                    isCleanAdmin = isCleanAdmin,
                    hmUserProperty = loadUserProperies(conn, userId),
                )
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                dslContext.select(
                    SYSTEM_USERS.PARENT_ID,
                    SYSTEM_USERS.ORG_TYPE,
                ).from(SYSTEM_USERS)
                    .where(SYSTEM_USERS.ID.equal(userId))
                    .fetchOne()?.let { record2 ->
                        userConfig = UserConfig(
                            userId = userId,
                            parentId = record2.getValue(SYSTEM_USERS.PARENT_ID) ?: 0,
                            orgType = record2.getValue(SYSTEM_USERS.ORG_TYPE) ?: OrgType.ORG_TYPE_WORKER,
                            isAdmin = isAdmin,
                            isCleanAdmin = isCleanAdmin,
                            hmUserProperty = loadUserProperies(conn, userId),
                        )
                    }
            }
        }

        //--- вторичная загрузка данных
        reloadUserNames(conn)
        loadUserPermission(conn, userConfig)
        loadUserIDList(conn, userConfig)

        return userConfig
    }

    override fun saveUserProperty(conn: CoreAdvancedConnection, userId: Int?, userConfig: UserConfig?, upName: String, upValue: String) {
        val uId = userId ?: userConfig?.userId ?: return

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                if (conn.executeUpdate(" UPDATE SYSTEM_user_property SET property_value = '$upValue' WHERE user_id = $uId AND property_name = '$upName' ") == 0) {
                    conn.executeUpdate(" INSERT INTO SYSTEM_user_property ( user_id , property_name , property_value ) VALUES ( $uId , '$upName' , '$upValue' ) ")
                }
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)

                if (dslContext.update(SYSTEM_USER_PROPERTY)
                        .set(SYSTEM_USER_PROPERTY.PROPERTY_VALUE, upValue)
                        .where(SYSTEM_USER_PROPERTY.USER_ID.equal(uId))
                        .and(SYSTEM_USER_PROPERTY.PROPERTY_NAME.equal(upName))
                        .execute() == 0
                ) {

                    dslContext.insertInto(SYSTEM_USER_PROPERTY, SYSTEM_USER_PROPERTY.USER_ID, SYSTEM_USER_PROPERTY.PROPERTY_NAME, SYSTEM_USER_PROPERTY.PROPERTY_VALUE)
                        .values(uId, upName, upValue)
                        .execute()
                }
            }
        }
        userConfig?.hmUserProperty?.put(upName, upValue)
    }

    //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
    override fun getUserNameColor(isDisabled: Boolean, lastLogonTime: Int): Int {
        val curTime = getCurrentTimeInt()

        return if (isDisabled) {
            cStandart.TABLE_CELL_FORE_COLOR_DISABLED
        } else if (curTime - lastLogonTime > 7 * 24 * 60 * 60) {
            cStandart.TABLE_CELL_FORE_COLOR_CRITICAL
        } else if (curTime - lastLogonTime > 1 * 24 * 60 * 60) {
            cStandart.TABLE_CELL_FORE_COLOR_WARNING
        } else {
            cStandart.TABLE_CELL_FORE_COLOR_NORMAL
        }
    }

    override fun checkAndSetNewPassword(conn: CoreAdvancedConnection, id: Int, pwd: DataString?) {
        var oldPassword = ""
        //--- запомнить старое значение пароля для возможной шифрации нового заданного пароля
        //--- на всякий случай проверка
        if (id != 0) {
            val rs = conn.executeQuery(" SELECT pwd FROM SYSTEM_users WHERE id = $id ")
            if (rs.next()) {
                oldPassword = rs.getString(1)
            }
            rs.close()
        }
        pwd?.let {
            val newPassword = pwd.text
            //--- если пароль не менялся, лишний раз перешифровывать не будем
            if (newPassword != oldPassword) {
                pwd.text = encodePassword(newPassword)
            }
        }
    }

    private fun reloadUserNames(conn: CoreAdvancedConnection) {
        val hmFullName = mutableMapOf<Int, String>()
        val hmShortName = mutableMapOf<Int, String>()

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC -> {
                val rs = conn.executeQuery(" SELECT id , full_name , short_name FROM SYSTEM_users WHERE id <> 0 ")
                while (rs.next()) {
                    val id = rs.getInt(1)
                    hmFullName[id] = rs.getString(2).trim()
                    hmShortName[id] = rs.getString(3).trim()
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SYSTEM_USERS.ID,
                    SYSTEM_USERS.FULL_NAME,
                    SYSTEM_USERS.SHORT_NAME,
                ).from(SYSTEM_USERS)
                    .where(SYSTEM_USERS.ID.notEqual(0))
                    .fetch()
                result.forEach { record3 ->
                    record3.getValue(SYSTEM_USERS.ID)?.let { id ->
                        hmFullName[id] = record3.getValue(SYSTEM_USERS.FULL_NAME)?.trim() ?: ""
                        hmShortName[id] = record3.getValue(SYSTEM_USERS.SHORT_NAME)?.trim() ?: ""
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

    private fun loadUserPermission(conn: CoreAdvancedConnection, userConfig: UserConfig) {
        val userPermissionEnable = mutableMapOf<String, MutableSet<String>>()
        val userPermissionDisable = mutableMapOf<String, MutableSet<String>>()

        //--- загрузить права доступа для этого пользователя
        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val rs = conn.executeQuery(
                    """
                        SELECT SYSTEM_alias.name , SYSTEM_permission.name , SYSTEM_role.name 
                        FROM SYSTEM_alias , SYSTEM_permission , SYSTEM_role_permission , SYSTEM_user_role , SYSTEM_role 
                        WHERE SYSTEM_user_role.user_id = ${userConfig.userId} 
                            AND SYSTEM_role_permission.permission_value = 1 
                            AND SYSTEM_role_permission.role_id = SYSTEM_user_role.role_id 
                            AND SYSTEM_permission.id = SYSTEM_role_permission.permission_id 
                            AND SYSTEM_alias.id = SYSTEM_permission.class_id 
                            AND SYSTEM_role.id = SYSTEM_user_role.role_id
                    """
                )
                //--- чтобы отрицательные роли (с символом "!" в начале названия роли) были последними в списке - для удобства удаления.
                //--- СЮРПРИЗ: PostgreSQL под Ubuntu с кодировкой ru_ru.UTF8 игнорирует знаки препинания при сортировке,
                //--- т.е. образом строки с "!" в начале могут быть где угодно и данный метод отменяется
                //" ORDER BY SYSTEM_role.name DESC ")
                while (rs.next()) {
                    val alias = rs.getString(1).trim()
                    val permName = rs.getString(2).trim()
                    val roleName = rs.getString(3).trim()

                    if (roleName.isEmpty()) {
                        continue
                    }

                    val hsPerm = if (roleName[0] == '!') {
                        userPermissionDisable.getOrPut(alias) { mutableSetOf() }
                    } else {
                        userPermissionEnable.getOrPut(alias) { mutableSetOf() }
                    }
                    hsPerm += permName
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                //--- работает независимо от этой настройки
                //val settings = Settings().withTransformTableListsToAnsiJoin(true)
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES/*, settings*/)
                val result = dslContext.select(
                    SYSTEM_ALIAS.NAME, SYSTEM_PERMISSION.NAME, SYSTEM_ROLE.NAME
                ).from(
                    SYSTEM_ALIAS, SYSTEM_PERMISSION, SYSTEM_ROLE_PERMISSION, SYSTEM_USER_ROLE, SYSTEM_ROLE
                ).where(SYSTEM_USER_ROLE.USER_ID.equal(userConfig.userId))
                    .and(SYSTEM_ROLE_PERMISSION.PERMISSION_VALUE.equal(1))
                    .and(SYSTEM_ROLE_PERMISSION.ROLE_ID.eq(SYSTEM_USER_ROLE.ROLE_ID))
                    .and(SYSTEM_PERMISSION.ID.equal(SYSTEM_ROLE_PERMISSION.PERMISSION_ID))
                    .and(SYSTEM_ALIAS.ID.equal(SYSTEM_PERMISSION.CLASS_ID))
                    .and(SYSTEM_ROLE.ID.equal(SYSTEM_USER_ROLE.ROLE_ID))
                    .fetch()
                //--- чтобы отрицательные роли (с символом "!" в начале названия роли) были последними в списке - для удобства удаления.
                //--- СЮРПРИЗ: PostgreSQL под Ubuntu с кодировкой ru_ru.UTF8 игнорирует знаки препинания при сортировке,
                //--- т.е. образом строки с "!" в начале могут быть где угодно и данный метод отменяется
                //" ORDER BY SYSTEM_role.name DESC ")
                result.forEach { record3 ->
                    val alias = record3.getValue(SYSTEM_ALIAS.NAME)?.trim() ?: ""
                    val permName = record3.getValue(SYSTEM_PERMISSION.NAME)?.trim() ?: ""
                    val roleName = record3.getValue(SYSTEM_ROLE.NAME)?.trim() ?: ""

                    if (roleName.isNotEmpty()) {
                        val hsPerm = if (roleName[0] == '!') {
                            userPermissionDisable.getOrPut(alias) { mutableSetOf() }
                        } else {
                            userPermissionEnable.getOrPut(alias) { mutableSetOf() }
                        }
                        hsPerm += permName
                    }
                }
            }
        }

        userConfig.userPermission.clear()
        //--- добавляем разрешительные права
        userConfig.userPermission.putAll(userPermissionEnable)
        //--- удаляем/вычитаем запретительные права
        for ((alias, hsPerm) in userPermissionDisable) {
            userConfig.userPermission[alias]?.minus(hsPerm)
        }
    }

    //--- загрузка userID других пользователей относительно положения с данным пользователем
    private fun loadUserIDList(conn: CoreAdvancedConnection, userConfig: UserConfig) {
        //--- список всех пользователей,
        //--- из которого путем последовательного исключения основных категорий пользователей
        //--- образуется список пользователей категории "все остальные"
        val hsOtherUsers = loadSubUserList(conn, 0)
        //--- добавить к списку пользователей псевдопользователя с userID == 0
        //--- (чтобы потом правильно обрабатывать (унаследованные) "ничьи" записи)
        hsOtherUsers.add(0)

        //--- ничейное (userId == 0)
        val hsNobodyUserIds = setOf(0)
        hsOtherUsers -= hsNobodyUserIds
        userConfig.hmRelationUser[UserRelation.NOBODY] = hsNobodyUserIds

        //--- свой userId
        val hsSelfUserIds = setOf(userConfig.userId)
        hsOtherUsers -= hsSelfUserIds
        userConfig.hmRelationUser[UserRelation.SELF] = hsSelfUserIds

        //--- userId коллег одного уровня в одном подразделении
        val hsEqualUserIds = loadUserIdList(conn, userConfig.parentId, userConfig.orgType)
        hsOtherUsers -= hsEqualUserIds
        userConfig.hmRelationUser[UserRelation.EQUAL] = hsEqualUserIds

        //--- userId начальников
        val hsBossUserIds = mutableSetOf<Int>()
        var pId = if (userConfig.orgType == OrgType.ORG_TYPE_WORKER) {
            userConfig.parentId
        } else if (userConfig.parentId != 0) {
            getUserParentId(conn, userConfig.parentId)
        } else {
            0
        }
        hsBossUserIds += loadUserIdList(conn, pId, OrgType.ORG_TYPE_BOSS)
        while (pId != 0) {
            pId = getUserParentId(conn, pId)
            hsBossUserIds += loadUserIdList(conn, pId, OrgType.ORG_TYPE_BOSS)
        }
        hsOtherUsers -= hsBossUserIds
        userConfig.hmRelationUser[UserRelation.BOSS] = hsBossUserIds

        //--- userId подчиненных
        val hsWorkerUserIds = mutableSetOf<Int>()
        if (userConfig.orgType == OrgType.ORG_TYPE_BOSS) {
            //--- на своем уровне
            hsWorkerUserIds += loadUserIdList(conn, userConfig.parentId, OrgType.ORG_TYPE_WORKER)
            //--- начальники подчиненных подразделений также являются прямыми подчиненными
            val alDivisionList = loadUserIdList(conn, userConfig.parentId, OrgType.ORG_TYPE_DIVISION).toMutableList()
            //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
            var i = 0
            while (i < alDivisionList.size) {
                val bpId = alDivisionList[i]
                hsWorkerUserIds += loadUserIdList(conn, bpId, OrgType.ORG_TYPE_BOSS)
                hsWorkerUserIds += loadUserIdList(conn, bpId, OrgType.ORG_TYPE_WORKER)

                alDivisionList += loadUserIdList(conn, bpId, OrgType.ORG_TYPE_DIVISION)
                i++
            }
        }
        hsOtherUsers -= hsWorkerUserIds
        userConfig.hmRelationUser[UserRelation.WORKER] = hsWorkerUserIds

        userConfig.hmRelationUser[UserRelation.OTHER] = hsOtherUsers
    }

    private fun loadSubUserList(conn: CoreAdvancedConnection, startPID: Int): MutableSet<Int> {
        val hsUser = mutableSetOf<Int>()

        val alDivisionList = mutableListOf(startPID)

        //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
        var i = 0
        while (i < alDivisionList.size) {
            val pID = alDivisionList[i]
            hsUser += loadUserIdList(conn, pID, OrgType.ORG_TYPE_BOSS)
            hsUser += loadUserIdList(conn, pID, OrgType.ORG_TYPE_WORKER)

            alDivisionList += loadUserIdList(conn, pID, OrgType.ORG_TYPE_DIVISION)
            i++
        }
        return hsUser
    }

    private fun getUserParentId(conn: CoreAdvancedConnection, userId: Int): Int {
        var parentId = 0

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val rs = conn.executeQuery(" SELECT parent_id FROM SYSTEM_users WHERE id = $userId ")
                if (rs.next()) {
                    parentId = rs.getInt(1)
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val record1 = dslContext.select(
                    SYSTEM_USERS.PARENT_ID,
                ).from(SYSTEM_USERS)
                    .where(SYSTEM_USERS.ID.equal(userId))
                    .fetchOne()
                record1?.getValue(SYSTEM_USERS.PARENT_ID)?.let { pid ->
                    parentId = pid
                }
            }
        }

        return parentId
    }

    private fun loadAdminRoles(conn: CoreAdvancedConnection, userId: Int): Pair<Boolean, Boolean> {
        var isAdmin = false
        var roleCount = 0

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                //--- загрузить список ролей пользователя
                val rs = conn.executeQuery(" SELECT role_id FROM SYSTEM_user_role WHERE user_id = $userId ")
                while (rs.next()) {
                    val roleId = rs.getInt(1)
//                    if (roleId == ROLE_GUEST) {
//                        isGuest = true
//                    }
                    if (roleId == ROLE_ADMIN) {
                        isAdmin = true
                    }
                    roleCount++
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SYSTEM_USER_ROLE.ROLE_ID,
                ).from(SYSTEM_USER_ROLE)
                    .where(SYSTEM_USER_ROLE.USER_ID.equal(userId))
                    .fetch()
                result.forEach { record1 ->
                    val roleId = record1.getValue(SYSTEM_USER_ROLE.ROLE_ID)
//                    if (roleId == ROLE_GUEST) {
//                        isGuest = true
//                    }
                    if (roleId == ROLE_ADMIN) {
                        isAdmin = true
                    }
                    roleCount++
                }
            }
        }

        return Pair(isAdmin, isAdmin && roleCount == 1)
    }

    private fun loadUserProperies(conn: CoreAdvancedConnection, userId: Int): MutableMap<String, String> {
        val hmUserProperty = mutableMapOf<String, String>()

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val rs = conn.executeQuery(" SELECT property_name , property_value FROM SYSTEM_user_property WHERE user_id = $userId ")
                while (rs.next()) {
                    hmUserProperty[rs.getString(1).trim()] = rs.getString(2).trim()
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SYSTEM_USER_PROPERTY.PROPERTY_NAME,
                    SYSTEM_USER_PROPERTY.PROPERTY_VALUE,
                ).from(SYSTEM_USER_PROPERTY)
                    .where(SYSTEM_USER_PROPERTY.USER_ID.equal(userId))
                    .fetch()
                result.forEach { record2 ->
                    val name = record2.getValue(SYSTEM_USER_PROPERTY.PROPERTY_NAME)
                    val value = record2.getValue(SYSTEM_USER_PROPERTY.PROPERTY_VALUE)
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

    override fun getAliasConfig(conn: CoreAdvancedConnection, aliasId: Int?, aliasName: String?): Map<String, AliasConfig> {
        val hmAliasConfigs = mutableMapOf<String, AliasConfig>()

        when (currentDataAccessMethod) {
            DataAccessMethodEnum.JDBC, DataAccessMethodEnum.JPA /* not implemented yet */ -> {
                val sSQL =
                    """
                        SELECT id, name, control_name, model_name, descr, authorization_need, 
                        show_row_no, show_user_column, table_page_size, newable, new_auto_read, default_parent_user 
                        FROM SYSTEM_alias "
                        WHERE 
                    """ +
                        if (aliasId != null) {
                            " id = $aliasId "
                        } else if (aliasName != null) {
                            " name = '$aliasName' "
                        } else {
                            " id <> 0 "
                        }

                val rs = conn.executeQuery(sSQL)
                while (rs.next()) {
                    val aliasConfig = AliasConfig(
                        id = rs.getInt(1),
                        name = rs.getString(2),
                        controlClassName = rs.getString(3),
                        modelClassName = rs.getString(4),
                        descr = rs.getString(5),
                        isAuthorization = rs.getInt(6) == 1,
                        isShowRowNo = rs.getInt(7) == 1,
                        isShowUserColumn = rs.getInt(8) == 1,
                        pageSize = rs.getInt(9),
                        isNewable = rs.getInt(10) == 1,
                        isNewAutoRead = rs.getInt(11) == 1,
                        isDefaultParentUser = rs.getInt(12) == 1,
                    )
                    hmAliasConfigs[aliasConfig.name] = aliasConfig
                }
                rs.close()
            }

            DataAccessMethodEnum.JOOQ -> {
                //!!! temporarily two bad ideas at once:
                //--- 1. conn is AdvancedConnection
                //--- 2. used SQLDialect.POSTGRES only
                val dslContext = DSL.using((conn as AdvancedConnection).conn, SQLDialect.POSTGRES)
                val result = dslContext.select(
                    SYSTEM_ALIAS.ID,
                    SYSTEM_ALIAS.NAME,
                    SYSTEM_ALIAS.CONTROL_NAME,
                    SYSTEM_ALIAS.MODEL_NAME,
                    SYSTEM_ALIAS.DESCR,
                    SYSTEM_ALIAS.AUTHORIZATION_NEED,
                    SYSTEM_ALIAS.SHOW_ROW_NO,
                    SYSTEM_ALIAS.SHOW_USER_COLUMN,
                    SYSTEM_ALIAS.TABLE_PAGE_SIZE,
                    SYSTEM_ALIAS.NEWABLE,
                    SYSTEM_ALIAS.NEW_AUTO_READ,
                    SYSTEM_ALIAS.DEFAULT_PARENT_USER,
                ).from(SYSTEM_ALIAS)
                    .where(
                        if (aliasId != null) {
                            SYSTEM_ALIAS.ID.equal(aliasId)
                        } else if (aliasName != null) {
                            SYSTEM_ALIAS.NAME.equal(aliasName)
                        } else {
                            SYSTEM_ALIAS.ID.notEqual(0)
                        }
                    )
                    .fetch()

                result.forEach { record12 ->
                    val aliasConfig = AliasConfig(
                        id = record12.getValue(SYSTEM_ALIAS.ID) ?: 0,
                        name = record12.getValue(SYSTEM_ALIAS.NAME) ?: "",
                        controlClassName = record12.getValue(SYSTEM_ALIAS.CONTROL_NAME) ?: "",
                        modelClassName = record12.getValue(SYSTEM_ALIAS.MODEL_NAME) ?: "",
                        descr = record12.getValue(SYSTEM_ALIAS.DESCR) ?: "",
                        isAuthorization = record12.getValue(SYSTEM_ALIAS.AUTHORIZATION_NEED) == 1,
                        isShowRowNo = record12.getValue(SYSTEM_ALIAS.SHOW_ROW_NO) == 1,
                        isShowUserColumn = record12.getValue(SYSTEM_ALIAS.SHOW_USER_COLUMN) == 1,
                        pageSize = record12.getValue(SYSTEM_ALIAS.TABLE_PAGE_SIZE) ?: 0,
                        isNewable = record12.getValue(SYSTEM_ALIAS.NEWABLE) == 1,
                        isNewAutoRead = record12.getValue(SYSTEM_ALIAS.NEW_AUTO_READ) == 1,
                        isDefaultParentUser = record12.getValue(SYSTEM_ALIAS.DEFAULT_PARENT_USER) == 1,
                    )
                    hmAliasConfigs[aliasConfig.name] = aliasConfig
                }
            }
        }

        return hmAliasConfigs
    }
}

