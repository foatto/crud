package foatto.server.util

import java.io.File
import java.util.*

object DescendingFileTimeComparator : Comparator<File> {
    override fun compare(f1: File, f2: File): Int {
        //--- менее рисковый метод,
        //--- чем просто return (int) ( ( (File) o1 ).lastModified() - ( (File) o2 ).lastModified() );
        //--- т.к. для переполнения достаточно разницы в 25 суток
        val lm1 = f1.lastModified()
        val lm2 = f2.lastModified()
        return if (lm1 > lm2) -1 else if (lm1 < lm2) 1 else 0
    }
}
