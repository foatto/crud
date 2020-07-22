//package foatto.office.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "OFFICE_people")
//open class PeopleEntity(
//
//    @Id
//    @Column(name = "id", unique = true, nullable = false)
//    var id: Int,
//
//    @Basic
//    @Column(name = "user_id", nullable = false)
//    var userId: Int,
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "user_id", nullable = false)
////    var user: UserEntity
//
//    @Basic
//    @Column(name = "manager_id", nullable = false)
//    var managerId: Int,
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "manager_id", nullable = false)
////    var manager: UserEntity
//
//    @Basic
//    @Column(name = "work_state", nullable = false)
//    var workState: Int,
//
//    @Basic  // Пользователи могут добавлять клиентов с одинаковым именем из-за специфических требований заказчика по доступу
//    @Column(name = "name", nullable = false, /*unique = true,*/ length = 250)
//    var name: String,
//
//    @Basic
//    @Column(name = "post", nullable = false, length = 250)
//    var post: String,
//
//    @Basic
//    @Column(name = "e_mail", nullable = false, length = 250)
//    var mail: String,
//
//    @Basic
//    @Column(name = "cell_no", nullable = false, length = 250)
//    var cell: String,
//
//    @Basic
//    @Column(name = "phone_no", nullable = false, length = 250)
//    var phone: String,
//
//    @Basic
//    @Column(name = "fax_no", nullable = false, length = 250)
//    var fax: String,
//
//    @Basic
//    @Column(name = "assistant_name", nullable = false, length = 250)
//    var assistantName: String,
//
//    @Basic
//    @Column(name = "assistant_mail", nullable = false, length = 250)
//    var assistantMail: String,
//
//    @Basic
//    @Column(name = "assistant_cell", nullable = false, length = 250)
//    var assistantCell: String,
//
//    @Basic
//    @Column(name = "contact_info", nullable = false, length = -1)
//    var contactInfo: String,
//
//    @Basic
//    @Column(name = "birth_ye", nullable = false)
//    var birthYe: Int,
//
//    @Basic
//    @Column(name = "birth_mo", nullable = false)
//    var birthMo: Int,
//
//    @Basic
//    @Column(name = "birth_da", nullable = false)
//    var birthDa: Int,
//
//    @Basic
//    @Column(name = "file_id", nullable = false)
//    var fileId: Int,
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "company_id", nullable = false)
//    var company: CompanyEntity,
//
//    @Basic
//    @Column(name = "business_id", nullable = false)
//    var businessId: Int,
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "business_id", nullable = false)
////    var business: BusinessEntity,
//
//    @AttributeOverrides(
//        AttributeOverride(name = "ye", column = Column(name = "action_ye")),
//        AttributeOverride(name = "mo", column = Column(name = "action_mo")),
//        AttributeOverride(name = "da", column = Column(name = "action_da")),
//        AttributeOverride(name = "ho", column = Column(name = "action_ho")),
//        AttributeOverride(name = "mi", column = Column(name = "action_mi"))
//    )
//    var actionDateTime: DateTimeField,
//
//    @AttributeOverrides(
//        AttributeOverride(name = "ye", column = Column(name = "plan_ye")),
//        AttributeOverride(name = "mo", column = Column(name = "plan_mo")),
//        AttributeOverride(name = "da", column = Column(name = "plan_da")),
//        AttributeOverride(name = "ho", column = Column(name = "plan_ho")),
//        AttributeOverride(name = "mi", column = Column(name = "plan_mi"))
//    )
//    var planDateTime: DateTimeField
//
//)
