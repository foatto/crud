package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataDateDT
import java.time.LocalDate

class ColumnDateDT(aTableName: String, aFieldName: String, aCaption: String) : ColumnSimple() {

    var default = LocalDate.now()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        tableName = aTableName
        addFieldName(aFieldName)
        caption = aCaption
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataDateDT(this)

}
