package worker.impl


import common.RequestMeasurement
import common.TSDBConfig
import common.WorkloadDTO
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.content.*
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import measurements.StatisticsHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import worker.Worker

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class InfluxWorker(
    influxConfig: TSDBConfig, workloadDTO: WorkloadDTO, private val statisticsHandler: StatisticsHandler
) : Worker {

    override val logger: Logger = LoggerFactory.getLogger("InfluxWorker")
    override val config: TSDBConfig = influxConfig

    private val workload = workloadDTO

    override suspend fun createDB() {
        logger.info("Creating DB with name ${config.dbName}")
        val client = HttpClient()
        val url = "${config.url}/query"
        val params = Pair("q", "CREATE DATABASE ${config.dbName}")

        val response = client.post<HttpResponse>(url) {
            url {
                parameters.append(params.first, params.second)
            }
        }
        response.close()
        client.close()
    }

    override suspend fun loadData() {
        logger.info("Loading data to DB with name ${config.dbName}")
        var query = workload.getNextQuery()
        statisticsHandler.total = workload.queries.size
        while (query != null) {
            val measurement = RequestMeasurement()
            val client = HttpClient()
            measurement.start = System.currentTimeMillis()
            val url = "${config.url}/write?db=${config.dbName}"
            val response = client.post<HttpResponse>(url) {
                body = TextContent(query!!, contentType = ContentType.Any)
            }
            logger.info("executing = $query")
            response.close()
            client.close()
            measurement.end = System.currentTimeMillis()
            statisticsHandler.addMeasurement(measurement)
            statisticsHandler.addDone()
            query = workload.getNextQuery()
        }
        logger.info("WE MUST STOP HERE")
    }

    override fun run() {
        runBlocking {
            createDB()
            loadData()
        }
    }

}