package foatto.shop.spring.entities

import javax.persistence.*

@Entity
@Table(name = "SHOP_catalog")
class CatalogEntity(

    @Id
    val id: Int,

    @Column(name = "in_active")
    val inActive: Int,

    @Column(name = "in_archive")
    val inArchive: Int,

    @Column(name = "parent_id")
    val parentId: Int,

    @Column(name = "record_type")
    val recordType: Int,

    val name: String,

    @Column(name = "is_production")
    val isProduction: Int,

    @Column(name = "profit_add")
    val profitAdd: Int,

    @Column(name = "is_mark")
    val isMark: Int,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CatalogEntity) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
