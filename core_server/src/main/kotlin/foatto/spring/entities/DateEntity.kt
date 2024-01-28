package foatto.spring.entities

import javax.persistence.Embeddable

@Embeddable
class DateEntity(
    val ye: Int,
    val mo: Int,
    val da: Int,
)
