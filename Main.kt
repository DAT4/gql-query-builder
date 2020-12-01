// https://kotlinlang.org/docs/reference/type-safe-builders.html

fun main() {

    val filters = Filter.Builder()
            .filters(FilterType.TIMEGT, System.currentTimeMillis()/1000)
            .filters(FilterType.PLACE, "HUSET")
            .build()

    val e = events(filters) {
        +title
        +genre
        +image
        +link
        +other
        +price
        +text
        +time
        +tickets
        location {
            area()
            place()
            address {
                city()
                street()
                no()
                state()
                zip()
            }
            coordinates {
                longitude()
                latitude()
            }
        }
    }

    println(e)

    //val data: Response = postStuff(GQL("$e"), "https://mama.sh/moro/api")
    //val out = data.errors ?: data.data.events
    //out.forEach {
    //    println(it)
    //}
}

interface Element {
    fun render(builder: StringBuilder, indent: String)
}

@DslMarker //Domain Specific Language
annotation class GraphQLMarker

@GraphQLMarker
class EdgeCase(val query: Query) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent${query.name}\n")
    }
}

@GraphQLMarker
abstract class Query(val name: String) : Element {
    val children = arrayListOf<Element>()
    protected fun <T : Element> visitEntity(entity: T, visit: T.() -> Unit = {}): T {
        entity.visit()
        children.add(entity)
        return entity
    }

    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$name")
        if (children.isNotEmpty()) {
            builder.append("{\n")
            for (c in children) {
                c.render(builder, "$indent  ")
            }
            builder.append("$indent}")
        }
        builder.append("\n")
    }

    operator fun Query.unaryPlus(): Query{
        return visitEntity(this)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}


class EVENTS(private val filter: Filter) : Query("events") {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("{$name")
        if (filter.filters.isNotEmpty()) {
            builder.append("(" + filter.filters.map {
                if (it.value is Int || it.value is Long) {
                    it.key.str + ":" + it.value + ","
                } else {
                    it.key.str + ":\"" + it.value + "\","
                }
            }.joinToString(" ").dropLast(1) + ")")
        }
        if (children.isNotEmpty()) {
            builder.append("{\n")
            for (c in children) {
                c.render(builder, "$indent  ")
            }
            builder.append("$indent}")
        }
        builder.append("\n}")
    }

    val title = object : Query("title") {}
    val genre = object : Query("genre") {}
    val image = object : Query("image") {}
    val link = object : Query("link") {}
    val other = object : Query("other") {}
    val price = object : Query("price") {}
    val text = object : Query("text") {}
    val tickets = object : Query("tickets") {}
    val time = object : Query("time") {}

    fun location(visit: LOCATION.() -> Unit) = visitEntity(LOCATION(), visit)
}

class Filter private constructor(val filters: MutableMap<FilterType, Any>) {
    class Builder {
        private val filters = mutableMapOf<FilterType, Any>()
        fun filters(key: FilterType, value: Any) = apply {
            this.filters[key] = value
        }

        fun build(): Filter {
            return Filter(filters)
        }
    }
}

fun events(filter: Filter, visit: EVENTS.() -> Unit): EVENTS {
    val events = EVENTS(filter)
    events.visit()
    return events
}

class LOCATION : Query("location") {
    fun area() = visitEntity(AREA())
    fun place() = visitEntity(PLACE())
    fun address(visit: ADDRESS.() -> Unit) = visitEntity(ADDRESS(), visit)
    fun coordinates(visit: COORDINATES.() -> Unit) = visitEntity(COORDINATES(), visit)
}

class AREA : Query("area")
class PLACE : Query("place")
class ADDRESS : Query("address") {
    fun city() = visitEntity(CITY())
    fun street() = visitEntity(STREET())
    fun no() = visitEntity(NO())
    fun state() = visitEntity(STATE())
    fun zip() = visitEntity(ZIP())
}

class CITY : Query("city")
class STREET : Query("street")
class NO : Query("no")
class STATE : Query("state")
class ZIP : Query("zip")
class COORDINATES : Query("coordinates") {
    fun longitude() = visitEntity(LONGITUDE())
    fun latitude() = visitEntity(LATITUDE())
}

class LONGITUDE : Query("longitude")
class LATITUDE : Query("latitude")

enum class FilterType(val str: String) {
    PLACE("place"),
    PRICELT("priceLT"),
    PRICEGT("priceGT"),
    TIMELT("timestampLT"),
    TIMEGT("timestampGT"),
    AREA("area"),
    TITLE("title"),
    GENRE("genre")
}

