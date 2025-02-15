package foatto.server.service

import foatto.core.util.getFilledNumberString
import foatto.core.util.getRandomInt
import foatto.server.SpringApp
import foatto.server.controller.FileController
import foatto.server.entity.FileStoreEntity
import foatto.server.repository.FileStoreRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileService(
    private val fileStoreRepository: FileStoreRepository,
) {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    @Value("\${temp_dir}")
    protected val tempDirName: String = ""

    fun getFileList(fileId: Int): List<Pair<Int, String>> =
        fileStoreRepository.findByFileIdOrderByName(fileId).map { fileStoreEntity ->
            val fileUrl = FileController.getFileUrl(fileStoreEntity.dir, fileStoreEntity.name)

            FileController.chmFileTime[fileUrl] = System.currentTimeMillis()

            Pair(fileStoreEntity.id, fileUrl)
        }

    fun addFile(fileId: Int, tempFileName: String, fileName: String) {
        val fileFromClient = File(tempDirName, tempFileName)
        val fileStoreId = getNextFileStoreId()

        val dirName = SpringApp.minioProxy?.let { minioProxy ->
            minioProxy.saveFile(
                objectName = fileStoreId.toString(),
                objectStream = fileFromClient.inputStream(),
                objectSize = fileFromClient.length()
            )

            fileStoreId.toString()
        } ?: run {
            val dirName = getFreeDir(fileName)
            val newFile = File(rootDirName, FileController.getFileUrl(dirName, fileName))

            fileFromClient.renameTo(newFile)

            dirName
        }

        val fileStoreEntity = FileStoreEntity(
            id = fileStoreId,
            fileId = fileId,
            name = fileName,
            dir = dirName,
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
                val deleteFile = File(rootDirName, FileController.getFileUrl(fileStoreEntity.dir, fileStoreEntity.name))
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
            val newDir = File(rootDirName, "${FileController.FILE_BASE}/$newDirName")
            newDir.mkdirs()
            val file = File(newDir, fileName)
            if (!file.exists()) {
                return newDirName
            }
            i++
        }
    }
}