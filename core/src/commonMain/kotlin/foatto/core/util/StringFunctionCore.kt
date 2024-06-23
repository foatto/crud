package foatto.core.util

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToLong

//fun prepareForHTML(sour: String): String = sour.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;").replace("\n", "<br>")

fun getFilledNumberString(num: Int, charCount: Int): String {
    var s = num.toString()
    if (s.length < charCount) {
        s = "0".repeat(charCount - s.length) + s
    }
    return s
}

fun getSplittedLong(value: Long, radix: Int = 10): String {
    val sIn = value.toString(radix)
    val digitInGroup = when (radix) {
        2 -> 4
        10 -> 3
        16 -> 2
        else -> 1
    }
    val groupCount = sIn.length / digitInGroup
    val groupLead = sIn.length % digitInGroup

    var sOut = if (groupLead > 0) {
        sIn.substring(0, groupLead)
    } else {
        ""
    }
    for (i in 0 until groupCount) {
        if (sOut.isNotEmpty()) {
            sOut += ' '
        }
        val tmpPos = groupLead + i * digitInGroup
        sOut += sIn.substring(tmpPos, tmpPos + digitInGroup)
    }
    return sOut
}

fun getSplittedDouble(
    value: Double,
    precision: Int,
    isUseThousandsDivider: Boolean = true,
    decimalDivider: Char = '.'
): String {
//    println("--------------------------------------")
//    println("value = $value")
//    println("precision = $precision")
    //--- additionally change the decimal point (for the Russian locale) to the universal decimal point
    val strValue: String =
        //--- negative precision - remove the zero ending of the fractional part
        if (precision < 0) {
            var s = value.toString().replace(',', '.')
            s = s.dropLastWhile { it == '0' }
            //--- if the accuracy turned out to be zero, then additionally remove the decimal point that is now unnecessary
            s.dropLastWhile { it == '.' }
        }
        //--- zero precision - round to integer
        else if (precision == 0) {
            value.roundToLong().toString()
        }
        //--- positive precision - round to the specified decimal place / dot
        else {
            val pow10 = 10.0.pow(precision)
            val s = (round(value * pow10) / pow10).toString().replace(",", ".")
            val intPart = s.substringBefore('.')
            var fracPart = s.substringAfter('.', "")
            fracPart = fracPart.substring(0, min(fracPart.length, precision)).padEnd(precision, '0')
            "$intPart.$fracPart"
        }

    val dividedValue = if (isUseThousandsDivider) {
        val dotPos = strValue.indexOf(".")
        val groupCount = (if (dotPos == -1) strValue.length else dotPos) / 3 // number of groups of numbers (full 3 characters each)
        val groupLead = (if (dotPos == -1) strValue.length else dotPos) % 3  // number of digits in the first incomplete group
        var strOut = ""
        if (groupLead > 0) {
            strOut += strValue.substring(0, groupLead)
        }
        for (i in 0 until groupCount) {
            if (strOut.isNotEmpty()) {
                strOut += ' '
            }
            val pos = groupLead + i * 3
            strOut += strValue.substring(pos, pos + 3)
        }
        //--- add fractional remainder, if any (note: dotPos == 0 cannot be)
        if (dotPos > 0) {
            strOut += strValue.substring(dotPos, strValue.length)
        }
        strOut
    } else {
        strValue
    }

    return if (decimalDivider == '.') {
        dividedValue
    } else {
        dividedValue.replace('.', decimalDivider)
    }

}
