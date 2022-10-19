package data.impl

import common.WorkloadDTO
import data.DataGenerator
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class InfluxDataGenerator : DataGenerator {

    override fun generateData(): WorkloadDTO {
        val queries = ConcurrentLinkedQueue<String>()
//        queries.add("cpu_load_short,host=server2,region=us-east value=0.515")

        for (i in 0..40000) {
            if (i.rem(10) == 0) {
                queries.add(getReadQuery())
            } else {
                queries.add("cpu_load_short,host=server2,region=us-east value=$i")
            }
        }
        return WorkloadDTO(queries)
    }

    override fun getReadQuery(): String {
        return "SELECT LAST(\"cpu_load_short\") FROM test-test"
    }

}