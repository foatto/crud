package foatto.server.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "MMS_sensor_calibration")
class SensorCalibrationEntity(

    @Id
    var id: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sensor_id")
    var sensor: SensorEntity?,

    @Column(name = "value_sensor")
    val sensorValue: Double?,

    @Column(name = "value_data")
    val dataValue: Double?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorCalibrationEntity) return false

        if (sensor != other.sensor) return false
        if (sensorValue != other.sensorValue) return false
        if (dataValue != other.dataValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensor?.hashCode() ?: 0
        result = 31 * result + (sensorValue?.hashCode() ?: 0)
        result = 31 * result + (dataValue?.hashCode() ?: 0)
        return result
    }
}
