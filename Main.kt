// https://kotlinlang.org/docs/reference/type-safe-builders.html

fun main() {

    val filters = Filter.Builder()
            .filters(FilterType.TIMEGT, System.currentTimeMillis()/1000)
            .build()

    val e = events(filters) {
        title
        genre
        image
        link
        other
        price
        text
        time
        tickets
        location {
            area
            place
            address {
                city
                street
                no
                state
                zip
            }
            coordinates {
                longitude
                latitude
            }
        }
    }

    println(e)

    val data: Response = postStuff(GQL("$e"), "https://mama.sh/moro/api")
    val out = data.errors ?: data.data.events
    out.forEach {
        println(it)
    }
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
    protected fun <T : Element> visitEntity(entity: T, visit: T.() -> Unit): T {
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

    operator fun Entity.unaryPlus(){
        children.add(this)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class Entity(parent: Query, name: String) :Query(name) {
    init {
        parent.children.add(this)
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

    val title = object : Entity(this,"title") {}
    val genre = object : Entity(this,"genre") {}
    val image = object : Entity(this, "image") {}
    val link = object : Entity(this, "link") {}
    val other = object : Entity(this, "other") {}
    val price = object : Entity(this, "price") {}
    val text = object : Entity(this, "text") {}
    val tickets = object : Entity(this, "tickets") {}
    val time = object : Entity(this, "time") {}

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
    val area  = object:Entity(this,"area"){}
    val place = object:Entity(this,"area"){}
    fun address(visit: ADDRESS.() -> Unit) = visitEntity(ADDRESS(), visit)
    fun coordinates(visit: COORDINATES.() -> Unit) = visitEntity(COORDINATES(), visit)
}

class ADDRESS : Query("address") {
    val city = object : Entity(this,"city"){}
    val street = object : Entity(this,"street"){}
    val no = object : Entity(this,"no"){}
    val state = object : Entity(this,"state"){}
    val zip = object : Entity(this,"zip"){}
}

class COORDINATES : Query("coordinates") {
    val longitude = object : Entity(this,"longitude"){}
    val latitude = object : Entity(this,"latitude"){}
}

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

