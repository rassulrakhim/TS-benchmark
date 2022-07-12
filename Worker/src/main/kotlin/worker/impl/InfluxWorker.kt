package worker.impl


import common.TSDBConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.content.*
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import worker.Worker

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class InfluxWorker(influxConfig: TSDBConfig, workload: String) : Worker {

    override val logger: Logger = LoggerFactory.getLogger("InfluxWorker")
    override val config: TSDBConfig = influxConfig
    private val data = workload


    override suspend fun createDB() {
        logger.info("Creating DB with name ${config.dbName}")
        val client = HttpClient()
        val url = "${config.host}:${config.port}/query"
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
        val client = HttpClient()
        val url = "${config.host}:${config.port}/write?db=${config.dbName}"
        val response = client.post<HttpResponse>(url) {
            body = TextContent(data, contentType = ContentType.Any)
        }
        logger.info("data = $data")
        response.close()
        client.close()
    }

    override fun run() {
        runBlocking {
            createDB()
            loadData()
        }
    }

}