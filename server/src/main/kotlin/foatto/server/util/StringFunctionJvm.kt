package foatto.server.util

import java.security.MessageDigest
import java.util.*

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val sLineSeparator = "\n"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun encodePassword(plainPassword: String): String {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(plainPassword.toByteArray(charset("UTF-8")))
    return Base64.getEncoder().encodeToString(md.digest())
}

//--- внутренние преобразования строк -------------------------------------------------------------------------------------------------------------------------------------------------

fun prepareForReport(sour: String): String = sour.replace("\t", "    ")
fun prepareForScript(sour: String): String = sour.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ')
fun prepareForSQL(sour: String): String = sour.replace('\'', '`').replace('"', '`')

fun addLoginAndPasswordToURL(url: String, login: String, password: String, withQuotes: Boolean): String {
    val quote = if (withQuotes) "\"" else ""
    val loginAndPassword = if (login.isNotEmpty() && password.isNotEmpty()) "$login:$password@" else ""

    val prefix = url.substringBefore("//", "")
    val slashes = if (url.contains("//")) "//" else ""
    val suffix = url.substringAfter("//")

    return "$quote$prefix$slashes$loginAndPassword$suffix$quote"
}

//--- разделяет слишком длинную строку, вставляя символ новой строки вместо определённых пробелов
fun getSplittedString(text: String, maxWidth: Int): String {
    //--- делим слишком длинные строки на отдельные подстроки переносом строки.
    //--- чтобы не делать лишние затратные операции со строками, проводим предварительные проверки:
    //--- 1. первичная проверка на общую длину
    if (text.length <= maxWidth) return text

    //--- длинная строка может быть уже поделена на подстроки -
    //--- 2. делаем вторую проверку на длину подстрок
    var isSplitNeed = false
    var st = StringTokenizer(text, sLineSeparator)
    while (st.hasMoreTokens())
        if (st.nextToken().length > maxWidth) {
            isSplitNeed = true
            break
        }
    if (!isSplitNeed) return text

    //--- разделение таки требуется
    //--- длина будет такой же, только некоторые пробелы заменятся на перевод строки
    val sbResult = StringBuilder(text.length)
    st = StringTokenizer(text, sLineSeparator)
    while (st.hasMoreTokens()) {
        val s = st.nextToken()
        //--- подстрока требует разделения, разделим ей на подстроки по пробелам
        if (s.length > maxWidth) {
            val sb = StringBuilder(s.length)
            var separatorPos = 0
            val st2 = StringTokenizer(s, " ")
            while (st2.hasMoreTokens()) {
                //--- прибавляем очередное слово
                sb.append(if (sb.isEmpty()) "" else " ").append(st2.nextToken())
                //--- уже надо делить и это не последняя подстрока в строке?
                //--- (добавлять отдельный разделитель после последней подстроки не имеет смысла)
                if (sb.length - separatorPos > maxWidth && st2.hasMoreTokens()) {
                    sb.append(sLineSeparator)
                    separatorPos = sb.length
                }
            }
            sbResult.append(if (sbResult.isEmpty()) "" else sLineSeparator).append(sb)
        } else sbResult.append(if (sbResult.isEmpty()) "" else sLineSeparator).append(s)
    }
    //--- убрать последний символ переноса строки, если есть
    if (sbResult.last() == '\n') sbResult.deleteCharAt(sbResult.length - 1)

    return sbResult.toString()
}

//--- транслитерация строк ------------------------------------------------------------------------------------------------------------------------------------------------------------

private val hmRusEng = hashMapOf(
    'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "j",
    'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f",
    'х' to "h", 'ц' to "ts", 'ч' to "ch", 'т' to "sh", 'щ' to "sch", 'ъ' to "`", 'ы' to "y", 'ь' to "`", 'э' to "e", 'ю' to "yu", 'я' to "ya",
    'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Д' to "D", 'Е' to "E", 'Ё' to "YO", 'Ж' to "ZH", 'З' to "Z", 'И' to "I", 'Й' to "J",
    'Л' to "K", 'Л' to "L", 'М' to "M", 'Н' to "N", 'О' to "O", 'П' to "P", 'Р' to "R", 'С' to "S", 'Т' to "T", 'У' to "U", 'Ф' to "F",
    'Х' to "H", 'Ц' to "TS", 'Ч' to "CH", 'Т' to "SH", 'Щ' to "SCH", 'Ъ' to "`", 'Ы' to "Y", 'Ь' to "`", 'Э' to "E", 'Ю' to "YU", 'Я' to "YA"
)

