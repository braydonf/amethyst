package com.vitorpamplona.amethyst.service.model

import android.util.Log
import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toByteArray
import com.vitorpamplona.amethyst.model.toHexKey
import nostr.postr.Utils
import java.util.Date

class LnZapPaymentRequestEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: List<List<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {

    fun walletServicePubKey() = tags.firstOrNull() { it.size > 1 && it[0] == "p" }?.get(1)

    fun lnInvoice(privKey: ByteArray): String? {
        return try {
            val sharedSecret = Utils.getSharedSecret(privKey, pubKey.toByteArray())

            return Utils.decrypt(content, sharedSecret)
        } catch (e: Exception) {
            Log.w("BookmarkList", "Error decrypting the message ${e.message}")
            null
        }
    }

    companion object {
        const val kind = 23194

        fun create(
            lnInvoice: String,
            walletServicePubkey: String,
            privateKey: ByteArray,
            createdAt: Long = Date().time / 1000
        ): LnZapPaymentRequestEvent {
            val pubKey = Utils.pubkeyCreate(privateKey)
            val serializedRequest = gson.toJson(PayInvoiceMethod(lnInvoice))

            val content = Utils.encrypt(
                serializedRequest,
                privateKey,
                walletServicePubkey.toByteArray()
            )

            val tags = mutableListOf<List<String>>()
            tags.add(listOf("p", walletServicePubkey))

            val id = generateId(pubKey.toHexKey(), createdAt, kind, tags, content)
            val sig = Utils.sign(id, privateKey)
            return LnZapPaymentRequestEvent(id.toHexKey(), pubKey.toHexKey(), createdAt, tags, content, sig.toHexKey())
        }
    }
}

// REQUEST OBJECTS

abstract class Request(val method: String, val params: Params)
abstract class Params

// PayInvoice Call

class PayInvoiceMethod(bolt11: String) : Request("pay_invoice", PayInvoiceParams(bolt11)) {
    class PayInvoiceParams(val invoice: String) : Params()
}
