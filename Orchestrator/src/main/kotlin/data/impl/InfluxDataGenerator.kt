package data.impl

import common.WorkloadDTO
import data.DataGenerator
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class InfluxDataGenerator : DataGenerator {

    override fun generateData(): WorkloadDTO {
        val queries = ConcurrentLinkedQueue<String>()
//        queries.add("cpu_load_short,host=server2,region=us-east value=0.515")
        val max = 100

        for (i in 0..10000) {
            queries.add("cpu_load_short,host=server2,region=us-east value=$i")
        }
        return WorkloadDTO(queries)
    }

}