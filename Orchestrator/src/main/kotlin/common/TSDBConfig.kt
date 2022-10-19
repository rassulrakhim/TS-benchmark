package common

/**
 * @author r.rakhim
 * @data 16.06.2022
 * Sealed Class for Configurations/DB Locations etc.
 */
data class TSDBConfig(
    val url: String,
    val dbName: String = "test-test4"
)


