package foatto.compose.utils

//actual fun getUSBDevicesList(func: (List<Pair<String, String>>) -> Unit) {}

actual fun sendOverUSB(
    configurationNo: Int?,
    interfaceNo: Int?,
    outEndpointNo: Int,
    inEndpointNo: Int,
    s: String,
    onResponse: (() -> Unit)?
) {}

actual fun sendOverFTDI(s: String) {}