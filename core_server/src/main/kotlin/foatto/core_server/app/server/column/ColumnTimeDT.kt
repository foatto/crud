package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataTimeDT
import java.time.LocalTime

class ColumnTimeDT(aTableName: String, aFieldName: String, aCaption: String, aWithSecond: Boolean = false) : ColumnSimple() {

    val withSecond = aWithSecond

    var default = LocalTime.now()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        tableName = aTableName
        addFieldName(aFieldName)
        caption = aCaption
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataTimeDT(this)
}
