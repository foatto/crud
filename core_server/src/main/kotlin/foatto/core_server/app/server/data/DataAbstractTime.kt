package foatto.core_server.app.server.data

import foatto.core_server.app.server.column.iColumn
import java.time.LocalTime

abstract class DataAbstractTime(aColumn: iColumn) : DataAbstract(aColumn) {

    protected val NULL_TIME = LocalTime.of(0, 0, 0)

    var localTime: LocalTime = NULL_TIME
        protected set

    protected var arrErrorValue: Array<String>? = null

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun setData(data: iData) {
        val dd = data as DataAbstractTime
        localTime = LocalTime.from(dd.localTime)
    }
}