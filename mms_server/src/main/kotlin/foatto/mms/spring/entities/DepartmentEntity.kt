package foatto.mms.spring.entities

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "MMS_department")
class DepartmentEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int,

    val name: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DepartmentEntity) return false

        if (userId != other.userId) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId
        result = 31 * result + name.hashCode()
        return result
    }
}