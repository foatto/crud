package foatto.mms.core_mms.xy

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.mAbstract
import foatto.mms.core_mms.ObjectSelector
import foatto.sql.CoreAdvancedConnection

class mShowState : mAbstract() {

    private lateinit var os: ObjectSelector

    val columnObject: ColumnInt
        get() = os.columnObject

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "MMS_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        //----------------------------------------------------------------------------------------------------------------------

        os = ObjectSelector()
        os.fillColumns(
            model = this,
            isRequired = true,
            isSelector = true,
            alTableHiddenColumn = alTableHiddenColumn,
            alFormHiddenColumn = alFormHiddenColumn,
            alFormColumn = alFormColumn,
            hmParentColumn = hmParentColumn,
            aSingleObjectMode = false,
            addedStaticColumnCount = -1
        )
    }
}
