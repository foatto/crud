package foatto.server.entity

import foatto.server.entity.converter.BooleanToIntConverter
import jakarta.persistence.*

@Entity
@Table(name = "MMS_sensor")
class SensorEntity(

    @Id
    var id: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    var obj: ObjectEntity?,

    val name: String?,       // inner/system sensor name for programmatically sensors adding

    @Column(name = "group_name")
    val group: String?,      // sensor group name for sensors logical linking and/or grouping

    val descr: String?,      // sensor visible description

    @Column(name = "port_num")
    var portNum: Int?,

    @Column(name = "sensor_type")
    val sensorType: Int?,

    @Column(name = "serial_no")
    val serialNo: String?,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "beg_ye")),
        AttributeOverride(name = "mo", column = Column(name = "beg_mo")),
        AttributeOverride(name = "da", column = Column(name = "beg_da")),
    )
    @Embedded
    val usingStartDate: DateEntity?,

    //--- geo-sensor data

    @Column(name = "min_moving_time")
    val minMovingTime: Int?,

    @Column(name = "min_parking_time")
    val minParkingTime: Int?,

    @Column(name = "min_over_speed_time")
    val minOverSpeedTime: Int?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_absolute_run")
    val isAbsoluteRun: Boolean?,

    @Column(name = "speed_round_rule")
    val speedRoundRule: Int?,

    @Column(name = "run_koef")
    val runKoef: Double?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_use_pos")
    val isUsePos: Boolean?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_use_speed")
    val isUseSpeed: Boolean?,

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_use_run")
    val isUseRun: Boolean?,

    //--- discrete/work/signal sensors

    @Column(name = "bound_value")
    val boundValue: Int?,

    @Column(name = "active_value")
    val activeValue: Int?,

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

    //--- discrete/work/signal & counter sensors

    @Column(name = "min_on_time")
    val minOnTime: Int?,

    @Column(name = "min_off_time")
    val minOffTime: Int?,

    //--- base sensor attributes

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

    @Column(name = "analog_indicator_delimiter_count")
    val indicatorDelimiterCount: Int?,

    @Column(name = "analog_indicator_multiplicator")
    val indicatorMultiplicator: Double?,

    //--- (fuel) counter sensor attributes

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_absolute_count")
    val isAbsoluteCount: Boolean?,

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

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "analog_is_using_calc")
    val isUsingCalc: Boolean?,

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

    @Column(name = "scheme_x")
    val schemeX: Int?,

    @Column(name = "scheme_y")
    val schemeY: Int?,

    //!!! для совместимости со старой версией
    @Column(name = "smooth_method")
    val smoothMethod: Int?,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorEntity) return false

        if (obj != other.obj) return false
        if (name != other.name) return false
        if (group != other.group) return false
        if (descr != other.descr) return false
        if (portNum != other.portNum) return false
        if (sensorType != other.sensorType) return false
        if (serialNo != other.serialNo) return false
        if (usingStartDate != other.usingStartDate) return false
        if (minMovingTime != other.minMovingTime) return false
        if (minParkingTime != other.minParkingTime) return false
        if (minOverSpeedTime != other.minOverSpeedTime) return false
        if (isAbsoluteRun != other.isAbsoluteRun) return false
        if (speedRoundRule != other.speedRoundRule) return false
        if (runKoef != other.runKoef) return false
        if (isUsePos != other.isUsePos) return false
        if (isUseSpeed != other.isUseSpeed) return false
        if (isUseRun != other.isUseRun) return false
        if (boundValue != other.boundValue) return false
        if (activeValue != other.activeValue) return false
        if (begWorkValue != other.begWorkValue) return false
        if (cmdOnId != other.cmdOnId) return false
        if (cmdOffId != other.cmdOffId) return false
        if (signalOn != other.signalOn) return false
        if (signalOff != other.signalOff) return false
        if (minOnTime != other.minOnTime) return false
        if (minOffTime != other.minOffTime) return false
        if (smoothTime != other.smoothTime) return false
        if (minIgnore != other.minIgnore) return false
        if (maxIgnore != other.maxIgnore) return false
        if (liquidName != other.liquidName) return false
        if (liquidNorm != other.liquidNorm) return false
        if (minView != other.minView) return false
        if (maxView != other.maxView) return false
        if (minLimit != other.minLimit) return false
        if (maxLimit != other.maxLimit) return false
        if (isAbsoluteCount != other.isAbsoluteCount) return false
        if (phase != other.phase) return false
        if (inOutType != other.inOutType) return false
        if (containerType != other.containerType) return false
        if (usingMinLen != other.usingMinLen) return false
        if (isUsingCalc != other.isUsingCalc) return false
        if (detectIncKoef != other.detectIncKoef) return false
        if (detectIncMinDiff != other.detectIncMinDiff) return false
        if (detectIncMinLen != other.detectIncMinLen) return false
        if (incAddTimeBefore != other.incAddTimeBefore) return false
        if (incAddTimeAfter != other.incAddTimeAfter) return false
        if (detectDecKoef != other.detectDecKoef) return false
        if (detectDecMinDiff != other.detectDecMinDiff) return false
        if (detectDecMinLen != other.detectDecMinLen) return false
        if (decAddTimeBefore != other.decAddTimeBefore) return false
        if (decAddTimeAfter != other.decAddTimeAfter) return false
        if (schemeX != other.schemeX) return false
        if (schemeY != other.schemeY) return false

        return true
    }

    override fun hashCode(): Int {
        var result = obj.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + group.hashCode()
        result = 31 * result + descr.hashCode()
        result = 31 * result + (portNum ?: 0)
        result = 31 * result + (sensorType ?: 0)
        result = 31 * result + serialNo.hashCode()
        result = 31 * result + usingStartDate.hashCode()
        result = 31 * result + (minMovingTime ?: 0)
        result = 31 * result + (minParkingTime ?: 0)
        result = 31 * result + (minOverSpeedTime ?: 0)
        result = 31 * result + (isAbsoluteRun?.hashCode() ?: 0)
        result = 31 * result + (speedRoundRule ?: 0)
        result = 31 * result + (runKoef?.hashCode() ?: 0)
        result = 31 * result + (isUsePos?.hashCode() ?: 0)
        result = 31 * result + (isUseSpeed?.hashCode() ?: 0)
        result = 31 * result + (isUseRun?.hashCode() ?: 0)
        result = 31 * result + (boundValue ?: 0)
        result = 31 * result + (activeValue ?: 0)
        result = 31 * result + (begWorkValue?.hashCode() ?: 0)
        result = 31 * result + (cmdOnId ?: 0)
        result = 31 * result + (cmdOffId ?: 0)
        result = 31 * result + (signalOn?.hashCode() ?: 0)
        result = 31 * result + (signalOff?.hashCode() ?: 0)
        result = 31 * result + (minOnTime ?: 0)
        result = 31 * result + (minOffTime ?: 0)
        result = 31 * result + (smoothTime ?: 0)
        result = 31 * result + (minIgnore?.hashCode() ?: 0)
        result = 31 * result + (maxIgnore?.hashCode() ?: 0)
        result = 31 * result + (liquidName?.hashCode() ?: 0)
        result = 31 * result + (liquidNorm?.hashCode() ?: 0)
        result = 31 * result + (minView?.hashCode() ?: 0)
        result = 31 * result + (maxView?.hashCode() ?: 0)
        result = 31 * result + (minLimit?.hashCode() ?: 0)
        result = 31 * result + (maxLimit?.hashCode() ?: 0)
        result = 31 * result + (isAbsoluteCount?.hashCode() ?: 0)
        result = 31 * result + (phase ?: 0)
        result = 31 * result + (inOutType ?: 0)
        result = 31 * result + (containerType ?: 0)
        result = 31 * result + (usingMinLen ?: 0)
        result = 31 * result + (isUsingCalc?.hashCode() ?: 0)
        result = 31 * result + (detectIncKoef?.hashCode() ?: 0)
        result = 31 * result + (detectIncMinDiff?.hashCode() ?: 0)
        result = 31 * result + (detectIncMinLen ?: 0)
        result = 31 * result + (incAddTimeBefore ?: 0)
        result = 31 * result + (incAddTimeAfter ?: 0)
        result = 31 * result + (detectDecKoef?.hashCode() ?: 0)
        result = 31 * result + (detectDecMinDiff?.hashCode() ?: 0)
        result = 31 * result + (detectDecMinLen ?: 0)
        result = 31 * result + (decAddTimeBefore ?: 0)
        result = 31 * result + (decAddTimeAfter ?: 0)
        result = 31 * result + (schemeX ?: 0)
        result = 31 * result + (schemeY ?: 0)

        return result
    }

}
