package common

/**
 * DTO Object for holding RunnerArguments
 */
data class RunnerArgumentsDTO(
    val threadsPerWorker: Int,
    val targets: List<String>,
    val type: TSDB,
    val scale: Int
)