fun translit(sour: String): StringBuilder {
    val dest = StringBuilder()
    for (ch in sour) dest.append(hmRusEng[ch] ?: ch)
    return dest
}

//--- обратный перевод латинских букв в номерах а/м в кириллические аналоги
//--- (на случай невозможности задавать гос.номер в параметрах русскими буквами в UTF-8, например, в тестах)
private val hmReverseTranslit = hashMapOf(
    'A' to 'А', 'a' to 'а', 'B' to 'В', 'b' to 'в', 'C' to 'С', 'c' to 'с', 'E' to 'Е', 'e' to 'е', 'H' to 'Н', 'h' to 'н', 'K' to 'К', 'k' to 'к',
    'M' to 'М', 'm' to 'м', 'O' to 'О', 'o' to 'о', 'P' to 'Р', 'p' to 'р', 'T' to 'Т', 't' to 'т', 'X' to 'Х', 'x' to 'х', 'Y' to 'У', 'y' to 'у'
)

fun reverseTranslit(sour: String): StringBuilder {
    val dest = StringBuilder()
    for (ch in sour) dest.append(hmReverseTranslit[ch] ?: ch)
    return dest
}

//--- преобразование числа <---> строки -----------------------------------------------------------------------------------------------------------------------------------------------

//fun getFilledNumberString( num: Int, charCount: Int ): String {
//    val sb = StringBuilder()
//    sb.append( num )
//    //--- дополнить слева нулями до charCount символов
//    while( sb.length < charCount ) sb.insert( 0, '0' )
//    return sb.toString()
//}
//
//fun getSplittedLong( value: Long, radix: Int = 10 ): StringBuilder {
//    val sbIn: StringBuilder
//    val digitInGroup: Int
//    when( radix ) {
//        2  -> {
//            sbIn = StringBuilder( java.lang.Long.toBinaryString( value ) )
//            digitInGroup = 4
//        }
//        10 -> {
//            sbIn = StringBuilder( java.lang.Long.toString( value ) )
//            digitInGroup = 3
//        }
//        16 -> {
//            sbIn = StringBuilder( java.lang.Long.toHexString( value ).toUpperCase() )
//            digitInGroup = 2
//        }
//        else -> return StringBuilder( "" )
//    }
//
//    val groupCount = sbIn.length / digitInGroup // кол-во групп цифр (по полных N знаков)
//    val groupLead = sbIn.length % digitInGroup  // кол-во цифр в первой неполной группе
//    val sbOut = StringBuilder()
//    if( groupLead > 0 ) sbOut.append( sbIn.subSequence( 0, groupLead ) )
//    for( i in 0 until groupCount ) {
//        if( !sbOut.isEmpty() ) sbOut.append( ' ' )
//        val tmpPos = groupLead + i * digitInGroup
//        sbOut.append( sbIn.subSequence( tmpPos, tmpPos + digitInGroup ) )
//    }
//    return sbOut
//}
//
//private val arrDoubleFormat = arrayOf(
//                                DecimalFormat( "0" ), DecimalFormat( "0.0" ), DecimalFormat( "0.00" ), DecimalFormat( "0.000" ), DecimalFormat(" 0.0000 "), DecimalFormat( "0.00000" ),
//                                DecimalFormat( "0.000000" ), DecimalFormat( "0.0000000" ), DecimalFormat( "0.00000000" ), DecimalFormat( "0.000000000" ) )
//
//fun getSplittedDouble( aValue: Double, aPrecision: Int ): String {
//    var value = aValue
//    var precision = aPrecision
//
//    //--- корректируем завышенную точность (отрицательную точность не трогаем, это индикатор автоопределения)
//    if (precision >= arrDoubleFormat.size) precision = arrDoubleFormat.size - 1
//
//    val precisionIndex = if (precision < 0) arrDoubleFormat.size - 1 else precision
//
//    //--- обеспечиваем округление до нужного знака после запятой
//    val pow10 = 10.0.pow(precisionIndex.toDouble())
//    value = round(value * pow10) / pow10
//
//    //--- дополнительно меняем десятичную запятую (для русской локали) на универсальную десятичную точку
//    val sbIn = StringBuilder(arrDoubleFormat[precisionIndex].format(value).replace(',', '.'))
//
//    //--- если задано автоматическое округление - убирается только нулевая оконцовка дробной части
//    //--- и вычисляется "автоматический" precision
//    if (precision < 0) {
//        precision = arrDoubleFormat.size - 1
//        while (sbIn[sbIn.length - (arrDoubleFormat.size - precision)] == '0') precision--
//        //--- если точность оказалась нулевой, то дополнительно убираем ненужную теперь десятичную точку
//        sbIn.delete( sbIn.length - ( arrDoubleFormat.size - precision ) + if( precision == 0 ) 0 else 1, sbIn.length )
//    }
//
//    val dotPos = sbIn.indexOf( "." )
//    val groupCount = ( if( dotPos == -1 ) sbIn.length else dotPos ) / 3 // кол-во групп цифр (по полных 3 знака)
//    val groupLead = ( if( dotPos == -1 ) sbIn.length else dotPos ) % 3  // кол-во цифр в первой неполной группе
//
//    var sOut = ""
//    if( groupLead > 0 ) sOut += sbIn.subSequence( 0, groupLead )
//    for( i in 0 until groupCount ) {
//        if( sOut.isNotEmpty() ) {
//            sOut += ' '
//        }
//        val pos = groupLead + i * 3
//        sOut += sbIn.subSequence( pos, pos + 3 )
//    }
//    //--- добавить дробный остаток, если есть (прим.: dotPos == 0 быть не может)
//    if( dotPos > 0 ) {
//        sOut += sbIn.subSequence( dotPos, sbIn.length )
//    }
//    return sOut
//}

