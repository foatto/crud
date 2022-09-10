package foatto.office

import foatto.core.link.AppAction
import foatto.core.link.FormPinMode
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

class mTask : mAbstract() {

    companion object {
        const val ALERT_TAG = "task"
    }

    //    public ColumnString getColumnOtherUserName() { return columnOtherUserName; }
    lateinit var columnDate: ColumnDate3Int
        private set
    lateinit var columnTaskSubj: ColumnString
        private set

    //--- оповещать только при добавлении поручения
    override fun getAddAlertTag() = ALERT_TAG

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        val isTaskOwner = aliasConfig.name.startsWith("office_task_out")

        //----------------------------------------------------------------------------------------

        modelTableName = "OFFICE_task"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        columnUser = ColumnInt(modelTableName, if(isTaskOwner) "out_user_id" else "in_user_id", userConfig.userId)

        columnActive = ColumnBoolean(modelTableName, "in_active", "", true)
        columnArchive = ColumnBoolean(modelTableName, "in_archive", "", false)

        //----------------------------------------------------------------------------------------------------------------------

        val columnOtherUserID = ColumnInt("SYSTEM_users", "id")
        val columnOtherUser = ColumnInt(modelTableName, if(isTaskOwner) "in_user_id" else "out_user_id", columnOtherUserID)

        val columnOtherUserName = ColumnString("SYSTEM_users", "full_name", if(isTaskOwner) "Кому" else "От кого", STRING_COLUMN_WIDTH)
        columnOtherUserName.selectorAlias = "system_user_people"
        columnOtherUserName.addSelectorColumn(columnOtherUser, columnOtherUserID)
        columnOtherUserName.addSelectorColumn(columnOtherUserName)
        columnOtherUserName.formPinMode = FormPinMode.OFF

        columnDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Срок")
        columnDate.formPinMode = FormPinMode.OFF

        columnTaskSubj = ColumnString(modelTableName, "subj", "Тема", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnTaskSubj.formPinMode = FormPinMode.OFF

        val columnFile = ColumnFile(application, modelTableName, "file_id", "Файлы")
        columnFile.formPinMode = FormPinMode.OFF

        //------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnActive!!)
        alTableHiddenColumn.add(columnArchive!!)
        alTableHiddenColumn.add(columnOtherUser)

        alTableGroupColumn.add(columnOtherUserName)
        alTableGroupColumn.add(columnDate)

        addTableColumn(columnTaskSubj)
        addTableColumn(columnFile)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnOtherUser)
        alFormHiddenColumn.add(columnActive!!)
        alFormHiddenColumn.add(columnArchive!!)

        alFormColumn.add(columnOtherUserName)
        alFormColumn.add(columnDate)
        alFormColumn.add(columnTaskSubj)
        alFormColumn.add(columnFile)

        //---------------------------------------------------------------------

        addTableSort(columnOtherUserName, true)
        addTableSort(columnDate, true)

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = if(userConfig.isAdmin) {
            columnUser!!
        }
        //--- особый случай: переход от имени пользователя на исходящие/входящие поручения
        //--- соответственно даст исходящие от меня на него и входящие от него ко мне поручения
        else {
            columnOtherUser
        }

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("office_task_thread", columnId, true, true))
        alChildData.add(ChildData("office_report_task_thread", columnId, AppAction.FORM, true))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_task_thread", "task_id", DependData.DELETE))
        //--- прямых связок там нет, но на всякий случай будем обнулять ссылки при удалении поручений
        //--- (т.к. текстовая информация из поручений там всё равно дублируется)
//        alDependData.add(DependData("OFFICE_meeting_plan", "task_id", DependData.SET, 0))
//        alDependData.add(DependData("OFFICE_meeting_result", "task_id", DependData.SET, 0))

    }
}
