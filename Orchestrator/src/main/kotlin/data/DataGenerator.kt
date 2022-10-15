package data

import common.WorkloadDTO

/**
 * @author r.rakhim
 * @date 16.06.2022
 */
interface DataGenerator {


    fun generateData(): WorkloadDTO

    fun getReadQuery(): String


}