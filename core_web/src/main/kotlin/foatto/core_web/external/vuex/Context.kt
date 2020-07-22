//package foatto.core_web.external.vuex
//
//external interface Context {
//    fun commit(type: String, payload: Any? = definedExternally, options: Any? = definedExternally)
//    fun dispatch(type: String, payload: Any? = definedExternally)
//    fun watch(selector: () -> Unit, callback: () -> Unit)
//    fun <T> watch(selector: () -> T, callback: (newVal: T, oldVal: T) -> Unit)
//    fun getters()
//    fun rootGetters()
//    fun rootState()
//    val state: State
//}
//
//fun Context.commit(mutation: MutationInterface) {
//    this.commit(mutation::class.js.name, mutation)
//}
//
//fun Context.dispatch(action: ActionInterface) {
//    this.dispatch(action::class.js.name, action)
//}
//
