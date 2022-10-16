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

    suspend fun reset(worker: WorkerMetaData) {
        val client = HttpClient()
        val url = "${worker.url}$RESET_URL"
        val response = client.get<HttpResponse>(url)
        client.close()
        if (response.status == HttpStatusCode.OK) {
            log.info("Worker on ${worker.url} now reseted.")
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


    suspend fun logResults() {
        do {
            delay(1000)
            //Waiting to measurements to be collected
        } while (measurementsCollected.not())
        val bestWriteDelays = mutableListOf<Pair<Int, Long>>()
        val bestReadDelays = mutableListOf<Pair<Int, Long>>()

        val averageWriteDelays = mutableListOf<Pair<Int, Double>>()
        val averageReadDelays = mutableListOf<Pair<Int, Double>>()

        val worstWriteDelays = mutableListOf<Pair<Int, Long>>()
        val worstReadDelays = mutableListOf<Pair<Int, Long>>()

        val overallWrites = mutableListOf<Pair<Int, Int>>()
        val overallReads = mutableListOf<Pair<Int, Int>>()


        for (i in workersMeasurements.keys) {
            val writeDelaysInWorker = mutableListOf<Long>()
            val readDelaysInWorker = mutableListOf<Long>()

            workersMeasurements[i]?.forEach { m ->
                val delay = m.end - m.start
                if (m.type == MeasurementType.READ) {
                    readDelaysInWorker.add(delay)
                } else {
                    writeDelaysInWorker.add(delay)
                }
            }
            bestWriteDelays.add(Pair(i, writeDelaysInWorker.min()!!))
            bestReadDelays.add(Pair(i, readDelaysInWorker.min()!!))

            averageWriteDelays.add(Pair(i, writeDelaysInWorker.average()))
            averageReadDelays.add(Pair(i, readDelaysInWorker.average()))

            worstWriteDelays.add(Pair(i, writeDelaysInWorker.max()!!))
            worstReadDelays.add(Pair(i, readDelaysInWorker.max()!!))

            overallWrites.add(Pair(i,writeDelaysInWorker.size))
            overallReads.add(Pair(i,readDelaysInWorker.size))

        }

        overallWrites.forEach {
            log.info("INSERT QUERIES FOR WORKER WITH ID ${it.first} is ${it.second} WAS DONE")
        }

        overallReads.forEach {
            log.info("SELECT QUERIES FOR WORKER WITH ID ${it.first} is ${it.second} WAS DONE")
        }


        bestWriteDelays.forEach {
            log.info("BEST INSERT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }

        bestReadDelays.forEach {
            log.info("BEST SELECT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }


        averageWriteDelays.forEach {
            log.info("AVERAGE INSERT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }

        averageReadDelays.forEach {
            log.info("AVERAGE SELECT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }


        worstWriteDelays.forEach {
            log.info("WORST INSERT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
        }

        worstReadDelays.forEach {
            log.info("WORST SELECT DELAY FOR WORKER WITH ID ${it.first} is ${it.second} ms")
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
        const val RESET_URL = "/api/reset"
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