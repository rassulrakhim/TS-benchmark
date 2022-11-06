package common

data class WorkerMetaData(
    var url: String = "",
    var id: Int = 0,
    var status: Status = Status.UNKNOWN,
    var threads: Int = 1,
    var databaseUrl: String = "",
    val username: String = "admin",
    val password: String = "admin",
    val databaseName : String = "test"
)