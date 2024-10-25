package foatto.server.util

import java.io.File

object AscendingFileNameComparator : Comparator<File> {
    override fun compare(f1: File, f2: File): Int {
        return f1.name.compareTo(f2.name)
    }
}
