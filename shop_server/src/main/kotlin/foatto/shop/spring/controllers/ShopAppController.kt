package foatto.shop.spring.controllers

import foatto.core.link.AppAction
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.MenuData
import foatto.core.util.AdvancedLogger
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.shop.DocumentTypeConfig
import foatto.shop.iShopApplication
import foatto.shop.spring.repositories.CatalogRepository
import foatto.shop.spring.repositories.DocumentContentRepository
import foatto.shop.spring.repositories.DocumentRepository
import foatto.spring.controllers.CoreAppController
import foatto.sql.CoreAdvancedStatement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ShopAppController : CoreAppController(), iShopApplication {

    @Value("\${shop_id}")
    override val shopId: String? = null

    @Value("\${edit_limit_days}")
    override val editLimitDays: String? = null

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

    @Value("\${work_hour.hour_in_work_day}")
    override val workHourInWorkDay: String? = null

    @Value("\${work_hour.hour_in_holy_day}")
    override val workHourInHolyDay: String? = null

    @Value("\${work_hour.user_id}")
    override val alWorkHourUserId: Array<String> = emptyArray()

    @Value("\${work_hour.work_day_hour_tax}")
    override val alWorkDayHourTax: Array<String> = emptyArray()

    @Value("\${work_hour.holy_day_hour_tax}")
    override val alHolyDayHourTax: Array<String> = emptyArray()

    @Value("\${work_hour.sales_percent}")
    override val alWorkHourSalesPercent: Array<String> = emptyArray()

    @Value("\${work_hour.other_share_part}")
    override val otherSharePart: String? = null

    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/app")
    override fun app(
        @RequestBody
        appRequest: AppRequest
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
                        "&${AppParameter.PARENT_ALIAS}=shop_warehouse&${AppParameter.PARENT_ID}=$shopId", ac.descr + " [Магазин]"
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

        if (alMenuDocument.size > 4) {
            alMenu.add(MenuData("", "Накладные", alMenuDocument.toTypedArray()))
        }

        //--- Журналы --------------------------------------------------------------------------------------------------------

        val alMenuJournal = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuJournal, "shop_cash", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuJournal, "shop_gift", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuJournal, "shop_work_hour", true)

        if (alMenuJournal.size > 0) {
            alMenu.add(MenuData("", "Журналы", alMenuJournal.toTypedArray()))
        }

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

        addSeparator(alMenuReport)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "shop_report_work_hour", false)

        if (alMenuReport.size > 4) {
            alMenu.add(MenuData("", "Отчёты", alMenuReport.toTypedArray()))
        }

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

        if (alMenuDir.size > 1) {
            alMenu.add(MenuData("", "Справочники", alMenuDir.toTypedArray()))
        }

        //--- Касса --------------------------------------------------------------------------------------------------------

        val alMenuFiscal = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuFiscal, "shop_fiscal_shift_close", false)

        if (alMenuFiscal.size > 0) {
            alMenu.add(MenuData("", "Касса", alMenuFiscal.toTypedArray()))
        }

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

        if (alMenuSystem.size > 2) {
            alMenu.add(MenuData("", "Система", alMenuSystem.toTypedArray()))
        }

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