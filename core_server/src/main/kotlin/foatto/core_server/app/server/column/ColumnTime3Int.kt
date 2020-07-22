package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataTime3Int
import java.time.LocalTime

class ColumnTime3Int(aTableName: String, aHourFieldName: String, aMinuteFieldName: String, aSecondFieldName: String?, aCaption: String) : ColumnSimple() {

    val withSecond: Boolean
        get() = alFieldName.size == 3

    var default = LocalTime.now()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        tableName = aTableName
        if(aSecondFieldName == null) {
            addFieldName(aHourFieldName, aMinuteFieldName)
        } else {
            addFieldName(aHourFieldName, aMinuteFieldName, aSecondFieldName)
        }
        caption = aCaption
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getData() = DataTime3Int(this)
}
