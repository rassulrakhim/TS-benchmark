package common

/**
 * @author r.rakhim
 * @data 16.06.2022
 * Sealed Class for Configurations/DB Locations etc.
 */
data class TSDBConfig(
    val host: String = "",
    val port: String = "",
    val dbName: String = "testDB1"
)

