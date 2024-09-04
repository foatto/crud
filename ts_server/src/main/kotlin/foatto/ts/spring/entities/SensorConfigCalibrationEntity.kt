package foatto.ts.spring.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "TS_sensor_calibration")
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
