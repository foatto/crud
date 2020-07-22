package foatto.mms.core_mms.sensor

class SensorConfigU( aId: Int, aName: String, aSumGroup: String, aGroup: String, aDescr: String, aPortNum: Int, aSensorType: Int,
                     val sensorValue: Int, val dataValue: Double,
                     val minIgnore: Int, val maxIgnore: Int,
                     aLiquidName: String ) : SensorConfig( aId, aName, aSumGroup, aGroup, aDescr, aPortNum, aSensorType ) {

    val liquidName = if( aLiquidName.isBlank()) "-" else aLiquidName
}
