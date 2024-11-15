package foatto.compose.external

import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

external class WebUSBSerialDevice {
    open var overridePortSettings: Boolean
    open var baudrate: Int
    open var bits: Int
    open var stop: Int
    open var parity: Boolean
    open var deviceFilters: JsAny

    fun requestNewPort(): Promise<WebUSBSerialPort>
}

abstract external class WebUSBSerialPort : JsAny {

    fun connect(receiveCallback: (data: Uint8Array) -> Unit, errorCallback: (error: JsAny) -> Unit)
    fun send(data: Uint8Array): Promise<USBOutTransferResult>

}