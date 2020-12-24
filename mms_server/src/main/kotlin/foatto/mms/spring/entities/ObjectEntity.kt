package foatto.mms.spring.entities

import javax.persistence.*

@Entity
@Table(name = "MMS_object")
class ObjectEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "is_disabled")
    val isDisabled: Int,

    val name: String,
    val model: String,
    val info: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    val group: GroupEntity,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    val department: DepartmentEntity,

    @OneToMany(mappedBy = "obj", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var sensors: MutableSet<SensorConfigEntity> = mutableSetOf(),

    /*
            if (rs.next()) {
                objectConfig.objectID = aObjectID
                objectConfig.userID = rs.getInt(1)
                objectConfig.isDisabled = rs.getInt(2) != 0
                val sbObjectInfo = StringBuilder(rs.getString(3))
                objectConfig.model = rs.getString(4)
                objectConfig.groupName = rs.getString(5)
                objectConfig.departmentName = rs.getString(6)
                objectConfig.info = rs.getString(7)

                //--- дополним наименование объекта его кратким логинным названием
                val shortUserName = userConfig.hmUserShortNames[objectConfig.userID]
                if (shortUserName != null && !shortUserName.isEmpty()) sbObjectInfo.append(" ( ").append(shortUserName).append(" ) ")
                objectConfig.name = sbObjectInfo.toString()
            } else {
                AdvancedLogger.error("ObjectConfig not exist for object_id = $aObjectID")
            }

     */
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectEntity) return false

        if (userId != other.userId) return false
        if (name != other.name) return false
        if (model != other.model) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId
        result = 31 * result + name.hashCode()
        result = 31 * result + model.hashCode()
        return result
    }
}
