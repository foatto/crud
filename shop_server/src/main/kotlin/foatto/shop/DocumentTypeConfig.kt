package foatto.shop

import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.column.iColumn

object DocumentTypeConfig {

    //--- типы накладных
    const val TYPE_ALL = 0    // псевдо-тип накладной для отображение полного списка накладных
    const val TYPE_IN = 1    // поступление
    const val TYPE_OUT = 2    // реализация
    const val TYPE_MOVE = 3    // перемещение
    const val TYPE_DESTROY = 4    // списание
    const val TYPE_RETURN_OUT = 5    // возврат от покупателя
    const val TYPE_RETURN_IN = 6    // возврат поставщику
    const val TYPE_RESORT = 7    // пересортица

    val hmDocTypeAlias = mutableMapOf<Int, String>()
    val hmAliasDocType = mutableMapOf<String, Int>()
    val hmAliasChild = mutableMapOf<String, String>()
    val hsUseSourWarehouse = mutableSetOf<Int>()
    val hsUseDestWarehouse = mutableSetOf<Int>()
    val hsUseSourCatalog = mutableSetOf<Int>()
    val hsUseDestCatalog = mutableSetOf<Int>()
    val hsUseSourNum = mutableSetOf<Int>()
    val hsUseDestNum = mutableSetOf<Int>()
    val hsUseClient = mutableSetOf<Int>()

