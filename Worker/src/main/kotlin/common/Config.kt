package common

/**
 * @author r.rakhim
 * @data 16.06.2022
 * Sealed Class for Configurations/DB Locations etc.
 */
sealed class Config(
    val host: String,
    val port: String
)

class InfluxConfig(
    host: String,
    port: String,
    val dataType: String? = "--data-raw",
    val dbName: String = "testDB"
) : Config(host, port)

/**
 * From configurations from command line to [Config].
 */
fun String.toConfig(tsbd: TSBD): Config {
    val splitLine = this.split(" ")
    val host = splitLine[0]
    val port = splitLine[1]

    return when (tsbd) {
        TSBD.INFLUX -> {

            InfluxConfig(host, port, dbName = splitLine[2])
        }
    }
}