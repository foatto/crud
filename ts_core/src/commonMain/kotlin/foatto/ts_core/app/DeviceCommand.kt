package foatto.ts_core.app

object DeviceCommand {
    //--- all commands will have 4 char (as like 32 bits) for 32-bit microcontroller easily preparing
    const val CMD_START_BLIND_CLIMB = "rise"
    const val CMD_START_DOWN = "down"
    const val CMD_RESTART = "boot"
    const val CMD_STOP = "stop"
    //--- manually lock & unlock the device
    const val CMD_LOCK = "lock"
    const val CMD_FREE = "free"
}