package worker.impl


import common.MeasurementType
import common.RequestMeasurement
import common.TSDBConfig
import common.WorkloadDTO
import kotlinx.coroutines.runBlocking
import measurements.StatisticsHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import worker.Worker
import java.sql.DriverManager

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class TimescaleWorker(
    config: TSDBConfig, workloadDTO: WorkloadDTO, private val statisticsHandler: StatisticsHandler
) : Worker {

    override val logger: Logger = LoggerFactory.getLogger("TimescaleWorker")
    override val config: TSDBConfig = config

    private val workload = workloadDTO

    override suspend fun createDB() {
        logger.info("Creating DB with name ${config.dbName}")
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
            try {
                measurement.start = System.currentTimeMillis()
                logger.info("executing = $query")
//                val url = "jdbc:postgresql://sr7ug1dsjk.pospgdoicm.tsdb.cloud.timescale.com:34002/tsdb"
//                val user = "tsdbadmin"
//                val password = "i9isgx9utk8tth30"
                val url = config.url
                val user = config.username
                val password = config.password

                try {
                    DriverManager.getConnection(url, user, password).use { con ->
                        measurement.start = System.currentTimeMillis()
                        con.createStatement().use { st ->
                            st.executeUpdate(query)
                        }
                    }
                } catch (ex: Throwable) {
                    logger.info("ERROR: ${ex.message}")
                }

                measurement.end = System.currentTimeMillis()
                statisticsHandler.addMeasurement(measurement)
                statisticsHandler.addDone()
                query = workload.getNextQuery()
            } catch (e: Throwable) {
                println("we are in catch")
                println(e.message)
            }

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