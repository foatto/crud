package foatto.server.entity

import foatto.server.entity.converter.BooleanToIntConverter
import foatto.server.entity.converter.SetToStringConverter
import jakarta.persistence.*

@Entity
@Table(name = "SYSTEM_users")
class UserEntity(

    @Id
    val id: Int,

    @Column(name = "parent_id")
    val parentId: Int?,

    @Column(name = "user_id")
    val userId: Int?,

    @Column(name = "org_type")
    val orgType: Int?,          //!!! Int for old-server compatibility

    @Convert(converter = BooleanToIntConverter::class)
    @Column(name = "is_disabled")
    val isDisabled: Boolean?,

    @Column(name = "full_name")
    val fullName: String?,

    @Column(name = "short_name")
    val shortName: String?,

    val login: String?,

    @Column(name = "pwd")
    var password: String?,

    @Convert(converter = SetToStringConverter::class)
    @Column(name = "app_role")
    val roles: Set<String> = emptySet(),

    @Column(name = "time_offset")
    var timeOffset: Int?,

    @Column(name = "e_mail")
    var eMail: String?,

    @Column(name = "contact_info")
    var contactInfo: String?,

    @Column(name = "use_thousands_divider")
    var useThousandsDivider: Boolean?,

    @Column(name = "decimal_separator")
    var decimalSeparator: String?,

    @Column(name = "file_id")
    var fileId: Int?,

    @Column(name = "at_count")
    val atCount: Int?,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "at_ye")),
        AttributeOverride(name = "mo", column = Column(name = "at_mo")),
        AttributeOverride(name = "da", column = Column(name = "at_da")),
        AttributeOverride(name = "ho", column = Column(name = "at_ho")),
        AttributeOverride(name = "mi", column = Column(name = "at_mi")),
        AttributeOverride(name = "se", column = Column(name = "at_se")),
    )
    @Embedded
    val lastLoginDateTime: DateTimeEntity,

    @Column(name = "last_ip")
    val lastIP: String?,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "pwd_ye")),
        AttributeOverride(name = "mo", column = Column(name = "pwd_mo")),
        AttributeOverride(name = "da", column = Column(name = "pwd_da")),
    )
    @Embedded
    val passwordLastChangeDate: DateEntity,

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false

        if (parentId != other.parentId) return false
        if (userId != other.userId) return false
        if (orgType != other.orgType) return false
        if (isDisabled != other.isDisabled) return false
        if (fullName != other.fullName) return false
        if (shortName != other.shortName) return false
        if (login != other.login) return false
        if (password != other.password) return false
        if (roles != other.roles) return false
        if (timeOffset != other.timeOffset) return false
        if (eMail != other.eMail) return false
        if (contactInfo != other.contactInfo) return false
        if (useThousandsDivider != other.useThousandsDivider) return false
        if (decimalSeparator != other.decimalSeparator) return false
        if (fileId != other.fileId) return false
        if (atCount != other.atCount) return false
        if (lastLoginDateTime != other.lastLoginDateTime) return false
        if (lastIP != other.lastIP) return false
        if (passwordLastChangeDate != other.passwordLastChangeDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parentId ?: 0
        result = 31 * result + (userId ?: 0)
        result = 31 * result + (orgType ?: 0)
        result = 31 * result + (isDisabled?.hashCode() ?: 0)
        result = 31 * result + (fullName?.hashCode() ?: 0)
        result = 31 * result + (shortName?.hashCode() ?: 0)
        result = 31 * result + (login?.hashCode() ?: 0)
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + roles.hashCode()
        result = 31 * result + (timeOffset ?: 0)
        result = 31 * result + (eMail?.hashCode() ?: 0)
        result = 31 * result + (contactInfo?.hashCode() ?: 0)
        result = 31 * result + (useThousandsDivider?.hashCode() ?: 0)
        result = 31 * result + (decimalSeparator?.hashCode() ?: 0)
        result = 31 * result + (fileId ?: 0)
        result = 31 * result + (atCount ?: 0)
        result = 31 * result + lastLoginDateTime.hashCode()
        result = 31 * result + (lastIP?.hashCode() ?: 0)
        result = 31 * result + passwordLastChangeDate.hashCode()
        return result
    }
}