//--- преобразования HEX <---> String -------------------------------------------------------------------------------------------------------------------------------------------------

private val arrHexChar = "0123456789ABCDEF".toCharArray()
private val hmHexInt = hashMapOf(
    '0' to 0x0, '1' to 0x1, '2' to 0x2, '3' to 0x3, '4' to 0x4, '5' to 0x5, '6' to 0x6, '7' to 0x7,
    '8' to 0x8, '9' to 0x9, 'A' to 0xA, 'B' to 0xB, 'C' to 0xC, 'D' to 0xD, 'E' to 0xE, 'F' to 0xF
)

fun byteArrayToHex(arrByte: ByteArray, aSb: StringBuilder?, withSpace: Boolean): StringBuilder {
    val sb = aSb ?: StringBuilder(arrByte.size * 2)
    for (b in arrByte) byteToHex(b, sb, withSpace)
    return sb
}

fun byteToHex(b: Byte, sb: StringBuilder, withSpace: Boolean): StringBuilder {
    sb.append(arrHexChar[(b.toInt() ushr 4) and 0x0F]).append(arrHexChar[b.toInt() and 0x0F])
    if (withSpace) sb.append(' ')
    return sb
}

fun byteToHex(b: Byte, withSpace: Boolean): String {
    val hi = arrHexChar[(b.toInt() ushr 4) and 0x0F]
    val lo = arrHexChar[b.toInt() and 0x0F]
    val space = if (withSpace) " " else ""

    return "$hi$lo$space"
}

fun hexToByte(hi: Char, lo: Char): Byte = (((hmHexInt[hi] ?: 0) shl 4) or (hmHexInt[lo] ?: 0)).toByte()

//--- преобразование списки <---> строки ----------------------------------------------------------------------------------------------------------------------------------------------

//--- разделение полного имени файла на путь (обычно для .mkdirs) и собственно имя файла
fun separateUnixPath(fileName: String): Pair<String, String> =
    Pair(fileName.substringBeforeLast('/', ""), fileName.substringAfterLast('/'))

//--- заменило собой ...FromList и ...FromSet
fun getStringFromIterable(iterable: Iterable<*>, delimiter: String, leftBrace: String = "", rightBrace: String = "") =
    getSBFromIterable(iterable, delimiter, leftBrace, rightBrace).toString()

fun getSBFromIterable(iterable: Iterable<*>, delimiter: String, leftBrace: String = "", rightBrace: String = ""): StringBuilder {
    val sb = StringBuilder()
    for (key in iterable) sb.append(if (sb.isEmpty()) "" else delimiter).append(leftBrace).append(key).append(rightBrace)
    return sb
}

