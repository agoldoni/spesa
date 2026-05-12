package it.agoldoni.spesa.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hivemq.client.mqtt.MqttClient
import dagger.hilt.android.AndroidEntryPoint
import it.agoldoni.spesa.data.ActiveMemberStore
import it.agoldoni.spesa.data.repository.SpesaRepository
import it.agoldoni.spesa.sync.MqttConfig
import it.agoldoni.spesa.sync.SyncSource
import it.agoldoni.spesa.ui.theme.SpesaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MqttConfigActivity : ComponentActivity() {

    @Inject lateinit var config: MqttConfig
    @Inject lateinit var syncSource: SyncSource
    @Inject lateinit var repository: SpesaRepository
    @Inject lateinit var activeMember: ActiveMemberStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpesaTheme {
                MqttConfigScreen(
                    config = config,
                    onSaved = {
                        repository.applyUserMemberConfig(config.username, config.alias) {
                            activeMember.set(it)
                        }
                        syncSource.reconnectIfNeeded()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MqttConfigScreen(
    config: MqttConfig,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(config.isEnabled) }
    var brokerUrl by remember { mutableStateOf(config.brokerUrl) }
    var port by remember { mutableStateOf(config.port.toString()) }
    var username by remember { mutableStateOf(config.username) }
    var password by remember { mutableStateOf(config.password) }
    var groupId by remember { mutableStateOf(config.groupId) }
    var useTls by remember { mutableStateOf(config.useTls) }
    var alias by remember { mutableStateOf(config.alias) }

    var brokerError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }
    var groupError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        brokerError = if (brokerUrl.isBlank()) "Broker obbligatorio" else null
        groupError = if (groupId.isBlank()) "Group ID obbligatorio" else null
        val p = port.toIntOrNull()
        portError = if (p == null || p !in 1..65535) "Porta non valida" else null
        return brokerError == null && groupError == null && portError == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurazione MQTT") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sincronizzazione attiva", modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            OutlinedTextField(
                value = brokerUrl,
                onValueChange = { brokerUrl = it.trim(); brokerError = null },
                label = { Text("Broker URL (host)") },
                isError = brokerError != null,
                supportingText = { brokerError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit); portError = null },
                label = { Text("Porta") },
                isError = portError != null,
                supportingText = { portError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (opzionale)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Alias visualizzato (opzionale)") },
                supportingText = { Text("Se valorizzato, sostituisce lo username nelle voci") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = groupId,
                onValueChange = { groupId = it.trim(); groupError = null },
                label = { Text("Group ID (es. famiglia-rossi)") },
                isError = groupError != null,
                supportingText = { groupError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TLS / SSL", modifier = Modifier.weight(1f))
                Switch(checked = useTls, onCheckedChange = { useTls = it })
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!validate()) return@Button
                    config.save(
                        enabled = enabled,
                        brokerUrl = brokerUrl,
                        port = port.toInt(),
                        username = username,
                        password = password,
                        groupId = groupId,
                        useTls = useTls,
                        alias = alias.trim()
                    )
                    Toast.makeText(context, "Configurazione salvata", Toast.LENGTH_SHORT).show()
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salva") }

            OutlinedButton(
                onClick = {
                    if (!validate()) return@OutlinedButton
                    val host = brokerUrl
                    val p = port.toInt()
                    val tls = useTls
                    val u = username
                    val pw = password
                    Toast.makeText(context, "Test in corso…", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { testConnection(host, p, u, pw, tls) } }
                        result.fold(
                            onSuccess = {
                                Toast.makeText(context, "Connessione OK", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { e ->
                                val msg = e.message ?: e::class.java.simpleName
                                Toast.makeText(context, "Errore: $msg", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Test connessione") }
        }
    }
}

private fun testConnection(
    host: String,
    port: Int,
    username: String,
    password: String,
    useTls: Boolean
) {
    val builder = MqttClient.builder()
        .useMqttVersion3()
        .identifier("spesa-test-${System.currentTimeMillis()}")
        .serverHost(host)
        .serverPort(port)
    if (useTls) builder.sslWithDefaultConfig()
    val client = builder.buildBlocking()
    if (username.isNotEmpty()) {
        client.connectWith()
            .simpleAuth()
            .username(username)
            .password(password.toByteArray(StandardCharsets.UTF_8))
            .applySimpleAuth()
            .send()
    } else {
        client.connectWith().send()
    }
    client.disconnect()
}
