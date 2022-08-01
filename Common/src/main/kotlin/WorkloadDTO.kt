package common

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author r.rakhim
 * @date 12.07.2022
 */
data class WorkloadDTO(val queries: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()) {

    fun getNextQuery(): String? {
        return this.queries.poll()
    }
}
