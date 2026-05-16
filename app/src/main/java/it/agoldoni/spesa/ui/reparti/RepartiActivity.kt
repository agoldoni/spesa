package it.agoldoni.spesa.ui.reparti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import it.agoldoni.spesa.data.entity.DepartmentEntity
import it.agoldoni.spesa.ui.theme.SpesaTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@AndroidEntryPoint
class RepartiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpesaTheme {
                RepartiScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepartiScreen(
    viewModel: RepartiViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val departments by viewModel.departments.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<DepartmentEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<DepartmentEntity?>(null) }

    if (showAddDialog) {
        NameDialog(
            title = "Nuovo reparto",
            initialValue = "",
            onConfirm = { name ->
                viewModel.addDepartment(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
            isDuplicate = { viewModel.isDuplicateName(it) }
        )
    }

    editTarget?.let { dept ->
        NameDialog(
            title = "Rinomina reparto",
            initialValue = dept.name,
            onConfirm = { name ->
                viewModel.renameDepartment(dept.id, name)
                editTarget = null
            },
            onDismiss = { editTarget = null },
            isDuplicate = { viewModel.isDuplicateName(it, excludeId = dept.id) }
        )
    }

    deleteTarget?.let { dept ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Elimina reparto") },
            text = { Text("Eliminare \"${dept.name}\"? I prodotti associati torneranno senza reparto.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDepartment(dept.id)
                    deleteTarget = null
                }) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Annulla") }
            }
        )
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to -> viewModel.moveItem(from.index, to.index) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reparti") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi reparto")
            }
        }
    ) { padding ->
        if (departments.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nessun reparto.\nTocca + per crearne uno.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(departments, key = { it.id }) { dept ->
                    ReorderableItem(reorderState, key = dept.id) {
                        DepartmentRow(
                            dept = dept,
                            onEdit = { editTarget = dept },
                            onDelete = { deleteTarget = dept },
                            dragHandle = {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Trascina per riordinare",
                                    modifier = Modifier.draggableHandle(
                                        onDragStarted = { viewModel.onDragStart() },
                                        onDragStopped = { viewModel.onDragEnd() }
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentRow(
    dept: DepartmentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dragHandle()
        Spacer(Modifier.width(8.dp))
        Text(
            text = dept.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Rinomina",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Elimina",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isDuplicate: (String) -> Boolean
) {
    var text by remember { mutableStateOf(initialValue) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; error = null },
                label = { Text("Nome reparto") },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = text.trim()
                when {
                    trimmed.isEmpty() -> error = "Il nome non può essere vuoto"
                    isDuplicate(trimmed) -> error = "Esiste già un reparto con questo nome"
                    else -> onConfirm(trimmed)
                }
            }) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
