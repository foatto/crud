package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "SYSTEM_file_store")
class FileStoreEntity(
    @Id
    val id: Int,

    @Column(name = "file_id")
    val fileId: Int?,

    val name: String?,

    val dir: String?,

    @Column(name = "file_size")
    val fileSize: Long?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileStoreEntity) return false

        if (fileId != other.fileId) return false
        if (name != other.name) return false
        if (dir != other.dir) return false
        if (fileSize != other.fileSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileId?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (dir?.hashCode() ?: 0)
        result = 31 * result + (fileSize?.hashCode() ?: 0)
        return result
    }
}