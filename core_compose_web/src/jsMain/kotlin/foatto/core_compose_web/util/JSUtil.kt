package foatto.core_compose_web.util

import foatto.core_compose_web.external.SHA_1
import foatto.core_compose_web.external.SHA_INPUT_TEXT
import foatto.core_compose_web.external.SHA_OUTPUT_B64
import foatto.core_compose_web.external.jsSHA
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.rgba

//--- старший байт прозрачности сначала двигаем, потом маскируем, т.к. он знаковый и может притащить за собой минус.
//--- заодно и другие байты сделаем так же
fun getColorFromInt(argb: Int): CSSColorValue = rgba(
    r = argb shr 16 and 0xFF,
    g = argb shr 8 and 0xFF,
    b = argb and 0xFF,
    a = (argb shr 24 and 0xFF) / 255.0,
)

fun encodePassword(password: String): String {
    val sha = jsSHA(SHA_1, SHA_INPUT_TEXT)
    sha.update(password)
    return sha.getHash(SHA_OUTPUT_B64) as String
}

