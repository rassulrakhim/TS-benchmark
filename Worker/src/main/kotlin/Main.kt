import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import common.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import measurements.StatisticsHandler
import worker.Worker
import java.io.BufferedWriter
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


var status = Status.WAITING
var id: Int = -1
var threads = 1
var executor: ExecutorService = Executors.newFixedThreadPool(threads)
var workload = WorkloadDTO()
var tsdb = TSDB.INFLUX
var tsdbConfig = TSDBConfig("no_url")
val statisticsHandler = StatisticsHandler()

fun main(args: Array<String>) {
    var port = 8000
    if (args.isNotEmpty()) {
        port = args[0].toInt()
    }
    embeddedServer(Netty, port, module = Application::module).start(wait = true)
}


fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Routing) {

        get("/api/status") {
            log.info("Responed $status")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondText(Status.WAITING.name, ContentType.Text.Plain)
        }

        get("/api/reset") {
            log.info("Reseting")
            statisticsHandler.reset()
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondText(Status.WAITING.name, ContentType.Text.Plain)
        }

        put("/api/workload") {
            val content = call.receiveText()
            workload = WorkloadDTO(loadWorkload(content))
            log.info("Got new workload with " + workload.queries.size + " items")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respond(HttpStatusCode.OK)
        }
        put("/api/tsdb") {
            val content = call.receiveText()
            tsdb = TSDB.valueOf(content)
            log.info("TSBD set to $tsdb")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respond(HttpStatusCode.OK)
        }

        put("/api/config") {
            val content = call.receiveText()
            tsdbConfig = configFromString(content)
            log.info("Config was set to $tsdbConfig.")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respond(HttpStatusCode.OK)
        }

        put("/api/id") {
            id = call.receiveText().toInt()
            log.info("Worker now has id = $id")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respond(HttpStatusCode.OK)
        }
        put("/api/thread") {
            threads = call.receiveText().toInt()
            log.info("New thread number is $threads")
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respond(HttpStatusCode.OK)
        }
        get("/api/start") {
            log.info("Starting Benchmark")
            status = Status.RUNNING
            ///statHandler
            executor = Executors.newFixedThreadPool(threads)

            repeat(threads) {
                val worker = Worker.getWorker(tsdb, tsdbConfig, workload, statisticsHandler)
                executor.execute(worker)
            }

            GlobalScope.launch {
                executor.shutdown()
            }
            status = Status.DONE
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respond(HttpStatusCode.OK)
        }
        get("/api/notification") {
            log.info("Notifications requested")
            val list = statisticsHandler.getNotifications()
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondText(GsonBuilder().create().toJson(list), ContentType.Text.Plain)
        }
        get("/api/measurements") {
            log.info("Measurements requested")
            val list = statisticsHandler.getMeasurements()
            println("meassurement requresttt ${list.size}")
            val reads = list.filter { it.type == MeasurementType.READ }.map { (it.end-it.start) }.sorted()
            println("READ 75%:")
            println(percentile(reads, 25.0))
            println("READ 50%:")
            println(percentile(reads, 50.0))
            println("READ 99%:")
            println(percentile(reads, 1.0))

            val writes = list.filter { it.type == MeasurementType.WRITE }.map { (it.end-it.start) }.sorted()
            println("Write 75%:")
            println(percentile(writes, 25.0))
            println("Write 50%:")
            println(percentile(writes, 50.0))
            println("Write 99%:")
            println(percentile(writes, 1.0))
            call.response.header("Access-Control-Allow-Origin", "*")
            call.respondText(GsonBuilder().create().toJson(list), ContentType.Text.Plain)
        }



    }


}


fun percentile(latencies: List<Long>, percentile: Double): Long {
    val index = Math.ceil(percentile / 100.0 * latencies.size).toInt()
    return latencies[index - 1]
}


fun loadWorkload(workloadAsText: String): ConcurrentLinkedQueue<String> {
    val customGson = GsonBuilder().create()
    val listType: Type = object : TypeToken<ConcurrentLinkedQueue<String?>?>() {}.type
    return customGson.fromJson(workloadAsText, listType)
}

fun configFromString(configAsText: String): TSDBConfig {
    val customGson = GsonBuilder().create()
    return customGson.fromJson(configAsText, TSDBConfig::class.java)
}

