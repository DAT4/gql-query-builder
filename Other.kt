import com.google.gson.Gson
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

 inline fun <reified T> postStuff(query: Any, url: String): T {
 val req = URL(url)
 val con = req.openConnection() as HttpURLConnection
 con.requestMethod = "POST"
 con.connectTimeout = 30000
 con.doOutput = true
 val json = Gson().toJson(query)
 val data = (json).toByteArray()
 con.setRequestProperty("Content-Type", "application/json")
 val request = DataOutputStream(con.outputStream)
 request.write(data)
 request.flush()
 con.inputStream.bufferedReader().use {
 val response = StringBuffer()
 var inputLine = it.readLine()
 while (inputLine != null) {
 response.append(inputLine)
 inputLine = it.readLine()
 }
 return Gson().fromJson(response.toString(), T::class.java)
 }
 }

 data class GQL(val query: String)
 data class Response(val data: Data, val errors: MutableList<GQLError>?)
 data class Data(val events: MutableList<Event>)
 data class GQLError(val message: String)
 data class Event(
 val title: String,
 val genre: String,
 val image: String,
 val link: String,
 val other: MutableList<String>,
 val price: Int,
 val text: String,
 val tickets: String,
 val location: Location,
 val time: Long,
 )

 data class Address(
 val city: String,
 val street: String,
 val no: String,
 val state: String,
 val zip: Int,
 )

 data class Coordinates(
 val longitude: Float,
 val latitude: Float,
 )

 data class Location(
 val area: String,
 val address: Address,
 val place: String,
 val coordinates: Coordinates,
 )