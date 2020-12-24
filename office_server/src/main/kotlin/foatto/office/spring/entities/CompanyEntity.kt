//package foatto.office.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "OFFICE_company")
//open class CompanyEntity(
//
//    @Id
//    @Column(name = "id", unique = true, nullable = false)
//    var id: Int,
//
//    @Basic
//    @Column(name = "in_black_list", nullable = false)
//    var inBlackList: Boolean,
//
//    @Basic
//    @Column(name = "name", nullable = false, unique = true, length = 250)
//    var name: String,
//
//    @Basic
//    @Column(name = "address", nullable = false, length = -1)
//    var address: String,
//
//    @Basic
//    @Column(name = "contact_info", nullable = false, length = -1)
//    var contactInfo: String,
//
//    @AttributeOverrides(
//        AttributeOverride(name = "ye", column = Column(name = "birth_ye")),
//        AttributeOverride(name = "mo", column = Column(name = "birth_mo")),
//        AttributeOverride(name = "da", column = Column(name = "birth_da"))
//    )
//    var birthDate: DateTimeField,
//
//    @Basic
//    @Column(name = "file_id", nullable = false)
//    var fileId: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "city_id", nullable = false)
//    var catalog: CityEntity
//
//)
