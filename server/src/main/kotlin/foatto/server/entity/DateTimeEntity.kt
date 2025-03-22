package foatto.server.entity

import jakarta.persistence.Embeddable

@Embeddable
class DateTimeEntity(
    val ye: Int?,
    val mo: Int?,
    val da: Int?,
    val ho: Int?,
    val mi: Int?,
    val se: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateTimeEntity) return false

        if (ye != other.ye) return false
        if (mo != other.mo) return false
        if (da != other.da) return false
        if (ho != other.ho) return false
        if (mi != other.mi) return false
        if (se != other.se) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ye ?: 0
        result = 31 * result + (mo ?: 0)
        result = 31 * result + (da ?: 0)
        result = 31 * result + (ho ?: 0)
        result = 31 * result + (mi ?: 0)
        result = 31 * result + (se ?: 0)
        return result
    }
}