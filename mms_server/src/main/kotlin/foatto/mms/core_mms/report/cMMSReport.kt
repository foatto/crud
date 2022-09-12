package foatto.mms.core_mms.report

import foatto.core.util.DateTime_DMY
import foatto.core.util.DateTime_DMYHMS
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractReport
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataTime3Int
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.UODGSelector
import foatto.mms.core_mms.ZoneData
import foatto.sql.CoreAdvancedConnection
import jxl.write.Label
import jxl.write.WritableSheet

abstract class cMMSReport : cAbstractReport() {

    //--- стандартные ширины столбцов
    // NNN                  = "N п/п"               = 5
    // dd.mm.yyyy hh:mm:ss  = "начало/окончание"    = 16    - в одну строку
    // dd.mm.yyyy hh:mm:ss  = "начало/окончание"    = 9     - в две строки
    // hhhh:mm:ss           = "длитель-ность"       = 9
    // A999AA116RUS         = "объект/скважина"    >= 20 ( нельзя уменьшить менее 20? )
    // A999AA116RUS         = "датчик/оборуд."     <= 20 ( можно уменьшить до 15? )
    // 9999.9               = "пробег"              = 7
    // 9999.9               = "время работы"        = 7
    // dd.mm.yyyy           = "дата"                = 9
    // АИ-95 ( осн. )( изм. )   = "наим. жидкости"  = 15
    // 9999.9               = "расход жидкости"     = 7

    protected fun fillReportParam(uodg: UODGSelector) {
        hmReportParam["report_object_user"] = (hmColumnData[uodg.columnObjectUser] as DataComboBox).intValue
        hmReportParam["report_object"] = (hmColumnData[uodg.columnObject] as DataInt).intValue
        hmReportParam["report_department"] = (hmColumnData[uodg.columnDepartment] as DataInt).intValue
        hmReportParam["report_group"] = (hmColumnData[uodg.columnGroup] as DataInt).intValue
    }

    protected fun fillReportParam(sros: SummaryReportOptionSelector) {
        hmReportParam["report_keep_place_for_comment"] = (hmColumnData[sros.columnKeepPlaceForComment] as DataBoolean).value
        hmReportParam["report_out_liquid_level_main_container_using"] = (hmColumnData[sros.columnOutLiquidLevelMainContainerUsing] as DataBoolean).value
        hmReportParam["report_out_temperature"] = (hmColumnData[sros.columnOutTemperature] as DataBoolean).value
        hmReportParam["report_out_density"] = (hmColumnData[sros.columnOutDensity] as DataBoolean).value
        hmReportParam["report_out_troubles"] = (hmColumnData[sros.columnOutTroubles] as DataBoolean).value
    }

    protected fun fillReportParam(sos: SumOptionSelector) {
        hmReportParam["report_out_group_sum"] = (hmColumnData[sos.columnOutGroupSum] as DataBoolean).value
        hmReportParam["report_sum_only"] = (hmColumnData[sos.columnSumOnly] as DataBoolean).value
        hmReportParam["report_sum_user"] = (hmColumnData[sos.columnSumUser] as DataBoolean).value
        hmReportParam["report_sum_object"] = (hmColumnData[sos.columnSumObject] as DataBoolean).value
    }

    protected fun fillReportParam(m: mOP) {
        hmReportParam["report_object"] = (hmColumnData[m.columnReportObject] as DataInt).intValue

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val begTime = (hmColumnData[m.columnReportBegTime] as DataTime3Int).localTime
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate
        val endTime = (hmColumnData[m.columnReportEndTime] as DataTime3Int).localTime

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth
        hmReportParam["report_beg_hour"] = begTime.hour
        hmReportParam["report_beg_minute"] = begTime.minute

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth
        hmReportParam["report_end_hour"] = endTime.hour
        hmReportParam["report_end_minute"] = endTime.minute
    }

    protected fun fillReportParam(m: mUODG) {
        fillReportParam(m.uodg)
    }

    protected fun fillReportParam(m: mUODGD) {
        fillReportParam(m.uodg)

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth
    }

    protected fun fillReportParam(m: mUODGP) {
        fillReportParam(m.uodg)

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val begTime = (hmColumnData[m.columnReportBegTime] as DataTime3Int).localTime
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate
        val endTime = (hmColumnData[m.columnReportEndTime] as DataTime3Int).localTime

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth
        hmReportParam["report_beg_hour"] = begTime.hour
        hmReportParam["report_beg_minute"] = begTime.minute

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth
        hmReportParam["report_end_hour"] = endTime.hour
        hmReportParam["report_end_minute"] = endTime.minute
    }

