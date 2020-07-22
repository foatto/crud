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
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "task_id", nullable = false)
//    var task: TaskEntity,
//
//    var dateTime: DateTimeField,
//
//    @Basic
//    @Column(name = "message", nullable = false, length = -1)
//    var message: String,
//
//    @Basic
//    @Column(name = "file_id", nullable = false)
//    var fileId: Int
//)
