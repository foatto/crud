//@file:JvmName("PlayDef")
//package foatto.core.app.video
//
//class PlayDef( cameraCount: Int ) {
//    //--- какой индекс (номер) потока используется для i-й камеры
//    //--- (для разных режимов показа используются разные потоки и не всегда в желаемом потоке могут быть быть файлы)
//    @JvmField var arrStreamIndex = IntArray( cameraCount )
//    //--- какой индекс (номер) файла из выбранного ранее/выше потока используется для i-й камеры для текущего воспроизведения
//    @JvmField var arrCurFileIndex = IntArray( cameraCount )
//    //--- индекс следующего файла (это может быть и не arrCurFileIndex + 1)
//    @JvmField var arrNextFileIndex = IntArray( cameraCount )
//    //--- ближайшее время следующего файла
//    @JvmField var minNextTime = java.lang.Long.MAX_VALUE
//
//    init {
//        for( cIndex in 0 until cameraCount ) {
//            //--- начальное значение = "поток не определён"
//            arrStreamIndex[ cIndex ] = -1
//            //--- начальное значение = "файл не найден"
//            arrCurFileIndex[ cIndex ] = -1
//            //--- начальное значение = "следующий файл не найден"
//            arrNextFileIndex[ cIndex ] = -1
//        }
//    }
//}
