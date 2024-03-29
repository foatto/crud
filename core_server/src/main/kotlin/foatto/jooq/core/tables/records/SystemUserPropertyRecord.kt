/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables.records


import foatto.jooq.core.tables.SystemUserProperty

import org.jooq.Field
import org.jooq.Record3
import org.jooq.Row3
import org.jooq.impl.TableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemUserPropertyRecord() : TableRecordImpl<SystemUserPropertyRecord>(SystemUserProperty.SYSTEM_USER_PROPERTY), Record3<Int?, String?, String?> {

    var userId: Int?
        set(value) = set(0, value)
        get() = get(0) as Int?

    var propertyName: String?
        set(value) = set(1, value)
        get() = get(1) as String?

    var propertyValue: String?
        set(value) = set(2, value)
        get() = get(2) as String?

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row3<Int?, String?, String?> = super.fieldsRow() as Row3<Int?, String?, String?>
    override fun valuesRow(): Row3<Int?, String?, String?> = super.valuesRow() as Row3<Int?, String?, String?>
    override fun field1(): Field<Int?> = SystemUserProperty.SYSTEM_USER_PROPERTY.USER_ID
    override fun field2(): Field<String?> = SystemUserProperty.SYSTEM_USER_PROPERTY.PROPERTY_NAME
    override fun field3(): Field<String?> = SystemUserProperty.SYSTEM_USER_PROPERTY.PROPERTY_VALUE
    override fun component1(): Int? = userId
    override fun component2(): String? = propertyName
    override fun component3(): String? = propertyValue
    override fun value1(): Int? = userId
    override fun value2(): String? = propertyName
    override fun value3(): String? = propertyValue

    override fun value1(value: Int?): SystemUserPropertyRecord {
        this.userId = value
        return this
    }

    override fun value2(value: String?): SystemUserPropertyRecord {
        this.propertyName = value
        return this
    }

    override fun value3(value: String?): SystemUserPropertyRecord {
        this.propertyValue = value
        return this
    }

    override fun values(value1: Int?, value2: String?, value3: String?): SystemUserPropertyRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        return this
    }

    /**
     * Create a detached, initialised SystemUserPropertyRecord
     */
    constructor(userId: Int? = null, propertyName: String? = null, propertyValue: String? = null): this() {
        this.userId = userId
        this.propertyName = propertyName
        this.propertyValue = propertyValue
    }
}
