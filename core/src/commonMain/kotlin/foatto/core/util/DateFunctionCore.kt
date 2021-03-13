package foatto.core.util

fun compareDateTimeArray(dateTime1: Array<Int>, dateTime2: Array<Int>): Int {
    dateTime1.forEachIndexed { index, _ ->
        if (dateTime1[index] != dateTime2[index]) {
            return dateTime1[index] - dateTime2[index]
        }
    }
    return 0
}