    protected fun fillReportParam(m: mUODGPZ) {
        fillReportParam(m.uodg)

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val begTime = (hmColumnData[m.columnReportBegTime] as DataTime3Int).localTime
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate
        val endTime = (hmColumnData[m.columnReportEndTime] as DataTime3Int).localTime

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth
        hmReportParam["report_beg_hour"] = begTime.hour
        hmReportParam["report_beg_minute"] = begTime.minute

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth
        hmReportParam["report_end_hour"] = endTime.hour
        hmReportParam["report_end_minute"] = endTime.minute

        hmReportParam["report_zone"] = (hmColumnData[m.columnReportZone] as DataInt).intValue
    }

    protected fun fillReportTitleWithDate(sheet: WritableSheet): Int {
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int

        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        return fillReportTitle(
            aliasConfig.descr,
            reportBegYear, reportBegMonth, reportBegDay,
            reportEndYear, reportEndMonth, reportEndDay,
            sheet, 1, 0
        )
    }

    private fun fillReportTitle(
        reportTitle: String,
        reportBegYear: Int, reportBegMonth: Int, reportBegDay: Int,
        reportEndYear: Int, reportEndMonth: Int, reportEndDay: Int,
        sheet: WritableSheet, offsX: Int, aOffsY: Int
    ): Int {
        var offsY = aOffsY

        sheet.addCell(Label(offsX, offsY++, reportTitle, wcfTitleL))
        sheet.addCell(
            Label(
                offsX, offsY++,
                "за период с ${DateTime_DMY(arrayOf(reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0))}" +
                    " по ${DateTime_DMY(arrayOf(reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0))}",
                wcfTitleL
            )
        )
        return offsY
    }

    protected fun fillReportTitleWithTime(sheet: WritableSheet): Int {
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportBegHour = hmReportParam["report_beg_hour"] as? Int ?: 0
        val reportBegMinute = hmReportParam["report_beg_minute"] as? Int ?: 0

        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int
        val reportEndHour = hmReportParam["report_end_hour"] as? Int ?: 0
        val reportEndMinute = hmReportParam["report_end_minute"] as? Int ?: 0

        return fillReportTitle(
            aliasConfig.descr,
            reportBegYear, reportBegMonth, reportBegDay, reportBegHour, reportBegMinute,
            reportEndYear, reportEndMonth, reportEndDay, reportEndHour, reportEndMinute,
            sheet, 1, 0
        )
    }

    private fun fillReportTitle(
        reportTitle: String,
        reportBegYear: Int, reportBegMonth: Int, reportBegDay: Int, reportBegHour: Int, reportBegMinute: Int,
        reportEndYear: Int, reportEndMonth: Int, reportEndDay: Int, reportEndHour: Int, reportEndMinute: Int,
        sheet: WritableSheet, offsX: Int, aOffsY: Int
    ): Int {
        var offsY = aOffsY

        sheet.addCell(Label(offsX, offsY++, reportTitle, wcfTitleL))
        sheet.addCell(
            Label(
                offsX, offsY++,
                "за период с ${DateTime_DMYHMS(arrayOf(reportBegYear, reportBegMonth, reportBegDay, reportBegHour, reportBegMinute, 0))}" +
                    " по ${DateTime_DMYHMS(arrayOf(reportEndYear, reportEndMonth, reportEndDay, reportEndHour, reportEndMinute, 0))}",
                wcfTitleL
            )
        )
        offsY++    // еще одна пустая строчка снизу
        return offsY
    }

    //--- универсальное заполнение заголовка отчета
    protected fun fillReportHeader(reportDepartment: Int, reportGroup: Int, sheet: WritableSheet, offsX: Int, aOffsY: Int): Int {
        var offsY = aOffsY
        if (reportDepartment != 0) {
            var name = "(неизвестно)"
            val rs = conn.executeQuery(" SELECT name FROM MMS_department WHERE id = $reportDepartment ")
            if (rs.next()) name = rs.getString(1)
            rs.close()

            sheet.addCell(Label(offsX, offsY, "Подразделение:", wcfTitleName))
            sheet.addCell(Label(offsX + 1, offsY, name, wcfTitleValue))
            offsY++
        }
        if (reportGroup != 0) {
            var name = "(неизвестно)"
            val rs = conn.executeQuery(" SELECT name FROM MMS_group WHERE id = $reportGroup ")
            if (rs.next()) name = rs.getString(1)
            rs.close()

            sheet.addCell(Label(offsX, offsY, "Группа:", wcfTitleName))
            sheet.addCell(Label(offsX + 1, offsY, name, wcfTitleValue))
            offsY++
        }
        offsY++    // еще одна пустая строчка снизу

        return offsY
    }

