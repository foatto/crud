package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataDateTimeInt
import java.time.ZoneId
import java.time.ZonedDateTime

class ColumnDateTimeInt(aTableName: String, aFieldName: String, aCaption: String, aWithSecond: Boolean, aZoneId: ZoneId) :
    ColumnAbstractDateTime(aTableName, aFieldName, aCaption, aWithSecond, aZoneId) {

    override fun getData() = DataDateTimeInt(this)

}
