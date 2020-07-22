package foatto.core_server.app.server.column

import foatto.core_server.app.server.data.DataDateTimeDT
import java.time.ZoneId

class ColumnDateTimeDT(aTableName: String, aFieldName: String, aCaption: String, aWithSecond: Boolean, aZoneId: ZoneId) :
    ColumnAbstractDateTime(aTableName, aFieldName, aCaption, aWithSecond, aZoneId) {

    override fun getData() = DataDateTimeDT(this)

}
