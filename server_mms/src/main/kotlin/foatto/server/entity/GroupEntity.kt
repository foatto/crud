package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "MMS_group")
class GroupEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    val name: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupEntity) return false

        if (userId != other.userId) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }
}