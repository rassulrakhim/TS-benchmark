package worker

import common.TSDB
import common.TSDBConfig
import common.WorkloadDTO
import measurements.StatisticsHandler
import org.slf4j.Logger
import worker.impl.InfluxWorker

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
interface Worker : Runnable {

    val config: TSDBConfig
    val logger: Logger

    /**
     * Executes Query to create DB.
     * throws [NotImplementedError] Exception if no implementation is needed.
     */
    suspend fun createDB(): Unit = throw NotImplementedError("No implementation was found.")

    /**
     * sends data to TSDB.
     * throws [NotImplementedError] Exception if no implementation is needed.
     */
    suspend fun loadData(): Unit = throw NotImplementedError("No implementation was found.")

    companion object {
        fun getWorker(
            tsdb: TSDB,
            config: TSDBConfig,
            workloadDTO: WorkloadDTO,
            statisticsHandler: StatisticsHandler
        ): Worker {
            return when (tsdb) {
                TSDB.INFLUX -> InfluxWorker(config, workloadDTO, statisticsHandler)
            }
        }
    }
}