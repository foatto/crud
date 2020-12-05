package foatto.core_web

import foatto.core_web.external.SHA_1
import foatto.core_web.external.SHA_INPUT_TEXT
import foatto.core_web.external.SHA_OUTPUT_B64
import foatto.core_web.external.jsSHA
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToLong

fun getColorFromInt( argb: Int ) =
    //--- старший байт прозрачности сначала двигаем, потом маскируем, т.к. он знаковый и может притащить за собой минус.
    //--- заодно и другие байты сделаем так же
    "rgba(${argb shr 16 and 0xFF},${argb shr 8 and 0xFF},${argb and 0xFF},${(argb shr 24 and 0xFF)/255.0})"

fun getSplittedDouble( value: Double, precision: Int ): String {
    //--- дополнительно меняем десятичную запятую (для русской локали) на универсальную десятичную точку
    val strValue: String =
        //--- нулевой precision - убираем нулевую оконцовку дробной части
        if( precision < 0 ) {
            var s = value.toString().replace( ',', '.' )
            s = s.dropLastWhile { it == '0' }
            //--- если точность оказалась нулевой, то дополнительно убираем ненужную теперь десятичную точку
            s.dropLastWhile { it == '.' }
        }
        //--- нулевой precision - округляем до целого
        else if( precision == 0 ) {
            value.roundToLong().toString()
        }
        //--- положительный precision - округляем до указанного знака после запятой/точки
        else {
            val pow10 = 10.0.pow(precision)
            val s = (round(value * pow10) / pow10).toString().replace(",", ".")
            val intPart = s.substringBefore('.')
            val fracPart = s.substringAfter('.').substring(0, precision).padEnd(precision, '0')
            "$intPart.$fracPart"
        }

    val dotPos = strValue.indexOf( "." )
    val groupCount = ( if( dotPos == -1 ) strValue.length else dotPos ) / 3 // кол-во групп цифр (по полных 3 знака)
    val groupLead = ( if( dotPos == -1 ) strValue.length else dotPos ) % 3  // кол-во цифр в первой неполной группе
    var strOut = ""
    if( groupLead > 0 ) strOut += strValue.substring( 0, groupLead )
    for( i in 0 until groupCount ) {
        if( strOut.isNotEmpty() ) strOut += ' '
        val pos = groupLead + i * 3
        strOut += strValue.substring( pos, pos + 3 )
    }
    //--- добавить дробный остаток, если есть (прим.: dotPos == 0 быть не может)
    if( dotPos > 0 ) strOut += strValue.substring( dotPos, strValue.length )
    return strOut
}

fun encodePassword( password: String ): String {
    val sha = jsSHA( SHA_1, SHA_INPUT_TEXT )
    sha.update( password )
    return sha.getHash( SHA_OUTPUT_B64 ) as String
}

