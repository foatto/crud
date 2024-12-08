package foatto.ts.core_ts.calc

class SensorData(
    private val i: Int? = null,
    private val d: Double? = null,
    private val s: String? = null,
) {
    fun toInt(): Int = i ?: d?.toInt() ?: 0
    fun toDouble(): Double = d ?: i?.toDouble() ?: 0.0
    override fun toString(): String = s ?: "null"
}