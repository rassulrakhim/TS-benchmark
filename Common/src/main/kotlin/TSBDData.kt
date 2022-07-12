package common

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
sealed class TSBDData()

data class InfluxData(val data: String) : TSBDData()

