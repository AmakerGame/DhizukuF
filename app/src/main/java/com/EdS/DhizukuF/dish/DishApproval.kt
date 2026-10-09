package com.EdS.DhizukuF.dish

import java.util.concurrent.ConcurrentHashMap

/**
 * In-process mailbox: RequestPermissionActivity publishes the user's decision here,
 * DishServer waits on it (both live in the Dhizuku process).
 */
object DishApproval {
    val results = ConcurrentHashMap<Int, Boolean>()
}
