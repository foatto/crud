/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables.records


import foatto.jooq.core.tables.SystemPermission

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record4
import org.jooq.Row4
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemPermissionRecord() : UpdatableRecordImpl<SystemPermissionRecord>(SystemPermission.SYSTEM_PERMISSION), Record4<Int?, Int?, String?, String?> {

    var id: Int?
        set(value) = set(0, value)
        get() = get(0) as Int?

    var classId: Int?
        set(value) = set(1, value)
        get() = get(1) as Int?

    var name: String?
        set(value) = set(2, value)
        get() = get(2) as String?

    var descr: String?
        set(value) = set(3, value)
        get() = get(3) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row4<Int?, Int?, String?, String?> = super.fieldsRow() as Row4<Int?, Int?, String?, String?>
    override fun valuesRow(): Row4<Int?, Int?, String?, String?> = super.valuesRow() as Row4<Int?, Int?, String?, String?>
    override fun field1(): Field<Int?> = SystemPermission.SYSTEM_PERMISSION.ID
    override fun field2(): Field<Int?> = SystemPermission.SYSTEM_PERMISSION.CLASS_ID
    override fun field3(): Field<String?> = SystemPermission.SYSTEM_PERMISSION.NAME
    override fun field4(): Field<String?> = SystemPermission.SYSTEM_PERMISSION.DESCR
    override fun component1(): Int? = id
    override fun component2(): Int? = classId
    override fun component3(): String? = name
    override fun component4(): String? = descr
    override fun value1(): Int? = id
    override fun value2(): Int? = classId
    override fun value3(): String? = name
    override fun value4(): String? = descr

    override fun value1(value: Int?): SystemPermissionRecord {
        this.id = value
        return this
    }

    override fun value2(value: Int?): SystemPermissionRecord {
        this.classId = value
        return this
    }

    override fun value3(value: String?): SystemPermissionRecord {
        this.name = value
        return this
    }

    override fun value4(value: String?): SystemPermissionRecord {
        this.descr = value
        return this
    }

    override fun values(value1: Int?, value2: Int?, value3: String?, value4: String?): SystemPermissionRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        return this
    }

    /**
     * Create a detached, initialised SystemPermissionRecord
     */
    constructor(id: Int? = null, classId: Int? = null, name: String? = null, descr: String? = null): this() {
        this.id = id
        this.classId = classId
        this.name = name
        this.descr = descr
    }
}