//--- преобразование числа/деньги ---> слова ----------------------------------------------------------------------------------------------------------------------------------------------

private val arrHundred = arrayOf("сто ", "двести ", "триста ", "четыреста ", "пятьсот ", "шестьсот ", "семьсот ", "восемьсот ", "девятьсот ")
private val arrTen = arrayOf("двадцать ", "тридцать ", "сорок ", "пятьдесят ", "шестьдесят ", "семьдесят ", "восемьдесят ", "девяносто ")
private val arrUnit1x = arrayOf(
    "десять ", "одиннадцать ", "двенадцать ", "тринадцать ", "четырнадцать ", "пятнадцать ", "шестнадцать ", "семнадцать ", "восемнадцать ", "девятнадцать "
)
private val arrUnitM = arrayOf("один ", "два ", "три ", "четыре ", "пять ", "шесть ", "семь ", "восемь ", "девять ")
private val arrUnitW = arrayOf("одна ", "две ", "три ", "четыре ", "пять ", "шесть ", "семь ", "восемь ", "девять ")

fun getWordOfMoney(money: Double): StringBuilder {
    val sbOut = StringBuilder()
    val rub = money.toInt()
    val kop = (money * 100).toInt() % 100

    //--- рубли
    if (rub > 0) { // слово "рубль" мужского рода
        sbOut.append(getWordOfCount(rub, true)).append("рубл")
        //--- исключения
        if (rub % 100 in 11..14) sbOut.append("ей ")
        else when (rub % 10) {
            1 -> sbOut.append("ь ")
            2, 3, 4 -> sbOut.append("я ")
            else -> sbOut.append("ей ")
        }
    }
    //--- копейки
    if (kop > 0) { // слово "копейка" женского рода
        sbOut.append(getWordOfCount(kop, false)).append("копе")
        //--- исключения
        if (kop in 11..14) sbOut.append("ек ")
        else when (kop % 10) {
            1 -> sbOut.append("йка ")
            2, 3, 4 -> sbOut.append("йки ")
            else -> sbOut.append("ек ")
        }
    }
    sbOut.setCharAt(0, Character.toUpperCase(sbOut[0]))

    return sbOut
}

fun getWordOfCount(count: Int, isMan: Boolean): StringBuilder {
    val sbOut = StringBuilder()

    //--- миллионы
    val s6 = count / 1000000
    if (s6 > 0) {                      // слово "миллион" мужского рода
        sbOut.append(getWordOfThousand(s6, true)).append("миллион")
        when (s6 % 10) {
            1 -> sbOut.append(' ')
            2, 3, 4 -> sbOut.append("а ")
            else -> sbOut.append("ов ")
        }
    }

    //--- тысячи
    val s3 = count % 1000000 / 1000
    if (s3 > 0) {                      // слово "тысяча" женского рода
        sbOut.append(getWordOfThousand(s3, false)).append("тысяч")
        //--- исключения - числа с 11 до 19 - пишутся без окончания
        if (s3 in 11..19) sbOut.append(' ')
        else when (s3 % 10) {
            1 -> sbOut.append("а ")
            2, 3, 4 -> sbOut.append("и ")
            else -> sbOut.append(' ')
        }
    }

    //--- до тысячи
    val s0 = count % 1000                  // род окончания зависит от переданного параметра
    if (s0 > 0) sbOut.append(getWordOfThousand(s0, isMan))

    return sbOut
}

private fun getWordOfThousand(count: Int, isMan: Boolean): StringBuilder {
    val sbOut = StringBuilder()

    val hundred = count / 100
    if (hundred > 0) sbOut.append(arrHundred[hundred - 1])

    val ten = count % 100 / 10
    val unit = count % 10
    //--- разбор десятков зависит от значения
    if (ten > 1) {
        sbOut.append(arrTen[ten - 2])
        if (unit > 0) sbOut.append(if (isMan) arrUnitM[unit - 1] else arrUnitW[unit - 1])
    } else if (ten == 1) sbOut.append(arrUnit1x[unit])
    else if (unit > 0) sbOut.append(if (isMan) arrUnitM[unit - 1] else arrUnitW[unit - 1])

    return sbOut
}