    init {
        hmDocTypeAlias[TYPE_ALL] = "shop_doc_all"
        hmDocTypeAlias[TYPE_IN] = "shop_doc_in"
        hmDocTypeAlias[TYPE_OUT] = "shop_doc_out"
        hmDocTypeAlias[TYPE_MOVE] = "shop_doc_move"
        hmDocTypeAlias[TYPE_DESTROY] = "shop_doc_destroy"
        hmDocTypeAlias[TYPE_RETURN_OUT] = "shop_doc_return_out"
        hmDocTypeAlias[TYPE_RETURN_IN] = "shop_doc_return_in"
        hmDocTypeAlias[TYPE_RESORT] = "shop_doc_resort"

        hmAliasDocType["shop_doc_all"] = TYPE_ALL
        hmAliasDocType["shop_doc_in"] = TYPE_IN
        hmAliasDocType["shop_doc_out"] = TYPE_OUT
        hmAliasDocType["shop_doc_move"] = TYPE_MOVE
        hmAliasDocType["shop_doc_destroy"] = TYPE_DESTROY
        hmAliasDocType["shop_doc_return_out"] = TYPE_RETURN_OUT
        hmAliasDocType["shop_doc_return_in"] = TYPE_RETURN_IN
        hmAliasDocType["shop_doc_resort"] = TYPE_RESORT

        hmAliasDocType["shop_doc_content_all"] = TYPE_ALL
        hmAliasDocType["shop_doc_content_in"] = TYPE_IN
        hmAliasDocType["shop_doc_content_out"] = TYPE_OUT
        hmAliasDocType["shop_doc_content_move"] = TYPE_MOVE
        hmAliasDocType["shop_doc_content_destroy"] = TYPE_DESTROY
        hmAliasDocType["shop_doc_content_return_out"] = TYPE_RETURN_OUT
        hmAliasDocType["shop_doc_content_return_in"] = TYPE_RETURN_IN
        hmAliasDocType["shop_doc_content_resort"] = TYPE_RESORT

        hmAliasChild["shop_doc_all"] = "shop_doc_content_all"
        hmAliasChild["shop_doc_in"] = "shop_doc_content_in"
        hmAliasChild["shop_doc_out"] = "shop_doc_content_out"
        hmAliasChild["shop_doc_move"] = "shop_doc_content_move"
        hmAliasChild["shop_doc_destroy"] = "shop_doc_content_destroy"
        hmAliasChild["shop_doc_return_out"] = "shop_doc_content_return_out"
        hmAliasChild["shop_doc_return_in"] = "shop_doc_content_return_in"
        hmAliasChild["shop_doc_resort"] = "shop_doc_content_resort"

        hsUseSourWarehouse.add(TYPE_ALL)
        hsUseSourWarehouse.add(TYPE_OUT)
        hsUseSourWarehouse.add(TYPE_MOVE)
        hsUseSourWarehouse.add(TYPE_DESTROY)
        hsUseSourWarehouse.add(TYPE_RETURN_IN)
        hsUseSourWarehouse.add(TYPE_RESORT)

        hsUseDestWarehouse.add(TYPE_ALL)
        hsUseDestWarehouse.add(TYPE_IN)
        hsUseDestWarehouse.add(TYPE_MOVE)
        hsUseDestWarehouse.add(TYPE_RETURN_OUT)
        //hsUseDestWarehouse.add(  TYPE_RESORT  ); - не показывается, но значение всегда д.б. равно sour_id

        hsUseSourCatalog.add(TYPE_ALL)
        hsUseSourCatalog.add(TYPE_OUT)
        hsUseSourCatalog.add(TYPE_MOVE)
        hsUseSourCatalog.add(TYPE_DESTROY)
        hsUseSourCatalog.add(TYPE_RETURN_IN)
        hsUseSourCatalog.add(TYPE_RESORT)

        hsUseDestCatalog.add(TYPE_ALL)
        hsUseDestCatalog.add(TYPE_IN)
        //hsUseDestCatalog.add(  TYPE_MOVE  ); - не показывается, но значение всегда д.б. равно sour_id
        hsUseDestCatalog.add(TYPE_RETURN_OUT)
        hsUseDestCatalog.add(TYPE_RESORT)

        hsUseSourNum.add(TYPE_ALL)
        hsUseSourNum.add(TYPE_OUT)
        hsUseSourNum.add(TYPE_MOVE)
        hsUseSourNum.add(TYPE_DESTROY)
        hsUseSourNum.add(TYPE_RETURN_IN)
        hsUseSourNum.add(TYPE_RESORT)

        hsUseDestNum.add(TYPE_ALL)
        hsUseDestNum.add(TYPE_IN)
        //hsUseDestNum.add(  TYPE_MOVE  ); - не показывается, но значение всегда д.б. равно sour_num
        hsUseDestNum.add(TYPE_RETURN_OUT)
        //hsUseDestNum.add(  TYPE_RESORT  ); - не показывается, но значение всегда д.б. равно sour_num

        hsUseClient.add(TYPE_ALL)
        hsUseClient.add(TYPE_IN)
        hsUseClient.add(TYPE_OUT)
        hsUseClient.add(TYPE_RETURN_OUT)
        hsUseClient.add(TYPE_RETURN_IN)
    }

    fun fillDocChild(alChildData: MutableList<ChildData>, columnID: iColumn) {
        alChildData.add(ChildData("shop_doc_all", columnID, true))
        alChildData.add(ChildData("shop_doc_out", columnID))
        alChildData.add(ChildData("shop_doc_move", columnID))
        alChildData.add(ChildData("shop_doc_in", columnID))
        alChildData.add(ChildData("shop_doc_resort", columnID))
        alChildData.add(ChildData("shop_doc_return_out", columnID))
        alChildData.add(ChildData("shop_doc_return_in", columnID))
        alChildData.add(ChildData("shop_doc_destroy", columnID))
    }

    fun fillDocContentChild(alChildData: MutableList<ChildData>, columnID: iColumn) {
        alChildData.add(ChildData("shop_doc_content_all", columnID, true))
        alChildData.add(ChildData("shop_doc_content_out", columnID))
        alChildData.add(ChildData("shop_doc_content_move", columnID))
        alChildData.add(ChildData("shop_doc_content_in", columnID))
        alChildData.add(ChildData("shop_doc_content_resort", columnID))
        alChildData.add(ChildData("shop_doc_content_return_out", columnID))
        alChildData.add(ChildData("shop_doc_content_return_in", columnID))
        alChildData.add(ChildData("shop_doc_content_destroy", columnID))
    }
}
