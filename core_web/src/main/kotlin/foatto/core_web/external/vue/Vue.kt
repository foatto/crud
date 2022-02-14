package foatto.core_web.external.vue

import kotlin.js.Json

inline fun that(): dynamic = js("this")
inline fun <T> that(): T = that().unsafeCast<T>()

external class Vue(json: Any) {
    companion object {
        fun <T> use(plugin: dynamic, options: T?)

//        fun <T> set(target: TypedJson<T>, key: String, value: T): T
//        fun <T> set(target: Array<T>, key: Int, value: T): T
//        fun set(target: Any, key: String, value: Any)
//        fun delete(target: Json, key: String)
//        fun <T> delete(target: TypedJson<T>, key: String)
//        fun <T> delete(target: Array<T>, key: Int)
        fun nextTick(callback: () -> Unit)
    }
}

external interface VueComponentOptions {
    var el: String?
    var template: String?
    var components: Json?

    //    var created: () -> Unit
    var mounted: () -> Unit
//    var updated: () -> Unit
//    var destroyed: () -> Unit

    var methods: Json?

//    var watch: Json?

    // могут быть get + set
    var computed: Json?
//    var computed: TypedJson<(VueComponent) -> Any>?

    var data: (() -> Json)?

//    var props: Array<String>
}

fun vueComponentOptions(): VueComponentOptions = js("{}")

