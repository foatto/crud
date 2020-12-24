//package foatto.shop.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_price")
//open class PriceEntity(
//
//    @Id
//    @Column(name = "id", unique = true, nullable = false)
//    var id: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "catalog_id", nullable = false)
//    var catalog: CatalogEntity,
//
//    @Basic
//    @Column(name = "price_type", nullable = false)
//    var type: Int,
//
//    var date: DateTimeField,
//
//    @Basic
//    @Column(name = "price_value", nullable = false)
//    var value: Double,
//
//    @Basic
//    @Column(name = "price_note", nullable = false, length = 250)
//    var note: String
//
//)
//
