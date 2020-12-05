package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core.util.getDateTime
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.column.ColumnInt
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object MMSFunction {

    fun getDayWorkParent(aStm: CoreAdvancedStatement, aHmParentData: MutableMap<String, Int>): IntArray? {
        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val parentDayWork = aHmParentData["mms_day_work"] ?: return null

        val rs = aStm.executeQuery(" SELECT object_id , ye , mo , da FROM MMS_day_work WHERE id = $parentDayWork ")
        rs.next()
        //--- добавляем парента - объект
        aHmParentData["mms_object"] = rs.getInt(1)
        val arrParentData = intArrayOf(rs.getInt(2), rs.getInt(3), rs.getInt(4))
        rs.close()

        return arrParentData
    }

    fun getDayShiftWorkParent(aStm: CoreAdvancedStatement, aZoneId: ZoneId, aHmParentData: MutableMap<String, Int>, isFactTime: Boolean): IntArray {
        lateinit var zdtBeg: ZonedDateTime
        lateinit var zdtEnd: ZonedDateTime

        //--- обработка перехода от рабочих смен, от путёвок, от журнала сменных работ
        val parentShift: Int? = aHmParentData["mms_work_shift"] ?: aHmParentData["mms_waybill"] ?: aHmParentData["mms_shift_work"]

        if (parentShift != null) {
            val rs = aStm.executeQuery(
                " SELECT object_id , " +
                    (if (isFactTime) " beg_dt_fact " else " beg_dt ") + " , " +
                    (if (isFactTime) " end_dt_fact " else " end_dt ") +
                    " FROM MMS_work_shift WHERE id = $parentShift "
            )
            rs.next()
            //--- добавляем парента - объект
            aHmParentData["mms_object"] = rs.getInt(1)

            zdtBeg = getDateTime(aZoneId, rs.getInt(2))
            zdtEnd = getDateTime(aZoneId, rs.getInt(3))
            rs.close()
        } else {
            //--- отдельная обработка перехода от журнала (суточных) пробегов,
            val arrADR = getDayWorkParent(aStm, aHmParentData)
            if (arrADR != null) {
                zdtBeg = ZonedDateTime.of(arrADR[0], arrADR[1], arrADR[2], 0, 0, 0, 0, aZoneId)
                zdtEnd = ZonedDateTime.of(arrADR[0], arrADR[1], arrADR[2], 0, 0, 0, 0, aZoneId).plus(1, ChronoUnit.DAYS)
            } else {
                val today = ZonedDateTime.now()
                zdtBeg = ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, 0, 0, 0, 0, aZoneId)
                zdtEnd = ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, 0, 0, 0, 0, aZoneId).plus(1, ChronoUnit.DAYS)
            }
        }

        return intArrayOf(
            zdtBeg.year, zdtBeg.monthValue, zdtBeg.dayOfMonth, zdtBeg.hour, zdtBeg.minute, zdtBeg.second,
            zdtEnd.year, zdtEnd.monthValue, zdtEnd.dayOfMonth, zdtEnd.hour, zdtEnd.minute, zdtEnd.second
        )
    }

    fun fillAllChildDataForGraphics(columnID: ColumnInt, alChildData: MutableList<ChildData>) {
        fillChildDataForGraphics(
            aliases = listOf(
                "mms_graphic_liquid",
                "mms_graphic_energo_power_full",
                "mms_graphic_energo_power_active",
                "mms_graphic_energo_power_reactive",
                "mms_graphic_energo_power_koef",
                "mms_graphic_energo_voltage",
                "mms_graphic_energo_current",   // График значений электрического тока (э/счётчик)
                "mms_graphic_weight",
                "mms_graphic_turn",
                "mms_graphic_pressure",
                "mms_graphic_temperature",
                "mms_graphic_voltage",
                "mms_graphic_power",
                "mms_graphic_density",
                "mms_graphic_speed",
            ),
            columnID = columnID,
            alChildData = alChildData
        )

    }

    fun fillChildDataForGraphics(aliases: List<String>, columnID: ColumnInt, alChildData: MutableList<ChildData>) {
        var firstItem = true
        aliases.forEach {
            alChildData.add(
                ChildData(
                    aGroup = "Графики",
                    aAlias = it,
                    aColumn = columnID,
                    aAction = AppAction.FORM,
                    aNewGroup = if (firstItem) {
                        firstItem = false
                        true
                    } else {
                        false
                    }
                )
            )
        }
    }
}
