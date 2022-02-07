package foatto.core_server.app.system

import foatto.core.app.BUTTON_KEY_EXIT
import foatto.core.app.BUTTON_KEY_SAVE
import foatto.core.app.ICON_NAME_EXIT
import foatto.core.app.ICON_NAME_SAVE
import foatto.core.link.AppAction
import foatto.core.link.FormButton
import foatto.core.link.FormCell
import foatto.core.link.FormData
import foatto.core.link.FormResponse
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.cStandart
import java.util.*

class cRolePermission : cStandart() {

    companion object {
        //--- префикс полей с checkbox'ами
        private val CHECKBOX_FIELD_PREFIX = "p_"

        //--- служебное поле со списком SYSTEM_role_permission.id
        private val ID_LIST_ID = "id_list_id"
    }

    override fun getForm(hmOut: MutableMap<String, Any>): FormResponse {

        val id = getIDFromParam()
        //--- мегаформа установки прав доступа открывается только при попытке их создания,
        //--- иначе запускаем обычную привычную форму
        if (id != 0) {
            return super.getForm(hmOut)
        }

        val refererID = hmParam[AppParameter.REFERER]
        var refererURL: String? = null
        if (refererID != null) {
            refererURL = chmSession[AppParameter.REFERER + refererID] as String
        }

        //--- подготовка "чистого" appParam для кнопок формы
        //--- (простое клонирование исходного hmParam здесь не годится,
        //--- т.к. придёт много попутных мусорных параметров, которые могут внезапно выстрелить где-нибудь
        val formParam = getFormParam()

        //--- начало нестандартной части ---------------------------------------------------------------------------------------

        //--- сбор парентов
        val classID = getParentId("system_alias")
        val roleID = getParentId("system_role")

        var sPerm = " SELECT SYSTEM_alias.descr , SYSTEM_permission.id , SYSTEM_permission.descr " +
            " FROM SYSTEM_alias , SYSTEM_permission WHERE SYSTEM_alias.id = SYSTEM_permission.class_id "

        if (classID != null && classID != 0)
            sPerm += " AND SYSTEM_alias.id = $classID "
        else
            sPerm += " AND SYSTEM_alias.id <> 0 "
        sPerm += " ORDER BY SYSTEM_alias.descr , SYSTEM_permission.descr "

        val alAliasData = mutableListOf<AliasData>()
        var rs = stm.executeQuery(sPerm)
        while (rs.next()) alAliasData.add(AliasData(rs.getString(1), rs.getInt(2), rs.getString(3)))
        rs.close()

        var sRole = " SELECT name , id FROM SYSTEM_role "
        if (roleID != null && roleID != 0)
            sRole += " WHERE id = $roleID "
        else
            sRole += " WHERE id <> 0 "
        sRole += " ORDER BY name "

        val tmRoleData = TreeMap<String, Int>()
        rs = stm.executeQuery(sRole)
        while (rs.next()) tmRoleData[rs.getString(1)] = rs.getInt(2)
        rs.close()

        var sIDList = ""

        //--- окончание нестандартной части ------------------------------------------------------------------------------------

        //--- заголовок формы
        val alHeader = mutableListOf<Pair<String, String>>()
        fillHeader(null, false, alHeader, hmOut)

        //--- основные поля
        val alFormCell = mutableListOf<FormCell>()
        for (ad in alAliasData) {
            val rowCaption = "${ad.aliasDescr} - ${ad.permDescr}"

            for (roleDescr in tmRoleData.keys) {
                val sSQL = " SELECT id , permission_value FROM SYSTEM_role_permission " +
                    " WHERE permission_id = ${ad.permID} " +
                    " AND role_id = ${tmRoleData[roleDescr]} "
                rs = stm.executeQuery(sSQL)
                if (rs.next()) {
                    val pid = rs.getInt(1)
                    val pv = rs.getInt(2)
                    val fieldName = "$CHECKBOX_FIELD_PREFIX$pid"
                    val fieldValue = pv != 0
                    sIDList += (if (sIDList.isEmpty()) "" else ",") + "$pid"

                    //--- основные поля - применяются сокращенные/оптимизированные варианты getFormCell
                    val fci = FormCell(fieldName, fieldValue, emptyArray())

                    fci.itEditable = true
                    fci.caption = rowCaption
                    alFormCell.add(fci)
                }
                rs.close()
            }
        }

        //--- служебное поле со списком ID
        val idListID = getRandomInt().toString()
        hmOut[ID_LIST_ID + idListID] = sIDList

        val alFormButton = mutableListOf<FormButton>()

        alFormButton.add(
            FormButton(
                url = AppParameter.setParam(AppParameter.setParam(formParam, AppParameter.ACTION, AppAction.SAVE), ID_LIST_ID, idListID),
                caption = model.getSaveButonCaption(aliasConfig),
                iconName = ICON_NAME_SAVE,
                withNewData = true,
                key = BUTTON_KEY_SAVE
            )
        )
        if (refererURL != null) {
            alFormButton.add(
                FormButton(
                    url = refererURL,
                    caption = "Выйти",
                    iconName = ICON_NAME_EXIT,
                    withNewData = false,
                    key = BUTTON_KEY_EXIT
                )
            )
        }

        return FormResponse(
            tab = aliasConfig.descr,
            alHeader = alHeader.toTypedArray(),
            columnCount = tmRoleData.size,    // это GRID-форма - укажем кол-во столбцов грид-формы
            alFormColumn = tmRoleData.keys.map { it.replace(' ', '\n') }.toTypedArray(),
            alFormCell = alFormCell.toTypedArray(),
            alFormButton = alFormButton.toTypedArray()
        )
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val id = getIDFromParam()
        //--- мегаформа установки прав доступа сохраняет только при попытке их создания,
        //--- иначе запускаем обычный процесс сохранения
        if (id != 0) return super.doSave(action, alFormData, hmOut)

        val idListID = hmParam[ID_LIST_ID]
        val sIDList = chmSession[ID_LIST_ID + idListID] as String

/*
        //!!! переделать обратно на StringTokenizer
        String[] arrID = sIDList.split( "," );
        for( String pid : arrID )
            dataWorker.alStm.get( 0 ).executeUpdate(
                new StringBuilder( " UPDATE SYSTEM_role_permission SET permission_value = " )
                          .append( bbIn.getBoolean() ? 1 : 0 ).append( " WHERE id = " ).append( pid ).toString() );

 */
        val arrID = sIDList.split(",")
        for (i in arrID.indices)
            stm.executeUpdate(
                " UPDATE SYSTEM_role_permission SET permission_value = ${(if (alFormData[i].booleanValue!!) 1 else 0)} WHERE id = ${arrID[i]} "
            )

        return AppParameter.setParam(chmSession[AppParameter.REFERER + hmParam[AppParameter.REFERER]] as String, AppParameter.ID, id.toString())
    }

    private class AliasData(val aliasDescr: String, val permID: Int, val permDescr: String)
}
