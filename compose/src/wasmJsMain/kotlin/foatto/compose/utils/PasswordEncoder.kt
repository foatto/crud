package foatto.compose.utils

import foatto.compose.external.SHA_1
import foatto.compose.external.SHA_INPUT_TEXT
import foatto.compose.external.SHA_OUTPUT_B64
import foatto.compose.external.jsSHA

internal actual fun encodePassword(password: String): String {
    val sha = jsSHA(SHA_1, SHA_INPUT_TEXT)
    sha.update(password)
    return sha.getHash(SHA_OUTPUT_B64)
}