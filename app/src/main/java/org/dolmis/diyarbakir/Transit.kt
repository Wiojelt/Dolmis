package org.dolmis.diyarbakir

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.HttpsURLConnection
import kotlin.math.*

data class Route(val code: String, val line: String, val name: String) {
    val title: String get() {
        val prefix = line.filterNot(Char::isWhitespace).map { Regex.escape(it.toString()) }.joinToString("\\s*")
        return name.replaceFirst(Regex("^$prefix\\s*[-–]?\\s*", RegexOption.IGNORE_CASE), "").replace(Regex("\\s+-\\s+"), " · ")
    }
    val displayCode: String get() = line.replace(" ", "")
    fun json() = JSONObject().put("routeCode", code).put("lineCode", line).put("name", name)
}
data class Point(val latitude: Double, val longitude: Double) {
    fun json() = JSONObject().put("latitude", latitude).put("longitude", longitude)
}
data class Stop(val id: String, val name: String, val point: Point?)
data class DirectionStop(val direction: String, val id: String, val name: String, val order: Int)
data class Bus(val id: String, val label: String, val point: Point, val speed: Double?, val direction: String, val updatedAt: String, val timestamp: Long) {
    fun age(now: Long) = (now - timestamp) / 1000.0
    fun fresh(now: Long) = timestamp > 0 && age(now) in -60.0..120.0
    fun usable(now: Long) = timestamp > 0 && age(now) in -60.0..600.0
    fun ageLabel(now: Long): String = when {
        age(now) < 60 -> "Az önce"
        else -> "${(age(now) / 60).toInt()} dk önce"
    }
}
fun JSONArray.objects() = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONArray.strings() = (0 until length()).map { optString(it) }.filter(String::isNotBlank)
fun routeFromJson(j: JSONObject): Route = Route(j.optString("routeCode", j.optString("lineCode")), j.optString("lineCode", j.optString("routeCode")), j.optString("name", j.optString("routeCode")))
fun pointFromJson(j: JSONObject): Point? {
    val lat = j.optDouble("latitude", Double.NaN); val lon = j.optDouble("longitude", Double.NaN)
    return if (lat.isFinite() && lon.isFinite() && lat in 36.0..40.0 && lon in 38.0..42.0) Point(lat, lon) else null
}
fun stopFromJson(j: JSONObject) = Stop(j.optString("stationId"), j.optString("name"), pointFromJson(j))
fun fold(text: String) = java.text.Normalizer.normalize(text.lowercase(Locale.forLanguageTag("tr")), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").replace('ı', 'i')
fun meters(a: Point, b: Point): Double {
    val r = Math.PI / 180
    val h = sin((b.latitude-a.latitude)*r/2).pow(2) + cos(a.latitude*r)*cos(b.latitude*r)*sin((b.longitude-a.longitude)*r/2).pow(2)
    return 6371000 * 2 * atan2(sqrt(h.coerceIn(0.0,1.0)), sqrt((1-h).coerceIn(0.0,1.0)))
}
fun distanceLabel(value: Double) = if (value < 1000) "${value.roundToInt()} m" else String.format(Locale.forLanguageTag("tr"), "%.1f km", value/1000)
fun timestamp(value: String): Long = runCatching {
    if (Regex("[zZ]$|[+-]\\d\\d:?\\d\\d$").containsMatchIn(value)) OffsetDateTime.parse(value).toInstant().toEpochMilli()
    else LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.ofHours(3)).toEpochMilli()
}.getOrDefault(0)

