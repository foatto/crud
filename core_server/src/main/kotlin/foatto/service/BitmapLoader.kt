//package foatto.service
//
//import foatto.core.app.xy.config.XyBitmapType
//import foatto.core.util.AdvancedLogger
//import foatto.core_server.service.CoreBitmapLoader
//import foatto.core_server.service.CoreServiceWorker
//import java.io.File
//import java.io.InputStream
//import javax.imageio.ImageIO
//
//class BitmapLoader(aConfigFileName: String) : CoreBitmapLoader(aConfigFileName) {
//
//    override val isRunOnce: Boolean
//        get() = true
//
//    //--- база не используется
//    override fun initDB() {}
//
//    override fun workBitmap(gis: InputStream, bmFile: File, sbMapURL: StringBuilder, bitmapName: String) {
//
//        val bi = ImageIO.read(gis)
//        gis.close()
//
//        //--- если нас не проигнорировали
//        if(bi != null) {
//            ImageIO.write(bi, XyBitmapType.BITMAP_EXT, bmFile)
//            if(CoreBitmapLoader.hsBlockedFileSize.contains(bmFile.length())) {
//                bmFile.delete()
//                AdvancedLogger.debug("BITMAP: $sbMapURL can't load from geoserver: server is busy.")
//            }
//            else {
//                AdvancedLogger.debug("BITMAP: $sbMapURL loaded from geoserver.")
//                //--- если центральный сервер задан, отдадим туда готовую картинку
//                if(appLink != null) sendBitmap(bitmapName, bmFile)
//            }
//        }
//    }
//
//    companion object {
//
//        @JvmStatic
//        fun main(args: Array<String>) {
//            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
//            try {
//                CoreServiceWorker.serviceWorkerName = "BitmapLoader"
//                if(args.size == 1) {
//                    BitmapLoader(args[0]).run()
//                    exitCode = 1
//                }
//                else println("Usage: ${CoreServiceWorker.serviceWorkerName} <ini-file-name>")
//            }
//            catch(t: Throwable) {
//                t.printStackTrace()
//            }
//
//            System.exit(exitCode)
//        }
//    }
//}
//
