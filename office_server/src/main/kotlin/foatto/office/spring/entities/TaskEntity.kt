//package foatto.office.entity
//
//import foatto.core_server.app.server.entity.DateField
//import javax.persistence.Basic
//import javax.persistence.Column
//import javax.persistence.Entity
//import javax.persistence.Id
//import javax.persistence.Table
//
//@Entity
//@Table(name = "OFFICE_task")
//open class TaskEntity(
//
//    @Id
//    @Column(name = "id", unique = true)
//    var id: Int,
//
//    @Basic
//    @Column(name = "out_user_id")
//    var outUserId: Int,
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "out_user_id")
////    var outUser: UserEntity
//
//    @Basic
//    @Column(name = "in_user_id")
//    var inUserId: Int,
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "in_user_id")
////    var inUser: UserEntity
//
//    @Basic
//    @Column(name = "in_archive")
//    var inArchive: Boolean,
//
//    var date: DateField,
//
//    @Basic
//    @Column(name = "subj", length = -1)
//    var subj: String,
//
//    @Basic
//    @Column(name = "file_id")
//    var fileId: Int
//
//)
