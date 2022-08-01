package worker

import com.google.gson.GsonBuilder
import common.*
import data.DataGeneratorFactory
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.net.ConnectException

class WorkerHandler(private val tsdb: TSDB, private val config: TSDBConfig) {
    val dataGenerator = DataGeneratorFactory().getDataGenerator(tsdb)
    val log = LoggerFactory.getLogger("WorkerHandler")

    fun getWorkerMetaDataList(arg: RunnerArgumentsDTO, threadsPerWorker: Int): List<WorkerMetaData> {
        val workerMetaDataList = mutableListOf<WorkerMetaData>()
        for (worker in arg.targets.withIndex()) {
            workerMetaDataList.add(
                WorkerMetaData(
                    url = worker.value.toProperUlr(),
                    id = worker.index,
                    threads = threadsPerWorker
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

    suspend fun setConfig(worker: WorkerMetaData) {
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


    companion object {
        const val STATUS_URL = "/api/status"
        const val ID_URL = "/api/id"
        const val THREADS_URL = "/api/thread"
        const val WORKLOAD_URL = "/api/workload"
        const val START_URL = "/api/start"
        const val TSDB_URL = "/api/tsdb"
        const val CONFIG_URL = "/api/config"
    }
}

fun String.toProperUlr(): String {
    return if (!this.startsWith("http://")) {
        "http://$this"
    } else this
}