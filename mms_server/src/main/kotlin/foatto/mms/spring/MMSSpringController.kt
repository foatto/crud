package foatto.mms.spring

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core.util.AdvancedLogger
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.sensor.config.*
import foatto.mms.iMMSApplication
import foatto.mms.spring.repositories.ObjectRepository
import foatto.spring.CoreSpringController
import foatto.sql.CoreAdvancedStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.net.URISyntaxException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class MMSSpringController : CoreSpringController(), iMMSApplication {

    @Value("\${max_enabled_over_speed}")
    override val maxEnabledOverSpeed: Int = 0

    @Value("\${expire_period}")
    override val expirePeriod: Int = 0

    //!!! временная шняга на время разработки del-web -------------------------------

    @RequestMapping(value = ["/json/tnoconnect", "/json/tnoserverinfo", "/json/tnoping"])
    @ResponseBody
    @Throws(URISyntaxException::class)
    fun proxyTNOConnect(
        @RequestBody
        body: String, method: HttpMethod, request: HttpServletRequest, response: HttpServletResponse
    ): ByteArray {
//println( "request = '$body' " )

        val thirdPartyApi = URI("http", null, "tcn-test.pla.ru", 17997, request.requestURI, request.queryString, null)

        val resp = RestTemplate().exchange(thirdPartyApi, method, HttpEntity(body), String::class.java)
//println( "response = '${resp.body}' " )

        //--- именно с таким явнозаданным извратом, иначе слетает кодировка
        return (resp.body ?: "").toByteArray(Charsets.UTF_8)
    }

    @GetMapping(value = ["/del_web/{fileName:.+}"])
    fun downloadDelWeb(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/$fileName")
    }

    @GetMapping(value = ["/del_web/fonts/{fileName:.+}"])
    fun downloadDelWebFonts(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/fonts/$fileName")
    }

    @GetMapping(value = ["/del_web/images/{fileName:.+}"])
    fun downloadDelWebImages(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/images/$fileName")
    }

    @GetMapping(value = ["/del_web/js/{fileName:.+}"])
    fun downloadDelWebJS(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/js/$fileName")
    }

    @GetMapping(value = ["/del_web/lib/{fileName:.+}"])
    fun downloadDelWebLib(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/lib/$fileName")
    }

    @GetMapping(value = ["/del_web/style/{fileName:.+}"])
    fun downloadDelWebStyle(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/style/$fileName")
    }

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

    @GetMapping(value = ["/map/{fileName:.+}"])
    fun downloadMaps(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/map/$fileName")
    }

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

//    @Autowired
//    private lateinit var sensorConfigRepository: SensorConfigRepository

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
    override fun checkLogSkipAliasPrefix(alias: String): Boolean {
        return alias.startsWith("mms_report_") || alias.startsWith("mms_graphic_") || alias.startsWith("mms_show_")
        //        return alias.startsWith(  "1c_"  ) ||
        //               alias.startsWith(  "ft_report_"  )
    }

    override fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Учёт -------------------------------------------------------------------------------------------------------

        val alMenuGeneral = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_object", true)

        addSeparator(alMenuGeneral)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_work_shift", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_waybill", true)

        addSeparator(alMenuGeneral)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_day_work", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGeneral, "mms_shift_work", true)

        if (alMenuGeneral.size > 2) alMenu.add(MenuData("", "Учёт", alMenuGeneral.toTypedArray()))

        //--- Общие отчёты --------------------------------------------------------------------------------------------------------

        val alMenuCommonReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuCommonReport, "mms_report_summary", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuCommonReport, "mms_report_day_work", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuCommonReport, "mms_report_work_shift", false)

        if (alMenuCommonReport.size > 0) alMenu.add(MenuData("", "Общие отчёты", alMenuCommonReport.toTypedArray()))

        //--- Отчёты по передвижной технике --------------------------------------------------------------------------------------------------------

        val alMenuMobileReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_waybill", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_waybill_compare", false)

        addSeparator(alMenuMobileReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_over_speed", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_parking", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_object_zone", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_moving_detail", false)

        addSeparator(alMenuMobileReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuMobileReport, "mms_report_downtime", false)

        if (alMenuMobileReport.size > 2) alMenu.add(MenuData("", "Отчёты по передвижной технике", alMenuMobileReport.toTypedArray()))

        //--- Отчёты по оборудованию --------------------------------------------------------------------------------------------------------

        val alMenuWorkReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuWorkReport, "mms_report_equip_service", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuWorkReport, "mms_report_work_detail", false)

        if (alMenuWorkReport.size > 0) alMenu.add(MenuData("", "Отчёты по оборудованию", alMenuWorkReport.toTypedArray()))

        //--- Отчёты по топливу --------------------------------------------------------------------------------------------------------

        val alMenuLiquidReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuLiquidReport, "mms_report_liquid_inc", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuLiquidReport, "mms_report_liquid_inc_waybill", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuLiquidReport, "mms_report_liquid_dec", false)

        if (alMenuLiquidReport.size > 0) alMenu.add(MenuData("", "Отчёты по топливу", alMenuLiquidReport.toTypedArray()))

        //--- Отчёты по превышениям в энергетике --------------------------------------------------------------------------------------------------------

        val alMenuEnergoOverReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuEnergoOverReport, "mms_report_over_energo_voltage", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuEnergoOverReport, "mms_report_over_energo_current", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuEnergoOverReport, "mms_report_over_energo_power_koef", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuEnergoOverReport, "mms_report_over_energo_power_active", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuEnergoOverReport, "mms_report_over_energo_power_reactive", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuEnergoOverReport, "mms_report_over_energo_power_full", false)

        if (alMenuEnergoOverReport.size > 0) alMenu.add(MenuData("", "Отчёты по превышениям в энергетике", alMenuEnergoOverReport.toTypedArray()))

        //--- Отчёты по прочим превышениям --------------------------------------------------------------------------------------------------------

        val alMenuOtherOverReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_mass_flow", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_volume_flow", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_power", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_density", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_weight", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_turn", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_pressure", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_temperature", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherOverReport, "mms_report_over_voltage", false)

        if (alMenuOtherOverReport.size > 0) alMenu.add(MenuData("", "Отчёты по прочим превышениям", alMenuOtherOverReport.toTypedArray()))

        //--- Прочие отчёты --------------------------------------------------------------------------------------------------------

        val alMenuOtherReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_trouble", false)

        addSeparator(alMenuOtherReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuOtherReport, "mms_report_data_out", false)

        if (alMenuOtherReport.size > 1) alMenu.add(MenuData("", "Прочие отчёты", alMenuOtherReport.toTypedArray()))

        //--- Графики --------------------------------------------------------------------------------------------------------

        val alMenuGraphic = mutableListOf<MenuData>()
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_liquid", false)

        addSeparator(alMenuGraphic)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_energo_power_full", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_energo_power_active", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_energo_power_reactive", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_energo_power_koef", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_energo_voltage", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_energo_current", false)

        addSeparator(alMenuGraphic)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_mass_flow", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_volume_flow", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_weight", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_turn", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_pressure", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_temperature", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_voltage", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_power", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_density", false)

        addSeparator(alMenuGraphic)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuGraphic, "mms_graphic_speed", false)

        if (alMenuGraphic.size > 2) alMenu.add(MenuData("", "Графики", alMenuGraphic.toTypedArray()))

        //--- Карта --------------------------------------------------------------------------------------------------------

        val alMenuMap = mutableListOf<MenuData>()
        addMenu(hmAliasConfig, hmAliasPerm, alMenuMap, "mms_show_object", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuMap, "mms_show_trace", false)

        //            addSeparator(  alMenuMap  );
        //
        //            addMenu(  hmAliasConfig, hmAliasPerm, alMenuMap, "mms_graphic_weight", false  );

        if (alMenuMap.size > 0) alMenu.add(MenuData("", "Карта", alMenuMap.toTypedArray()))

        //--- Контроль --------------------------------------------------------------------------------------------------------

        val alMenuControl = mutableListOf<MenuData>()
        addMenu(hmAliasConfig, hmAliasPerm, alMenuControl, "mms_show_state", false)

        //            addSeparator(  alMenuMap  );
        //
        //            addMenu(  hmAliasConfig, hmAliasPerm, alMenuMap, "mms_graphic_weight", false  );

        if (alMenuControl.size > 0) alMenu.add(MenuData("", "Контроль", alMenuControl.toTypedArray()))

        //--- Справочники --------------------------------------------------------------------------------------------------------

        val alMenuDir = mutableListOf<MenuData>()
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "mms_department", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "mms_group", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "mms_worker", true)

        addSeparator(alMenuDir)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "mms_zone", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "mms_user_zone", true)

        if (alMenuDir.size > 1) alMenu.add(MenuData("", "Справочники", alMenuDir.toTypedArray()))

        //--- Устройства ------------------------------------------------------------------------------------------------------

        val alMenuDevice = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_service_order", true)

        addSeparator(alMenuDevice)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_device", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_device_command_history", true)

        addSeparator(alMenuDevice)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDevice, "mms_log_journal", true)

        if (alMenuDevice.size > 2) alMenu.add(MenuData("", "Контроллеры", alMenuDevice.toTypedArray()))

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

    @Autowired
    private lateinit var objectRepository: ObjectRepository

    override fun getObjectConfig(userConfig: UserConfig, objectId: Int): ObjectConfig {
        val objectEntity = objectRepository.findByIdOrNull(objectId) ?: "ObjectConfig not exist for object_id = $objectId".let {
            AdvancedLogger.error(it)
            throw Exception(it)
        }

        //--- add the name of the object with its short login name
        val shortUserName = userConfig.hmUserShortNames[objectEntity.userId]

        val objectConfig = ObjectConfig(
            objectId = objectId,
            userId = objectEntity.userId,
            isDisabled = objectEntity.isDisabled != 0,
            name = objectEntity.name + if (shortUserName.isNullOrBlank()) "" else " ($shortUserName)",
            model = objectEntity.model,
            groupName = objectEntity.group.name,
            departmentName = objectEntity.department.name,
            info = objectEntity.info,
        )

        //--- fill report title
        objectConfig.alTitleName.add("Владелец:")
        objectConfig.alTitleValue.add(userConfig.hmUserFullNames[objectConfig.userId] ?: "(неизвестно)")
        objectConfig.alTitleName.add("Наименование:")
        objectConfig.alTitleValue.add(objectConfig.name)
        objectConfig.alTitleName.add("Модель:")
        objectConfig.alTitleValue.add(objectConfig.model)
        objectConfig.alTitleName.add("Группа:")
        objectConfig.alTitleValue.add(objectConfig.groupName)
        objectConfig.alTitleName.add("Подразделение:")
        objectConfig.alTitleValue.add(objectConfig.departmentName)

        //--- sensor configuration loading
        //--- (notes: "serial no" and "start date of operation" will be ignored)
        objectEntity.sensors.forEach { sensorEntity ->

            val sensorType = sensorEntity.sensorType
            val portNum = sensorEntity.portNum

            val hmSC = objectConfig.hmSensorConfig.getOrPut(sensorType, { mutableMapOf() })
            when (sensorType) {
                SensorConfig.SENSOR_SIGNAL -> {
                    hmSC[portNum] = SensorConfigSignal(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        boundValue = sensorEntity.boundValue ?: 0,
                        activeValue = sensorEntity.activeValue ?: 0,
                        minIgnore = sensorEntity.minIgnore ?: 0.0,
                        maxIgnore = sensorEntity.maxIgnore ?: 0.0,
                    )
                }
                SensorConfig.SENSOR_GEO -> {
                    objectConfig.scg = SensorConfigGeo(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        minMovingTime = sensorEntity.minMovingTime ?: 0,
                        minParkingTime = sensorEntity.minParkingTime ?: 0,
                        minOverSpeedTime = sensorEntity.minOverSpeedTime ?: 0,
                        isAbsoluteRun = (sensorEntity.isAbsoluteRun ?: 0) != 0,
                        speedRoundRule = sensorEntity.speedRoundRule ?: 0,
                        runKoef = sensorEntity.runKoef ?: 1.0,
                        isUsePos = sensorEntity.isUsePos != 0,
                        isUseSpeed = sensorEntity.isUseSpeed != 0,
                        isUseRun = sensorEntity.isUseRun != 0,
                        liquidName = sensorEntity.liquidName ?: "",
                        liquidNorm = sensorEntity.liquidNorm ?: 0.0,
                        maxSpeedLimit = (sensorEntity.maxLimit ?: 0.0).toInt(),
                    )
                }
                SensorConfig.SENSOR_WORK -> {
                    hmSC[portNum] = SensorConfigWork(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        boundValue = sensorEntity.boundValue ?: 0,
                        activeValue = sensorEntity.activeValue ?: 0,
                        minOnTime = sensorEntity.minOnTime ?: 0,
                        minOffTime = sensorEntity.minOffTime ?: 0,
                        begWorkValue = sensorEntity.begWorkValue ?: 0.0,
                        cmdOnId = sensorEntity.cmdOnId ?: 0,
                        cmdOffId = sensorEntity.cmdOffId ?: 0,
                        signalOn = SignalConfig(sensorEntity.signalOn ?: ""),
                        signalOff = SignalConfig(sensorEntity.signalOff ?: ""),
                        minIgnore = sensorEntity.minIgnore ?: 0.0,
                        maxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        liquidName = sensorEntity.liquidName ?: "",
                        liquidNorm = sensorEntity.liquidNorm ?: 0.0,
                    )
                }
                SensorConfig.SENSOR_LIQUID_USING -> {
                    hmSC[portNum] = SensorConfigCounter(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        isAbsoluteCount = (sensorEntity.isAbsoluteCount ?: 0) != 0,
                        liquidName = sensorEntity.liquidName ?: "",
                    )
                }
                SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE -> {
                    hmSC[portNum] = SensorConfig(
                        id = sensorEntity.id,
                        name = sensorEntity.name,
                        group = sensorEntity.group,
                        descr = sensorEntity.descr,
                        portNum = portNum,
                        sensorType = sensorType,
                    )
                }
                SensorConfig.SENSOR_MASS_ACCUMULATED, SensorConfig.SENSOR_VOLUME_ACCUMULATED -> {
                    hmSC[portNum] = SensorConfigLiquidSummary(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        liquidName = sensorEntity.liquidName ?: "",
                    )
                }
                SensorConfig.SENSOR_ENERGO_COUNT_AD, SensorConfig.SENSOR_ENERGO_COUNT_AR,
                SensorConfig.SENSOR_ENERGO_COUNT_RD, SensorConfig.SENSOR_ENERGO_COUNT_RR -> {
                    hmSC[portNum] = SensorConfigEnergoSummary(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        phase = sensorEntity.phase ?: 0,
                    )
                }
                SensorConfig.SENSOR_LIQUID_FLOW_CALC, SensorConfig.SENSOR_WEIGHT,
                SensorConfig.SENSOR_TURN, SensorConfig.SENSOR_PRESSURE,
                SensorConfig.SENSOR_TEMPERATURE, SensorConfig.SENSOR_VOLTAGE,
                SensorConfig.SENSOR_POWER, SensorConfig.SENSOR_DENSITY,
                SensorConfig.SENSOR_MASS_FLOW, SensorConfig.SENSOR_VOLUME_FLOW -> {
                    hmSC[portNum] = SensorConfigAnalogue(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        minView = sensorEntity.minView ?: 0.0,
                        maxView = sensorEntity.maxView ?: 0.0,
                        minLimit = sensorEntity.minLimit ?: 0.0,
                        maxLimit = sensorEntity.maxLimit ?: 0.0,
                    )
                }
                SensorConfig.SENSOR_ENERGO_VOLTAGE, SensorConfig.SENSOR_ENERGO_CURRENT,
                SensorConfig.SENSOR_ENERGO_POWER_KOEF, SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                SensorConfig.SENSOR_ENERGO_POWER_REACTIVE, SensorConfig.SENSOR_ENERGO_POWER_FULL -> {
                    hmSC[portNum] = SensorConfigEnergoAnalogue(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        phase = sensorEntity.phase ?: 0,
                        aMinView = sensorEntity.minView ?: 0.0,
                        aMaxView = sensorEntity.maxView ?: 0.0,
                        aMinLimit = sensorEntity.minLimit ?: 0.0,
                        aMaxLimit = sensorEntity.maxLimit ?: 0.0,
                    )
                }
                SensorConfig.SENSOR_LIQUID_LEVEL -> {
                    hmSC[portNum] = SensorConfigLiquidLevel(
                        aId = sensorEntity.id,
                        aName = sensorEntity.name,
                        aGroup = sensorEntity.group,
                        aDescr = sensorEntity.descr,
                        aPortNum = portNum,
                        aSensorType = sensorType,
                        aSmoothMethod = sensorEntity.smoothMethod ?: 0,
                        aSmoothTime = (sensorEntity.smoothTime ?: 0) * 60,
                        aMinIgnore = sensorEntity.minIgnore ?: 0.0,
                        aMaxIgnore = sensorEntity.maxIgnore ?: 0.0,
                        liquidName = sensorEntity.liquidName ?: "",
                        aMinView = sensorEntity.minView ?: 0.0,
                        aMaxView = sensorEntity.maxView ?: 0.0,
                        aMinLimit = sensorEntity.minLimit ?: 0.0,
                        aMaxLimit = sensorEntity.maxLimit ?: 0.0,
                        usingMinLen = sensorEntity.usingMinLen ?: 0,
                        isUsingCalc = (sensorEntity.isUsingCalc ?: 0) != 0,
                        detectIncKoef = sensorEntity.detectIncKoef ?: 0.0,
                        detectIncMinDiff = sensorEntity.detectIncMinDiff ?: 0.0,
                        detectIncMinLen = sensorEntity.detectIncMinLen ?: 0,
                        incAddTimeBefore = sensorEntity.incAddTimeBefore ?: 0,
                        incAddTimeAfter = sensorEntity.incAddTimeAfter ?: 0,
                        detectDecKoef = sensorEntity.detectDecKoef ?: 0.0,
                        detectDecMinDiff = sensorEntity.detectDecMinDiff ?: 0.0,
                        detectDecMinLen = sensorEntity.detectDecMinLen ?: 0,
                        decAddTimeBefore = sensorEntity.decAddTimeBefore ?: 0,
                        decAddTimeAfter = sensorEntity.decAddTimeAfter ?: 0,
                    )
                }
                else -> {
                    AdvancedLogger.error("Unknown sensorType = $sensorType for sensorId = ${sensorEntity.id}")
                }
            }

            hmSC[portNum]?.let { sensorConfig ->
                if (sensorConfig is SensorConfigBase) {
                    sensorEntity.calibration
                        //--- sorting set for interpolation
                        .sortedBy { sensorConfigCalibrationEntity -> sensorConfigCalibrationEntity.sensorValue }
                        .forEach { sensorConfigCalibrationEntity ->
                            sensorConfig.alValueSensor.add(sensorConfigCalibrationEntity.sensorValue)
                            sensorConfig.alValueData.add(sensorConfigCalibrationEntity.sensorData)
                        }
                }
            }

        }

        return objectConfig
    }

}