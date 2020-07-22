package foatto.core.util

import java.io.File
import java.util.Comparator

object AscendingFileNameComparator : Comparator<File> {
    override fun compare( f1: File, f2: File ): Int {
        return f1.name.compareTo( f2.name )
    }
}
