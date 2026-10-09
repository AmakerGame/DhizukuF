package com.EdS.DhizukuF.dish

import android.content.Context
import android.os.Binder
import android.os.Parcel
import android.util.Log

/**
 * Binder handed to dish clients. The caller identity is Binder.getCallingUid(), set by the kernel.
 *
 * TX_EXEC (1):          in: String[] args            out: kind, [exit, out, err | message]
 * TX_WAIT_APPROVAL (2): in: -                        out: kind, [message]
 */
class DishBinder(private val context: Context) : Binder() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code != TX_EXEC && code != TX_WAIT_APPROVAL) return super.onTransact(code, data, reply, flags)
        if (reply == null) return true

        val uid = getCallingUid()
        val args = data.createStringArray()?.toList() ?: emptyList()
        val result = try {
            if (code == TX_EXEC) DishEngine.execute(context, uid, args)
            else DishEngine.awaitApproval(context, uid)
        } catch (t: Throwable) {
            Log.w("DishBinder", "request failed", t)
            DishEngine.Reply(
                DishEngine.KIND_DENIED,
                message = "Internal error: ${t.javaClass.simpleName}: ${t.message}"
            )
        }

        reply.writeInt(result.kind)
        if (result.kind == DishEngine.KIND_EXECUTED) {
            reply.writeInt(result.code)
            reply.writeString(result.out)
            reply.writeString(result.err)
        } else {
            reply.writeString(result.message)
        }
        return true
    }

    companion object {
        const val TX_EXEC = 1
        const val TX_WAIT_APPROVAL = 2
    }
}
