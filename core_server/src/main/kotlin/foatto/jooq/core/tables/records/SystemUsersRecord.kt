/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables.records


import foatto.jooq.core.tables.SystemUsers

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record22
import org.jooq.Row22
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemUsersRecord() : UpdatableRecordImpl<SystemUsersRecord>(SystemUsers.SYSTEM_USERS), Record22<Int?, Int?, Int?, Int?, String?, String?, String?, String?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, String?, String?, Int?, String?, Int?> {

    var id: Int?
        set(value) = set(0, value)
        get() = get(0) as Int?

    var parentId: Int?
        set(value) = set(1, value)
        get() = get(1) as Int?

    var isDisabled: Int?
        set(value) = set(2, value)
        get() = get(2) as Int?

    var orgType: Int?
        set(value) = set(3, value)
        get() = get(3) as Int?

    var login: String?
        set(value) = set(4, value)
        get() = get(4) as String?

    var pwd: String?
        set(value) = set(5, value)
        get() = get(5) as String?

    var fullName: String?
        set(value) = set(6, value)
        get() = get(6) as String?

    var shortName: String?
        set(value) = set(7, value)
        get() = get(7) as String?

    var atCount: Int?
        set(value) = set(8, value)
        get() = get(8) as Int?

    var atYe: Int?
        set(value) = set(9, value)
        get() = get(9) as Int?

    var atMo: Int?
        set(value) = set(10, value)
        get() = get(10) as Int?

    var atDa: Int?
        set(value) = set(11, value)
        get() = get(11) as Int?

    var atHo: Int?
        set(value) = set(12, value)
        get() = get(12) as Int?

    var atMi: Int?
        set(value) = set(13, value)
        get() = get(13) as Int?

    var pwdYe: Int?
        set(value) = set(14, value)
        get() = get(14) as Int?

    var pwdMo: Int?
        set(value) = set(15, value)
        get() = get(15) as Int?

    var pwdDa: Int?
        set(value) = set(16, value)
        get() = get(16) as Int?

    var eMail: String?
        set(value) = set(17, value)
        get() = get(17) as String?

    var contactInfo: String?
        set(value) = set(18, value)
        get() = get(18) as String?

    var fileId: Int?
        set(value) = set(19, value)
        get() = get(19) as Int?

    var lastIp: String?
        set(value) = set(20, value)
        get() = get(20) as String?

    var userId: Int?
        set(value) = set(21, value)
        get() = get(21) as Int?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record22 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row22<Int?, Int?, Int?, Int?, String?, String?, String?, String?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, String?, String?, Int?, String?, Int?> = super.fieldsRow() as Row22<Int?, Int?, Int?, Int?, String?, String?, String?, String?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, String?, String?, Int?, String?, Int?>
    override fun valuesRow(): Row22<Int?, Int?, Int?, Int?, String?, String?, String?, String?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, String?, String?, Int?, String?, Int?> = super.valuesRow() as Row22<Int?, Int?, Int?, Int?, String?, String?, String?, String?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, Int?, String?, String?, Int?, String?, Int?>
    override fun field1(): Field<Int?> = SystemUsers.SYSTEM_USERS.ID
    override fun field2(): Field<Int?> = SystemUsers.SYSTEM_USERS.PARENT_ID
    override fun field3(): Field<Int?> = SystemUsers.SYSTEM_USERS.IS_DISABLED
    override fun field4(): Field<Int?> = SystemUsers.SYSTEM_USERS.ORG_TYPE
    override fun field5(): Field<String?> = SystemUsers.SYSTEM_USERS.LOGIN
    override fun field6(): Field<String?> = SystemUsers.SYSTEM_USERS.PWD
    override fun field7(): Field<String?> = SystemUsers.SYSTEM_USERS.FULL_NAME
    override fun field8(): Field<String?> = SystemUsers.SYSTEM_USERS.SHORT_NAME
    override fun field9(): Field<Int?> = SystemUsers.SYSTEM_USERS.AT_COUNT
    override fun field10(): Field<Int?> = SystemUsers.SYSTEM_USERS.AT_YE
    override fun field11(): Field<Int?> = SystemUsers.SYSTEM_USERS.AT_MO
    override fun field12(): Field<Int?> = SystemUsers.SYSTEM_USERS.AT_DA
    override fun field13(): Field<Int?> = SystemUsers.SYSTEM_USERS.AT_HO
    override fun field14(): Field<Int?> = SystemUsers.SYSTEM_USERS.AT_MI
    override fun field15(): Field<Int?> = SystemUsers.SYSTEM_USERS.PWD_YE
    override fun field16(): Field<Int?> = SystemUsers.SYSTEM_USERS.PWD_MO
    override fun field17(): Field<Int?> = SystemUsers.SYSTEM_USERS.PWD_DA
    override fun field18(): Field<String?> = SystemUsers.SYSTEM_USERS.E_MAIL
    override fun field19(): Field<String?> = SystemUsers.SYSTEM_USERS.CONTACT_INFO
    override fun field20(): Field<Int?> = SystemUsers.SYSTEM_USERS.FILE_ID
    override fun field21(): Field<String?> = SystemUsers.SYSTEM_USERS.LAST_IP
    override fun field22(): Field<Int?> = SystemUsers.SYSTEM_USERS.USER_ID
    override fun component1(): Int? = id
    override fun component2(): Int? = parentId
    override fun component3(): Int? = isDisabled
    override fun component4(): Int? = orgType
    override fun component5(): String? = login
    override fun component6(): String? = pwd
    override fun component7(): String? = fullName
    override fun component8(): String? = shortName
    override fun component9(): Int? = atCount
    override fun component10(): Int? = atYe
    override fun component11(): Int? = atMo
    override fun component12(): Int? = atDa
    override fun component13(): Int? = atHo
    override fun component14(): Int? = atMi
    override fun component15(): Int? = pwdYe
    override fun component16(): Int? = pwdMo
    override fun component17(): Int? = pwdDa
    override fun component18(): String? = eMail
    override fun component19(): String? = contactInfo
    override fun component20(): Int? = fileId
    override fun component21(): String? = lastIp
    override fun component22(): Int? = userId
    override fun value1(): Int? = id
    override fun value2(): Int? = parentId
    override fun value3(): Int? = isDisabled
    override fun value4(): Int? = orgType
    override fun value5(): String? = login
    override fun value6(): String? = pwd
    override fun value7(): String? = fullName
    override fun value8(): String? = shortName
    override fun value9(): Int? = atCount
    override fun value10(): Int? = atYe
    override fun value11(): Int? = atMo
    override fun value12(): Int? = atDa
    override fun value13(): Int? = atHo
    override fun value14(): Int? = atMi
    override fun value15(): Int? = pwdYe
    override fun value16(): Int? = pwdMo
    override fun value17(): Int? = pwdDa
    override fun value18(): String? = eMail
    override fun value19(): String? = contactInfo
    override fun value20(): Int? = fileId
    override fun value21(): String? = lastIp
    override fun value22(): Int? = userId

    override fun value1(value: Int?): SystemUsersRecord {
        this.id = value
        return this
    }

    override fun value2(value: Int?): SystemUsersRecord {
        this.parentId = value
        return this
    }

    override fun value3(value: Int?): SystemUsersRecord {
        this.isDisabled = value
        return this
    }

    override fun value4(value: Int?): SystemUsersRecord {
        this.orgType = value
        return this
    }

    override fun value5(value: String?): SystemUsersRecord {
        this.login = value
        return this
    }

    override fun value6(value: String?): SystemUsersRecord {
        this.pwd = value
        return this
    }

    override fun value7(value: String?): SystemUsersRecord {
        this.fullName = value
        return this
    }

    override fun value8(value: String?): SystemUsersRecord {
        this.shortName = value
        return this
    }

    override fun value9(value: Int?): SystemUsersRecord {
        this.atCount = value
        return this
    }

    override fun value10(value: Int?): SystemUsersRecord {
        this.atYe = value
        return this
    }

    override fun value11(value: Int?): SystemUsersRecord {
        this.atMo = value
        return this
    }

    override fun value12(value: Int?): SystemUsersRecord {
        this.atDa = value
        return this
    }

    override fun value13(value: Int?): SystemUsersRecord {
        this.atHo = value
        return this
    }

    override fun value14(value: Int?): SystemUsersRecord {
        this.atMi = value
        return this
    }

    override fun value15(value: Int?): SystemUsersRecord {
        this.pwdYe = value
        return this
    }

    override fun value16(value: Int?): SystemUsersRecord {
        this.pwdMo = value
        return this
    }

    override fun value17(value: Int?): SystemUsersRecord {
        this.pwdDa = value
        return this
    }

    override fun value18(value: String?): SystemUsersRecord {
        this.eMail = value
        return this
    }

    override fun value19(value: String?): SystemUsersRecord {
        this.contactInfo = value
        return this
    }

    override fun value20(value: Int?): SystemUsersRecord {
        this.fileId = value
        return this
    }

    override fun value21(value: String?): SystemUsersRecord {
        this.lastIp = value
        return this
    }

    override fun value22(value: Int?): SystemUsersRecord {
        this.userId = value
        return this
    }

    override fun values(value1: Int?, value2: Int?, value3: Int?, value4: Int?, value5: String?, value6: String?, value7: String?, value8: String?, value9: Int?, value10: Int?, value11: Int?, value12: Int?, value13: Int?, value14: Int?, value15: Int?, value16: Int?, value17: Int?, value18: String?, value19: String?, value20: Int?, value21: String?, value22: Int?): SystemUsersRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        this.value7(value7)
        this.value8(value8)
        this.value9(value9)
        this.value10(value10)
        this.value11(value11)
        this.value12(value12)
        this.value13(value13)
        this.value14(value14)
        this.value15(value15)
        this.value16(value16)
        this.value17(value17)
        this.value18(value18)
        this.value19(value19)
        this.value20(value20)
        this.value21(value21)
        this.value22(value22)
        return this
    }

    /**
     * Create a detached, initialised SystemUsersRecord
     */
    constructor(id: Int? = null, parentId: Int? = null, isDisabled: Int? = null, orgType: Int? = null, login: String? = null, pwd: String? = null, fullName: String? = null, shortName: String? = null, atCount: Int? = null, atYe: Int? = null, atMo: Int? = null, atDa: Int? = null, atHo: Int? = null, atMi: Int? = null, pwdYe: Int? = null, pwdMo: Int? = null, pwdDa: Int? = null, eMail: String? = null, contactInfo: String? = null, fileId: Int? = null, lastIp: String? = null, userId: Int? = null): this() {
        this.id = id
        this.parentId = parentId
        this.isDisabled = isDisabled
        this.orgType = orgType
        this.login = login
        this.pwd = pwd
        this.fullName = fullName
        this.shortName = shortName
        this.atCount = atCount
        this.atYe = atYe
        this.atMo = atMo
        this.atDa = atDa
        this.atHo = atHo
        this.atMi = atMi
        this.pwdYe = pwdYe
        this.pwdMo = pwdMo
        this.pwdDa = pwdDa
        this.eMail = eMail
        this.contactInfo = contactInfo
        this.fileId = fileId
        this.lastIp = lastIp
        this.userId = userId
    }
}
