package foatto.office

import foatto.app.CoreSpringController
import foatto.core.link.AppAction
import foatto.core.link.FormPinMode
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
import foatto.sql.CoreAdvancedStatement

class mTask : mAbstract() {

    companion object {
        val ALERT_TAG = "task"
    }

    //    public ColumnString getColumnOtherUserName() { return columnOtherUserName; }
    lateinit var columnDate: ColumnDate3Int
        private set
    lateinit var columnTaskSubj: ColumnString
        private set

    //--- оповещать только при добавлении поручения
    override fun getAddAlertTag(): String? {
        return ALERT_TAG
    }

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val isTaskOwner = aliasConfig.alias.startsWith("office_task_out")

        //----------------------------------------------------------------------------------------

        tableName = "OFFICE_task"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnUser = ColumnInt(tableName, if(isTaskOwner) "out_user_id" else "in_user_id", userConfig.userID)

        columnActive = ColumnBoolean(tableName, "in_active", "", true)
        columnArchive = ColumnBoolean(tableName, "in_archive", "", false)

        //----------------------------------------------------------------------------------------------------------------------

        val columnOtherUserID = ColumnInt("SYSTEM_users", "id")
        val columnOtherUser = ColumnInt(tableName, if(isTaskOwner) "in_user_id" else "out_user_id", columnOtherUserID)

        val columnOtherUserName = ColumnString("SYSTEM_users", "full_name", if(isTaskOwner) "Кому" else "От кого", STRING_COLUMN_WIDTH)
        columnOtherUserName.selectorAlias = "system_user_people"
        columnOtherUserName.addSelectorColumn(columnOtherUser, columnOtherUserID)
        columnOtherUserName.addSelectorColumn(columnOtherUserName)
        columnOtherUserName.formPinMode = FormPinMode.OFF

        columnDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Срок")
        columnDate.formPinMode = FormPinMode.OFF

        columnTaskSubj = ColumnString(tableName, "subj", "Тема", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnTaskSubj.formPinMode = FormPinMode.OFF

        val columnFile = ColumnFile(tableName, "file_id", "Файлы")
        columnFile.formPinMode = FormPinMode.OFF

        //------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnActive!!)
        alTableHiddenColumn.add(columnArchive!!)
        alTableHiddenColumn.add(columnOtherUser)

        alTableGroupColumn.add(columnOtherUserName)
        alTableGroupColumn.add(columnDate)

        addTableColumn(columnTaskSubj)
        addTableColumn(columnFile)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnOtherUser)
        alFormHiddenColumn.add(columnActive!!)
        alFormHiddenColumn.add(columnArchive!!)

        alFormColumn.add(columnOtherUserName)
        alFormColumn.add(columnDate)
        alFormColumn.add(columnTaskSubj)
        alFormColumn.add(columnFile)

        //---------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnOtherUserName)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnDate)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        if(userConfig.isAdmin) hmParentColumn["system_user"] = columnUser!!
        //--- особый случай: переход от имени пользователя на исходящие/входящие поручения
        //--- соответственно даст исходящие от меня на него и входящие от него ко мне поручения
        else hmParentColumn["system_user"] = columnOtherUser

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("office_task_thread", columnID!!, true, true))
        alChildData.add(ChildData("office_report_task_thread", columnID!!, AppAction.FORM, true))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_task_thread", "task_id", DependData.DELETE))
        //--- прямых связок там нет, но на всякий случай будем обнулять ссылки при удалении поручений
        //--- (т.к. текстовая информация из поручений там всё равно дублируется)
        alDependData.add(DependData("OFFICE_meeting_plan", "task_id", DependData.SET, 0))
        alDependData.add(DependData("OFFICE_meeting_result", "task_id", DependData.SET, 0))

    }
}
