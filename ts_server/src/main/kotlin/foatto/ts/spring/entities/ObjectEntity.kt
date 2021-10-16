package foatto.ts.spring.entities

import javax.persistence.*

@Entity
@Table(name = "TS_object")
class ObjectEntity(

    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int,

    val name: String,
    val model: String,

    @OneToMany(mappedBy = "obj", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var sensors: MutableSet<SensorConfigEntity> = mutableSetOf(),

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
