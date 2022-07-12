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
    }

    val type: TSDB by parser.mapping(
        "--influx" to TSDB.INFLUX,
        help = "TS TYPE"
    )

    val scale: Int by parser.storing("--scale", help = "scale") { toInt() }.default(1000)

}

/**
 * Arguments as DTO Object
 */
fun RunnerArguments.toDTO(): RunnerArgumentsDTO {
    return RunnerArgumentsDTO(
        threadsPerWorker,
        targets,
        type, scale
    )
}