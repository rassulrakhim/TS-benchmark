package worker

import common.TSBDData

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
interface Worker {

    /**
     * Executes Query to create DB.
     * throws [NotImplementedError] Exception if no implementation is needed.
     */
    suspend fun createDB(): Unit = throw NotImplementedError("No implementation was found.")

    /**
     * returns generated data.
     * throws [NotImplementedError] Exception if no implementation is needed.
     */
    suspend fun getGeneratedData(): TSBDData = throw NotImplementedError("No implementation was found.")

    /**
     * sends data to TSDB.
     * throws [NotImplementedError] Exception if no implementation is needed.
     */
    suspend fun loadData(): Unit = throw NotImplementedError("No implementation was found.")


}