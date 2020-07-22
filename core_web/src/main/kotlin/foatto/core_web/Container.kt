package foatto.core_web

data class Container(
    val nonReactiveState: NonReactiveState
//    val stomp: Stomp,
)

data class NonReactiveState(
    var aaa: String = ""
//        var mapStructureState: MutableMap<String, MapStructureState> = mutableMapOf(),
//        var mapLocalState: MutableMap<String, MapLocalState> = mutableMapOf(
//            "" to MapLocalState(
//                highlightedLines = emptyArray(),
//                highlightedJunctions = emptyArray(),
//                zoom = MapLocalState.Zoom.MAP,
//                showZtp = false,
//                zoomedGroup = null,
//                hash = "",
//                selectedEmergencyJournalEntries = emptySet(),
//                popupParameters = MapLocalState.PopupParameters()
//            )
//        ),
//        var map: MapManager? = null,
//        var headerParameters: MutableMap<String, HeaderState> = mutableMapOf(
//            "" to HeaderState(
//                powerWantage = NetworkStateParameter(name = "powerWantage"),
//                saidi = NetworkStateParameter(name = "saidi"),
//                hash = "",
//                timstamp = 0.0
//            )
//        ),
//        var deviceStateParameters: MutableMap<String, AsdPanelState> = mutableMapOf(
//            "" to AsdPanelState(
//                deviceStates = listOf(),
//                hash = ""
//            )
//        ),
//        var eventLogParameters: MutableMap<String, EventLogPanelState> = mutableMapOf(
//            "" to EventLogPanelState(
//                entries = listOf(),
//                hash = ""
//            )
//        ),
//        var asdLogParameters: MutableMap<String, AsdLogPanelState> = mutableMapOf(
//            "" to AsdLogPanelState(
//                entries = listOf(),
//                hash = ""
//            )
//        ),
//        var commandLogParameters: MutableMap<String, CommandLogPanelState> = mutableMapOf(
//            "" to CommandLogPanelState(
//                entries = listOf(),
//                hash = ""
//            )
//        ),
//        var emergencyJournalPanelState: MutableMap<String, EmergencyJournalPanelState> = mutableMapOf(
//            "" to EmergencyJournalPanelState(
//                entries = listOf(),
//                hash = ""
//            )
//        ),
//        var activityShownBuildings: MutableMap<String, ActivityShownBuildings> = mutableMapOf(
//            "" to ActivityShownBuildings(
//                shownBuildings = emptySet(),
//                hash = ""
//            )
//        ),
//        var buildingShownBuildings: MutableMap<String, BuildingShownBuildings> = mutableMapOf(
//            "" to BuildingShownBuildings(
//                shownBuildings = emptySet(),
//                hash = ""
//            )
//        ),
//        var deviceSwitchShownBuildings: MutableMap<String, DeviceSwitchShownBuildings> = mutableMapOf(
//            "" to DeviceSwitchShownBuildings(
//                shownBuildings = emptySet(),
//                hash = ""
//            )
//        )
)
