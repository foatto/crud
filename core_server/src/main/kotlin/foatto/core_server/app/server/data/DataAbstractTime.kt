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

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun getDateTimeField(alDateTimeField: Array<Pair<String, String>>, withSecond: Boolean): Array<Pair<String, String>> =
        alDateTimeField.toMutableList().apply {
            add(Pair(getFieldCellName(0), if (errorText == null) localTime.hour.toString() else arrErrorValue!![0]))
            add(Pair(getFieldCellName(1), if (errorText == null) (if (localTime.minute < 10) "0" else "") + localTime.minute else arrErrorValue!![1]))
            if (withSecond) {
                add(Pair(getFieldCellName(2), if (errorText == null) (if (localTime.second < 10) "0" else "") + localTime.second else arrErrorValue!![2]))
            }
        }.toTypedArray()
}
