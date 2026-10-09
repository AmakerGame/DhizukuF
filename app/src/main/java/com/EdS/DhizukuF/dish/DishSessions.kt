package com.EdS.DhizukuF.dish

import java.util.concurrent.ConcurrentHashMap

/**
 * A dish client picks a random token and proves who it is once: either Android tells us the
 * sender of the broadcast (API 34+) or the permission activity, started by the client with
 * `am start`, reads the real launching uid. Later commands only carry the token.
 */
object DishSessions {
    private const val TTL_MS = 30 * 60_000L
    private const val MAX_SESSIONS = 256

    private class Claim(val uid: Int, var lastUsed: Long)

    private val claims = ConcurrentHashMap<String, Claim>()

    fun isValidToken(token: String?): Boolean =
        token != null && token.length == 32 && token.all { it in '0'..'9' || it in 'a'..'f' }

    fun claim(token: String, uid: Int) {
        prune()
        claims[token] = Claim(uid, System.currentTimeMillis())
    }

    fun uidOf(token: String): Int? {
        val claim = claims[token] ?: return null
        val now = System.currentTimeMillis()
        if (now - claim.lastUsed > TTL_MS) {
            claims.remove(token)
            return null
        }
        claim.lastUsed = now
        return claim.uid
    }

    private fun prune() {
        val now = System.currentTimeMillis()
        claims.entries.removeAll { now - it.value.lastUsed > TTL_MS }
        if (claims.size >= MAX_SESSIONS) claims.clear()
    }
}
