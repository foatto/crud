package foatto.core.util

fun String.normalizeForDouble() = this.replace(',', '.').replace(" ", "")