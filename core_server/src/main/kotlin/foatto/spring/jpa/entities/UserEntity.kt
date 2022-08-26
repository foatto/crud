package foatto.spring.jpa.entities

import javax.persistence.*

@Entity
@Table(name = "SYSTEM_users")
class UserEntity(

    @Id
    val id: Int,

    @Column(name = "parent_id")
    val parentId: Int,

    @Column(name = "user_id")
    val userId: Int,

    @Column(name = "is_disabled")
    val isDisabled: Int,

    @Column(name = "org_type")
    val orgType: Int,

    val login: String,

    @Column(name = "pwd")
    val password: String,

    @Column(name = "full_name")
    val fullName: String,

    @Column(name = "short_name")
    val shortName: String,

    @Column(name = "at_count")
    val atCount: Int,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "at_ye")),
        AttributeOverride(name = "mo", column = Column(name = "at_mo")),
        AttributeOverride(name = "da", column = Column(name = "at_da")),
        AttributeOverride(name = "ho", column = Column(name = "at_ho")),
        AttributeOverride(name = "mi", column = Column(name = "at_mi")),
    )
    @Embedded
    val lastLoginDateTime: DateTimeEntity,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "pwd_ye")),
        AttributeOverride(name = "mo", column = Column(name = "pwd_mo")),
        AttributeOverride(name = "da", column = Column(name = "pwd_da")),
    )
    @Embedded
    val passwordLastChangeDate: DateEntity,

    @Column(name = "e_mail")
    val eMail: String,

    @Column(name = "contact_info")
    val contactInfo: String,

    @Column(name = "file_id")
    val fileId: Int,

    @Column(name = "last_ip")
    val lastIP: String,

    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false

        if (parentId != other.parentId) return false
        if (userId != other.userId) return false
        if (isDisabled != other.isDisabled) return false
        if (orgType != other.orgType) return false
        if (login != other.login) return false
        if (password != other.password) return false
        if (fullName != other.fullName) return false
        if (shortName != other.shortName) return false
        if (atCount != other.atCount) return false
        if (lastLoginDateTime != other.lastLoginDateTime) return false
        if (passwordLastChangeDate != other.passwordLastChangeDate) return false
        if (eMail != other.eMail) return false
        if (contactInfo != other.contactInfo) return false
        if (fileId != other.fileId) return false
        if (lastIP != other.lastIP) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parentId
        result = 31 * result + userId
        result = 31 * result + isDisabled
        result = 31 * result + orgType
        result = 31 * result + login.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + shortName.hashCode()
        result = 31 * result + atCount
        result = 31 * result + lastLoginDateTime.hashCode()
        result = 31 * result + passwordLastChangeDate.hashCode()
        result = 31 * result + eMail.hashCode()
        result = 31 * result + contactInfo.hashCode()
        result = 31 * result + fileId
        result = 31 * result + lastIP.hashCode()
        return result
    }
}
