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

fun getCurrentTimeInt(): Int = Clock.System.now().epochSeconds.toInt()
fun getCurrentTimeLong(): Long = Clock.System.now().epochSeconds

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

//-------------------------------------------------------------------------------------------------

fun getLocalDateTime(timeOffset: Int, seconds: Int): LocalDateTime = getLocalDateTime(getTimeZone(timeOffset), seconds)
fun getLocalDateTime(timeZone: TimeZone, seconds: Int): LocalDateTime = Instant.fromEpochSeconds(seconds.toLong()).toLocalDateTime(timeZone)

//-------------------------------------------------------------------------------------------------

fun getDateTimeDMYHMSInts(timeZone: TimeZone, seconds: Int): List<Int> = getDateTimeDMYHMSInts(getLocalDateTime(timeZone, seconds))
fun getDateTimeDMYHMSInts(ldt: LocalDateTime): List<Int> = listOf(ldt.dayOfMonth, ldt.monthNumber, ldt.year, ldt.hour, ldt.minute, ldt.second)

fun getDateTimeYMDHMSInts(timeZone: TimeZone, seconds: Int): List<Int> = getDateTimeYMDHMSInts(getLocalDateTime(timeZone, seconds))
fun getDateTimeYMDHMSInts(ldt: LocalDateTime): List<Int> = listOf(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute, ldt.second)

fun getDateTimeDMYHMSStrings(timeZone: TimeZone, seconds: Int): List<String> = getDateTimeDMYHMSStrings(getLocalDateTime(timeZone, seconds))
fun getDateTimeDMYHMSStrings(ldt: LocalDateTime): List<String> = listOf(getDigit(ldt.dayOfMonth), getDigit(ldt.monthNumber), getDigit(ldt.year), getDigit(ldt.hour), getDigit(ldt.minute), getDigit(ldt.second))

fun getDateTimeYMDHMSStrings(timeZone: TimeZone, seconds: Int): List<String> = getDateTimeYMDHMSStrings(getLocalDateTime(timeZone, seconds))
fun getDateTimeYMDHMSStrings(ldt: LocalDateTime): List<String> = listOf(getDigit(ldt.year), getDigit(ldt.monthNumber), getDigit(ldt.dayOfMonth), getDigit(ldt.hour), getDigit(ldt.minute), getDigit(ldt.second))

//-------------------------------------------------------------------------------------------------

fun getDateTimeDMYHMSString(timeOffset: Int, seconds: Int): String {
    val dt = getLocalDateTime(timeOffset, seconds)
    return getDateTimeDMYHMSString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}
fun getDateTimeDMYHMSString(timeZone: TimeZone, seconds: Int): String {
    val dt = getLocalDateTime(timeZone, seconds)
    return getDateTimeDMYHMSString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}
fun getDateTimeDMYHMSString(ldt: LocalDateTime): String =
    getDateTimeDMYHMSString(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute, ldt.second)

fun getDateTimeDMYHMString(timeOffset: Int, seconds: Int): String {
    val dt = getLocalDateTime(timeOffset, seconds)
    return getDateTimeDMYHMString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute)
}
fun getDateTimeDMYHMString(timeZone: TimeZone, seconds: Int): String {
    val dt = getLocalDateTime(timeZone, seconds)
    return getDateTimeDMYHMString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute)
}
fun getDateTimeDMYHMString(ldt: LocalDateTime): String =
    getDateTimeDMYHMString(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute)

fun getDateTimeYMDHMSString(timeOffset: Int, seconds: Int): String {
    val dt = getLocalDateTime(timeOffset, seconds)
    return getDateTimeYMDHMSString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}
fun getDateTimeYMDHMSString(timeZone: TimeZone, seconds: Int): String {
    val dt = getLocalDateTime(timeZone, seconds)
    return getDateTimeYMDHMSString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute, dt.second)
}
fun getDateTimeYMDHMSString(ldt: LocalDateTime): String =
    getDateTimeYMDHMSString(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute, ldt.second)

fun getDateTimeYMDHMString(timeOffset: Int, seconds: Int): String {
    val dt = getLocalDateTime(timeOffset, seconds)
    return getDateTimeYMDHMString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute)
}
fun getDateTimeYMDHMString(timeZone: TimeZone, seconds: Int): String {
    val dt = getLocalDateTime(timeZone, seconds)
    return getDateTimeYMDHMString(dt.year, dt.monthNumber, dt.dayOfMonth, dt.hour, dt.minute)
}
fun getDateTimeYMDHMString(ldt: LocalDateTime): String =
    getDateTimeYMDHMString(ldt.year, ldt.monthNumber, ldt.dayOfMonth, ldt.hour, ldt.minute)

