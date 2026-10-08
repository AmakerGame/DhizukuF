package com.EdS.DhizukuF.dish

import java.util.concurrent.ConcurrentHashMap

/** The user's answer to an access request. */
enum class DishDecision { ALLOW, DENY, BLOCK }

/**
 * In-process mailbox: the request dialog and the request notification publish the user's
 * decision here, DishServer waits on it (all of them live in the Dhizuku process).
 */
object DishApproval {
    val results = ConcurrentHashMap<Int, DishDecision>()

    fun publish(uid: Int, decision: DishDecision) {
        results[uid] = decision
    }
}