/** Fixed host, read-only endpoints, TLS validation, and strict response field selection. */
object TransitApi {
    private val endpoints = setOf("live-data", "line-directions", "route-stations", "route-coordinate", "station-routes", "schedules")
    private val keys = setOf("routeCode", "lineCode", "routeDirection", "stationId", "scheduleDate")
    private data class Cached(val at: Long, val body: String)
    private val cache = ConcurrentHashMap<String, Cached>()
    suspend fun get(endpoint: String, params: Map<String, String>): JSONArray = withContext(Dispatchers.IO) {
        require(endpoint in endpoints)
        val uri = Uri.Builder().scheme("https").authority("www.diyarbakir.bel.tr").appendEncodedPath("api/transport/asis/$endpoint")
        params.forEach { (k,v) ->
            require(k in keys && v.length <= 60 && v.isNotBlank() && v.matches(Regex("[\\p{L}\\p{N}_.:+ -]+")))
            uri.appendQueryParameter(k,v)
        }
        val address = uri.build().toString()
        val ttl = if (endpoint == "live-data") 15000 else 300000
        cache[address]?.takeIf { SystemClock.elapsedRealtime()-it.at < ttl }?.let { return@withContext JSONArray(it.body) }
        val connection = URL(address).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 8000; connection.readTimeout = 12000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "Dolmis/0.2 (independent public-transit viewer)")
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == 200) { "Transit unavailable" }
            val bytes = connection.inputStream.use { input ->
                val buffer = java.io.ByteArrayOutputStream(); val part = ByteArray(16384)
                while (true) { val count = input.read(part); if (count < 0) break; check(buffer.size()+count <= 4*1024*1024); buffer.write(part,0,count) }
                buffer.toByteArray()
            }
            val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
            val items = envelope.optJSONArray("items") ?: error("Transit unavailable")
            check(items.length() <= 10000)
            // No upstream raw object, driver name or financial field is retained in app state.
            val result = JSONArray()
            items.objects().forEach { item ->
                val raw = item.optJSONObject("raw"); val safe = JSONObject()
                fun copy(from: JSONObject?, source: String, target: String = source) {
                    val value = from?.opt(source)
                    if (value is Number || value is Boolean || (value is String && value.length <= 300)) safe.put(target, value)
                }
                when (endpoint) {
                    "live-data" -> {
                        listOf("id","label","plate","latitude","longitude","speed","updatedAt").forEach { copy(item,it) }
                        copy(raw,"routeCode"); copy(raw,"routeDriction","routeDirection")
                    }
                    "line-directions" -> {
                        listOf("code","stationId","order").forEach { copy(item,it) }; copy(raw,"stopName","name")
                    }
                    "route-stations" -> {
                        listOf("stationId","name","order").forEach { copy(item,it) }
                        if (!safe.has("order")) copy(raw,"sequence","order")
                        listOf("latitude","longitude").forEach { key -> raw?.optDouble(key,Double.NaN)?.takeIf(Double::isFinite)?.let { safe.put(key,it) } }
                    }
                    "route-coordinate" -> listOf("latitude","longitude").forEach { copy(item,it) }
                    "station-routes" -> {
                        listOf("lineCode","routeCode","name").forEach { copy(item,it) }; copy(raw,"routeLongName","name")
                    }
                    // A nonempty schedule schema has not been verified; never display guessed times.
                    "schedules" -> Unit
                }
                if (endpoint != "schedules") result.put(safe)
            }
            if (endpoint != "schedules") cache[address] = Cached(SystemClock.elapsedRealtime(), result.toString())
            result
        } finally { connection.disconnect() }
    }
}

