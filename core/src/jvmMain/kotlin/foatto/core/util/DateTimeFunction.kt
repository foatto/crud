package foatto.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.abs

fun getZoneId(timeOffset: Int?) = ZoneOffset.ofTotalSeconds(
    //--- если смещение не задано, то используем UTC-время напрямую (чтобы и с ошибкой не вылетало и было заметно, что что-то не так со временем :)
    if (timeOffset == null) {
        0
    }
    //--- если смещение <= максимально возможного смещения в секундах (43 200 сек), значит оно задано в секундах (логично)
    else if (abs(timeOffset) <= 12 * 60 * 60) {
        timeOffset
    }
    //--- в противном случае смещение задано в старом варианте - в миллисекундах
    //--- (минимальное значение будет начинаться с 1 час * 60 * 60 * 1000 = 3 600 000 мс, что всяко не совпадает с верхней границей в 43 200 от предущего варианта)
    else {
        timeOffset / 1000
    }
)

fun getCurrentTimeInt() = (System.currentTimeMillis() / 1000).toInt()
fun getCurrentDayStart(zoneId: ZoneId): ZonedDateTime {
    val today = ZonedDateTime.now(zoneId)
    return ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, 0, 0, 0, 0, zoneId)
}

fun getNextDayStart(zoneId: ZoneId): ZonedDateTime {
    val today = ZonedDateTime.now(zoneId)
    return ZonedDateTime.of(today.year, today.monthValue, today.dayOfMonth, 0, 0, 0, 0, zoneId).plus(1, ChronoUnit.DAYS)
}
//        val begTime = gc.toEpochSecond().toInt()
//        val endTime = gc.plus(1, ChronoUnit.DAYS).toEpochSecond().toInt()

// "2000-11-15 14:54:50"
fun YMDHMS_DateTime(zoneId: ZoneId, ymdhms: String): ZonedDateTime =
    ZonedDateTime.of(
        ymdhms.substring(0..3).toInt(),
        ymdhms.substring(5..6).toInt(),
        ymdhms.substring(8..9).toInt(),
        ymdhms.substring(11..12).toInt(),
        ymdhms.substring(14..15).toInt(),
        ymdhms.substring(17..18).toInt(),
        0,
        zoneId
    )

fun getDateTime(zoneId: ZoneId, second: Int) = ZonedDateTime.ofInstant(Instant.ofEpochSecond(second.toLong()), zoneId)
fun getDateTime(zoneId: ZoneId, arrDT: Array<Int>) = ZonedDateTime.of(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5], 0, zoneId)

fun getDateTimeInt(zoneId: ZoneId, arrDT: Array<Int>) = getDateTimeInt(getDateTime(zoneId, arrDT))
fun getDateTimeInt(zdt: ZonedDateTime) = zdt.toEpochSecond().toInt()

fun getDateTimeArray(zoneId: ZoneId, second: Int) = getDateTimeArray(getDateTime(zoneId, second))
fun getDateTimeArray(zdt: ZonedDateTime) = arrayOf(zdt.year, zdt.monthValue, zdt.dayOfMonth, zdt.hour, zdt.minute, zdt.second)
fun getDateArray(ld: LocalDate) = arrayOf(ld.year, ld.monthValue, ld.dayOfMonth)

fun DateTime_YMDHMS(zoneId: ZoneId, second: Int) = DateTime_YMDHMS(getDateTimeArray(zoneId, second))
fun DateTime_YMDHMS(zdt: ZonedDateTime) = DateTime_YMDHMS(getDateTimeArray(zdt))
fun DateTime_YMDHMS(arrDT: Array<Int>) =
    "${if (arrDT[0] < 10) "0" else ""}${arrDT[0]}." +
    "${if (arrDT[1] < 10) "0" else ""}${arrDT[1]}." +
    "${if (arrDT[2] < 10) "0" else ""}${arrDT[2]} " +
    "${if (arrDT[3] < 10) "0" else ""}${arrDT[3]}:" +
    "${if (arrDT[4] < 10) "0" else ""}${arrDT[4]}:" +
    "${if (arrDT[5] < 10) "0" else ""}${arrDT[5]}"

