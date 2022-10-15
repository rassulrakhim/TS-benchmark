package measurements

import common.RequestMeasurement
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * @author r.rakhim
 * @date 13.01.2022
 */
class StatisticsHandler {

    val log = LoggerFactory.getLogger("StatisticsHandler")

    private var count = 0
    var total = 0
    private var percentageToNotify = 0
    private var notifications: ArrayList<String> = ArrayList()
    private var measurements = ArrayList<RequestMeasurement>()


    @Synchronized
    fun addDone() {
        count++
        val percentage = getPercentage()
        if (percentage >= percentageToNotify) {
            GlobalScope.launch {
                notifications.add(percentage.toPercentageLog())
            }
            percentageToNotify += DEFAULT_NOTIFICATION_STEP
        }
    }

    @Synchronized
    fun addMeasurement(measurement: RequestMeasurement) {
        measurements.add(measurement)
    }

    fun getNotifications(): ArrayList<String> {
        val copy = notifications.clone() as ArrayList<String>
        resetNotifications()
        return copy
    }

    fun getMeasurements(): ArrayList<RequestMeasurement> {
        return measurements
    }

    private fun resetNotifications() {
        this.notifications = ArrayList()
    }

    private fun getPercentage(): Int = count * 100 / total

    companion object {
        private const val DEFAULT_NOTIFICATION_STEP = 10
        private const val PERCENTAGE_NOTIFICATION_LOG = "% requests are done."
        fun Int.toPercentageLog(): String = "$this$PERCENTAGE_NOTIFICATION_LOG"
    }

}