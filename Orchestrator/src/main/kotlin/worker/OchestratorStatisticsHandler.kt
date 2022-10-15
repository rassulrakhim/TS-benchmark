package worker

import common.RequestMeasurement
import org.slf4j.LoggerFactory

/**
 * @author r.rakhim
 * @date 13.01.2022
 */
class OchestratorStatisticsHandler {

    val log = LoggerFactory.getLogger("OrchestratorStatisticsHandler")
    private var measurements = ArrayList<Pair<Int, RequestMeasurement>>()


    @Synchronized
    fun addMeasurement(index: Int, measurement: RequestMeasurement) {
        measurements.add(Pair(index, measurement))
    }

    fun getMeasurements(index: Int): List<RequestMeasurement> {
        return measurements.filter { it.first == index }.map { it.second }.toList()
    }


    companion object {

    }

}