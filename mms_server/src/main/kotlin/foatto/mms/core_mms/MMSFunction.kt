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

    fun getDayWorkParent(aStm: CoreAdvancedStatement, aHmParentData: MutableMap<String, Int>): Array<Int>? {
        //--- отдельная обработка перехода от журнала (суточных) пробегов
        val parentDayWork = aHmParentData["mms_day_work"] ?: return null

        val rs = aStm.executeQuery(" SELECT object_id , ye , mo , da FROM MMS_day_work WHERE id = $parentDayWork ")
        rs.next()
        //--- добавляем парента - объект
        aHmParentData["mms_object"] = rs.getInt(1)
        val arrParentData = arrayOf(rs.getInt(2), rs.getInt(3), rs.getInt(4))
        rs.close()

        return arrParentData
    }

    fun getDayShiftWorkParent(
        aStm: CoreAdvancedStatement,
        aZoneId: ZoneId,
        aHmParentData: MutableMap<String, Int>,
        isFactTime: Boolean
    ): Array<Int> {
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

        return arrayOf(
            zdtBeg.year, zdtBeg.monthValue, zdtBeg.dayOfMonth, zdtBeg.hour, zdtBeg.minute, zdtBeg.second,
            zdtEnd.year, zdtEnd.monthValue, zdtEnd.dayOfMonth, zdtEnd.hour, zdtEnd.minute, zdtEnd.second
        )
    }

    fun fillChildDataForPeriodicReports(columnID: ColumnInt, alChildData: MutableList<ChildData>) {
        val aliases = mutableListOf(
            "mms_report_summary",
            "mms_report_summary_bngre",
            "mms_report_day_work",
            "mms_report_work_shift",
            "mms_report_waybill",
            "mms_report_waybill_compare",
        )

        fillChildDataForReports(
            group = "Общие отчёты",
            aliases = aliases,
            columnID = columnID,
            alChildData = alChildData,
            aFirstItem = true,
        )
    }

    fun fillChildDataForLiquidIncDecReports(columnID: ColumnInt, alChildData: MutableList<ChildData>, withIncWaybillReport: Boolean, newGroup: Boolean) {
        val aliases = mutableListOf("mms_report_liquid_inc")
        if (withIncWaybillReport) {
            aliases.add("mms_report_liquid_inc_waybill")
        }
        aliases.add("mms_report_liquid_dec")

        fillChildDataForReports(
            group = "Отчёты по топливу",
            aliases = aliases,
            columnID = columnID,
            alChildData = alChildData,
            aFirstItem = newGroup,
        )
    }

    fun fillChildDataForGeoReports(columnID: ColumnInt, alChildData: MutableList<ChildData>, withMovingDetailReport: Boolean) {
        val aliases = mutableListOf(
            "mms_report_over_speed",
            "mms_report_parking",
            "mms_report_object_zone",
        )
        if (withMovingDetailReport) {
            aliases.add("mms_report_moving_detail")
        }
        fillChildDataForReports(
            group = "Отчёты по передвижной технике",
            aliases = aliases,
            columnID = columnID,
            alChildData = alChildData,
            aFirstItem = false,
        )
    }

    fun fillChildDataForOverReports(columnID: ColumnInt, alChildData: MutableList<ChildData>) {
        fillChildDataForReports(
            group = "Отчёты по прочим превышениям",
            aliases = listOf(
                "mms_report_over_mass_flow",
                "mms_report_over_volume_flow",
                "mms_report_over_power",
                "mms_report_over_density",
                "mms_report_over_weight",
                "mms_report_over_turn",
                "mms_report_over_pressure",
                "mms_report_over_temperature",
                "mms_report_over_voltage",
            ),
            columnID = columnID,
            alChildData = alChildData,
            aFirstItem = false,
        )
    }

    fun fillChildDataForEnergoOverReports(columnID: ColumnInt, alChildData: MutableList<ChildData>) {
        fillChildDataForReports(
            group = "Отчёты по превышениям в энергетике",
            aliases = listOf(
                "mms_report_over_energo_voltage",
                "mms_report_over_energo_current",
                "mms_report_over_energo_power_koef",
                "mms_report_over_energo_power_active",
                "mms_report_over_energo_power_reactive",
                "mms_report_over_energo_power_full",
            ),
            columnID = columnID,
            alChildData = alChildData,
            aFirstItem = false,
        )
    }

    fun fillChildDataForReports(group: String, aliases: List<String>, columnID: ColumnInt, alChildData: MutableList<ChildData>, aFirstItem: Boolean) {
        var firstItem = aFirstItem
        aliases.forEach {
            alChildData.add(
                ChildData(
                    aGroup = group,
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
                "mms_graphic_mass_flow",
                "mms_graphic_volume_flow",
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
