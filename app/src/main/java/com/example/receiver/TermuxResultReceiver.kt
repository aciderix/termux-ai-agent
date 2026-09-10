package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        val isPing = intent.getBooleanExtra("is_ping", false)
        
        if (bundle != null) {
            val stdout = bundle.getString("stdout", "") ?: ""
            val stderr = bundle.getString("stderr", "") ?: ""
            val exitCode = bundle.getInt("exitCode", -1)

            if (isPing) {
                ResultBus.emitPingResult(true)
                return
            }

            val resultText = buildString {
                if (stdout.isNotEmpty()) append("Sortie standard:\n$stdout\n")
                if (stderr.isNotEmpty()) append("Erreur standard:\n$stderr\n")
                append("Code de sortie: $exitCode")
            }

            ResultBus.emitResult(resultText)
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
