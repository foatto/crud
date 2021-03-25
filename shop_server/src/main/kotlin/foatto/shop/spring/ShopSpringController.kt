package foatto.shop.spring

import foatto.core.link.*
import foatto.core.util.AdvancedLogger
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.shop.DocumentTypeConfig
import foatto.shop.iShopApplication
import foatto.shop.spring.repositories.CatalogRepository
import foatto.shop.spring.repositories.DocumentContentRepository
import foatto.shop.spring.repositories.DocumentRepository
import foatto.spring.CoreSpringController
import foatto.sql.CoreAdvancedStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
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
class ShopSpringController : CoreSpringController(), iShopApplication {

    @Value("\${discount_limits}")
    override val discountLimits: Array<String> = emptyArray()

    @Value("\${discount_values}")
    override val discountValues: Array<String> = emptyArray()

    @Value("\${fiscal_once_only}")
    override val fiscalOnceOnly: String? = null

    @Value("\${fiscal_index}")
    override val fiscalIndex: String? = null

    @Value("\${fiscal_urls}")
    override val fiscalUrls: Array<String> = emptyArray()

    @Value("\${fiscal_line_cutters}")
    override val fiscalLineCutters: Array<String> = emptyArray()

    @Value("\${fiscal_cashiers}")
    override val fiscalCashiers: Array<String> = emptyArray()

    @Value("\${fiscal_tax_modes}")
    override val fiscalTaxModes: Array<String> = emptyArray()

    @Value("\${fiscal_place}")
    override val fiscalPlace: String? = null

//!!! сделать статику через nginx и убрать в проекте привязку к tomcat-embed-core-XXX.jar ---

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        download(response, "${rootDirName}/web/index.html")
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

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/app")
    @Transactional
    override fun app(
        //authentication: Authentication,
        @RequestBody
        appRequest: AppRequest
        //@CookieValue("SESSION") sessionId: String
    ): AppResponse {
        return super.app(appRequest)
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
    override fun getReplication(
        @RequestBody
        getReplicationRequest: GetReplicationRequest
    ): GetReplicationResponse {
        return super.getReplication(getReplicationRequest)
    }

    @PostMapping("/api/put_replication")
    override fun putReplication(
        @RequestBody
        putReplicationRequest: PutReplicationRequest
    ): PutReplicationResponse {
        return super.putReplication(putReplicationRequest)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/save_user_property")
    override fun saveUserProperty(
        @RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        return super.saveUserProperty(saveUserPropertyRequest)
    }

    @PostMapping("/api/change_password")
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

    //--- пропускаем логи по запуску модулей из 1С, отчётов и показов картографии
    override fun checkLogSkipAliasPrefix(alias: String): Boolean {
        return alias.startsWith("shop_report_")
        //        return alias.startsWith(  "1c_"  ) ||
        //               alias.startsWith(  "ft_report_"  ) ||
        //               alias.startsWith(  "ft_show_"  );
    }

    override fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Накладные --------------------------------------------------------------------------------------------------------

        val alMenuDocument = mutableListOf<MenuData>()

        if (checkMenuPermission(hmAliasConfig, hmAliasPerm, "shop_doc_out")) {
            hmAliasConfig["shop_doc_out"]?.let { ac ->
                alMenuDocument += MenuData(
                    "${AppParameter.ALIAS}=shop_doc_out&${AppParameter.ACTION}=${AppAction.TABLE}" +
                        "&${AppParameter.PARENT_ALIAS}=shop_warehouse&${AppParameter.PARENT_ID}=582901431", ac.descr + " [Магазин]"
                )
            }
        }
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_out", true)

        addSeparator(alMenuDocument)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_move", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_return_out", true)

        addSeparator(alMenuDocument)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_in", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_resort", true)

