import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import common.TSDBConfig
import common.WorkloadDTO
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import worker.WorkerHandler
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author r.rakhim
 * @date 23.06.2022
 *
 * -influx -host:port -t=10
 *
 *
 *
 */
fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::RunnerArguments).run {
        val log = LoggerFactory.getLogger("Runner")

        val workerHandler = WorkerHandler(type)
        val workloadDTO = if (this.workload) {
            val bufferedReader: BufferedReader = File("workload.txt").bufferedReader()
            val queries = ConcurrentLinkedQueue<String>()
            bufferedReader.useLines { lines -> lines.forEach { queries.add(it) } }
            WorkloadDTO(queries)
        } else workerHandler.dataGenerator.generateData(this.scale, this.readFrequency)


        // create work meta data objects
        val workers = workerHandler.getWorkerMetaDataList(
            this.toDTO(),
            threadsPerWorker,
            this.type.name,
            this.username,
            this.password
        )

        ///check workers --> they must be waiting
        runBlocking { workers.forEach { workerHandler.getStatus(it) } }

        /// cleanWorker? setID, setThreads and setWorkload
        try {
            runBlocking {
                workers.forEach {
                    workerHandler.setId(it)
                    workerHandler.reset(it)
                    workerHandler.setThreads(it)
                    workerHandler.setWorkload(it, workloadDTO)
                    workerHandler.setTSDB(it)
                    workerHandler.setConfig(it, TSDBConfig(it.databaseUrl, it.databaseName, it.username, it.password))
                }
            }
        } catch (e: Exception) {
            log.error("Error while setting up workers. ${e.message}")
            return@mainBody
        }


        runBlocking {
            workers.forEach { workerHandler.startBenchmark(it) }
            workerHandler.startNotificationListener(workers)
            workerHandler.logResults()
        }


        return@mainBody
    }
}

