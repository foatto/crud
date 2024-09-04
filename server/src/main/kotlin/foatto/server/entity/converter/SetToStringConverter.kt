package foatto.server.entity.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class SetToStringConverter : AttributeConverter<Set<String>?, String?> {

    override fun convertToDatabaseColumn(attribute: Set<String>?): String? =
        attribute?.joinToString(" ")

    override fun convertToEntityAttribute(dbData: String?): Set<String>? =
        dbData?.split(' ')?.toSet() ?: emptySet()
}