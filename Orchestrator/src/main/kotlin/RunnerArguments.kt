import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import common.RunnerArgumentsDTO
import common.TSDB

class RunnerArguments(parser: ArgParser) {

    val threadsPerWorker: Int by parser.storing("-t", help = "fff") { toInt() }.default(10)

    val targets: List<String> by parser.storing("--targets", help = "targets host:port") {
        toString().split(",")
    }.default(listOf())

    val databases: List<String> by parser.storing("--databases", help = "database urls") {
        toString().split(",")
    }.default(listOf())

    val type: TSDB by parser.mapping(
        "--influx" to TSDB.INFLUX,
        "--clickhouse" to TSDB.CLICKHOUSE,
        "--timescale" to TSDB.TIMESCALE,
        help = "TS TYPE"
    ).default(TSDB.TIMESCALE)
    val workload: Boolean by  parser.storing("--workload", help = "workload") { toBoolean() }.default(false)

    val readFrequency: Int by parser.storing("--freq", help = "scale") { toInt() }.default(10)
    val scale: Long by parser.storing("--scale", help = "scale") { toLong() }.default(10000)
    val username: String by parser.storing("-u", help = "fff").default("admin")
    val password: String by parser.storing("-p", help = "fff").default("admin")

}

/**
 * Arguments as DTO Object
 */
fun RunnerArguments.toDTO(): RunnerArgumentsDTO {
    return RunnerArgumentsDTO(
        threadsPerWorker,
        targets,
        type,
        readFrequency,
        databases
    )
}