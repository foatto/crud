package foatto.server.util

import java.security.MessageDigest
import java.util.Base64

fun encodePassword(plainPassword: String): String {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(plainPassword.toByteArray(charset("UTF-8")))
    return Base64.getEncoder().encodeToString(md.digest())
}
