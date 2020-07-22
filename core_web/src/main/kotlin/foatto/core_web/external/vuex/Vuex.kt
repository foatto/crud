//package foatto.core_web.external.vuex
//
//import foatto.core_web.external.createTypedJson
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//@JsModule("vuex")
//@JsNonModule
//@JsName("Vuex")
//external class Vuex {
//    class Store(storeConfig: StoreConfig) {
//        fun dispatch(type: String, payload: Any? = definedExternally)
//        fun commit(type: String, payload: Any? = definedExternally)
//    }
//
//    companion object
//}
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//class StoreConfig {
//    var state = State()
//    // getters
//    var actions = createTypedJson<Function<Unit>>()
//    var mutations = createTypedJson<Function<Unit>>()
//}
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//@Suppress("UnsafeCastFromDynamic")
//class State {
//    //--- комплексное реактивное свойство
//    val components: Components = Components()
//        .let {
//            //Это чтобы получить объект Components без лишних методов, заполненный пустыми строками
//            // и чтобы при добавлении свойств в Components не приходилось писать их в двух местах
//            val jsonString = JSON.stringify(it)
//            js("JSON.parse(jsonString)")
//        }
//    //--- простые реактивные свойства
////        var loggedIn = false
////        var loginFailedReason = ""
////        var showGeoMarkerWindow = false
////        var showToolTipWindow = false
//}
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//data class Components(
//    var aaa: String = ""
////    var mapStructure: String = "",
////    var mapState: String = "",
////    var grounds: String = "",
////    var geoMarkers: String = "",
////    var activityWindows: String = "",
////    var buildingWindows: String = "",
////    var deviceSwitchWindows: String = "",
////    var mapLocalState: String = "",
////    var headerParameters: String = "",
////    var deviceStateParameters: String = "",
////    var asdLogParameters: String = "",
////    var eventLogParameters: String = "",
////    var commandLogParameters: String = "",
////    var emergencyJournalPanelState: String = "",
////    var activityShownBuildings: String = "",
////    var buildingShownBuildings: String = "",
////    var deviceSwitchShownBuildings: String = ""
//)
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//interface ActionInterface
//
//interface ActionHandler<in A : ActionInterface> {
//    fun handle(context: Context, action: A)
//}
//
////fun Vuex.Store.dispatch(action: ActionInterface) {
////    this.dispatch(action::class.js.name, action)
////}
//
////--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//interface MutationInterface
//
//interface MutationHandler<in M : MutationInterface> {
//    fun handle(state: State, mutation: M)
//}
//
//typealias MutationHandlerMethod = (state: State, mutation: MutationInterface) -> Unit
//
