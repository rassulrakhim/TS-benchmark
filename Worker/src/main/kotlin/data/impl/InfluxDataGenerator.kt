package data.impl

import common.InfluxData
import data.DataGenerator

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class InfluxDataGenerator : DataGenerator {

    override fun generateData(): InfluxData {
        return InfluxData("cpu_load_short,host=server2,region=us-west value=0.55")
    }

}