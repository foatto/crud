package foatto.core_server.app.server.column

import java.time.ZoneId
import java.time.ZonedDateTime

abstract class ColumnAbstractDateTime(aTableName: String, aFieldName: String, aCaption: String, aWithSecond: Boolean, aZoneId: ZoneId) : ColumnSimple() {

    var withSecond = aWithSecond
        private set
    var zoneId = aZoneId
        private set

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var default = ZonedDateTime.now(zoneId)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        columnTableName = aTableName
        addFieldName(aFieldName)
        caption = aCaption
    }
}
