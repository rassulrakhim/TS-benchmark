package common

data class WorkerMetaData(
    var url: String = "",
    var id: Int = 0,
    var status: Status = Status.UNKNOWN,
    var threads: Int = 1
)