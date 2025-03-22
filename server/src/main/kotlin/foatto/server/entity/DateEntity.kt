package foatto.server.entity

import jakarta.persistence.Embeddable

@Embeddable
class DateEntity(
    val ye: Int?,
    val mo: Int?,
    val da: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateEntity) return false

        if (ye != other.ye) return false
        if (mo != other.mo) return false
        if (da != other.da) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ye ?: 0
        result = 31 * result + (mo ?: 0)
        result = 31 * result + (da ?: 0)
        return result
    }
}
