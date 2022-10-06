//package foatto.shop.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "SHOP_cash")
//open class CashEntity(
//
//    @Id
//    @Column(name = "id", unique = true)
//    var id: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "warehouse_id")
//    var warehouse: WarehouseEntity,
//
//    var date: DateTimeField,
//
//    @Basic
//    @Column(name = "cash_put")
//    var cashPut: Double,
//
//    @Basic
//    @Column(name = "cash_used")
//    var cashUsed: Double,
//
//    @Basic
//    @Column(name = "debt_out")
//    var debtOut: Double,
//
//    @Basic
//    @Column(name = "debt_in")
//    var debtIn: Double,
//
//    @Basic
//    @Column(name = "cash_rest")
//    var cashRest: Double,
//
//    @Basic
//    @Column(name = "descr", length = -1)
//    var descr: String
//
//)
