package foatto.mms

import foatto.app.CoreSpringApp
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.PathVariable
import javax.servlet.http.HttpServletResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import java.net.URISyntaxException
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import java.net.URI

@RestController     // = @Controller + @ResponseBody
open class MMSSpringController( aJdbcTemplate: JdbcTemplate ) : CoreSpringController( aJdbcTemplate ) {

    @Value("\${max_enabled_over_speed}")
    val maxEnabledOverSpeed: Int = 0

    @Value("\${expire_period}")
    val expirePeriod: Int = 0

//!!! временная шняга на время разработки del-web -------------------------------

    @RequestMapping(value=[ "/json/tnoconnect", "/json/tnoserverinfo", "/json/tnoping" ])
    @ResponseBody
    @Throws(URISyntaxException::class)
    fun proxyTNOConnect(@RequestBody body: String, method: HttpMethod, request: HttpServletRequest, response: HttpServletResponse): ByteArray {
//println( "request = '$body' " )

        val thirdPartyApi = URI("http", null, "tcn-test.pla.ru", 17997, request.requestURI, request.queryString, null)

        val resp = RestTemplate().exchange(thirdPartyApi, method, HttpEntity(body), String::class.java)
//println( "response = '${resp.body}' " )

        //--- именно с таким явнозаданным извратом, иначе слетает кодировка
        return (resp.body ?: "").toByteArray( Charsets.UTF_8 )
    }

    @GetMapping(value = ["/del_web/{fileName:.+}"])
    fun downloadDelWeb(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/del_web/$fileName" )
    }

    @GetMapping(value = ["/del_web/fonts/{fileName:.+}"])
    fun downloadDelWebFonts(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/del_web/fonts/$fileName" )
    }

    @GetMapping(value = ["/del_web/images/{fileName:.+}"])
    fun downloadDelWebImages(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/del_web/images/$fileName" )
    }

    @GetMapping(value = ["/del_web/js/{fileName:.+}"])
    fun downloadDelWebJS(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/del_web/js/$fileName" )
    }

    @GetMapping(value = ["/del_web/lib/{fileName:.+}"])
    fun downloadDelWebLib(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/del_web/lib/$fileName" )
    }

