package foatto.shop.spring.entities

import javax.persistence.*

@Entity
@Table(name = "SHOP_catalog")
class CatalogEntity(

    @Id
    val id: Int,

    @Column(name = "in_active", nullable = false)
    val inActive: Int,

    @Column(name = "in_archive", nullable = false)
    val inArchive: Int,

    //!!! EAGER здесь вряд ли подойдёт из-за возможного зацикливания с id == parent_id = 0 (там не null!)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parentCatalog: CatalogEntity?,

    @Column(name = "record_type")
    val recordType: Int,

    val name: String,

    @Column(name = "is_production")
    val isProduction: Int,

    @Column(name = "profit_add", nullable = false)
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
