package foatto.office

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnFile
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mCompany : mAbstract() {

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "OFFICE_company"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        val columnCompanyBlackList = ColumnBoolean(modelTableName, "in_black_list", "В чёрном списке")
        val columnCompanyName = ColumnString(modelTableName, "name", "Наименование", STRING_COLUMN_WIDTH)

        val columnCityID = ColumnInt("OFFICE_city", "id")
        val columnCity = ColumnInt(modelTableName, "city_id", columnCityID)
        val columnCityName = ColumnString("OFFICE_city", "name", "Город", STRING_COLUMN_WIDTH)
        val columnCityPhoneCode = ColumnString("OFFICE_city", "phone_code", "Код города", STRING_COLUMN_WIDTH)

        columnCityName.selectorAlias = "office_city"
        columnCityName.addSelectorColumn(columnCity, columnCityID)
        columnCityName.addSelectorColumn(columnCityName)
        columnCityName.addSelectorColumn(columnCityPhoneCode)

        val columnCompanyAddress = ColumnString(modelTableName, "address", "Адрес компании", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        val columnCompanyContactInfo = ColumnString(modelTableName, "contact_info", "Доп. информация по компании", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        val columnCompanyBirthDate = ColumnDate3Int(modelTableName, "birth_ye", "birth_mo", "birth_da", "Дата образования компании")
        val columnFile = ColumnFile(application, modelTableName, "file_id", "Файлы")

        //----------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnCity)
        //--- показывать не надо, но для селектора необходимо наличие в выборке
        alTableHiddenColumn.add(columnCityPhoneCode)
        alTableHiddenColumn.add(columnCompanyAddress)
        alTableHiddenColumn.add(columnCompanyContactInfo)
        alTableHiddenColumn.add(columnCompanyBirthDate)

        addTableColumn(columnCompanyBlackList)
        addTableColumn(columnCompanyName)
        addTableColumn(columnCityName)
        //        addTableColumn( columnFile );

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnCity)

        alFormColumn.add(columnCompanyBlackList)
        alFormColumn.add(columnCompanyName)
        alFormColumn.add(columnCompanyAddress)
        alFormColumn.add(columnCompanyContactInfo)
        alFormColumn.add(columnCityName)
        alFormColumn.add(columnCityPhoneCode)
        alFormColumn.add(columnCompanyBirthDate)
        alFormColumn.add(columnFile)

        //---------------------------------------------------------------------

        addTableSort(columnCompanyName, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["office_city"] = columnCity

        //----------------------------------------------------------------------------------------

//        alChildData.add(ChildData("office_reminder", columnID, true))
//        alChildData.add(ChildData("office_reminder_call", columnID))
//        alChildData.add(ChildData("office_reminder_meet", columnID))
//        alChildData.add(ChildData("office_reminder_call_remember", columnID))
//        alChildData.add(ChildData("office_reminder_input_call", columnID))
//        //alChildData.add( new ChildData( "office_reminder_meeting" , columnID ) ); - неприменимо для совещаний
//        alChildData.add(ChildData("office_reminder_other", columnID))

        alChildData.add(ChildData("office_people", columnId, true))
//        alChildData.add(ChildData("office_client_not_need", columnID))
//        alChildData.add(ChildData("office_client_in_work", columnID))
//        alChildData.add(ChildData("office_client_out_work", columnID))
//        alChildData.add(ChildData("office_client_work_view", columnID, true))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_people", "company_id"))

    }

}
