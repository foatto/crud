package foatto.mms.core_mms

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mWorker : mAbstract() {

    override fun init(
        application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_worker"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")
        columnUser = ColumnInt(modelTableName, "user_id", userConfig.userId)

        //----------------------------------------------------------------------------------------------------------------------

        val columnTabNo = ColumnString(modelTableName, "tab_no", "Табельный номер", STRING_COLUMN_WIDTH)
        val columnWorkerName = ColumnString(modelTableName, "name", "Ф.И.О.", STRING_COLUMN_WIDTH)
        columnWorkerName.isRequired = true

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)

        addTableColumn(columnTabNo)
        addTableColumn(columnWorkerName)

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)

        alFormColumn.add(columnTabNo)
        alFormColumn.add(columnWorkerName)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnWorkerName, true)

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData("mms_work_shift", columnId, true))
        alChildData.add(ChildData("mms_waybill", columnId, true))

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("MMS_work_shift", "worker_id"))
    }

}