    @GetMapping(value = ["/del_web/style/{fileName:.+}"])
    fun downloadDelWebStyle(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/del_web/style/$fileName" )
    }

//-------------------------------------------------------------------------------------------------

//!!! сделать статику через nginx и убрать в проекте привязку к tomcat-embed-core-9.0.12.jar ---

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        download(response, "${CoreSpringApp.rootDirName}/web/index.html" )
    }

    @GetMapping(value = ["/files/{dirName:.+}/{fileName:.+}"])
    fun downloadFile(response: HttpServletResponse, @PathVariable("dirName") dirName: String, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/files/$dirName/$fileName" )
    }

    @GetMapping(value = ["/map/{fileName:.+}"])
    fun downloadMaps(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/map/$fileName" )
    }

    @GetMapping(value = ["/reports/{fileName:.+}"])
    fun downloadReports(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/reports/$fileName")
    }

    @GetMapping(value = ["/web/{fileName:.+}"])
    fun downloadWeb(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/web/$fileName" )
    }

    @GetMapping(value = ["/web/images/{fileName:.+}"])
    fun downloadWebImages(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/web/images/$fileName" )
    }

    @GetMapping(value = ["/web/js/{fileName:.+}"])
    fun downloadWebJS(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/web/js/$fileName" )
    }

    @GetMapping(value = ["/web/lib/{fileName:.+}"])
    fun downloadWebLib(response: HttpServletResponse, @PathVariable("fileName") fileName: String ) {
        download(response, "${CoreSpringApp.rootDirName}/web/lib/$fileName" )
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
        return super.app( appRequest )
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

    @PostMapping("/api/xy")
    @Transactional
    override fun xy(
        //authentication: Authentication,
        @RequestBody
        xyActionRequest: XyActionRequest
        //@CookieValue("SESSION") sessionId: String
    ): XyActionResponse {
         return super.xy(xyActionRequest)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    @PostMapping("/api/update")
//    override fun update(
//        @RequestBody
//        updateRequest: UpdateRequest
//    ): UpdateResponse {
//        return super.update( updateRequest )
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

    @PostMapping("/api/get_replication")
    @Transactional
    override fun getReplication(
        @RequestBody
        getReplicationRequest: GetReplicationRequest
    ): GetReplicationResponse {
        return super.getReplication(getReplicationRequest)
    }

    @PostMapping("/api/put_replication")
    @Transactional
    override fun putReplication(
        @RequestBody
        putReplicationRequest: PutReplicationRequest
    ): PutReplicationResponse {
        return super.putReplication(putReplicationRequest)
    }

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
    override fun checkLogSkipAliasPrefix( alias: String ): Boolean {
        return alias.startsWith( "mms_report_" ) || alias.startsWith( "mms_graphic_" ) || alias.startsWith( "mms_show_" )
        //        return alias.startsWith(  "1c_"  ) ||
        //               alias.startsWith(  "ft_report_"  )
    }

   override fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig ): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Учёт -------------------------------------------------------------------------------------------------------

        val alMenuGeneral = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_object", true )

        addSeparator( alMenuGeneral )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_work_shift", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_waybill", true )

        addSeparator( alMenuGeneral )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_day_work", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_shift_work", true )

        addSeparator( alMenuGeneral )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_downtime", true )

        if( alMenuGeneral.size > 3 ) alMenu.add( MenuData( "", "Учёт", alMenuGeneral ) )

        //--- Общие отчёты --------------------------------------------------------------------------------------------------------

        val alMenuCommonReport = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuCommonReport, "mms_report_summary", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuCommonReport, "mms_report_day_work", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuCommonReport, "mms_report_work_shift", false )

        if( alMenuCommonReport.size > 0 ) alMenu.add( MenuData( "", "Общие отчёты", alMenuCommonReport ) )

        //--- Отчёты по передвижной технике --------------------------------------------------------------------------------------------------------

        val alMenuMobileReport = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_waybill", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_waybill_compare", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_summary_without_waybill", false )

        addSeparator( alMenuMobileReport )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_over_speed", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_parking", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_object_zone", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_moving_detail", false )

        addSeparator( alMenuMobileReport )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_downtime", false )

        if( alMenuMobileReport.size > 2 ) alMenu.add( MenuData( "", "Отчёты по передвижной технике", alMenuMobileReport ) )

        //--- Отчёты по оборудованию --------------------------------------------------------------------------------------------------------

        val alMenuWorkReport = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuWorkReport, "mms_report_equip_service", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuWorkReport, "mms_report_work_detail", false )

        if( alMenuWorkReport.size > 0 ) alMenu.add( MenuData( "", "Отчёты по оборудованию", alMenuWorkReport ) )

        //--- Отчёты по топливу --------------------------------------------------------------------------------------------------------

        val alMenuLiquidReport = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuLiquidReport, "mms_report_liquid_inc", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuLiquidReport, "mms_report_liquid_inc_waybill", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuLiquidReport, "mms_report_liquid_dec", false )

        if( alMenuLiquidReport.size > 0 ) alMenu.add( MenuData( "", "Отчёты по топливу", alMenuLiquidReport ) )

        //--- Прочие отчёты --------------------------------------------------------------------------------------------------------

        val alMenuOtherReport = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_over_weight", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_over_turn", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_over_pressure", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_over_temperature", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_over_voltage", false )

        addSeparator( alMenuOtherReport )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_trouble", false )

        addSeparator( alMenuOtherReport )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_data_out", false )

        if( alMenuOtherReport.size > 6 ) alMenu.add( MenuData( "", "Прочие отчёты", alMenuOtherReport ) )

        //--- Графики --------------------------------------------------------------------------------------------------------

        val alMenuGraphic = mutableListOf<MenuData>()
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_liquid", false )

        addSeparator( alMenuGraphic )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_weight", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_turn", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_pressure", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_temperature", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_voltage", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_power", false )

        addSeparator( alMenuGraphic )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_speed", false )

        if( alMenuGraphic.size > 2 ) alMenu.add( MenuData( "", "Графики", alMenuGraphic ) )

        //--- Карта --------------------------------------------------------------------------------------------------------

        val alMenuMap = mutableListOf<MenuData>()
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMap, "mms_show_object", false )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuMap, "mms_show_trace", false )

        //            addSeparator(  alMenuMap  );
        //
        //            addMenu(  hmAliasConfig, hmAliasPerm, alMenuMap, "mms_graphic_weight", false  );

        if( alMenuMap.size > 0 ) alMenu.add( MenuData( "", "Карта", alMenuMap ) )

        //--- Контроль --------------------------------------------------------------------------------------------------------

        val alMenuControl = mutableListOf<MenuData>()
        addMenu( hmAliasConfig, hmAliasPerm, alMenuControl, "mms_show_state", false )

        //            addSeparator(  alMenuMap  );
        //
        //            addMenu(  hmAliasConfig, hmAliasPerm, alMenuMap, "mms_graphic_weight", false  );

        if( alMenuControl.size > 0 ) alMenu.add( MenuData( "", "Контроль", alMenuControl ) )

        //--- Справочники --------------------------------------------------------------------------------------------------------

        val alMenuDir = mutableListOf<MenuData>()
        addMenu( hmAliasConfig, hmAliasPerm, alMenuDir, "mms_department", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuDir, "mms_group", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuDir, "mms_worker", true )

        addSeparator( alMenuDir )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuDir, "mms_zone", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuDir, "mms_user_zone", true )

        if( alMenuDir.size > 1 ) alMenu.add( MenuData( "", "Справочники", alMenuDir ) )

        //--- Устройства ------------------------------------------------------------------------------------------------------

        val alMenuDevice = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_service_order", true )

        addSeparator( alMenuDevice )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_device", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_device_command_history", true )

        addSeparator( alMenuDevice )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_log_journal", true )

        if( alMenuDevice.size > 2 ) alMenu.add( MenuData( "", "Контроллеры", alMenuDevice ) )

        //--- Система --------------------------------------------------------------------------------------------------------

        val alMenuSystem = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_alias", true )

        addSeparator( alMenuSystem )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_role", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_permission", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role_permission", true )

        addSeparator( alMenuSystem )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_log_user", true )

        if( alMenuSystem.size > 2 ) alMenu.add( MenuData( "", "Система", alMenuSystem ) )

        //----------------------------------------------------------------------------------------------------------------------

        return alMenu
    }

}