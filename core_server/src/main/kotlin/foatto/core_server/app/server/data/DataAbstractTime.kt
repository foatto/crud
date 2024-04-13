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
        arrErrorValue = null
        errorText = null
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun getDateTimeField(alDateTimeField: List<String>, withSecond: Boolean): List<String> =
        alDateTimeField.toMutableList().apply {
            add(if (errorText == null) localTime.hour.toString() else arrErrorValue!![0])
            add(if (errorText == null) (if (localTime.minute < 10) "0" else "") + localTime.minute else arrErrorValue!![1])
            if (withSecond) {
                add(if (errorText == null) (if (localTime.second < 10) "0" else "") + localTime.second else arrErrorValue!![2])
            }
        }
}
