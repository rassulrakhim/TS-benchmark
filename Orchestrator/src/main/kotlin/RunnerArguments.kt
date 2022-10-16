import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import common.RunnerArgumentsDTO
import common.TSDB

class RunnerArguments(parser: ArgParser) {

    val threadsPerWorker: Int by parser.storing(
        "-t", help = "fff"
    ) { toInt() }.default(10)

    val targets: List<String> by parser.storing("--targets", help = "targets host:port") {
        toString().split(",")
    }.default(listOf("http://10.166.0.30:8000","http://10.166.0.31:8000","http://10.166.0.32:8000"))

    val databases: List<String> by parser.storing("--databases", help = "database urls") {
        toString().split(",")
    }.default(listOf("http://10.166.0.15:8086","http://10.166.0.16:8086","http://10.166.0.17:8086"))

    val type: TSDB by parser.mapping(
        "--influx" to TSDB.INFLUX,
        "--clickhouse" to TSDB.CLICKHOUSE,
        help = "TS TYPE"
    ).default(TSDB.INFLUX)

    val insertFrequency: Int by parser.storing("--scale", help = "scale") { toInt() }.default(100)

}

/**
 * Arguments as DTO Object
 */
fun RunnerArguments.toDTO(): RunnerArgumentsDTO {
    return RunnerArgumentsDTO(
        threadsPerWorker,
        targets,
        type,
        insertFrequency,
        databases
    )
}