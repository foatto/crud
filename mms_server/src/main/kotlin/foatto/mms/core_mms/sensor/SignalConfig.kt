package foatto.mms.core_mms.sensor

import java.util.*

class SignalConfig( aSignalConfig: String ) {
    var and = true
    var alPort = mutableListOf<Int>()

    init {
        var sSignalConfig = aSignalConfig
        if( !sSignalConfig.isEmpty() ) {
            val operationChar = sSignalConfig[ 0 ]
            //--- логический AND разрешающих сигналов
            if( operationChar == '*' || operationChar == '&' ) {
                and = true
                sSignalConfig = sSignalConfig.substring( 1 )
            }
            //--- логический OR разрешающих сигналов
            else if( operationChar == '+' || operationChar == '|' ) {
                and = false
                sSignalConfig = sSignalConfig.substring( 1 )
            }
            //--- если не указано - то логический AND разрешающих сигналов
            else and = true

            val st = StringTokenizer( sSignalConfig, " ,;" )
            while( st.hasMoreTokens() ) {
                val sPort = st.nextToken()
                try {
                    alPort.add( sPort.toInt() )
                }
                catch(t: Throwable) {}
            }
        }
    }
}
