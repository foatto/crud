package foatto.office

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mCity : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "OFFICE_city"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        val columnCityName = ColumnString(modelTableName, "name", "Название", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        val columnCityCode = ColumnString(modelTableName, "phone_code", "Код города", STRING_COLUMN_WIDTH)

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnCityName)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        addTableColumn(columnCityName)
        addTableColumn(columnCityCode)

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnCityName)
        alFormColumn.add(columnCityCode)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnCityName, true)

        //----------------------------------------------------------------------------------------------------------------------

//        alChildData.add(ChildData("office_reminder", columnID, true))
//        alChildData.add(ChildData("office_reminder_call", columnID))
//        alChildData.add(ChildData("office_reminder_meet", columnID))
//        alChildData.add(ChildData("office_reminder_call_remember", columnID))
//        alChildData.add(ChildData("office_reminder_input_call", columnID))
//        //alChildData.add( new ChildData( "office_reminder_meeting" , columnID ) ); - неприменимо для совещаний
//        alChildData.add(ChildData("office_reminder_other", columnID))

        alChildData.add(ChildData("office_company", columnId, true))
        alChildData.add(ChildData("office_people", columnId))
//        alChildData.add(ChildData("office_client_not_need", columnID))
//        alChildData.add(ChildData("office_client_in_work", columnID))
//        alChildData.add(ChildData("office_client_out_work", columnID))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_company", "city_id"))
    }
}
