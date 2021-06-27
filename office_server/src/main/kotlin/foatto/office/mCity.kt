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

        tableName = "OFFICE_city"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        val columnCityName = ColumnString(tableName, "name", "Название", STRING_COLUMN_WIDTH)
        columnCityName.isRequired = true
        columnCityName.setUnique(true, null)

        val columnCityCode = ColumnString(tableName, "phone_code", "Код города", STRING_COLUMN_WIDTH)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)

        addTableColumn(columnCityName)
        addTableColumn(columnCityCode)

        alFormHiddenColumn.add(columnID)

        alFormColumn.add(columnCityName)
        alFormColumn.add(columnCityCode)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnCityName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

//        alChildData.add(ChildData("office_reminder", columnID, true))
//        alChildData.add(ChildData("office_reminder_call", columnID))
//        alChildData.add(ChildData("office_reminder_meet", columnID))
//        alChildData.add(ChildData("office_reminder_call_remember", columnID))
//        alChildData.add(ChildData("office_reminder_input_call", columnID))
//        //alChildData.add( new ChildData( "office_reminder_meeting" , columnID ) ); - неприменимо для совещаний
//        alChildData.add(ChildData("office_reminder_other", columnID))

        alChildData.add(ChildData("office_company", columnID, true))
        alChildData.add(ChildData("office_people", columnID))
//        alChildData.add(ChildData("office_client_not_need", columnID))
//        alChildData.add(ChildData("office_client_in_work", columnID))
//        alChildData.add(ChildData("office_client_out_work", columnID))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_company", "city_id"))
    }
}
