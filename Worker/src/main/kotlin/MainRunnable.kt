import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.runBlocking
import worker.impl.InfluxWorker



fun main() {
    val config = "http://localhost 8086 mytestdb"
    val influxWorker = InfluxWorker(config)
    runBlocking { influxWorker.loadData() }
}