package foatto.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

//-------------------------------------------------------------------------------------------------

fun getCurrentTimeInt() = Clock.System.now().epochSeconds.toInt()
fun getCurrentTimeLong() = Clock.System.now().epochSeconds

//-------------------------------------------------------------------------------------------------

fun getTimeZone(timeOffset: Int?): FixedOffsetTimeZone = FixedOffsetTimeZone(
    UtcOffset(
        seconds =
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
)

fun getLocalDateTime(timeOffset: Int, sec: Int): LocalDateTime = getLocalDateTime(getTimeZone(timeOffset), sec)
fun getLocalDateTime(timeZone: TimeZone, sec: Int): LocalDateTime = Instant.fromEpochSeconds(sec.toLong()).toLocalDateTime(timeZone)

//-------------------------------------------------------------------------------------------------

fun DateTime_DMYHMS(timeOffset: Int, sec: Int): String {
    val dt = getLocalDateTime(timeOffset, sec)
    return DateTime_DMYHMS(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}

fun DateTime_DMYHM(timeOffset: Int, sec: Int): String {
    val dt = getLocalDateTime(timeOffset, sec)
    return DateTime_DMYHM(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute)
}

fun DateTime_YMDHMS(timeOffset: Int, sec: Int): String {
    val dt = getLocalDateTime(timeOffset, sec)
    return DateTime_YMDHMS(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}

fun DateTime_YMDHM(timeOffset: Int, sec: Int): String {
    val dt = getLocalDateTime(timeOffset, sec)
    return DateTime_YMDHM(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute)
}

//-------------------------------------------------------------------------------------------------

fun DateTime_DMYHMS(timeZone: TimeZone, sec: Int): String {
    val dt = getLocalDateTime(timeZone, sec)
    return DateTime_DMYHMS(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}

fun DateTime_DMYHM(timeZone: TimeZone, sec: Int): String {
    val dt = getLocalDateTime(timeZone, sec)
    return DateTime_DMYHM(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute)
}

fun DateTime_YMDHMS(timeZone: TimeZone, sec: Int): String {
    val dt = getLocalDateTime(timeZone, sec)
    return DateTime_YMDHMS(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}

fun DateTime_YMDHM(timeZone: TimeZone, sec: Int): String {
    val dt = getLocalDateTime(timeZone, sec)
    return DateTime_YMDHM(dt.year, dt.month.ordinal + 1, dt.dayOfMonth, dt.hour, dt.minute)
}

//-------------------------------------------------------------------------------------------------

fun DateTime_DMYHMS(arrDT: Array<Int>): String = DateTime_DMYHMS(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun DateTime_DMYHM(arrDT: Array<Int>): String = DateTime_DMYHM(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])
fun DateTime_YMDHMS(arrDT: Array<Int>): String = DateTime_YMDHMS(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun DateTime_YMDHM(arrDT: Array<Int>): String = DateTime_YMDHM(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])

fun DateTime_DMYHMS(arrDT: IntArray): String = DateTime_DMYHMS(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun DateTime_DMYHM(arrDT: IntArray): String = DateTime_DMYHM(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])
fun DateTime_YMDHMS(arrDT: IntArray): String = DateTime_YMDHMS(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun DateTime_YMDHM(arrDT: IntArray): String = DateTime_YMDHM(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])

//-------------------------------------------------------------------------------------------------

fun DateTime_DMYHMS(ye: Int, mo: Int, da: Int, ho: Int, mi: Int, se: Int): String =
    "${getDigit(da)}.${getDigit(mo)}.${getDigit(ye)} ${getDigit(ho)}:${getDigit(mi)}:${getDigit(se)}"

fun DateTime_DMYHM(ye: Int, mo: Int, da: Int, ho: Int, mi: Int): String =
    "${getDigit(da)}.${getDigit(mo)}.${getDigit(ye)} ${getDigit(ho)}:${getDigit(mi)}"

fun DateTime_YMDHMS(ye: Int, mo: Int, da: Int, ho: Int, mi: Int, se: Int): String =
    "${getDigit(ye)}.${getDigit(mo)}.${getDigit(da)} ${getDigit(ho)}:${getDigit(mi)}:${getDigit(se)}"

fun DateTime_YMDHM(ye: Int, mo: Int, da: Int, ho: Int, mi: Int): String =
    "${getDigit(ye)}.${getDigit(mo)}.${getDigit(da)} ${getDigit(ho)}:${getDigit(mi)}"

//-------------------------------------------------------------------------------------------------

private fun getDigit(value: Int): String = "${if (value < 10) '0' else ""}${value}"

//-------------------------------------------------------------------------------------------------

/*
fun DateTime_Arr(dt: Date) =
    arrayOf(
        dt.getUTCFullYear(),
        dt.getUTCMonth() + 1,
        dt.getUTCDate(),
        dt.getUTCHours(),
        dt.getUTCMinutes(),
        dt.getUTCSeconds()
    )

fun DateTime_Arr(timeOffset: Int, sec: Int) = DateTime_Arr(Date((sec + timeOffset) * 1000L))

--------------------------------------------------

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

fun getDateTime(zoneId: ZoneId, arrDT: Array<Int>) = ZonedDateTime.of(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5], 0, zoneId)

fun getDateTimeInt(zoneId: ZoneId, arrDT: Array<Int>) = getDateTimeInt(getDateTime(zoneId, arrDT))
fun getDateTimeInt(zdt: ZonedDateTime) = zdt.toEpochSecond().toInt()

fun getDateTimeArray(zoneId: ZoneId, second: Int) = getDateTimeArray(getDateTime(zoneId, second))
fun getDateTimeArray(zdt: ZonedDateTime) = arrayOf(zdt.year, zdt.monthValue, zdt.dayOfMonth, zdt.hour, zdt.minute, zdt.second)
fun getDateArray(ld: LocalDate) = arrayOf(ld.year, ld.monthValue, ld.dayOfMonth)

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
 */

//fun compareDateTimeArray(dateTime1: Array<Int>, dateTime2: Array<Int>): Int {
//    dateTime1.forEachIndexed { index, _ ->
//        if (dateTime1[index] != dateTime2[index]) {
//            return dateTime1[index] - dateTime2[index]
//        }
//    }
//    return 0
//}

