package foatto.server.entity

import jakarta.persistence.*

@Entity
@Table(name = "MMS_device")
class DeviceEntity(
    @Id
    val id: Int,

    @Column(name = "user_id")
    val userId: Int?,

    @Column(name = "device_index")
    var index: Int?,

    @Column(name = "type")
    val type: Int?,

    @Column(name = "serial_no")
    val serialNo: String?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    var obj: ObjectEntity?,

    @Column(name = "cell_imei")
    val cellImei: String?,
    @Column(name = "cell_owner")
    val cellOwner: Int?,
    @Column(name = "cell_num")
    val cellNumber: String?,
    @Column(name = "cell_icc")
    val cellIcc: String?,
    @Column(name = "cell_operator")
    val cellOperator: String?,

    @Column(name = "cell_imei_2")
    val cellImei2: String?,
    @Column(name = "cell_owner_2")
    val cellOwner2: Int?,
    @Column(name = "cell_num_2")
    val cellNumber2: String?,
    @Column(name = "cell_icc_2")
    val cellIcc2: String?,
    @Column(name = "cell_operator_2")
    val cellOperator2: String?,

    @Column(name = "fw_version")
    val fwVersion: String?,

    @Column(name = "last_session_time")
    val lastSessionTime: Int?,
    @Column(name = "last_session_status")
    val lastSessionStatus: String?,
    @Column(name = "last_session_error")
    val lastSessionError: String?,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "beg_ye")),
        AttributeOverride(name = "mo", column = Column(name = "beg_mo")),
        AttributeOverride(name = "da", column = Column(name = "beg_da")),
    )
    @Embedded
    val usingStartDate: DateEntity?,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceEntity) return false

        if (userId != other.userId) return false
        if (index != other.index) return false
        if (type != other.type) return false
        if (serialNo != other.serialNo) return false
        if (obj != other.obj) return false
        if (cellImei != other.cellImei) return false
        if (cellOwner != other.cellOwner) return false
        if (cellNumber != other.cellNumber) return false
        if (cellIcc != other.cellIcc) return false
        if (cellOperator != other.cellOperator) return false
        if (cellImei2 != other.cellImei2) return false
        if (cellOwner2 != other.cellOwner2) return false
        if (cellNumber2 != other.cellNumber2) return false
        if (cellIcc2 != other.cellIcc2) return false
        if (cellOperator2 != other.cellOperator2) return false
        if (fwVersion != other.fwVersion) return false
        if (lastSessionTime != other.lastSessionTime) return false
        if (lastSessionStatus != other.lastSessionStatus) return false
        if (lastSessionError != other.lastSessionError) return false
        if (usingStartDate != other.usingStartDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId ?: 0
        result = 31 * result + (index ?: 0)
        result = 31 * result + (type ?: 0)
        result = 31 * result + (serialNo?.hashCode() ?: 0)
        result = 31 * result + (obj?.hashCode() ?: 0)
        result = 31 * result + (cellImei?.hashCode() ?: 0)
        result = 31 * result + (cellOwner ?: 0)
        result = 31 * result + (cellNumber?.hashCode() ?: 0)
        result = 31 * result + (cellIcc?.hashCode() ?: 0)
        result = 31 * result + (cellOperator?.hashCode() ?: 0)
        result = 31 * result + (cellImei2?.hashCode() ?: 0)
        result = 31 * result + (cellOwner2 ?: 0)
        result = 31 * result + (cellNumber2?.hashCode() ?: 0)
        result = 31 * result + (cellIcc2?.hashCode() ?: 0)
        result = 31 * result + (cellOperator2?.hashCode() ?: 0)
        result = 31 * result + (fwVersion?.hashCode() ?: 0)
        result = 31 * result + (lastSessionTime ?: 0)
        result = 31 * result + (lastSessionStatus?.hashCode() ?: 0)
        result = 31 * result + (lastSessionError?.hashCode() ?: 0)
        result = 31 * result + (usingStartDate?.hashCode() ?: 0)
        return result
    }
}
