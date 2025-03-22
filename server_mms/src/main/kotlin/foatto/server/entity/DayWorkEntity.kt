package foatto.server.entity

import jakarta.persistence.*

@Entity
@Table(name = "MMS_day_work")
class DayWorkEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    var obj: ObjectEntity?,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "ye")),
        AttributeOverride(name = "mo", column = Column(name = "mo")),
        AttributeOverride(name = "da", column = Column(name = "da")),
    )
    @Embedded
    val day: DateEntity?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DayWorkEntity) return false

        if (userId != other.userId) return false
        if (obj != other.obj) return false
        if (day != other.day) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (obj?.hashCode() ?: 0)
        result = 31 * result + (day?.hashCode() ?: 0)
        return result
    }
}
