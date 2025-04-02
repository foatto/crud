package foatto.server.service

import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getFilledNumberString
import foatto.core.util.getRandomInt
import foatto.core.util.getRandomLong
import foatto.server.SpringApp
import foatto.server.entity.FileStoreEntity
import foatto.server.model.FileStoreData
import foatto.server.repository.FileStoreRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service
class FileStoreService(
    private val fileStoreRepository: FileStoreRepository,
) {

    companion object {
        const val FILE_URL_BASE: String = "file"
        private const val FILE_PATH_BASE = "files"

        private val chmRefData = ConcurrentHashMap<Long, FileStoreData>()
        private val chmRefTime = ConcurrentHashMap<Long, Int>()
    }

    @Value("\${root_dir}")
    private val rootDirName: String = ""

    @Value("\${temp_dir}")
    private val tempDirName: String = ""

    @Value("\${server_url}")
    private val serverUrl: String = ""

    fun getFileRefUrl(ref: Long): String = "$FILE_URL_BASE/$ref"

    fun getFileName(ref: Long): String? = chmRefData[ref]?.name

    fun getFileSize(ref: Long): Long? = chmRefData[ref]?.fileSize

    fun getFilePath(dirName: String?, fileName: String?) = "$FILE_PATH_BASE/$dirName/$fileName"

    fun getFileStoreData(ref: Long): FileStoreData? =
        if (getCurrentTimeInt() < (chmRefTime[ref] ?: 0)) {
            chmRefData[ref]
        } else {
            null
        }

    fun getShortFileLink(copyRef: Long, hour: Int): String {
        val newRef = getRandomLong()

        return chmRefData[copyRef]?.let { fileStoreData ->
            chmRefData[newRef] = fileStoreData
            chmRefTime[newRef] = getCurrentTimeInt() + hour * 3600

            serverUrl + "/" + getFileRefUrl(newRef)
        } ?: "(файл не найден)"
    }

    fun getFileList(fileId: Int, expireHour: Int): List<Long> =
        fileStoreRepository.findByFileIdOrderByName(fileId).mapNotNull { fileStoreEntity ->
            fileStoreEntity.name?.let { name ->
                fileStoreEntity.dir?.let { dir ->
                    val ref = getRandomLong()

                    chmRefData[ref] = FileStoreData(
                        id = fileStoreEntity.id,
                        name = name,
                        dir = dir,
                        fileSize = fileStoreEntity.fileSize,
                    )
                    chmRefTime[ref] = getCurrentTimeInt() + expireHour * 3600

                    ref
                }
            }
        }

    fun addFile(fileId: Int, tempFileName: String, fileName: String) {
        val fileFromClient = File(tempDirName, tempFileName)
        val fileSize = fileFromClient.length()
        val fileStoreId = getNextFileStoreId()

        val dirName = SpringApp.minioProxy?.let { minioProxy ->
            minioProxy.saveFile(
                objectName = fileStoreId.toString(),
                objectStream = fileFromClient.inputStream(),
                objectSize = fileFromClient.length()
            )
            fileFromClient.delete()

            fileStoreId.toString()
        } ?: run {
            val dirName = getFreeDir(fileName)
            val newFile = File(rootDirName, getFilePath(dirName, fileName))

            fileFromClient.renameTo(newFile)

            dirName
        }

        val fileStoreEntity = FileStoreEntity(
            id = fileStoreId,
            fileId = fileId,
            name = fileName,
            dir = dirName,
            fileSize = fileSize,
        )
        fileStoreRepository.saveAndFlush(fileStoreEntity)
    }

    fun deleteFile(fileId: Int?, fileStoreId: Int?) {
        val fileStoreEntities = fileStoreId?.let {
            fileStoreRepository.findByIdOrNull(fileStoreId)?.let { fse ->
                listOf(fse)
            } ?: emptyList()
        } ?: run {
            fileId?.let {
                fileStoreRepository.findByFileId(fileId)
            } ?: emptyList()
        }

        for (fileStoreEntity in fileStoreEntities) {
            SpringApp.minioProxy?.removeFile(
                objectName = fileStoreEntity.id.toString()
            ) ?: run {
                val deleteFile = File(rootDirName, getFilePath(fileStoreEntity.dir, fileStoreEntity.name))
                if (deleteFile.exists()) {
                    deleteFile.delete()
                }
            }
        }

        fileStoreRepository.deleteAllInBatch(fileStoreEntities)
    }

    fun getNextFileId(): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (fileStoreRepository.existsByFileId(nextId)) {
                continue
            }
            return nextId
        }
    }

    private fun getNextFileStoreId(): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (fileStoreRepository.existsById(nextId)) {
                continue
            }
            return nextId
        }
    }

    private fun getFreeDir(fileName: String): String {
        var i = 0
        while (true) {
            val newDirName = getFilledNumberString(i, 8)
            val newDir = File(rootDirName, "$FILE_PATH_BASE/$newDirName")
            newDir.mkdirs()
            val file = File(newDir, fileName)
            if (!file.exists()) {
                return newDirName
            }
            i++
        }
    }
}