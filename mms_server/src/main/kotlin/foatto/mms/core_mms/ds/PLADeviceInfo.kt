@file:JvmName("PLADeviceInfo")
package foatto.mms.core_mms.ds

import foatto.core.util.AdvancedByteBuffer

class PLADeviceInfo( byteBuffer: AdvancedByteBuffer ) {
    var ringHeadPos = 0        // голова кольцевого буфера
    var ringTailPos = 0        // хвост кольцевого буфера
    var ringCount = 0        // кол-во кругов (износ флэш)
    var DFErrors = 0        // кол-во ошибок флэш
    var resetCounter = 0    // кол-во рестартов
    var systemVoltage = 0   // напряжение бортовой сети
    var batteryVoltage = 0  // напряжение резервной батареи
    var chargeCycles = 0    // кол-во циклов заряд/разряд резервной батареи
    var temperature = 0        // текущая температура контроллера
    var signalStrength = 0  // качество GSM сигнала 0..99 %
    var trafficCount = 0    // GPRS траффик байт
    var travelDistance = 0  // общий пробег в м
    var accelLevel = 0      // текущая настройка акселерометра

    init {
        ringHeadPos = byteBuffer.getShort().toInt() and 0xFFFF
        ringTailPos = byteBuffer.getShort().toInt() and 0xFFFF
        ringCount = byteBuffer.getShort().toInt() and 0xFFFF
        DFErrors = byteBuffer.getShort().toInt() and 0xFFFF
        resetCounter = byteBuffer.getShort().toInt() and 0xFFFF
        systemVoltage = byteBuffer.getShort().toInt() and 0xFFFF
        batteryVoltage = byteBuffer.getShort().toInt() and 0xFFFF
        chargeCycles = byteBuffer.getShort().toInt() and 0xFFFF
        temperature = byteBuffer.getShort().toInt()    // температура - величина со знаком
        signalStrength = byteBuffer.getByte().toInt() and 0xFF
        trafficCount = byteBuffer.getInt()
        travelDistance = Math.round( byteBuffer.getFloat().toDouble() * 1.852 * 1000.0 ).toInt()  // в милях, переводим в метры
        accelLevel = byteBuffer.getByte().toInt() and 0xFF

        //--- пропускаем 100 резервных байт
        for( i in 0..99 ) byteBuffer.getByte()
        // попробовать byteBuffer.get( new byte[ 100 ] );
    }
}
