//package foatto.office.entity
//
//import foatto.core_server.app.server.entity.DateTimeField
//import javax.persistence.*
//
//@Entity
//@Table(name = "OFFICE_task_thread")
//open class TaskThreadEntity(
//
//    @Id
//    @Column(name = "id", unique = true)
//    var id: Int,
//
//    @Basic
//    @Column(name = "user_id")
//    var userId: Int,
//
////    @ManyToOne(fetch = FetchType.LAZY)
////    @JoinColumn(name = "user_id")
////    var user: UserEntity
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "task_id")
//    var task: TaskEntity,
//
//    var dateTime: DateTimeField,
//
//    @Basic
//    @Column(name = "message", length = -1)
//    var message: String,
//
//    @Basic
//    @Column(name = "file_id")
//    var fileId: Int
//)