class TransitState(context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("dolmis", Context.MODE_PRIVATE)
    val lines = JSONArray(context.assets.open("data/lines.json").bufferedReader().use { it.readText() }).objects().map(::routeFromJson).sortedWith(compareBy { it.displayCode })
    val stations = JSONArray(context.assets.open("data/stations.json").bufferedReader().use { it.readText() }).objects().map(::stopFromJson)
    var route by mutableStateOf(runCatching { routeFromJson(JSONObject(prefs.getString("route", "")!!)) }.getOrElse { lines.first { it.code == "Z7" } })
    var tab by mutableStateOf(0)
    var direction by mutableStateOf("all")
    var buses by mutableStateOf(emptyList<Bus>())
    var routeStops by mutableStateOf(emptyList<Stop>())
    var directionStops by mutableStateOf(emptyList<DirectionStop>())
    var coordinates by mutableStateOf(emptyMap<String,List<Point>>())
    var loading by mutableStateOf(false)
    var geometryLoading by mutableStateOf(false)
    var lastUpdate by mutableStateOf<Long?>(null)
    var error by mutableStateOf(false)
    var favoriteLines by mutableStateOf(prefs.getStringSet("favoriteLines", emptySet())!!.toSet())
    var favoriteStops by mutableStateOf(prefs.getStringSet("favoriteStops", emptySet())!!.toSet())
    var location by mutableStateOf<Point?>(null)
    var message by mutableStateOf<String?>(null)
    var sheet by mutableStateOf<String?>(null)
    var selectedStop by mutableStateOf<Stop?>(null)
    var stationRoutes by mutableStateOf(emptyList<Route>())
    var stationLoading by mutableStateOf(false)
    var stationError by mutableStateOf(false)
    var scheduleLoading by mutableStateOf(false)
    var scheduleMessage by mutableStateOf("")
    var selectedBus by mutableStateOf<String?>(null)
    private var generation = 0
    fun selectRoute(next: Route) {
        generation++; route=next; tab=0; buses=emptyList(); routeStops=emptyList(); directionStops=emptyList(); coordinates=emptyMap()
        error=false; lastUpdate=null; selectedBus=null; sheet=null; loading=false; geometryLoading=false
        prefs.edit().putString("route",next.json().toString()).apply()
        loadGeometry()
        scope.launch { refresh() }
    }
    fun importLegacy(text: String) {
        if (prefs.getBoolean("migrated",false)) return
        runCatching {
            val data=JSONObject(text); val favorites=data.optJSONObject("favorites")
            favorites?.optJSONArray("lines")?.let { favoriteLines= favoriteLines+it.strings() }
            favorites?.optJSONArray("stops")?.let { favoriteStops= favoriteStops+it.strings() }
            if (!prefs.contains("route")) data.optJSONObject("route")?.let { old -> lines.find { it.code == old.optString("routeCode") }?.let(::selectRoute) }
            saveFavorites()
        }
        prefs.edit().putBoolean("migrated",true).apply()
    }
    fun favoriteLine(code: String) { favoriteLines = if (code in favoriteLines) favoriteLines-code else favoriteLines+code; saveFavorites() }
    fun favoriteStop(id: String) { favoriteStops = if (id in favoriteStops) favoriteStops-id else favoriteStops+id; saveFavorites() }
    private fun saveFavorites() { prefs.edit().putStringSet("favoriteLines", favoriteLines).putStringSet("favoriteStops",favoriteStops).apply() }
    fun visibleBuses(now: Long) = buses.filter { it.usable(now) && (direction == "all" || it.direction == direction) }
    fun visibleStops(): List<Stop> {
        if (directionStops.isEmpty()) return routeStops
        val byId=routeStops.associateBy(Stop::id)
        return directionStops.filter { direction == "all" || it.direction == direction }.distinctBy(DirectionStop::id).mapNotNull { byId[it.id] }
    }
    suspend fun refresh() {
        if (loading) return
        val requestGeneration=generation; val selected=route; loading=true
        try {
            val items=TransitApi.get("live-data",mapOf("routeCode" to selected.code))
            if (requestGeneration != generation) return
            buses=items.objects().filter { !it.has("routeCode") || fold(it.optString("routeCode")).replace(" ","")==fold(selected.code).replace(" ","") }.mapNotNull { j ->
                val point=pointFromJson(j) ?: return@mapNotNull null
                val plate=j.optString("plate").ifBlank { j.optString("label") }
                Bus(j.optString("id").ifBlank { plate }, plate.ifBlank { "${selected.displayCode} · Otobüs" },point,j.optDouble("speed",Double.NaN).takeIf(Double::isFinite),j.optString("routeDirection"),j.optString("updatedAt"), timestamp(j.optString("updatedAt")))
            }
            lastUpdate=System.currentTimeMillis(); error=false
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { if (requestGeneration==generation) error=true }
        finally { if (requestGeneration==generation) { loading=false; if (routeStops.isEmpty() && !geometryLoading) loadGeometry() } }
    }
    fun manualRefresh() {
        if (lastUpdate?.let { System.currentTimeMillis()-it < 10000 } == true) { message="Yeni sorgu için birkaç saniye bekle."; return }
        scope.launch { refresh() }
    }
    fun loadGeometry() {
        if (geometryLoading) return
        val requestGeneration=generation; val selected=route; geometryLoading=true
        scope.launch {
            supervisorScope {
                suspend fun fetch(endpoint: String, params: Map<String,String>): JSONArray? = try { TransitApi.get(endpoint,params) } catch(c: CancellationException) { throw c } catch(_: Exception) { null }
                val stops=async { fetch("route-stations",mapOf("routeCode" to selected.code)) }
                val directions=async { fetch("line-directions",mapOf("lineCode" to selected.line)) }
                val paths=listOf("G","D").associateWith { d -> async { fetch("route-coordinate",mapOf("routeCode" to selected.code,"routeDirection" to d)) } }
                val stopData=stops.await(); val directionData=directions.await()
                if (requestGeneration != generation) return@supervisorScope
                routeStops=stopData?.objects()?.map(::stopFromJson) ?: emptyList()
                directionStops=directionData?.objects()?.map { DirectionStop(it.optString("code"),it.optString("stationId"),it.optString("name"),it.optInt("order")) } ?: emptyList()
                val coords=paths.mapValues { (_,v) -> v.await()?.objects()?.mapNotNull(::pointFromJson) ?: emptyList() }
                if (requestGeneration==generation) { coordinates=coords; geometryLoading=false }
            }
        }
    }
    fun showStop(stop: Stop) {
        selectedStop=stop; sheet="station"; stationRoutes=emptyList(); stationLoading=true; stationError=false
        scope.launch {
            try { val data=TransitApi.get("station-routes",mapOf("stationId" to stop.id)); if(selectedStop?.id==stop.id)stationRoutes=data.objects().map(::routeFromJson).distinctBy(Route::code) }
            catch(c: CancellationException){throw c} catch(_: Exception){if(selectedStop?.id==stop.id)stationError=true}
            finally { if(selectedStop?.id==stop.id)stationLoading=false }
        }
    }
    fun showSchedule() {
        val selected=route; sheet="schedule"; scheduleLoading=true
        scope.launch {
            scheduleMessage=try {
                TransitApi.get("schedules",mapOf("lineCode" to selected.line,"scheduleDate" to LocalDate.now(ZoneId.of("Europe/Istanbul")).toString()+"T00:00:00.000Z"))
                "Bu hat için doğrulanmış saat verisi alınamadı. Bu, sefer olmadığı anlamına gelmez."
            } catch(c: CancellationException){throw c} catch(_: Exception){"Sefer saati servisine şu anda ulaşılamıyor."}
            scheduleLoading=false
        }
    }
}
