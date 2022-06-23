package worker

import common.Config
import common.TSBD
import common.toConfig
import data.DataGenerator
import data.DataGeneratorFactory
import org.slf4j.Logger

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
abstract class WorkerCommon(tsbd: TSBD, configLine: String) : Worker {

    protected val dataGenerator: DataGenerator = DataGeneratorFactory().getDataGenerator(tsbd)

    private var sealedConfig: Config = configLine.toConfig(tsbd)

    abstract val logger: Logger

    protected fun getSealedConfig(): Config {
        return sealedConfig
    }

}