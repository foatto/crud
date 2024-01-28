package foatto.spring.entities

import javax.persistence.Embeddable

@Embeddable
class DateTimeEntity(
    val ye: Int,
    val mo: Int,
    val da: Int,
    val ho: Int,
    val mi: Int,
)