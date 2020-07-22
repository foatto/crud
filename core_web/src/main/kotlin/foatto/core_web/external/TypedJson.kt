package foatto.core_web.external

import kotlin.js.Json
import kotlin.reflect.KProperty

///////////////////////////////////////////////
external class Object {
    companion object {
        fun <K> keys(obj: Any): Array<K>
        fun <K, V> entries(obj: Any): Array<ProtoMapEntry<K, V>>
    }
}

///////////////////////////////////////////////


operator fun <T> Json.getValue(thisRef: Any?, property: KProperty<*>): T {
    return get(property.name).unsafeCast<T>()
}

operator fun Json.setValue(thisRef: Any?, property: KProperty<*>, value: Any?) {
    set(property.name, value)
}


///////////////////////////////////////////////
class TypedJson<T>

fun <T> createTypedJson(): TypedJson<T> = js("{}")
fun <T> createTypedJson(vararg pairs: Pair<String, T>): TypedJson<T> {
    val res: dynamic = js("({})")
    for ((name, value) in pairs) {
        res[name] = value
    }
    return res
}

operator fun <T> TypedJson<T>.iterator(): Iterator<String> = Object.keys<String>(this).iterator()
operator fun <T> TypedJson<T>.get(propertyName: String): T? = asDynamic()[propertyName]
operator fun <T> TypedJson<T>.set(propertyName: String, value: T) {
    asDynamic()[propertyName] = value
}

fun <T> TypedJson<T>.contains(value: T): Boolean {
    for (id in this) if (this[id] == value) return true
    return false
}

fun <T> TypedJson<T>.any(predicate: (pair: Pair<String, T>) -> Boolean): Boolean {
    for (id in this) if (predicate(id to this[id]!!)) return true
    return false
}

fun <T> TypedJson<T>.find(predicate: (pair: Pair<String, T>) -> Boolean): Map<String, T> {
    val result = mutableMapOf<String, T>()
    for (id in this) if (predicate(id to this[id]!!)) result[id] = this[id]!!
    return result
}

fun <T> TypedJson<T>.toMap(): Map<String, T> {
    val result = mutableMapOf<String, T>()
    for (id in this) result[id] = this[id]!!
    return result
}

fun <T> TypedJson<T>.toJson(): Json = asDynamic()
fun createJson(): Json = js("{}")
//fun <T> Json.toTypedJson() :TypedJson<T> = asDynamic()

///////////////////////////////////////////////
external class TypedJsArray<V>

fun <V> createTypedJsArray(): TypedJsArray<V> = js("[]")
operator fun <V> TypedJsArray<V>.iterator(): Iterator<V> = asDynamic().unsafeCast<Array<V>>().iterator()
operator fun <V> TypedJsArray<V>.get(propertyName: String): V? = asDynamic()[propertyName]
fun <V> TypedJsArray<V>.push(value: V) {
    asDynamic().push(value)
}

fun <T> TypedJsArray<T>.toArray(): Array<Any> = asDynamic()

fun <V> TypedJsArray<V>.toMutableList(): MutableList<V> = mutableListOf(*asDynamic().unsafeCast<Array<V>>())
fun <V> TypedJsArray<V>.toList(): List<V> = listOf(*asDynamic().unsafeCast<Array<V>>())

fun <T> Array<T>.push(item: T) {
    asDynamic().push(item)
}

///////////////////////////////////////////////


///////////////////////////////////////////////
external class ProtoMap<K, V> {
    fun entries(): dynamic
}

external class ProtoMapEntry<K, V>

operator fun <K, V> ProtoMapEntry<K, V>.component1(): K = asDynamic()[1][0]
operator fun <K, V> ProtoMapEntry<K, V>.component2(): V = asDynamic()[1][1]
operator fun <V> ProtoMap<String, V>.iterator(): Iterator<ProtoMapEntry<String, V>> =
    Object.entries<String, V>(this.entries()["arr_"]).iterator()

///////////////////////////////////////////////
fun <T> Map<String, T>.toJson(): Json {
    val res: dynamic = js("({})")
    for ((name, value) in this) {
        res[name] = value
    }
    return res
}


//---------------------------

private val regex = Regex("""(/\*.*\*/|//.*$)""")

fun toJson(cssLike: String, mapOfVariables: Map<String, String>? = null): Json {
    val mainJson = createJson()

    cssLike.trim()
        .replace(regex, "")
        .split("}")
        .forEach {
            if (it.isNotBlank()) {
                val (name, rules) = it.split("{")
                val jsonRules = createJson()
                rules
                    .trim()
                    .split(";")
                    .forEach {
                        if (it.isNotBlank()) {
                            val (propName, propValue) = it.split(":")
                            jsonRules[propName.trim()] = propValue.trim()
                        }
                    }
                mainJson[name.trim()] = jsonRules
            }
        }
    mapOfVariables?.forEach { mainJson[it.key] = it.value }
    return mainJson
}



