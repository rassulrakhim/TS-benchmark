package data

import common.TSBD
import data.impl.InfluxDataGenerator

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class DataGeneratorFactory {

    /**
     * Returns DataGenerator for specific TSDB.
     */
    fun getDataGenerator(name: TSBD): DataGenerator {
        return when (name) {
            TSBD.INFLUX -> InfluxDataGenerator()
        }
    }
}