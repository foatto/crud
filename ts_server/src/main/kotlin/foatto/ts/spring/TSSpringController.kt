package foatto.ts.spring

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.spring.CoreSpringController
import foatto.sql.CoreAdvancedStatement
import foatto.ts.iTSApplication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletResponse

@RestController
class TSSpringController : CoreSpringController(), iTSApplication {

//-------------------------------------------------------------------------------------------------

//!!! сделать статику через nginx и убрать в проекте привязку к tomcat-embed-core-9.0.12.jar ---

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        download(response, "${rootDirName}/web/index.html")
    }

    @GetMapping(value = ["/files/{dirName:.+}/{fileName:.+}"])
    fun downloadFile(
        response: HttpServletResponse,
        @PathVariable("dirName")
        dirName: String,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/files/$dirName/$fileName")
    }

//    @GetMapping(value = ["/map/{fileName:.+}"])
//    fun downloadMaps(
//        response: HttpServletResponse,
//        @PathVariable("fileName")
//        fileName: String
//    ) {
//        download(response, "${rootDirName}/map/$fileName")
//    }

    @GetMapping(value = ["/reports/{fileName:.+}"])
    fun downloadReports(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/reports/$fileName")
    }

    @GetMapping(value = ["/web/{fileName:.+}"])
    fun downloadWeb(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/$fileName")
    }

    @GetMapping(value = ["/web/images/{fileName:.+}"])
    fun downloadWebImages(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/images/$fileName")
    }

    @GetMapping(value = ["/web/js/{fileName:.+}"])
    fun downloadWebJS(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/js/$fileName")
    }

    @GetMapping(value = ["/web/lib/{fileName:.+}"])
    fun downloadWebLib(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/lib/$fileName")
    }

//-------------------------------------------------------------------------------------------------

    @PostMapping("/api/app")
    @Transactional
    override fun app(
        //authentication: Authentication,
        @RequestBody
        appRequest: AppRequest
        //@CookieValue("SESSION") sessionId: String
    ): AppResponse {

//        val sensorConfigRepository = Introspector.getBeanInfo(): SensorConfigRepository
//        val sensorConfigEntity = sensorConfigRepository.findByObjectId(0)
//        println("------------------------------------")
//        println(sensorConfigEntity)

        return super.app(appRequest)
    }

    @PostMapping("/api/graphic")
    @Transactional
    override fun graphic(
        //authentication: Authentication,
        @RequestBody
        graphicActionRequest: GraphicActionRequest
        //@CookieValue("SESSION") sessionId: String
    ): GraphicActionResponse {
        return super.graphic(graphicActionRequest)
    }

//    @PostMapping("/api/xy")
//    @Transactional
//    override fun xy(
//        //authentication: Authentication,
//        @RequestBody
//        xyActionRequest: XyActionRequest
//        //@CookieValue("SESSION") sessionId: String
//    ): XyActionResponse {
//        return super.xy(xyActionRequest)
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/get_file")
    override fun getFile(
        @RequestBody
        getFileRequest: GetFileRequest
    ): GetFileResponse {
        return super.getFile(getFileRequest)
    }

    @PostMapping("/api/put_file")
    override fun putFile(
        @RequestBody
        putFileRequest: PutFileRequest
    ): PutFileResponse {
        return super.putFile(putFileRequest)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    @PostMapping("/api/get_replication")
//    @Transactional
//    override fun getReplication(
//        @RequestBody
//        getReplicationRequest: GetReplicationRequest
//    ): GetReplicationResponse {
//        return super.getReplication(getReplicationRequest)
//    }
//
//    @PostMapping("/api/put_replication")
//    @Transactional
//    override fun putReplication(
//        @RequestBody
//        putReplicationRequest: PutReplicationRequest
//    ): PutReplicationResponse {
//        return super.putReplication(putReplicationRequest)
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/save_user_property")
    @Transactional
    override fun saveUserProperty(
        @RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        return super.saveUserProperty(saveUserPropertyRequest)
    }

    @PostMapping("/api/change_password")
    @Transactional
    override fun changePassword(
        @RequestBody
        changePasswordRequest: ChangePasswordRequest
    ): ChangePasswordResponse {
        return super.changePassword(changePasswordRequest)
    }

    @PostMapping("/api/logoff")
    override fun logoff(
        @RequestBody
        logoffRequest: LogoffRequest
    ): LogoffResponse {
        return super.logoff(logoffRequest)
    }

    @PostMapping("/api/upload_form_file")
    override fun uploadFormFile(
        @RequestParam("form_file_ids")
        arrFormFileId: Array<String>,
        @RequestParam("form_file_blobs")
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {
        return super.uploadFormFile(arrFormFileId, arrFormFileBlob)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- пропускаем логи по запуску модулей из 1С, отчётов, графиков, показов картографии
    override fun checkLogSkipAliasPrefix(alias: String): Boolean {
        return alias.startsWith("ts_report_") || alias.startsWith("ts_graphic_") || alias.startsWith("ts_show_")
        //        return alias.startsWith(  "1c_"  ) ||
        //               alias.startsWith(  "ft_report_"  )
    }

    override fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Учёт -------------------------------------------------------------------------------------------------------

        val alMenuGeneral = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "ts_object", true)

        if (alMenuGeneral.size > 0) alMenu.add(MenuData("", "Учёт", alMenuGeneral.toTypedArray()))

        //--- Контроль --------------------------------------------------------------------------------------------------------

        val alMenuControl = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuControl, "ts_show_state", false)

        if (alMenuControl.size > 0) alMenu.add(MenuData("", "Контроль", alMenuControl.toTypedArray()))

        //--- Устройства ------------------------------------------------------------------------------------------------------

        val alMenuDevice = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_device", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_device_command_history", true)

        addSeparator(alMenuDevice)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "ts_log_journal", true)

        if (alMenuDevice.size > 1) alMenu.add(MenuData("", "Приборы", alMenuDevice.toTypedArray()))

        //--- Система --------------------------------------------------------------------------------------------------------

        val alMenuSystem = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_alias", true)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_role", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_permission", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role_permission", true)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_log_user", true)

        if (alMenuSystem.size > 2) alMenu.add(MenuData("", "Система", alMenuSystem.toTypedArray()))

        //----------------------------------------------------------------------------------------------------------------------

        return alMenu
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

}