/*
//@JsModule(vue.MODULE)
//@JsNonModule
//@JsName(vue.CLASS)
//external open class Vue(options: ComponentOptions<Vue>? = definedExternally) {
//
//    companion object {
//        // Global Config
//        val config: VueConfig
//        // Global API
//        fun extend(options: Any /* ComponentOptions<Vue> | FunctionalComponentOptions */): Any // typeof Vue
//
//        fun nextTick(callback: () -> Unit, context: Array<Any>? = definedExternally)
//        fun nextTick(): Promise<Unit>
//        fun <T> set(target: Any, key: String, value: T): T
//        fun <T> set(target: Array<T>, key: Int, value: T): T
//        fun delete(target: Json, key: String)
//        fun <T> delete(target: Array<T>, key: Int)
//        fun directive(id: String, definition: DirectiveConfig? = definedExternally): DirectiveOptions
//        fun filter(id: String, definition: Function<Unit>? = definedExternally): Function<Unit>
//        fun component(id: String, definition: Component? = definedExternally): Any // typeof Vue
//        fun component(id: String, definition: AsyncComponent? = definedExternally): Any // typeof Vue
//        fun <T> use(plugin: PluginConfig, options: T?)
//        fun mixin(mixin: Any /* typeof Vue | ComponentOptions */)
//        fun compile(template: String)
//        val version: String
//    }
//
//    // Instance Properties
//    var `$data`: Vue
//    val `$el`: HTMLElement
//    val `$options`: ComponentOptions<Vue>
//    val `$parent`: Vue
//    val `$root`: Vue
//    val `$children`: Array<Vue>
//    val `$refs`: JsonOf<Ref?> // { [key: String]: Vue | Element | Array<Vue> | Array<Element> }
//    val `$slots`: JsonOf<Array<VNode>?> // { [key: String]: Array<VNode> }
//    val `$scopedSlots`: ScopedSlotMap
//    val `$isServer`: Boolean
//    val `$ssrContext`: Any
//    val `$props`: Any
//    val `$vnode`: VNode
//    val `$attrs`: Any // { [key: String]: String } | void
//    val `$listeners`: Any // { [key: String]: Function | Array<Function> } | void
//
//    var `$createElement`: CreateElement
//
//    // Instance Methods / Data
//    fun <T> `$watch`(
//            exp: String,
//            callback: WatchHandler<T>,
//            options: WatchOptions? = definedExternally): () -> Unit
//
//    fun <T> `$watch`(
//            fn: () -> T,
//            callback: WatchHandler<T>,
//            options: WatchOptions? = definedExternally): () -> Unit
//
//    fun <T> `$set`(target: Any, key: String, value: T): T
//    fun <T> `$set`(target: Array<T>, key: Int, value: T): T
//    fun `$delete`(target: Json, key: String)
//    fun <T> `$delete`(target: Array<T>, key: Int)
//
//    // Instance Methods / Event
//    fun `$on`(event: String, callback: Function<Unit>): Vue // -> this
//
//    fun `$on`(event: Array<String>, callback: Function<Unit>): Vue // -> this
//    fun `$once`(event: String, callback: Function<Unit>): Vue // -> this
//    fun `$off`(event: String? = definedExternally, callback: Function<Unit>? = definedExternally): Vue // -> this
//    fun `$off`(event: Array<String>? = definedExternally, callback: Function<Unit>? = definedExternally): Vue // -> this
//    fun `$emit`(event: String, vararg args: Any): Vue // -> this
//
//    // Instance Methods / Lifecycle
//    fun `$mount`(elementOrSelector: Any? /* Element | String */ = definedExternally,
//                 hydrating: Boolean? = definedExternally): Vue // -> this
//    fun `$forceUpdate`()
//    fun `$destroy`()
//    fun `$nextTick`(callback: () -> Unit) // V.() -> Unit
//    fun `$nextTick`(): Promise<Unit>
//}
//
//external interface VueConfig {
//
//    val silent: Boolean
//    val optionMergeStrategies: JsonOf<Function<Any>?> // { [key: String]: Function }
//    val devtools: Boolean
//    val productionTip: Boolean
//    val performance: Boolean
//    val errorHandler: (err: Error, vm: Vue, info: String) -> Unit
//    val warnHandler: (msg: String, vm: Vue, trace: String) -> Unit
//    val ignoredElements: Array<String>
//    val keyCodes: JsonOf<Int> // { [key: String]: Number }
//}
//
//external interface CompileResult {
//    fun render(createElement: Any /* typeof Vue.prototype.$createElement */): VNode
//    var staticRenderFns: Array<() -> VNode>
//}
//
///**
// * `Vue | Element | Array<Vue> | Array<Element>`
// */
//external interface Ref
//
//inline fun Ref(vm: Vue): Ref = vm.asDynamic()
//inline fun Ref(element: HTMLElement): Ref = element.asDynamic()
//inline fun Ref(vms: Array<Vue>): Ref = vms.asDynamic()
//inline fun Ref(elements: Array<HTMLElement>): Ref = elements.asDynamic()
//
//inline fun Ref.toVue(): Vue = this.asDynamic()
//inline fun Ref.toHTMLElement(): HTMLElement = this.asDynamic()
//inline fun Ref.toVueList(): Array<Vue> = this.asDynamic()
//inline fun Ref.toHTMLElementList(): Array<HTMLElement> = this.asDynamic()
//
////=== options.kt ========================================================================================================================================================================
//
///**
// * `new (varargs args: Any): Any`
// */
//typealias Constructor<T> = Function<T>
//
//object js {
//    val String: Constructor<String> = js("String")
//    val Number: Constructor<Int> = js("Number")
//    val Boolean: Constructor<Boolean> = js("Boolean")
//    val Function: Constructor<Function<Any>> = js("Function")
//    val Object: Constructor<Any> = js("Object")
//    val Array: Constructor<Array<Any>> = js("Array")
//    val Symbol: Constructor<Any> = js("Symbol") // TODO specify type parameter
//}
//
///**
// * `typeof Vue | ComponentOptions | FunctionalComponentOptions`
// */
//external interface Component
//
//inline fun Component(typeOfVue: Any): Component = typeOfVue.asDynamic() // TODO change Any
//inline fun <V : Vue> Component(options: ComponentOptions<V>): Component = options.asDynamic()
//inline fun Component(functionalOptions: FunctionalComponentOptions): Component = functionalOptions.asDynamic()
//
//inline fun Component.toTypeOfVue(): Any = this.asDynamic() // TODO change Any
//inline fun <V : Vue> Component.toComponentOptions(): ComponentOptions<V> = this.asDynamic()
//inline fun Component.toFunctionalComponentOptions(): FunctionalComponentOptions = this.asDynamic()
//
//typealias AsyncComponent = (resolve: (component: Component) -> Unit, reject: (reason: Any?) -> Unit) -> AsyncComponentResult
//
///**
// * `Promise<Component> | Component | void`
// */
//external interface AsyncComponentResult
//
//inline fun AsyncComponentResult(promise: Promise<Component>): AsyncComponentResult = promise.asDynamic()
//inline fun AsyncComponentResult(component: Component): AsyncComponentResult = component.asDynamic()
//inline fun AsyncComponentResult(void: Void): AsyncComponentResult = void.asDynamic()
//
//external interface ComponentOptions<V : Vue> {
//    // Data
//    var data: Data<V>? // Object | V.() -> Object
//    var props: Props?
//    var propsData: Json?
//    var computed: JsonOf<ComputedConfig<*>>? // { [key: String]: V.() -> Any | ComputedOptions }
//    var methods: JsonOf<Function<Any?>?>? // { [key: String]: V.(args: Array<Any>) -> Any }
//    var watch: JsonOf<Watcher?>? // { [key: String]: String | WatchHandler<V, Any> | ({ handler: WatchHandler<V, Any> } & WatchOptions) }
//    // DOM
//    var el: ElementConfig?
//    var template: String?
//    var render: RenderFunction? // V.(createElement: CreateElement) -> VNode
//    var renderError: RenderErrorFunction?
//    var staticRenderFns: Array<RenderFunction>?
//    // Lifecycle Hooks
//    var beforeCreate: LifecycleHookFunction?
//    var created: LifecycleHookFunction?
//    var beforeMount: LifecycleHookFunction?
//    var mounted: LifecycleHookFunction?
//    var beforeUpdate: LifecycleHookFunction?
//    var updated: LifecycleHookFunction?
//    var activated: LifecycleHookFunction?
//    var deactivated: LifecycleHookFunction?
//    var beforeDestroy: LifecycleHookFunction?
//    var destroyed: LifecycleHookFunction?
//    // Assets
//    var directives: JsonOf<DirectiveConfig?>? // { [key: String]: DirectiveOptions | DirectiveFunction }
//    var components: JsonOf<ComponentConfig?>? // { [key: String]: Component | AsyncComponent }
//    var transitions: JsonOf<Json>? // { [key: String]: Object }
//    var filters: JsonOf<Function<Any>?>? // { [key: String]: Function }
//    // Composition
//    var provide: Data<Json>? // Object | () -> Object
//    var inject: Any? // Array<String> | { [key: String]: String | Symbol }
//    var parent: Vue?
//    var mixins: Array<Any>? // Array<ComponentOptions | typeof Vue>
//    var extends: Any? // ComponentOptions | typeof Vue
//    // Misc
//    var model: ModelOptions?
//    var name: String?
//    var delimiters: Delimiter?
//    var comments: Boolean?
//    var inheritAttrs: Boolean?
//}
//
//external interface FunctionalComponentOptions {
//    var name: String?
//    var props: Props?
//    var functional: Boolean
//    var render: (createElement: CreateElement, context: RenderContext) -> VNode? // VNode | Unit
//}
//
//external interface RenderContext {
//    var props: Any
//    var children: Array<VNode>
//    var slots: () -> Any
//    var data: VNodeData
//    var parent: Vue
//    var injections: Any
//}
//
//external interface PropOptions<T> {
//    var type: TypeConfig<T>?
//    var required: Boolean?
//    var default: T?
//    var validator: ((value: T) -> Boolean)?
//}
//
//external interface ComputedOptions<T> {
//    var get: (() -> T)? // V.() -> Any
//    var set: ((value: T) -> Unit)? // V.(value: Any) -> Unit
//    var cache: Boolean?
//}
//
///**
// * `V.(value: T, oldValue: T) -> Unit`
// */
//typealias WatchHandler<T> = (value: T, oldValue: T) -> Unit
//
//external interface WatchOptions {
//    var deep: Boolean?
//    var immediate: Boolean?
//}
//
//typealias DirectiveFunction = (el: HTMLElement, binding: VNodeDirective, vnode: VNode, oldVnode: VNode) -> Unit
//
//external interface DirectiveOptions {
//    var bind: DirectiveFunction?
//    var inserted: DirectiveFunction?
//    var update: DirectiveFunction?
//    var componentUpdated: DirectiveFunction?
//    var unbind: DirectiveFunction?
//}
//
///**
// * `T | () -> T`
// */
//external interface Data<T>
//
//inline fun <T> Data(json: T): Data<T> = json.asDynamic()
//inline fun <T> Data(factory: () -> T): Data<T> = factory.asDynamic()
//
//inline fun <T> Data<T>.toObject(): T = this.asDynamic()
//inline fun <T> Data<T>.toFactory(): () -> T = this.asDynamic()
//
///**
// * `Array<String> | { [propertyName: String]: PropOptions | Constructor | Array<Constructor> }`
// */
//external interface Props
//
//inline fun Props(propNames: Array<String>): Props = propNames.asDynamic()
//inline fun Props(propConfig: JsonOf<PropConfig?>): Props = propConfig.asDynamic()
//
//inline fun Props.toNames(): Array<String> = this.asDynamic()
//inline fun Props.toConfig(): JsonOf<PropConfig?> = this.asDynamic()
//
///**
// * `PropOptions | Constructor | Array<Constructor>`
// */
//external interface PropConfig
//
//inline fun <T> PropConfig(options: PropOptions<T>): PropConfig = options.asDynamic()
//inline fun <T> PropConfig(constructor: Constructor<T>): PropConfig = constructor.asDynamic()
//inline fun PropConfig(constructors: Array<Constructor<*>>): PropConfig = constructors.asDynamic()
//
//inline fun <T> PropConfig.toOptions(): PropOptions<T> = this.asDynamic()
//inline fun <T> PropConfig.toConstructor(): Constructor<T> = this.asDynamic()
//inline fun PropConfig.toConstructorList(): Array<Constructor<*>> = this.asDynamic()
//
///**
// * `ComputedOptions<T> | () -> T`
// */
//external interface ComputedConfig<T>
//
//inline fun <T> ComputedConfig(factory: () -> T): ComputedConfig<T> = factory.asDynamic()
//inline fun <T> ComputedConfig(options: ComputedOptions<T>): ComputedConfig<T> = options.asDynamic()
//
//inline fun <T> ComputedConfig<T>.toFactory(): () -> T = this.asDynamic()
//inline fun <T> ComputedConfig<T>.toOptions(): ComputedOptions<T> = this.asDynamic()
//
///**
// * `String | WatchHandler<V, Any> | ({ handler: WatchHandler<V, Any> } & WatchOptions)`
// */
//external interface Watcher
//
//inline fun Watcher(methodName: String): Watcher = methodName.asDynamic()
//inline fun <T> Watcher(handler: WatchHandler<T>): Watcher = handler.asDynamic()
//inline fun <T> Watcher(options: WatchHandlerOptions<T>): Watcher = options.asDynamic()
//
//inline fun Watcher.toMethodName(): String = this.asDynamic()
//inline fun <T> Watcher.toHandler(): WatchHandler<T> = this.asDynamic()
//inline fun <T> Watcher.toOptions(): WatchHandlerOptions<T> = this.asDynamic()
//
///**
// * `{ handler: WatchHandler<V, Any> } & WatchOptions`
// */
//external interface WatchHandlerOptions<T> : WatchOptions {
//    var handler: WatchHandler<T>
//}
//
///**
// * `String | HTMLElement`
// */
//external interface ElementConfig
//
//inline fun ElementConfig(selector: String): ElementConfig = selector.asDynamic()
//inline fun ElementConfig(element: HTMLElement): ElementConfig = element.asDynamic()
//
//inline fun ElementConfig.toSelector(): String = this.asDynamic()
//inline fun ElementConfig.toElement(): HTMLElement = this.asDynamic()
//
///**
// * `(createElement: CreateElement) -> VNode`
// */
//typealias RenderFunction = (createElement: CreateElement) -> VNode
//
///**
// * `(createElement: CreateElement, error: Error) -> VNode`
// */
//typealias RenderErrorFunction = (createElement: CreateElement, error: Error) -> VNode
//
///**
// * `V.() -> Unit`
// */
//typealias LifecycleHookFunction = () -> Unit
//
///**
// * `DirectiveOptions | DirectiveFunction`
// */
//external interface DirectiveConfig
//
//inline fun DirectiveConfig(options: DirectiveOptions): DirectiveConfig = options.asDynamic()
//inline fun DirectiveConfig(function: DirectiveFunction): DirectiveConfig = function.asDynamic()
//
//inline fun DirectiveConfig.toOptions(): DirectiveOptions = this.asDynamic()
//inline fun DirectiveConfig.toFunction(): DirectiveFunction = this.asDynamic()
//
///**
// * `Component | AsyncComponent`
// */
//external interface ComponentConfig
//
//inline fun ComponentConfig(component: Component): ComponentConfig = component.asDynamic()
//inline fun ComponentConfig(asyncComponent: AsyncComponent): ComponentConfig = asyncComponent.asDynamic()
//
//inline fun ComponentConfig.toComponent(): Component = this.asDynamic()
//inline fun ComponentConfig.toAsyncComponent(): AsyncComponent = this.asDynamic()
//
//external interface ModelOptions {
//    var prop: String?
//    var event: String?
//}
//
///**
// * `[String, String]`
// */
//typealias Delimiter = Array<String>
//
//inline fun Delimiter(begin: String, end: String) = arrayOf(begin, end)
//
///**
// * `Constructor | Array<Constructor> | null`
// */
//external interface TypeConfig<T>
//
//inline fun <T> TypeConfig(constructor: Constructor<T>): TypeConfig<T> = constructor.asDynamic()
//inline fun TypeConfig(constructors: Array<Constructor<*>>): TypeConfig<*> = constructors.asDynamic()
//
//inline fun <T> TypeConfig<T>.toConstructor(): Constructor<T> = this.asDynamic()
//inline fun TypeConfig<*>.toConstructorList(): Array<Constructor<*>> = this.asDynamic()
//
////=== vnode.kt ========================================================================================================================================================================
//
//external interface VNode {
//    var tag: String?
//    var data: VNodeData?
//    var children: Array<VNode>?
//    var text: String?
//    var elm: Node?
//    var ns: String?
//    var context: Vue?
//    var key: Key?
//    var componentOptions: VNodeComponentOptions?
//    var componentInstance: Vue?
//    var parent: VNode?
//    var raw: Boolean?
//    var isStatic: Boolean?
//    var isRootInsert: Boolean
//    var isComment: Boolean
//}
//
//external interface VNodeData {
//    var key: Key?
//    var slot: String?
//    var scopedSlots: ScopedSlotMap?
//    var ref: String?
//    var tag: String?
//    var staticClass: String?
//    var `class`: Any?
//    var staticStyle: Json? // { [key: String]: Any }
//    var style: OneOrMany<Json>? // Array<Object> | Object
//    var props: Json? // { [key: String]: Any }
//    var attrs: Json? // { [key: String]: Any }
//    var domProps: Json? // { [key: String]: Any }
//    var hook: JsonOf<Function<Any>>? // { [key: String]: Function }
//    var on: JsonOf<OneOrMany<Function<Any>>>? // { [key: String]: Function | Array<Function> }
//    var nativeOn: JsonOf<OneOrMany<Function<Any>>>? // { [key: String]: Function | Array<Function> }
//    var transition: Json?
//    var show: Boolean?
//    var inlineTemplate: InlineTemplate?
//    var directives: Array<VNodeDirective>?
//    var keepAlive: Boolean?
//}
//
//external interface InlineTemplate {
//    var render: Function<Unit>
//    var staticRenderFns: Array<Function<Unit>>
//}
//
//external interface VNodeDirective {
//    val name: String
//    val value: Any
//    val oldValue: Any
//    val expression: Any
//    val arg: String
//    val modifiers: JsonOf<Boolean> // { [key: String]: Boolean }
//}
//
//external interface VNodeComponentOptions {
//    var Ctor: Any // typeof Vue
//    var propsData: Json?
//    var listeners: Json?
//    var children: VNodeChildren?
//    var tag: String?
//}
//
///**
// * `String | Number`
// */
//external interface Key
//
//inline fun Key(name: String): Key = name.asDynamic()
//inline fun Key(index: Int): Key = index.asDynamic()
//
//inline fun Key.toName(): String = this.asDynamic()
//inline fun Key.toIndex(): Int = this.asDynamic()
//
///**
// * `{ [propertyName: String]: ScopedSlot }`
// */
//external interface ScopedSlotMap : JsonOf<ScopedSlot?>
//
///**
// * `(props: Any) -> VNodeChildrenArrayContents | String`
// */
//external interface ScopedSlot
//
//inline fun ScopedSlot(func: (props: Any) -> VNodeChildrenArrayContents): ScopedSlot = func.asDynamic()
//inline fun ScopedSlot(value: String): ScopedSlot = value.asDynamic() // TODO change parameter name
//
//inline fun ScopedSlot.toFunction(): (props: Any) -> VNodeChildrenArrayContents = this.asDynamic()
//inline fun ScopedSlot.toString(): String = this.asDynamic() // TODO change method name
//
///**
// * `VNodeChildrenArrayContents | [ScopedSlot] | String`
// */
//external interface VNodeChildren
//
//inline fun VNodeChildren(contents: VNodeChildrenArrayContents): VNodeChildren = contents.asDynamic()
//inline fun VNodeChildren(scopedSlot: Array<ScopedSlot>): VNodeChildren = scopedSlot.asDynamic()
//inline fun VNodeChildren(value: String): VNodeChildren = value.asDynamic() // TODO change parameter name
//
//inline fun VNodeChildren.toContents(): VNodeChildrenArrayContents = this.asDynamic()
//inline fun VNodeChildren.toScopedSlot(): Array<ScopedSlot> = this.asDynamic()
//inline fun VNodeChildren.toString(): String = this.asDynamic() // TODO change method name
//
///**
// * `{ [x: Number]: VNode | String | VNodeChildren }`
// */
//external interface VNodeChildrenArrayContents
//
//inline operator fun VNodeChildrenArrayContents.get(x: Int): Contents = this.asDynamic()[x]
//inline operator fun VNodeChildrenArrayContents.set(x: Int, value: Contents) {
//    this.asDynamic()[x] = value
//}
//
///**
// * `VNode | String | VNodeChildren`
// */
//external interface Contents
//
//inline fun Contents(node: VNode): Contents = node.asDynamic()
//inline fun Contents(value: String): Contents = value.asDynamic() // TODO change parameter name
//inline fun Contents(children: VNodeChildren): Contents = children.asDynamic()
//
//inline fun Contents.toVNode(): VNode = this.asDynamic()
//inline fun Contents.toString(): String = this.asDynamic() // TODO change method name
//inline fun Contents.toVNodeChildren(): VNodeChildren = this.asDynamic()
//
////=== json.kt ========================================================================================================================================================================
//
//// Type is not `Json` because we do not want to have `get` and `set` functions.
//inline fun <T : Any> json(): T = js("({})")
//
//fun <T : Any> json(init: T.() -> Unit): T = json<T>().apply(init)
//
//fun <V : Vue> ComponentOptions(init: ComponentOptions<V>.() -> Unit): ComponentOptions<V> = json(init)
//fun FunctionalComponentOptions(init: FunctionalComponentOptions.() -> Unit): FunctionalComponentOptions = json(init)
//fun RenderContext(init: RenderContext.() -> Unit): RenderContext = json(init)
//fun <T> PropOptions(init: PropOptions<T>.() -> Unit): PropOptions<T> = json(init)
//fun <T> ComputedOptions(init: ComputedOptions<T>.() -> Unit): ComputedOptions<T> = json(init)
//fun WatchOptions(init: WatchOptions.() -> Unit): WatchOptions = json(init)
//fun DirectiveOptions(init: DirectiveOptions.() -> Unit): DirectiveOptions = json(init)
//fun <T> WatchHandlerOptions(init: WatchHandlerOptions<T>.() -> Unit): WatchHandlerOptions<T> = json(init)
//fun ModelOptions(init: ModelOptions.() -> Unit): ModelOptions = json(init)
//
////=== util.kt ========================================================================================================================================================================
//
///**
// * JavaScript native `this`
// */
//external val `this`: dynamic
//
///**
// * Typed JavaScript native `this`
// */
//inline fun <T : Any> thisAs(): T = `this`
//
///**
// * Type of `void 0`
// */
//external interface Void
//
///**
// * Constant of `void 0`
// */
//val void: Void = js("void 0")
//
///**
// * `{ [propertyName: String]: T }`
// */
//external interface JsonOf<T>
//
//inline operator fun <T> JsonOf<T>.get(propertyName: String): T = this.asDynamic()[propertyName]
//inline operator fun <T> JsonOf<T>.set(propertyName: String, value: T) {
//    this.asDynamic()[propertyName] = value
//}
//
///**
// * `T | Array<T>`
// */
//external interface OneOrMany<T>
//
//inline fun <T> OneOrMany(value: T): OneOrMany<T> = value.asDynamic()
//inline fun <T> OneOrMany(value: Array<T>): OneOrMany<T> = value.asDynamic()
//
////=== plugin.kt ========================================================================================================================================================================
//
//typealias PluginFunction<T> = (Vue: Any /* typeof Vue */, options: T?) -> Unit
//
//external interface PluginObject<T> : JsonOf<Any?> {
//    var install: PluginFunction<T>
//}
//
///**
// * `PluginObject | PluginFunction`
// */
//external interface PluginConfig
//
//inline fun <T> PluginConfig(value: PluginObject<T>): PluginConfig = value.asDynamic()
//inline fun <T> PluginConfig(value: PluginFunction<T>): PluginConfig = value.asDynamic()
//
//inline fun <T> PluginConfig.toObject(): PluginObject<T> = this.asDynamic()
//inline fun <T> PluginConfig.toFunction(): PluginFunction<T> = this.asDynamic()
//
////=== array.kt ========================================================================================================================================================================
//
///**
// * The push() method adds one or more elements to the end of an array and returns the new length of the array.
// */
//inline fun <T> Array<T>.push(vararg element: T): Int
//        = this.asDynamic().push.apply(this, element)
//
///**
// * The pop() method removes the last element from an array and returns that element.
// * This method changes the length of the array.
// */
//inline fun <T> Array<T>.pop(): T?
//        = this.asDynamic().pop()
//
///**
// * The shift() method removes the first element from an array and returns that element.
// * This method changes the length of the array.
// */
//inline fun <T> Array<T>.shift(): T?
//        = this.asDynamic().shift()
//
///**
// * The unshift() method adds one or more elements to the beginning of an array and returns the new length of the array.
// */
//inline fun <T> Array<T>.unshift(vararg element: T): Int
//        = this.asDynamic().unshift.apply(this, element)
//
///**
// * The splice() method changes the contents of an array by removing existing elements.
// */
//inline fun <T> Array<T>.splice(index: Int): Array<T>
//        = this.asDynamic().splice.apply(this, arrayOf(index))
//
///**
// * The splice() method changes the contents of an array by removing existing elements.
// */
//inline fun <T> Array<T>.splice(index: Int, howMany: Int): Array<T>
//        = this.asDynamic().splice.apply(this, arrayOf(index, howMany))
//
///**
// * The splice() method changes the contents of an array by removing existing elements and adding new elements.
// */
//inline fun <T> Array<T>.splice(index: Int, howMany: Int, vararg element: T): Array<T>
//        = this.asDynamic().splice.apply(this, arrayOf(index, howMany) + element)
//
///**
// * The sort() method sorts the elements of an array in place and returns the array.
// * The sort is not necessarily stable.
// * The default sort order is according to string Unicode code points.
// */
//inline fun <T> Array<T>.sort(): Array<T>
//        = this.asDynamic().sort()
//
///**
// * The sort() method sorts the elements of an array in place and returns the array.
// * The sort is not necessarily stable.
// */
//inline fun <T> Array<T>.sort(noinline compareFunction: (a: T, b: T) -> Int): Array<T>
//        = this.asDynamic().sort(compareFunction)
//
///**
// * The reverse() method reverses an array in place.
// * The first array element becomes the last, and the last array element becomes the first.
// */
//inline fun <T> Array<T>.reverse(): Array<T>
//        = this.asDynamic().reverse()

*/