        addSeparator(alMenuDocument)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_return_in", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_destroy", true)

        addSeparator(alMenuDocument)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDocument, "shop_doc_all", true)

        if (alMenuDocument.size > 4) alMenu.add(MenuData("", "Накладные", alMenuDocument.toTypedArray()))

        //--- Журналы --------------------------------------------------------------------------------------------------------

        val alMenuJournal = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuJournal, "shop_cash", true)

        if (alMenuJournal.size > 0) alMenu.add(MenuData("", "Журналы", alMenuJournal.toTypedArray()))

        //--- Отчёты --------------------------------------------------------------------------------------------------------

        val alMenuReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_warehouse_state", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_operation_summary", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_cash_history", false)

        addSeparator(alMenuReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_doc_content", false)

        addSeparator(alMenuReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_price_tag", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_price_list", false)

        addSeparator(alMenuReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_operation_history", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_minus_detector", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_doc_error", false)

        if (alMenuReport.size > 3) alMenu.add(MenuData("", "Отчёты", alMenuReport.toTypedArray()))

        //--- Справочники --------------------------------------------------------------------------------------------------------

        val alMenuDir = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_catalog", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_catalog_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_client", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_warehouse", true)

        addSeparator(alMenuDir)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_price", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_price_in", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "shop_price_out", true)

        if (alMenuDir.size > 1) alMenu.add(MenuData("", "Справочники", alMenuDir.toTypedArray()))

        //--- Касса --------------------------------------------------------------------------------------------------------

        val alMenuFiscal = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuFiscal, "shop_fiscal_shift_close", false)

        if (alMenuFiscal.size > 0) alMenu.add(MenuData("", "Касса", alMenuFiscal.toTypedArray()))

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
    private lateinit var catalogRepository: CatalogRepository

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    @Autowired
    private lateinit var documentContentRepository: DocumentContentRepository

    override fun getDocumentDate(docId: Int): Triple<Int, Int, Int> =
        documentRepository.findByIdOrNull(docId)?.let { doc ->
            Triple(doc.date.ye, doc.date.mo, doc.date.da)
        } ?: Triple(0, 1, 1)

    override fun setDocumentDiscount(docId: Int, discount: Double) {
        documentRepository.findByIdOrNull(docId)?.let { doc ->
            doc.discount = discount
            documentRepository.save(doc)
        }
    }

    override fun isDocumentFiscable(docId: Int): Boolean {
        val doc = documentRepository.findByIdOrNull(docId)
        return (doc?.isFiscaled ?: 0) == 0
    }

    override fun checkCatalogMarkable(aCatalogId: Int): Boolean {
        var catalogId = aCatalogId
        while (true) {
            val catalogEntity = catalogRepository.findByIdOrNull(catalogId) ?: "Catalog item not exist for catalog_id = $catalogId".let {
                AdvancedLogger.error(it)
                throw Exception(it)
            }
            //--- it's a markable node(group)/item
            if (catalogEntity.isMark != 0) {
                return true
            }
            //--- it's a top node(group)/item
            if (catalogId == 0) {
                return false
            }
            //-- level up
            catalogId = catalogEntity.parentId
        }
    }

    override fun findIncomeCatalogIdByMark(markCode: String): Int? {
        val documentContentEntities = documentContentRepository.findAllByMarkCode(markCode)
        val lastDocumentContentEntity = documentContentEntities.filter { documentContentEntity ->
            documentContentEntity.isDeleted == 0 &&
                documentContentEntity.document.isDeleted == 0 &&
                documentContentEntity.document.type in setOf(
                DocumentTypeConfig.TYPE_IN,
                DocumentTypeConfig.TYPE_RETURN_OUT,
                DocumentTypeConfig.TYPE_RESORT,
            )
        }
            .maxByOrNull { documentContentEntity ->
                val date = documentContentEntity.document.date
                val type = documentContentEntity.document.type
                date.ye * 6000 + date.mo * 400 + date.da * 10 + getIncomeDocumentTypePriority(type)
            }
        return lastDocumentContentEntity?.destCatalog?.id
    }

    override fun findOutcomeCatalogIdByMark(markCode: String): Int? {
        val documentContentEntities = documentContentRepository.findAllByMarkCode(markCode)
        val lastDocumentContentEntity = documentContentEntities.filter { documentContentEntity ->
            documentContentEntity.isDeleted == 0 &&
                documentContentEntity.document.isDeleted == 0 &&
                documentContentEntity.document.type in setOf(
                DocumentTypeConfig.TYPE_OUT,
                DocumentTypeConfig.TYPE_DESTROY,
                DocumentTypeConfig.TYPE_RETURN_IN,
                //DocumentTypeConfig.TYPE_RESORT, - no rectricts at resort operations
            )
        }
            .maxByOrNull { documentContentEntity ->
                val date = documentContentEntity.document.date
                val type = documentContentEntity.document.type
                date.ye * 6000 + date.mo * 400 + date.da * 10 + getOutcomeDocumentTypePriority(type)
            }
        return lastDocumentContentEntity?.sourCatalog?.id
    }

    private fun getIncomeDocumentTypePriority(docType: Int) =
        when (docType) {
            DocumentTypeConfig.TYPE_IN -> 1
            DocumentTypeConfig.TYPE_RETURN_OUT -> 2
            DocumentTypeConfig.TYPE_RESORT -> 3
            else -> 0
        }

    private fun getOutcomeDocumentTypePriority(docType: Int) =
        when (docType) {
            DocumentTypeConfig.TYPE_OUT -> 2
            DocumentTypeConfig.TYPE_DESTROY -> 2
            DocumentTypeConfig.TYPE_RETURN_IN -> 2
            //DocumentTypeConfig.TYPE_RESORT -> 1   - no rectricts at resort operations
            else -> 0
        }
}