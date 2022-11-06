package data.impl

import common.WorkloadDTO
import data.DataGenerator
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class TimescaleDataGenerator : DataGenerator {

    override fun generateData(scale:Long, reads: Int): WorkloadDTO {
        val queries = ConcurrentLinkedQueue<String>()

        for (i in 0..scale) {
            if (i.rem(reads) == 0L) {
                queries.add(getReadQuery())
            } else {
                queries.add("INSERT INTO cpu_load_short VALUES (NOW(),'server2','us-east',$i)")
            }
        }
        return WorkloadDTO(queries)
    }

    override fun getReadQuery(): String {
        return "SELECT * FROM cpu_load_short LIMIT 1"
    }

}