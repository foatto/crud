package foatto.ts.spring.entities

import javax.persistence.*

@Entity
@Table(name = "TS_sensor")
class SensorConfigEntity(

    @Id
    val id: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    val obj: ObjectEntity,

    val name: String,       // inner/system sensor name for programmatically sensors adding

    @Column(name = "group_name")
    val group: String,      // sensor group name for sensors logical linking

    val descr: String,      // sensor visible description

    @Column(name = "port_num")
    val portNum: Int,

    @Column(name = "sensor_type")
    val sensorType: Int,

    //--- base sensor attributes

    @Column(name = "smooth_method")
    val smoothMethod: Int?,

    @Column(name = "smooth_time")
    val smoothTime: Int?,

    @Column(name = "ignore_min_sensor")
    val minIgnore: Double?,

    @Column(name = "ignore_max_sensor")
    val maxIgnore: Double?,

    //--- analogue sensor attributes

    @Column(name = "analog_min_view")
    val minView: Double?,

    @Column(name = "analog_max_view")
    val maxView: Double?,

    @Column(name = "analog_min_limit")
    val minLimit: Double?,

    @Column(name = "analog_max_limit")
    val maxLimit: Double?,

    @OneToMany(mappedBy = "sensor", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var calibration: MutableSet<SensorConfigCalibrationEntity> = mutableSetOf(),

    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorConfigEntity) return false

        if (obj != other.obj) return false
        if (name != other.name) return false
        if (group != other.group) return false
        if (descr != other.descr) return false
        if (portNum != other.portNum) return false
        if (sensorType != other.sensorType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = obj.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + group.hashCode()
        result = 31 * result + descr.hashCode()
        result = 31 * result + portNum
        result = 31 * result + sensorType
        return result
    }
}
