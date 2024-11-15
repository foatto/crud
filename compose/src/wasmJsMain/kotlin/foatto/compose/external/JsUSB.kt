package foatto.compose.external

import org.khronos.webgl.DataView
import org.khronos.webgl.Uint8Array
import org.w3c.dom.events.EventTarget
import kotlin.js.Promise

/*abstract*/ external class jsUSB {
    fun getUSB(): USB
}

abstract external class USB : EventTarget {
    //--- Нерабочий метод. Лучше использовать requestDevice с пустым фильтром.
    fun getDevices(): Promise<JsArray<USBDevice>>

    fun requestDevice(filters: JsAny): Promise<USBDevice>

    //--- events
/*
navigator.usb.addEventListener("connect", (event) => {
  // Add event.device to the UI.
});
или
navigator.usb.onconnect = (event) => {
  // Add event.device to the UI.
};
 */
//    fun connect()
/*
navigator.usb.addEventListener("disconnect", (event) => {
  // Remove event.device from the UI.
});
или
navigator.usb.ondisconnect = (event) => {
  // Remove event.device from the UI.
};
 */
//    fun disconnect()
}

abstract external class USBDevice : JsAny {
    open val configuration: USBConfiguration?
//    open val configurations: JsArray<USBConfiguration>

    open val deviceClass: Int
    open val deviceSubclass: Int
    open val deviceProtocol: Int

    open val deviceVersionMajor: Int
    open val deviceVersionMinor: Int
    open val deviceVersionSubminor: Int

    open val usbVersionMajor: Int
    open val usbVersionMinor: Int
    open val usbVersionSubminor: Int

    open val manufacturerName: String
    open val productId: String
    open val productName: String
    open val serialNumber: String
    open val vendorId: Int

    open val opened: Boolean

    fun open(): Promise<JsAny>
    fun close(): Promise<JsAny>
    fun forget(): Promise<JsAny>
    fun reset(): Promise<JsAny>

    fun selectConfiguration(configurationValue: Int): Promise<JsAny>
//    fun selectAlternateInterface(interfaceNumber: Int, alternateSetting): Promise<JsAny>    //???

/*
async function connectDevice(usbDevice) {
  await usbDevice.open();
  if (usbDevice.configuration === null) await usbDevice.selectConfiguration(1);
  await usbDevice.claimInterface(0);
} */
    fun claimInterface(interfaceNumber: Int): Promise<JsAny>    //???
    fun releaseInterface(interfaceNumber: Int): Promise<JsAny>    //???

    fun transferIn(endpointNumber: Int, length: Int): Promise <USBInTransferResult>
    fun transferOut(endpointNumber: Int, data: Uint8Array): Promise<USBOutTransferResult>

/*
    setup:

requestType
Must be one of three values specifying whether the transfer is "standard" (common to all USB devices) "class" (common to an industry-standard class of devices) or "vendor".

recipient
Specifies the target of the transfer on the device, one of "device", "interface", "endpoint", or "other".

request
A vendor-specific command.

value
Vendor-specific request parameters.

index
The interface number of the recipient.
     */
//    fun controlTransferIn(setup, length: Int): Promise<USBInTransferResult>
//    fun controlTransferOut(setup, length: Int): Promise<USBOutTransferResult>

//    fun isochronousTransferIn(endpointNumber: Int, packetLengths: JsArray<Int>): Promise<USBIsochronousInTransferResult>
//    fun isochronousTransferOut(endpointNumber: Int, data: Uint8Array, packetLengths: JsArray<Int>): Promise <USBIsochronousOutTransferResult>

/*
while (true) {
  let result = await data.transferIn(1, 6);

  if (result.data && result.data.byteLength === 6) {
    console.log(`Channel 1: ${result.data.getUint16(0)}`);
    console.log(`Channel 2: ${result.data.getUint16(2)}`);
    console.log(`Channel 5: ${result.data.getUint16(4)}`);
  }

  if (result.status === "stall") {
    console.warn("Endpoint stalled. Clearing.");
    await device.clearHalt("in", 1);
  }
} */
    //--- direction: "in", "out"
    fun clearHalt(direction: String, endpointNumber: Int): Promise<JsAny>    //???

}

public abstract external class USBConfiguration : JsAny {
    open val interfaces: JsArray<USBInterface>
    open val configurationName: String
    //open val configurationValue:
}

public abstract external class USBInterface : JsAny {
    open val interfaceNumber: Int
    //open val alternate:
    //open val alternates:
    open val claimed: Boolean
}


public abstract external class USBInTransferResult : JsAny {
    open val data: DataView
    /*
"ok" - The transfer was successful.
"stall" - The device indicated an error by generating a stall condition on the endpoint. A stall on the control endpoint does not need to be cleared. A stall on a bulk or interrupt endpoint must be cleared by calling clearHalt() before transferIn() can be called again.
"babble" - The device responded with more data than was expected.     */
    open val status: String
}

public abstract external class USBOutTransferResult : JsAny {
    open val bytesWritten: Int
/*
"ok" - The transfer was successful.
"stall" - The device indicated an error by generating a stall condition on the endpoint. A stall on a bulk or interrupt endpoint must be cleared by calling clearHalt() before transferOut() can be called again. */
    open val status: String
}

//public abstract external class USBIsochronousInTransferResult : JsAny {
////    open val data: DataView
//    open val packets: JsArray<USBIsochronousInTransferPacket>
//}
//
//public abstract external class USBIsochronousOutTransferResult : JsAny {
//    open val packets: JsArray<USBIsochronousOutTransferPacket>
//}

//--- TODO: удалить, если так и не понадобится. На данный момент это проще сделать нетипобезопасно через js("...")

/*
    //vendorId
    //productId
    //classCode
    //subclassCode
    //protocolCode
    //serialNumber
    const filters = [
      { vendorId: 0x1209, productId: 0xa800 },
      { vendorId: 0x1209, productId: 0xa850 },
    ];
 */
//external class USBDeviceFilters {
//    var filters: JsArray<USBDeviceFilter>
//}
/*

dictionary USBDeviceRequestOptions {
  required sequence<USBDeviceFilter> filters;
  sequence<USBDeviceFilter> exclusionFilters = [];
};
 */

public abstract external class USBDeviceRequestOptions : JsAny {
    open var filters: JsArray<USBDeviceFilter>
}

public abstract external class USBDeviceFilter : JsAny {
  open var /*unsigned short*/ vendorId: Int
  open var /*unsigned short*/ productId: Int
  open var /*octet*/ classCode: Int
  open var /*octet*/ subclassCode: Int
  open var /*octet*/ protocolCode: Int
  open var /*DOMString*/ serialNumber: String
}
