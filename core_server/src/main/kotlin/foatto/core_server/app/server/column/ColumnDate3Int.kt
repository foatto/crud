package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataDate3Int
import java.time.LocalDate

class ColumnDate3Int(
    aTableName: String,
    aYearFieldName: String,
    aMonthFieldName: String,
    aDayFieldName: String,
    aCaption: String
) : ColumnSimple(), iUniqableColumn {

    var default = LocalDate.now()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        columnTableName = aTableName
        addFieldName(aYearFieldName, aMonthFieldName, aDayFieldName)
        caption = aCaption
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataDate3Int(this)

}
