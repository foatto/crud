package foatto.mms.spring.entities

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "MMS_sensor_calibration")
class SensorConfigCalibrationEntity(

    @Id
    val id: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sensor_id")
    val sensor: SensorConfigEntity,

    @Column(name = "value_sensor")
    val sensorValue: Double,

    @Column(name = "value_data")
    val sensorData: Double,

    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorConfigCalibrationEntity) return false

        if (sensor != other.sensor) return false
        if (sensorValue != other.sensorValue) return false
        if (sensorData != other.sensorData) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sensor.hashCode()
        result = 31 * result + sensorValue.hashCode()
        result = 31 * result + sensorData.hashCode()
        return result
    }
}
