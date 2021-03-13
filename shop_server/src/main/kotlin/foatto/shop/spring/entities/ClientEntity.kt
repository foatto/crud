package foatto.shop.spring.entities

import javax.persistence.*

@Entity
@Table(name = "SHOP_client")
class ClientEntity(
    @Id
    var id: Int,
    var name: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClientEntity) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

