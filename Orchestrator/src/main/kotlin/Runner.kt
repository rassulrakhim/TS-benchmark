import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import common.TSDBConfig
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import worker.WorkerHandler

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

        ///generate data/workload
//        val config = TSDBConfig(host = "http://localhost", port = "8086", dbName = "c")
        val workerHandler = WorkerHandler(type)
        val workloadDTO = workerHandler.dataGenerator.generateData()


        // create work meta data objects
        val workers = workerHandler.getWorkerMetaDataList(this.toDTO(), threadsPerWorker)

        ///check workers --> they must be waiting
        runBlocking { workers.forEach { workerHandler.getStatus(it) } }

        /// cleanWorker? setID, setThreads and setWorkload
        try {
            runBlocking {
                workers.forEach {
                    workerHandler.setId(it)
                    workerHandler.setThreads(it)
                    workerHandler.setWorkload(it, workloadDTO)
                    workerHandler.setTSDB(it)
                    workerHandler.setConfig(it, TSDBConfig(it.databaseUrl))
                }
            }
        } catch (e: Exception) {
            log.error("Error while setting up workers. ${e.message}")
            return@mainBody
        }

        ///start benchmark
        runBlocking {
            workers.forEach { workerHandler.startBenchmark(it) }
            workerHandler.startNotificationListener(workers)
            workerHandler.logResults()
        }

        /// request status update


        //print results

        return@mainBody
    }

}

