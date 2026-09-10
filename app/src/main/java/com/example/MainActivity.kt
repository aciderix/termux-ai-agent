package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.Message
import com.example.viewmodel.Role
import com.example.viewmodel.SetupStatus
import com.example.viewmodel.TermuxAgentViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: TermuxAgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: TermuxAgentViewModel, modifier: Modifier = Modifier) {
    val setupStatus by viewModel.setupStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkSetup()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (setupStatus) {
            SetupStatus.INITIALIZING -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            SetupStatus.READY -> {
                ChatScreen(viewModel = viewModel)
            }
            else -> {
                SetupScreen(
                    status = setupStatus,
                    onRetry = { viewModel.checkSetup() }
                )
            }
        }
    }
}

@Composable
fun SetupScreen(status: SetupStatus, onRetry: () -> Unit) {
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRetry()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Attention",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Configuration requise",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (status) {
            SetupStatus.MISSING_TERMUX -> {
                Text("L'application Termux n'est pas installée sur cet appareil.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                    context.startActivity(intent)
                }) {
                    Text("Télécharger Termux (F-Droid)")
                }
            }
            SetupStatus.MISSING_TERMUX_API -> {
                Text("L'extension Termux:API n'est pas installée. Elle est nécessaire pour les fonctionnalités système.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux.api/"))
                    context.startActivity(intent)
                }) {
                    Text("Télécharger Termux:API")
                }
            }
            SetupStatus.MISSING_PERMISSION -> {
                Text("La permission d'exécuter des commandes Termux en arrière-plan n'a pas été accordée.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    permissionLauncher.launch("com.termux.permission.RUN_COMMAND")
                }) {
                    Text("Accorder la permission")
                }
            }
            SetupStatus.MISSING_EXTERNAL_APPS_CONFIG -> {
                Text(
                    text = "Termux n'autorise pas l'exécution de commandes par des applications tierces (allow-external-apps=false).",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                val command = "mkdir -p ~/.termux && echo \"allow-external-apps = true\" >> ~/.termux/termux.properties && termux-reload-settings"
                
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = command,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Termux config", command))
                    Toast.makeText(context, "Commande copiée !", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copier la commande")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ouvrez Termux, collez et exécutez la commande, puis revenez ici.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Vérifier à nouveau")
        }
    }
}

val allModels = listOf(
    "qwen3.7-plus", "qwen3.7-max", "qwen3.7-flash", "qwen3.6-plus", "qwen3.6-max-preview", "qwen3.6-flash", "qwen3-coder-plus", "qwen3-coder-flash",
    "us.anthropic.claude-sonnet-4-6", "us.anthropic.claude-sonnet-4-5-20250929-v1:0", "us.anthropic.claude-opus-4-8", "us.anthropic.claude-opus-4-7", "us.anthropic.claude-opus-4-6-v1", "us.anthropic.claude-opus-4-5-20251101-v1:0", "us.anthropic.claude-haiku-4-5-20251001-v1:0",
    "claude-sonnet-5", "claude-sonnet-4-6", "claude-sonnet-4-5-20250929", "claude-opus-5", "claude-opus-4-8", "claude-opus-4-7", "claude-opus-4-6", "claude-opus-4-5-20251101", "claude-haiku-4-5-20251001", "claude-fable-5-1", "claude-fable-5",
    "deepseek-v4-pro", "deepseek-v4-flash", "deepseek-reasoner", "deepseek-chat",
    "gemini-3.8-flash", "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-3.1-flash-lite-preview", "gemini-3-flash-preview",
    "mistral-small-2603", "mistral-medium-3-5", "mistral-large-2512",
    "gpt-5.3-codex", "gpt-5.6-terra", "gpt-5.6-sol", "gpt-5.6-luna", "gpt-5.5-pro", "gpt-5.5", "gpt-5.4-nano", "gpt-5.4-mini", "gpt-5.4", "gpt-5", "gpt-4o", "o3",
    "moonshotai/kimi-k3", "moonshotai/kimi-k2.7-code", "moonshotai/kimi-k2.6",
    "grok-code-fast-1", "grok-4.6", "grok-4.5", "grok-4.3",
    "glm-5.3", "glm-5.2", "glm-5.1", "glm-5"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: TermuxAgentViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar / Model Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.padding(16.dp)
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Modèle") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                allModels.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            viewModel.selectModel(selectionOption)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Écrivez un message...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                    }
                },
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val isUser = message.role == Role.USER
    val isSystem = message.role == Role.SYSTEM || message.role == Role.TOOL
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isSystem -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isSystem -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = message.content,
            color = textColor,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .padding(12.dp)
        )
    }
}
