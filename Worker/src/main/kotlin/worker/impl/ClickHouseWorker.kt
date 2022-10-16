package worker.impl


import common.MeasurementType
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
class ClickHouseWorker(
    config: TSDBConfig, workloadDTO: WorkloadDTO, private val statisticsHandler: StatisticsHandler
) : Worker {

    override val logger: Logger = LoggerFactory.getLogger("ClickhouseWorker")
    override val config: TSDBConfig = config

    private val workload = workloadDTO

    override suspend fun createDB() {
        logger.info("Creating DB with name ${config.dbName}")
        val client = HttpClient()
        val url = "${config.url}/query=CREATE TABLE cpu_load_short (host String, region String, value Int) ENGINE = Log&password=123"

        val response = client.post<HttpResponse>(url)
        response.close()
        client.close()
        statisticsHandler
    }

    override suspend fun loadData() {
        logger.info("Loading data to DB with name ${config.dbName}")
        var query = workload.getNextQuery()
        statisticsHandler.total = workload.queries.size
        while (query != null) {
            val type = if (query.contains("SELECT")) {
                MeasurementType.READ
            } else MeasurementType.WRITE
            val measurement = RequestMeasurement(type = type)
            val client = HttpClient()
            measurement.start = System.currentTimeMillis()
            val url = "${config.url}/$query&password=123"
            val response = client.post<HttpResponse>(url)
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