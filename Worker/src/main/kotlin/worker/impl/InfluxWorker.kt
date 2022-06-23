package worker.impl

import common.InfluxConfig
import common.InfluxData
import common.TSBD.INFLUX
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import worker.WorkerCommon

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class InfluxWorker(configLine: String) : WorkerCommon(INFLUX, configLine) {

    private val config = getSealedConfig() as InfluxConfig

    override val logger: Logger = LoggerFactory.getLogger("InfluxWorker")

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

    override suspend fun getGeneratedData(): InfluxData {
        return dataGenerator.generateData() as InfluxData
    }

    override suspend fun loadData() {
        logger.info("Loading data to DB with name ${config.dbName}")
        val client = HttpClient()
        val url = "${config.host}:${config.port}/write?db=${config.dbName}"
        val data = getGeneratedData().data
        val response = client.post<HttpResponse>(url) {
            body = TextContent(data, contentType = ContentType.Any)
        }
        response.close()
        client.close()
    }

}