package data

import common.TSDB
import data.impl.ClickhouseDataGenerator
import data.impl.InfluxDataGenerator
import data.impl.TimescaleDataGenerator

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
class DataGeneratorFactory {

    /**
     * Returns DataGenerator for specific TSDB.
     */
    fun getDataGenerator(name: TSDB): DataGenerator {
        return when (name) {
            TSDB.INFLUX -> InfluxDataGenerator()
            TSDB.CLICKHOUSE -> ClickhouseDataGenerator()
            TSDB.TIMESCALE -> TimescaleDataGenerator()
        }
    }
}