fun DateTime_DMYHMS(zoneId: ZoneId, second: Int) = DateTime_DMYHMS(getDateTimeArray(zoneId, second))
fun DateTime_DMYHMS(zdt: ZonedDateTime) = DateTime_DMYHMS(getDateTimeArray(zdt))
fun DateTime_DMYHMS(arrDT: Array<Int>) =
    "${if (arrDT[2] < 10) "0" else ""}${arrDT[2]}." +
    "${if (arrDT[1] < 10) "0" else ""}${arrDT[1]}." +
    "${if (arrDT[0] < 10) "0" else ""}${arrDT[0]} " +
    "${if (arrDT[3] < 10) "0" else ""}${arrDT[3]}:" +
    "${if (arrDT[4] < 10) "0" else ""}${arrDT[4]}:" +
    "${if (arrDT[5] < 10) "0" else ""}${arrDT[5]}"

fun DateTime_DMYHM(zoneId: ZoneId, second: Int) = DateTime_DMYHM(getDateTimeArray(zoneId, second))
fun DateTime_DMYHM(zdt: ZonedDateTime) = DateTime_DMYHM(getDateTimeArray(zdt))
fun DateTime_DMYHM(arrDT: Array<Int>) =
    "${if (arrDT[2] < 10) "0" else ""}${arrDT[2]}." +
    "${if (arrDT[1] < 10) "0" else ""}${arrDT[1]}." +
    "${if (arrDT[0] < 10) "0" else ""}${arrDT[0]} " +
    "${if (arrDT[3] < 10) "0" else ""}${arrDT[3]}:" +
    "${if (arrDT[4] < 10) "0" else ""}${arrDT[4]}"

fun DateTime_DMY(zoneId: ZoneId, second: Int) = DateTime_DMY(getDateTimeArray(zoneId, second))
fun DateTime_DMY(zdt: ZonedDateTime) = DateTime_DMY(getDateTimeArray(zdt))
fun DateTime_DMY(ld: LocalDate) = DateTime_DMY(getDateArray(ld))
fun DateTime_DMY(arrDT: Array<Int>) =
    "${if (arrDT[2] < 10) "0" else ""}${arrDT[2]}." +
        "${if (arrDT[1] < 10) "0" else ""}${arrDT[1]}." +
        "${if (arrDT[0] < 10) "0" else ""}${arrDT[0]}"

fun Time_HMS(ld: LocalTime) =
    "${if (ld.hour < 10) "0" else ""}${ld.hour}:" +
        "${if (ld.minute < 10) "0" else ""}${ld.minute}:" +
        "${if (ld.second < 10) "0" else ""}${ld.second}"

fun Time_HM(ld: LocalTime) =
    "${if (ld.hour < 10) "0" else ""}${ld.hour}:" +
        "${if (ld.minute < 10) "0" else ""}${ld.minute}"

fun secondIntervalToString(beg: Int, end: Int) = secondIntervalToString(end - beg)
fun secondIntervalToString(interval: Int): String {
    val ho = interval / 3600
    val mi = interval % 3600 / 60
    val se = interval % 60

    return "${if (ho < 10) "0" else ""}$ho:" +
        "${if (mi < 10) "0" else ""}$mi:" +
        "${if (se < 10) "0" else ""}$se"
}

fun getTimeFromDirName(dirName: String, delimiters: String, zoneId: ZoneId): Int =
    try {
        val st = StringTokenizer(dirName, delimiters)
        val zdt = ZonedDateTime.of(st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), 0, 0, 0, 0, zoneId)
        zdt.toEpochSecond().toInt()
    } catch (t: Throwable) {
        0
    }

fun getTimeFromFileName(fileName: String, delimiters: String, zoneId: ZoneId): Pair<Int, Int>? =
    try {
        //--- "-" - чтобы разделять числа между собой, "." - чтобы отделить расширение файла от чисел
        val st = StringTokenizer(fileName, delimiters)

        val zdtBeg = ZonedDateTime.of(
            st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(),
            st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), 0, zoneId
        )

        val zdtEnd = ZonedDateTime.of(
            st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(),
            st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), 0, zoneId
        )

        Pair(zdtBeg.toEpochSecond().toInt(), zdtEnd.toEpochSecond().toInt())
    } catch (t: Throwable) {
        null
    }