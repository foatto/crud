package foatto.compose.utils

import io.ktor.util.*
import java.security.MessageDigest

internal actual fun encodePassword(password: String): String {
    val bytes = MessageDigest
        .getInstance("SHA-1")
        .digest(password.toByteArray())

    return bytes.encodeBase64()
}
