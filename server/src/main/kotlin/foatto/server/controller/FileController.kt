package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.response.form.FormFileUploadResponse
import foatto.core.model.response.form.FormFileUploadParams
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

@RestController
class FileController {

    companion object {
        const val FILE_BASE: String = "files"

        val chmFileTime: ConcurrentHashMap<String, Long> = ConcurrentHashMap<String, Long>()

        fun getFileUrl(dirName: String?, fileName: String?): String = "$FILE_BASE/$dirName/$fileName"

        fun download(response: HttpServletResponse, path: String) {
            val file = File(path.replace("..", "").replace("//", ""))
            val mimeType = if (path.endsWith(".wasm", true)) {
                "application/wasm"
            } else {
                URLConnection.guessContentTypeFromName(file.name)
            }

            response.contentType = mimeType
            response.setContentLength(file.length().toInt())
            response.outputStream.write(file.readBytes())
        }
    }

    @Value("\${root_dir}")
    private val rootDirName: String = ""

    @Value("\${temp_dir}")
    private val tempDirName: String = ""

    @Value("\${file_access_period}")
    private val fileAccessPeriod: String = "24"  // 24 hour file accessibility by default

    @PostMapping(ApiUrl.UPLOAD_FORM_FILE)
    fun uploadFormFile(
        @RequestParam(FormFileUploadParams.FORM_FILE_IDS)
        arrFormFileId: Array<String>, // со стороны web-клиента ограничение на передачу массива или только строк или только файлов
        @RequestParam(FormFileUploadParams.FORM_FILE_BLOBS)
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {

        arrFormFileId.forEachIndexed { i, id ->
            arrFormFileBlob[i].transferTo(File(tempDirName, id))
        }

        return FormFileUploadResponse()
    }

    @GetMapping(value = ["/$FILE_BASE/{dirName}/{fileName}"])
    fun downloadFile(
        response: HttpServletResponse,
        @PathVariable("dirName")
        dirName: String,
        @PathVariable("fileName")
        fileName: String
    ) {
        val fileUrl = getFileUrl(dirName, fileName)
        val fileTime = chmFileTime[fileUrl] ?: 0L
        if (System.currentTimeMillis() < fileTime + (fileAccessPeriod.toIntOrNull() ?: 24) * 60 * 60) {
            download(response, "$rootDirName/$fileUrl")
        } else {
            response.status = 403   // forbidden
        }
    }

}