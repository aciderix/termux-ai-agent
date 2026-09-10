package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isPing = intent.getBooleanExtra("is_ping", false)
        val resultBundle = intent.getBundleExtra("result")
        
        if (resultBundle != null) {
            val stdout = resultBundle.getString("stdout", "") ?: ""
            val stderr = resultBundle.getString("stderr", "") ?: ""
            val exitCode = resultBundle.getInt("exitCode", -1)
            val errmsg = resultBundle.getString("errmsg", "") ?: ""
            val err = resultBundle.getInt("err", 0)

            if (isPing) {
                ResultBus.emitPingResult(true)
                return
            }

            val resultText = buildString {
                if (errmsg.isNotEmpty()) append("Erreur Termux interne:\n$errmsg\n")
                if (err != 0) append("Code erreur interne: $err\n")
                if (stdout.isNotEmpty()) append("Sortie standard:\n$stdout\n")
                if (stderr.isNotEmpty()) append("Erreur standard:\n$stderr\n")
                append("Code de sortie: $exitCode")
            }

            ResultBus.emitResult(resultText)
        } else {
            if (isPing) {
                ResultBus.emitPingResult(false)
            } else {
                ResultBus.emitResult("Erreur: Le résultat Termux est vide (result_bundle introuvable). Vérifiez 'allow-external-apps=true' dans ~/.termux/termux.properties")
            }
        }
    }
}

object ResultBus {
    private val listeners = mutableListOf<(String) -> Unit>()
    private val pingListeners = mutableListOf<(Boolean) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun emitResult(result: String) {
        listeners.forEach { it.invoke(result) }
    }

    fun addPingListener(listener: (Boolean) -> Unit) {
        pingListeners.add(listener)
    }
    
    fun removePingListener(listener: (Boolean) -> Unit) {
        pingListeners.remove(listener)
    }

    fun emitPingResult(success: Boolean) {
        pingListeners.forEach { it.invoke(success) }
    }
}
