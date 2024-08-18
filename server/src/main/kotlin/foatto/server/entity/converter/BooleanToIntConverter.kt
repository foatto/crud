package foatto.server.entity.converter

import javax.persistence.AttributeConverter
import javax.persistence.Converter

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