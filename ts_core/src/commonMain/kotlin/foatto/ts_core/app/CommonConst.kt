package foatto.ts_core.app

const val ICON_NAME_TROUBLE_TYPE_CONNECT = "ts_object_trouble_type_connect"
const val ICON_NAME_TROUBLE_TYPE_WARNING = "ts_object_trouble_type_warning"
const val ICON_NAME_TROUBLE_TYPE_ERROR = "ts_object_trouble_type_error"

//--- all commands will have 4 char (as like 32 bits) for 32-bit microcontroller easily preparing
const val CMD_NO_COMMAND = "noop"
const val CMD_START_BLIND_CLIMB = "rise"
const val CMD_START_DOWN = "down"
const val CMR_RESTART = "boot"
const val CMR_STOP = "stop"