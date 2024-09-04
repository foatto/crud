package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "SYSTEM_users_property")
class UserPropertyEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @Column(name = "property_name")
    val name: String?,

    @Column(name = "property_value")
    var value: String?,

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserPropertyEntity) return false

        if (userId != other.userId) return false
        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}
