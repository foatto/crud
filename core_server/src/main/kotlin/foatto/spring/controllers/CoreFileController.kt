//package foatto.spring.controllers
//
//import foatto.core.link.FormFileUploadResponse
//import foatto.core.util.getCurrentTimeInt
//import io.minio.MinioClient
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.web.bind.annotation.GetMapping
//import org.springframework.web.bind.annotation.PathVariable
//import org.springframework.web.bind.annotation.PostMapping
//import org.springframework.web.bind.annotation.RequestParam
//import org.springframework.web.bind.annotation.RestController
//import org.springframework.web.multipart.MultipartFile
//import java.io.File
//import java.net.URLConnection
//import java.util.concurrent.ConcurrentHashMap
//import javax.servlet.http.HttpServletResponse
//
//@RestController
//class CoreFileController {
//
//    @Value("\${root_dir}")
//    val rootDirName: String = ""
//
//    @Value("\${temp_dir}")
//    val tempDirName: String = ""
//
////------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
////    //--- прописывать у каждого наследника
////    @PostMapping("/api/get_file")
////    fun getFile(
////        @RequestBody
////        getFileRequest: GetFileRequest
////    ): GetFileResponse {
////        val getFileBegTime = getCurrentTimeInt()
////
////        val file = File(getFileRequest.altServerDirName ?: rootDirName, getFileRequest.fullFileName)
////        val getFileResponse = GetFileResponse(if (file.exists()) FileInputStream(file).readAllBytes() else null)
////        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
////        if (getCurrentTimeInt() - getFileBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
////            AdvancedLogger.error("--- Long Get File Query = " + (getCurrentTimeInt() - getFileBegTime))
////            AdvancedLogger.error(getFileRequest.toString())
////        }
////        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );
////
////        return getFileResponse
////    }
////
////    //--- прописывать у каждого наследника
////    @PostMapping("/api/put_file")
////    fun putFile(
////        @RequestBody
////        putFileRequest: PutFileRequest
////    ): PutFileResponse {
////        val putFileBegTime = getCurrentTimeInt()
////
////        val uploadFileName = putFileRequest.fullFileName
////
////        //--- для правильного срабатывания mkdirs надо выделить путь из общего имени файла
////        val (dirName, fileName) = separateUnixPath(uploadFileName)
////        val dir = File(rootDirName, dirName)
////        val file = File(dir, fileName)
////
////        dir.mkdirs()
////        val fos = FileOutputStream(file)
////        fos.write(putFileRequest.fileData)
////        fos.close()
////        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
////        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
////        //AdvancedLogger.debug( "FILE: file = $uploadFileName received from ${( selectionKey!!.channel() as SocketChannel ).socket().inetAddress}" )
////
////        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
////        if (getCurrentTimeInt() - putFileBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
////            AdvancedLogger.error("--- Long Put File Query = " + (getCurrentTimeInt() - putFileBegTime))
////            AdvancedLogger.error(putFileRequest.toString())
////        }
////        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );
////
////        return PutFileResponse()
////    }
//
//}