package worker

import com.google.gson.GsonBuilder
import common.*
import data.DataGeneratorFactory
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.ConnectException

class WorkerHandler(private val tsdb: TSDB) {
    val dataGenerator = DataGeneratorFactory().getDataGenerator(tsdb)
    val log = LoggerFactory.getLogger("WorkerHandler")
    var workersMeasurements: MutableMap<Int, List<RequestMeasurement>> = mutableMapOf()
    var measurementsCollected = false
    val statisticsHandler = OchestratorStatisticsHandler()

    fun getWorkerMetaDataList(arg: RunnerArgumentsDTO, threadsPerWorker: Int): List<WorkerMetaData> {
        val workerMetaDataList = mutableListOf<WorkerMetaData>()
        for (worker in arg.targets.withIndex()) {
            workerMetaDataList.add(
                WorkerMetaData(
                    url = worker.value.toProperUlr(),
                    id = worker.index,
                    threads = threadsPerWorker,
                    databaseUrl = arg.databases[worker.index]
                )
            )
        }
        return workerMetaDataList
    }

    suspend fun getStatus(worker: WorkerMetaData): Status {
        val url = "${worker.url}$STATUS_URL"
        var answer = Status.UNKNOWN.name
        //Make call to worker
        try {
            val client = HttpClient()
            answer = client.get(url)
            client.close()
        } catch (e: ConnectException) {
            log.info("Unable to connect to worker with id=${worker.id} url=${url}")
            return Status.valueOf(answer)
        }
        log.info("Worker with id=${worker.id} now has Status = $answer")
        return Status.valueOf(answer)
    }

    suspend fun setId(worker: WorkerMetaData) {
        val client = HttpClient()
        val url = "${worker.url}$ID_URL"
        val response = client.put<HttpResponse>(url) {
            body = "${worker.id}"
        }
        client.close()
        if (response.status == HttpStatusCode.OK) {
            log.info("Worker on ${worker.url} now has id = ${worker.id}.")
        }
    }

    suspend fun setThreads(worker: WorkerMetaData) {
        val client = HttpClient()
        val url = "${worker.url}$THREADS_URL"
        val response = client.put<HttpResponse>(url) {
            body = "${worker.threads}"
        }
        client.close()
        if (response.status == HttpStatusCode.OK) {
            log.info("Worker on ${worker.url} now has ${worker.threads} Threads.")
        }
    }

    suspend fun setTSDB(worker: WorkerMetaData) {
        val client = HttpClient()
        val url = "${worker.url}$TSDB_URL"
        val response = client.put<HttpResponse>(url) {
            body = tsdb.name
        }
        client.close()
        if (response.status == HttpStatusCode.OK) {
            log.info("Worker on ${worker.url} now has ${tsdb}.")
        }
    }

    suspend fun setConfig(worker: WorkerMetaData, config: TSDBConfig) {
        val client = HttpClient()
        val url = "${worker.url}$CONFIG_URL"
        val response = client.put<HttpResponse>(url) {
            body = GsonBuilder().create().toJson(config)
        }
        client.close()
        if (response.status == HttpStatusCode.OK) {
            log.info("Worker on ${worker.url} now has ${config}.")
        }
    }

    suspend fun setWorkload(worker: WorkerMetaData, workloadDTO: WorkloadDTO) {
        val client = HttpClient()
        val url = "${worker.url}$WORKLOAD_URL"
        val response = client.put<String>(url) {
            body = GsonBuilder().create().toJson(workloadDTO.queries)
        }
        client.close()
    }

    suspend fun startBenchmark(worker: WorkerMetaData) {
        val client = HttpClient()
        val url = "${worker.url}$START_URL"
        val response = client.get<HttpResponse>(url)
        if (response.status == HttpStatusCode.OK) {
            log.info("Worker with id ${worker.id} now has started.")
        }
        client.close()
    }

