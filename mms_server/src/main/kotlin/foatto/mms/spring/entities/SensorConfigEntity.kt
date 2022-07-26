package foatto.mms.spring.entities

import javax.persistence.*

@Entity
@Table(name = "MMS_sensor")
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

//--- not use
//    serial_no           VARCHAR( 250 ), -- серийный номер
//    beg_ye              INT,    -- дата ввода в эксплуатацию
//    beg_mo              INT,
//    beg_da              INT,

    //--- geo-sensor data

    @Column(name = "min_moving_time")
    val minMovingTime: Int?,

    @Column(name = "min_parking_time")
    val minParkingTime: Int?,

    @Column(name = "min_over_speed_time")
    val minOverSpeedTime: Int?,

    @Column(name = "is_absolute_run")
    val isAbsoluteRun: Int?,

    @Column(name = "speed_round_rule")
    val speedRoundRule: Int?,

    @Column(name = "run_koef")
    val runKoef: Double?,

    @Column(name = "is_use_pos")
    val isUsePos: Int?,

    @Column(name = "is_use_speed")
    val isUseSpeed: Int?,

    @Column(name = "is_use_run")
    val isUseRun: Int?,

    //--- discrete/work/signal sensors

    @Column(name = "bound_value")
    val boundValue: Int?,

    @Column(name = "active_value")
    val activeValue: Int?,

    @Column(name = "min_on_time")
    val minOnTime: Int?,

    @Column(name = "min_off_time")
    val minOffTime: Int?,

    @Column(name = "beg_work_value")
    val begWorkValue: Double?,

    @Column(name = "cmd_on_id")
    val cmdOnId: Int?,

    @Column(name = "cmd_off_id")
    val cmdOffId: Int?,

    @Column(name = "signal_on")
    val signalOn: String?,

    @Column(name = "signal_off")
    val signalOff: String?,

    //--- base sensor attributes

    @Column(name = "smooth_method")
    val smoothMethod: Int?,

    @Column(name = "smooth_time")
    val smoothTime: Int?,

    @Column(name = "ignore_min_sensor")
    val minIgnore: Double?,

    @Column(name = "ignore_max_sensor")
    val maxIgnore: Double?,

    @Column(name = "liquid_name")
    val liquidName: String?,

    @Column(name = "liquid_norm")
    val liquidNorm: Double?,

    //--- analogue sensor attributes

    @Column(name = "analog_min_view")
    val minView: Double?,

    @Column(name = "analog_max_view")
    val maxView: Double?,

    @Column(name = "analog_min_limit")
    val minLimit: Double?,

    @Column(name = "analog_max_limit")
    val maxLimit: Double?,

    //--- (fuel) counter sensor attributes

    @Column(name = "is_absolute_count")
    val isAbsoluteCount: Int?,

    //--- energo sensor attributes

    @Column(name = "energo_phase")
    val phase: Int?,

    //--- for counter and mass/volume accumulated sensors

    @Column(name = "in_out_type")
    val inOutType: Int?,

    //--- liquid level sensors only

    @Column(name = "container_type")
    val containerType: Int?,

    @Column(name = "analog_using_min_len")
    val usingMinLen: Int?,

    @Column(name = "analog_is_using_calc")
    val isUsingCalc: Int?,

    @Column(name = "analog_detect_inc")
    val detectIncKoef: Double?,

    @Column(name = "analog_detect_inc_min_diff")
    val detectIncMinDiff: Double?,

    @Column(name = "analog_detect_inc_min_len")
    val detectIncMinLen: Int?,

    @Column(name = "analog_inc_add_time_before")
    val incAddTimeBefore: Int?,

    @Column(name = "analog_inc_add_time_after")
    val incAddTimeAfter: Int?,

    @Column(name = "analog_detect_dec")
    val detectDecKoef: Double?,

    @Column(name = "analog_detect_dec_min_diff")
    val detectDecMinDiff: Double?,

    @Column(name = "analog_detect_dec_min_len")
    val detectDecMinLen: Int?,

    @Column(name = "analog_dec_add_time_before")
    val decAddTimeBefore: Int?,

    @Column(name = "analog_dec_add_time_after")
    val decAddTimeAfter: Int?,

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