    protected fun fillReportHeader(objectConfig: ObjectConfig, sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        for (i in objectConfig.alTitleName.indices) {
            sheet.addCell(Label(1, offsY + i, objectConfig.alTitleName[i], wcfTitleName))
            sheet.addCell(Label(2, offsY + i, objectConfig.alTitleValue[i], wcfTitleValue))
        }
        offsY += objectConfig.alTitleName.size + 1 // + пропуск строки между заголовком и шапкой отчета
        return offsY
    }

    protected fun fillReportHeader(zoneData: ZoneData?, sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        val zoneInfo = zoneData?.let {
            var sZone = zoneData.name
            if (zoneData.descr.isNotEmpty()) {
                sZone += " (${zoneData.descr})"
            }
            sZone
        } ?: "(все)"
        sheet.addCell(Label(1, offsY, "Геозона:", wcfTitleName))
        sheet.addCell(Label(2, offsY, zoneInfo, wcfTitleValue))
        offsY += 2      // + пропуск строки между заголовком и шапкой отчета
        return offsY
    }

    companion object {

        //--- суммирование расхода топлива
        //--- tmFuelUsing - текущий расход
        //--- postFix - AutoCalc.FUEL_TYPE_USING / AutoCalc.FUEL_TYPE_CALC
        //--- tmFuelUsingMP - частная сумма ( например, в вдиежни

        //    public static void fillFuelUsing(  TreeMap<String,Double> tmFuelUsing, String postFix,
        //                                      TreeMap<String,Double> tmFuelUsingMP, TreeMap<String,Double> tmFuelUsingSum  ) {
        //        if(  tmFuelUsing != null && ! tmFuelUsing.isEmpty()  )
        //            for(  String fuelName : tmFuelUsing.keySet()  ) {
        //                double fuelUsing = tmFuelUsing.get(  fuelName  );
        //                String fullFuelName = new StringBuilder(  fuelName  ).append(  postFix  ).toString();
        //
        //                //--- fuel using in moving/parking
        //                Double sumFuelUsing = tmFuelUsingMP.get(  fullFuelName  );
        //                tmFuelUsingMP.put(  fullFuelName, (  sumFuelUsing == null ? 0 : sumFuelUsing  ) + fuelUsing  );
        //
        //                //--- common fuel using
        //                sumFuelUsing = tmFuelUsingSum.get(  fullFuelName  );
        //                tmFuelUsingSum.put(  fullFuelName, (  sumFuelUsing == null ? 0 : sumFuelUsing  ) + fuelUsing  );
        //            }
        //    }

        private var objectAliasID = 0
        fun loadObjectList(conn: CoreAdvancedConnection, userConfig: UserConfig, userID: Int, departmentID: Int, groupID: Int, alObject: MutableList<Int>) {

            //--- однократная инициализация aliasID
            if (objectAliasID == 0) {
                val rs = conn.executeQuery(" SELECT id FROM SYSTEM_alias WHERE name = 'mms_object' ")
                if (rs.next()) {
                    objectAliasID = rs.getInt(1)
                }
                rs.close()
            }
            val hsObjectPermission = userConfig.userPermission["mms_object"]!!

            val sb = StringBuilder(" SELECT id , user_id FROM MMS_object WHERE id <> 0 ")
            if (userID != 0) sb.append(" AND user_id = ").append(userID)
            if (departmentID != 0) sb.append(" AND department_id = ").append(departmentID)
            if (groupID != 0) sb.append(" AND group_id = ").append(groupID)
            sb.append(" ORDER BY name ")

            val rs = conn.executeQuery(sb.toString())
            while (rs.next()) {
                val aID = rs.getInt(1)
                val uID = rs.getInt(2)

                if (userID != 0 ||
                    checkPerm(userConfig, hsObjectPermission, PERM_TABLE, uID)
                ) {
                    alObject.add(aID)
                }
            }
            rs.close()
        }
    }
}