//-------------------------------------------------------------------------------------------------

fun getDateTimeDMYHMSString(alDT: List<Int>): String = getDateTimeDMYHMSString(alDT[0], alDT[1], alDT[2], alDT[3], alDT[4], alDT[5])
fun getDateTimeDMYHMString(alDT: List<Int>): String = getDateTimeDMYHMString(alDT[0], alDT[1], alDT[2], alDT[3], alDT[4])
fun getDateTimeYMDHMSString(alDT: List<Int>): String = getDateTimeYMDHMSString(alDT[0], alDT[1], alDT[2], alDT[3], alDT[4], alDT[5])
fun getDateTimeYMDHMString(alDT: List<Int>): String = getDateTimeYMDHMString(alDT[0], alDT[1], alDT[2], alDT[3], alDT[4])

fun getDateTimeDMYHMSString(arrDT: Array<Int>): String = getDateTimeDMYHMSString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun getDateTimeDMYHMString(arrDT: Array<Int>): String = getDateTimeDMYHMString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])
fun getDateTimeYMDHMSString(arrDT: Array<Int>): String = getDateTimeYMDHMSString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun getDateTimeYMDHMString(arrDT: Array<Int>): String = getDateTimeYMDHMString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])

fun getDateTimeDMYHMSString(arrDT: IntArray): String = getDateTimeDMYHMSString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun getDateTimeDMYHMString(arrDT: IntArray): String = getDateTimeDMYHMString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])
fun getDateTimeYMDHMSString(arrDT: IntArray): String = getDateTimeYMDHMSString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4], arrDT[5])
fun getDateTimeYMDHMString(arrDT: IntArray): String = getDateTimeYMDHMString(arrDT[0], arrDT[1], arrDT[2], arrDT[3], arrDT[4])

//-------------------------------------------------------------------------------------------------

fun getDateTimeDMYHMSString(ye: Int, mo: Int, da: Int, ho: Int, mi: Int, se: Int): String =
    "${getDigit(da)}.${getDigit(mo)}.${getDigit(ye)} ${getDigit(ho)}:${getDigit(mi)}:${getDigit(se)}"

fun getDateTimeDMYHMString(ye: Int, mo: Int, da: Int, ho: Int, mi: Int): String =
    "${getDigit(da)}.${getDigit(mo)}.${getDigit(ye)} ${getDigit(ho)}:${getDigit(mi)}"

fun getDateTimeYMDHMSString(ye: Int, mo: Int, da: Int, ho: Int, mi: Int, se: Int): String =
    "${getDigit(ye)}.${getDigit(mo)}.${getDigit(da)} ${getDigit(ho)}:${getDigit(mi)}:${getDigit(se)}"

fun getDateTimeYMDHMString(ye: Int, mo: Int, da: Int, ho: Int, mi: Int): String =
    "${getDigit(ye)}.${getDigit(mo)}.${getDigit(da)} ${getDigit(ho)}:${getDigit(mi)}"

fun getDateYMDString(ye: Int, mo: Int, da: Int): String =
    "${getDigit(ye)}.${getDigit(mo)}.${getDigit(da)}"

fun getDateDMYString(ye: Int, mo: Int, da: Int): String =
    "${getDigit(da)}.${getDigit(mo)}.${getDigit(ye)}"

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

fun getDateTimeInt(zoneId: ZoneId, arrDT: Array<Int>) = getDateTimeInt(getDateTime(zoneId, arrDT))
fun getDateTimeInt(zdt: ZonedDateTime) = zdt.toEpochSecond().toInt()

fun getDateTimeFromYMDHMSInts(zoneId: ZoneId, alDT: List<Int?>): ZonedDateTime? =
    if (alDT.any { v -> v == null }) {
        null
    } else {
        ZonedDateTime.of(alDT[0]!!, alDT[1]!!, alDT[2]!!, alDT[3]!!, alDT[4]!!, alDT[5]!!, 0, zoneId)
    }

fun getDateTimeFromDMYHMSStrings(zoneId: ZoneId, alDT: List<String>): ZonedDateTime? = getDateTimeFromYMDHMSInts(
    zoneId,
    listOf(
        alDT[2].toIntOrNull(),
        alDT[1].toIntOrNull(),
        alDT[0].toIntOrNull(),
        alDT[3].toIntOrNull(),
        alDT[4].toIntOrNull(),
        alDT[5].toIntOrNull(),
    ),
)

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

