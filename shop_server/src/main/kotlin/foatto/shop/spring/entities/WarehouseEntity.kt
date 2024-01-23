package foatto.shop.spring.entities

import javax.persistence.*

@Entity
@Table(name = "SHOP_warehouse")
class WarehouseEntity(
    @Id
    val id: Int,
    var name: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WarehouseEntity) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

