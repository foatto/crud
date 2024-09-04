package foatto.server.entity.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class BooleanToIntConverter : AttributeConverter<Boolean?, Int?> {

    override fun convertToDatabaseColumn(attribute: Boolean?): Int? =
        if (attribute == true) {
            1
        } else {
            0
        }

    override fun convertToEntityAttribute(dbData: Int?): Boolean? =
        dbData != 0
}