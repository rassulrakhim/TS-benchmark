package data.impl

import common.WorkloadDTO
import data.DataGenerator
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class ClickhouseDataGenerator : DataGenerator {

    override fun generateData(): WorkloadDTO {
        val queries = ConcurrentLinkedQueue<String>()
//        queries.add("cpu_load_short,host=server2,region=us-east value=0.515")
        val max = 100

        for (i in 0..10000) {
            if (i.rem(10) == 0) {
                queries.add(getReadQuery())
            } else {
                queries.add("INSERT INTO cpu_load_short VALUES ('server2','us-east',$i)")
            }
        }
        return WorkloadDTO(queries)
    }

    override fun getReadQuery(): String {
        return "SELECT * FROM cpu_load_short LIMIT 1"
    }

}