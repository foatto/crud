package foatto.server.entity

import foatto.server.entity.converter.BooleanToIntConverter
import jakarta.persistence.*

@Entity
@Table(name = "MMS_object")
class ObjectEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_disabled")
    val isDisabled: Boolean?,
    @Column(name = "disable_reason")
    val disableReason: String?,

    val name: String?,
    val model: String?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    val department: DepartmentEntity?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    val group: GroupEntity?,

    val info: String?,

    @Column(name = "e_mail")
    val eMail: String?,

    @Column(name = "scheme_file_id")
    val fileId: Int?,

//    @Column(name = "last_alert")
//    val lastAlertTime: Int?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_auto_work_shift")
    val isAutoWorkShiftEnabled: Boolean?,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectEntity) return false

        if (userId != other.userId) return false
        if (isDisabled != other.isDisabled) return false
        if (disableReason != other.disableReason) return false
        if (name != other.name) return false
        if (model != other.model) return false
        if (department != other.department) return false
        if (group != other.group) return false
        if (info != other.info) return false
        if (eMail != other.eMail) return false
        if (fileId != other.fileId) return false
//        if (lastAlertTime != other.lastAlertTime) return false
        if (isAutoWorkShiftEnabled != other.isAutoWorkShiftEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (isDisabled?.hashCode() ?: 0)
        result = 31 * result + (disableReason?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (model?.hashCode() ?: 0)
        result = 31 * result + (department?.hashCode() ?: 0)
        result = 31 * result + (group?.hashCode() ?: 0)
        result = 31 * result + (info?.hashCode() ?: 0)
        result = 31 * result + (eMail?.hashCode() ?: 0)
        result = 31 * result + (fileId ?: 0)
//        result = 31 * result + (lastAlertTime ?: 0)
        result = 31 * result + (isAutoWorkShiftEnabled?.hashCode() ?: 0)
        return result
    }
}
