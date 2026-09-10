package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.ChatRequest
import com.example.network.ChatResponse
import com.example.network.Min1AiApi
import com.example.network.PromptObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.receiver.ResultBus
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

enum class Role {
    USER, AGENT, SYSTEM, TOOL
}

data class Message(
    val role: Role,
    val content: String
)

enum class SetupStatus {
    INITIALIZING,
    MISSING_TERMUX,
    MISSING_TERMUX_API,
    MISSING_PERMISSION,
    MISSING_EXTERNAL_APPS_CONFIG,
    READY
}

class TermuxAgentViewModel(application: Application) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.1min.ai/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(Min1AiApi::class.java)

    private val _messages = MutableStateFlow<List<Message>>(
        listOf(
            Message(
                Role.SYSTEM,
                "Tu es un agent IA avec accès à Termux. Pour exécuter une commande bash, écris-la à l'intérieur des balises <cmd>ta commande</cmd>. L'application l'exécutera automatiquement et te donnera le résultat."
            )
        )
    )
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedModel = MutableStateFlow("qwen3.7-plus")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()
    
    private val _setupStatus = MutableStateFlow(SetupStatus.INITIALIZING)
    val setupStatus: StateFlow<SetupStatus> = _setupStatus.asStateFlow()
    
    init {
        ResultBus.addListener { result ->
            val toolMsg = Message(Role.TOOL, "Resultat de la commande:\n$result")
            _messages.value = _messages.value + toolMsg
            processChat()
        }
    }

    fun checkSetup() {
        val context = getApplication<Application>()
        
        if (!isPackageInstalled(context, "com.termux")) {
            _setupStatus.value = SetupStatus.MISSING_TERMUX
            return
        }
        if (!isPackageInstalled(context, "com.termux.api")) {
            _setupStatus.value = SetupStatus.MISSING_TERMUX_API
            return
        }
        if (ContextCompat.checkSelfPermission(context, "com.termux.permission.RUN_COMMAND") != PackageManager.PERMISSION_GRANTED) {
            _setupStatus.value = SetupStatus.MISSING_PERMISSION
            return
        }
        
        pingTermux()
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun pingTermux() {
        val context = getApplication<Application>()
        _setupStatus.value = SetupStatus.INITIALIZING
        
        val intent = Intent("com.termux.RUN_COMMAND")
        intent.setClassName("com.termux", "com.termux.app.RunCommandService")
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/echo")
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("ping"))
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        
        val resultIntent = Intent(context, com.example.receiver.TermuxResultReceiver::class.java)
        resultIntent.putExtra("is_ping", true)
        
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            1,
            resultIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )
        intent.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)

        try {
            var pingReceived = false
            val listener = object : (Boolean) -> Unit {
                override fun invoke(success: Boolean) {
                    pingReceived = true
                    _setupStatus.value = SetupStatus.READY
                    ResultBus.removePingListener(this)
                }
            }
            ResultBus.addPingListener(listener)
            
            context.startService(intent)
            
            viewModelScope.launch {
                delay(2000)
                if (!pingReceived) {
                    ResultBus.removePingListener(listener)
                    _setupStatus.value = SetupStatus.MISSING_EXTERNAL_APPS_CONFIG
                }
            }
        } catch (e: Exception) {
            _setupStatus.value = SetupStatus.MISSING_EXTERNAL_APPS_CONFIG
        }
    }

    fun selectModel(model: String) {
        _selectedModel.value = model
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = Message(Role.USER, text)
        _messages.value = _messages.value + userMsg
        processChat()
    }

    private fun processChat() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Build prompt
                val promptText = _messages.value.joinToString("\n\n") {
                    when (it.role) {
                        Role.SYSTEM -> "Système: ${it.content}"
                        Role.USER -> "Utilisateur: ${it.content}"
                        Role.AGENT -> "Assistant: ${it.content}"
                        Role.TOOL -> "Retour commande Termux: ${it.content}"
                    }
                } + "\n\nAssistant:"

                val request = ChatRequest(
                    model = _selectedModel.value,
                    promptObject = PromptObject(prompt = promptText)
                )

                val apiKey = BuildConfig.MIN1_API_KEY
                
                val response = api.chat(apiKey, request)
                val replyText = response.text ?: "Erreur: Pas de réponse."
                
                val agentMsg = Message(Role.AGENT, replyText)
                _messages.value = _messages.value + agentMsg

                // Check for command
                val cmdRegex = "<cmd>(.*?)</cmd>".toRegex(RegexOption.DOT_MATCHES_ALL)
                val match = cmdRegex.find(replyText)
                if (match != null) {
                    val cmd = match.groupValues[1].trim()
                    executeTermuxCommand(cmd)
                }

            } catch (e: Exception) {
                _messages.value = _messages.value + Message(Role.SYSTEM, "Erreur réseau: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun executeTermuxCommand(cmd: String) {
        val context = getApplication<Application>()
        val intent = Intent("com.termux.RUN_COMMAND")
        intent.setClassName("com.termux", "com.termux.app.RunCommandService")
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        
        val resultIntent = Intent(context, com.example.receiver.TermuxResultReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            0,
            resultIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )
        intent.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)

        try {
            context.startService(intent)
        } catch (e: Exception) {
            _messages.value = _messages.value + Message(Role.SYSTEM, "Erreur d'exécution: L'application Termux ou Termux:API n'est pas installée, ou la permission n'est pas accordée. ${e.message}")
        }
    }
}
