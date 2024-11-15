package foatto.compose.utils

//expect fun getUSBDevicesList(func: (List<Pair<String, String>>) -> Unit)

expect fun sendOverUSB(
    configurationNo: Int?,
    interfaceNo: Int?,
    outEndpointNo: Int,
    inEndpointNo: Int,
    s: String,
    onResponse: (() -> Unit)? = null
)

expect fun sendOverFTDI(s: String)