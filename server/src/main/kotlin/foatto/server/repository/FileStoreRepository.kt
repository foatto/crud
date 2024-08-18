package foatto.server.repository

import foatto.server.entity.FileStoreEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FileStoreRepository : JpaRepository<FileStoreEntity, Int> {
    fun findByFileId(fileId: Int): List<FileStoreEntity>
    fun findByFileIdOrderByName(fileId: Int): List<FileStoreEntity>
    fun existsByFileId(fileId: Int): Boolean
}
