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
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnFile
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mTask : mAbstract() {

    companion object {
        const val ALERT_TAG = "task"

        private val MAX_USER_COUNT = 20
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //    public ColumnString getColumnOtherUserName() { return columnOtherUserName; }
    lateinit var columnDate: ColumnDate3Int
        private set
    lateinit var columnTaskSubj: ColumnString
        private set
    lateinit var columnFile: ColumnFile
        private set
    lateinit var columnTaskLastUpdate: ColumnDateTimeInt
        private set

    lateinit var columnOtherUser: ColumnInt
        private set
    val alColumnOtherUser = mutableListOf<ColumnInt>()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- оповещать только при добавлении поручения
    override fun getAddAlertTag() = ALERT_TAG

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        deleteQuestion = "Подтвердите удаление, пожалуйста"

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        val isTaskOwner = aliasConfig.name.startsWith("office_task_out")

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        modelTableName = "OFFICE_task"

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        columnUser = ColumnInt(modelTableName, if (isTaskOwner) "out_user_id" else "in_user_id", userConfig.userId)

        columnActive = ColumnBoolean(modelTableName, "in_active", "", true)
        columnArchive = ColumnBoolean(modelTableName, "in_archive", "", false)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        columnDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Срок").apply {
            formPinMode = FormPinMode.OFF
        }

        columnTaskSubj = ColumnString(modelTableName, "subj", "Тема", 12, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            formPinMode = FormPinMode.OFF
        }

        columnFile = ColumnFile(application, modelTableName, "file_id", "Файлы").apply {
            formPinMode = FormPinMode.OFF
        }

        columnTaskLastUpdate = ColumnDateTimeInt(modelTableName, "last_update", "Последнее обновление", true, zoneId).apply {
            isEditable = false
        }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        val alColumnOtherUserName = mutableListOf<ColumnString>()

        lateinit var columnOtherUserName: ColumnString

        val columnOtherUserId = ColumnInt("SYSTEM_users", "id")
        columnOtherUser = if (id == 0 && isTaskOwner) {
            ColumnInt(modelTableName, "in_user_id")
        } else {
            ColumnInt(modelTableName, if (isTaskOwner) "in_user_id" else "out_user_id", columnOtherUserId)
        }

        //--- multi-worker task attach for task adding
        if (id == 0 && isTaskOwner) {
            for (ui in 0 until MAX_USER_COUNT) {
                val selfLinkUserTableName = "SYSTEM_users_$ui"

                val colOtherUserId = ColumnInt(selfLinkUserTableName, "id").apply {
                    selfLinkTableName = "SYSTEM_users"
                }

                val colOtherUser = ColumnInt(modelTableName, "tmp_user_id_$ui", colOtherUserId, 0)

                val colUserName = ColumnString(selfLinkUserTableName, "full_name", "Кому № ${ui + 1}", STRING_COLUMN_WIDTH).apply {
                    selfLinkTableName = "SYSTEM_users"
                    selectorAlias = "system_user_people"
                    addSelectorColumn(colOtherUser, colOtherUserId)
                    addSelectorColumn(this)
                }

                alColumnOtherUser += colOtherUser
                alColumnOtherUserName += colUserName
            }
        } else {
            columnOtherUserName = ColumnString("SYSTEM_users", "full_name", if (isTaskOwner) "Кому" else "От кого", STRING_COLUMN_WIDTH).apply {
                selectorAlias = "system_user_people"
                addSelectorColumn(columnOtherUser, columnOtherUserId)
                addSelectorColumn(this)
                formPinMode = FormPinMode.OFF
            }
        }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId
        alTableHiddenColumn += columnUser!!
        alTableHiddenColumn += columnActive!!
        alTableHiddenColumn += columnArchive!!
        alTableHiddenColumn += columnOtherUser

        //--- Во избежание позднейших "улучшений/оптимизаций": именно такое "лишнее" сравнение. Сравнивать id != 0 вместо else нельзя, т.к. там может быть и null.
        if (id == 0 && isTaskOwner) {
        } else {
            alTableGroupColumn += columnOtherUserName
        }
//        alTableGroupColumn += columnDate

        addTableColumn(columnDate)
        addTableColumn(columnTaskSubj)
        addTableColumn(columnFile)
        addTableColumn(columnTaskLastUpdate)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnUser!!
        alFormHiddenColumn += columnActive!!
        alFormHiddenColumn += columnArchive!!
        alFormHiddenColumn += columnOtherUser
        //--- multi-worker task attach for task adding
        if (id == 0 && isTaskOwner) {
            alFormHiddenColumn += alColumnOtherUser
        }
        alFormHiddenColumn += columnTaskLastUpdate

        //--- Во избежание позднейших "улучшений/оптимизаций": именно такое "лишнее" сравнение. Сравнивать id != 0 вместо else нельзя, т.к. там может быть и null.
        if (id == 0 && isTaskOwner) {
        } else {
            alFormColumn += columnOtherUserName
        }
        alFormColumn += columnDate
        alFormColumn += columnTaskSubj
        //--- multi-worker task attach for task adding
        if (id == 0 && isTaskOwner) {
            alFormColumn += alColumnOtherUserName
        }
        alFormColumn += columnFile

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        //--- Во избежание позднейших "улучшений/оптимизаций": именно такое "лишнее" сравнение. Сравнивать id != 0 вместо else нельзя, т.к. там может быть и null.
        if (id == 0 && isTaskOwner) {
        } else {
            addTableSort(columnOtherUserName, true)
        }
        addTableSort(columnDate, true)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = if (userConfig.isAdmin) {
            columnUser!!
        }
        //--- особый случай: переход от имени пользователя на исходящие/входящие поручения
        //--- соответственно даст исходящие от меня на него и входящие от него ко мне поручения
        else {
            columnOtherUser
        }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alChildData += ChildData("office_task_thread", columnId, true, true)
        alChildData += ChildData("office_report_task_thread", columnId, AppAction.FORM, true)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alDependData += DependData("OFFICE_task_thread", "task_id", DependData.DELETE)
        //--- прямых связок там нет, но на всякий случай будем обнулять ссылки при удалении поручений
        //--- (т.к. текстовая информация из поручений там всё равно дублируется)
//        alDependData.add(DependData("OFFICE_meeting_plan", "task_id", DependData.SET, 0))
//        alDependData.add(DependData("OFFICE_meeting_result", "task_id", DependData.SET, 0))

    }
}