    fun startNotificationListener(workers: List<WorkerMetaData>) {
        GlobalScope.launch {
            var end = false
            do {
                delay(1000)
                val workersDone: MutableMap<Int, Boolean> = mutableMapOf()  //worker id to done
                for (w in workers) {
                    try {
                        val client = HttpClient()
                        val url = "${w.url}$NOTIFICATION_URL"
                        val response = client.get<String>(url)
                        for (n in parseNotifications(response)) {
                            log.info("Worker with id=${w.id}: $n")
                            if (n.contains("100%")) {
                                workersDone[w.id] = true
                                workersMeasurements[w.id] = getMeasurements(w).toList()
                            }
                        }
                    } catch (e: ConnectException) {
                        log.error("Error while getting notofications from worker " + w.id + ", " + w.url + ": " + e.toString())
                    }
                }
                if (workers.size == workersDone.size && workersMeasurements.size == workers.size) {
                    end = true
                }
            } while (!end)
            measurementsCollected = true
        }
    }

    fun startReadQueries(workers: List<WorkerMetaData>, insertFrequency: Int) {
        GlobalScope.launch {
            do {
                delay(insertFrequency.toLong())
                for (w in workers) {
                    try {
                        val client = HttpClient()
                        val url = w.databaseUrl
                        val query = dataGenerator.getReadQuery()
                        val measurement = RequestMeasurement()
                        measurement.start = System.currentTimeMillis()
                        val response = client.post<HttpResponse>(url) {
                            body = TextContent(query, contentType = ContentType.Any)
                        }
                        measurement.end = System.currentTimeMillis()
                        statisticsHandler.addMeasurement(w.id,measurement)
                        client.close()
                        response.close()
                        println("read below")
                        println(response.content)
                    } catch (e: Throwable) {
                        log.error("Error while getting notofications from worker " + w.id + ", " + w.url + ": " + e.toString())
                    }
                }
            } while (!measurementsCollected)
        }
    }

    suspend fun logResults() {
        do {
            delay(1000)
            //Waiting to measurements to be collected
        } while (measurementsCollected.not())
        val bestDelays = mutableListOf<Pair<Int, Long>>()
        val readDelays = mutableListOf<Pair<Int, Long>>()
        for (i in workersMeasurements.keys) {
            val delaysInWorker = mutableListOf<Long>()
            println("---")
            println(statisticsHandler.getMeasurements(i))
            val readDelaysInWorker = statisticsHandler.getMeasurements(i).map { it.end - it.start }

            workersMeasurements[i]?.forEach { m ->
                val delay = m.end - m.start
                delaysInWorker.add(delay)
            }
            bestDelays.add(Pair(i, delaysInWorker.min()!!))
            readDelays.add(Pair(i, readDelaysInWorker.min()!!))
        }

        bestDelays.forEach {
            log.info("BEST INSERT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }

        readDelays.forEach {
            log.info("BEST READ DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }
    }


    private suspend fun getMeasurements(w: WorkerMetaData): Array<RequestMeasurement> {
        val client = HttpClient()
        val url = "${w.url}$MEASUREMENTS_URL"
        val response = client.get<String>(url)
        return parseMeasurements(response)
    }


    private fun parseMeasurements(measurements: String): Array<RequestMeasurement> {
        val customGson = GsonBuilder().create()
        return customGson.fromJson(measurements, Array<RequestMeasurement>::class.java)
    }

    private fun parseNotifications(notifications: String): Array<String> {
        val customGson = GsonBuilder().create()
        return customGson.fromJson(notifications, Array<String>::class.java)
    }


    companion object {
        const val STATUS_URL = "/api/status"
        const val ID_URL = "/api/id"
        const val THREADS_URL = "/api/thread"
        const val WORKLOAD_URL = "/api/workload"
        const val START_URL = "/api/start"
        const val TSDB_URL = "/api/tsdb"
        const val CONFIG_URL = "/api/config"
        const val NOTIFICATION_URL = "/api/notification"
        const val MEASUREMENTS_URL = "/api/measurements"
    }
}

fun String.toProperUlr(): String {
    return if (!this.startsWith("http://")) {
        "http://$this"
    } else this
}