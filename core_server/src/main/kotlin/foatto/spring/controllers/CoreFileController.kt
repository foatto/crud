package foatto.spring.controllers

import foatto.core.link.FormFileUploadResponse
import foatto.core.link.GetFileRequest
import foatto.core.link.GetFileResponse
import foatto.core.link.PutFileRequest
import foatto.core.link.PutFileResponse
import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.separateUnixPath
import foatto.spring.CoreSpringApp
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

//--- добавлять у каждого наследника
//@RestController
abstract class CoreFileController {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    @Value("\${temp_dir}")
    val tempDirName: String = ""

    //--- прописывать у каждого наследника
//    @PostMapping("/api/get_file")
    open fun getFile(
        //@RequestBody
        getFileRequest: GetFileRequest
    ): GetFileResponse {
        val getFileBegTime = getCurrentTimeInt()

        val file = File(getFileRequest.altServerDirName ?: rootDirName, getFileRequest.fullFileName)
        val getFileResponse = GetFileResponse(if (file.exists()) FileInputStream(file).readAllBytes() else null)
        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - getFileBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Get File Query = " + (getCurrentTimeInt() - getFileBegTime))
            AdvancedLogger.error(getFileRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return getFileResponse
    }

    //--- прописывать у каждого наследника
//    @PostMapping("/api/put_file")
    open fun putFile(
        //@RequestBody
        putFileRequest: PutFileRequest
    ): PutFileResponse {
        val putFileBegTime = getCurrentTimeInt()

        val uploadFileName = putFileRequest.fullFileName

        //--- для правильного срабатывания mkdirs надо выделить путь из общего имени файла
        val (dirName, fileName) = separateUnixPath(uploadFileName)
        val dir = File(rootDirName, dirName)
        val file = File(dir, fileName)

        dir.mkdirs()
        val fos = FileOutputStream(file)
        fos.write(putFileRequest.fileData)
        fos.close()
        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
        //AdvancedLogger.debug( "FILE: file = $uploadFileName received from ${( selectionKey!!.channel() as SocketChannel ).socket().inetAddress}" )

        //--- если запрос длился/обрабатывался дольше MAX_TIME_PER_REQUEST, покажем его
        if (getCurrentTimeInt() - putFileBegTime > CoreSpringApp.MAX_TIME_PER_REQUEST) {
            AdvancedLogger.error("--- Long Put File Query = " + (getCurrentTimeInt() - putFileBegTime))
            AdvancedLogger.error(putFileRequest.toString())
        }
        //AdvancedLogger.error( "Query time = " + ( System.currentTimeMillis() - appBegTime ) / 1000 );

        return PutFileResponse()
    }

    //--- прописывать у каждого наследника
//    @PostMapping("/api/upload_form_file")
    open fun uploadFormFile(
//        @RequestParam("form_file_ids")
        arrFormFileId: Array<String>, // со стороны web-клиента ограничение на передачу массива или только строк или только файлов
//        @RequestParam("form_file_blobs")
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {

        arrFormFileId.forEachIndexed { i, id ->
            arrFormFileBlob[i].transferTo(File(tempDirName, id))
        }

        return FormFileUploadResponse()
    }

}