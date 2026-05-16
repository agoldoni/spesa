package it.agoldoni.spesa.ui.reparti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.agoldoni.spesa.data.entity.DepartmentEntity
import it.agoldoni.spesa.data.repository.SpesaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepartiViewModel @Inject constructor(
    private val repo: SpesaRepository
) : ViewModel() {

    private val _departments = MutableStateFlow<List<DepartmentEntity>>(emptyList())
    val departments: StateFlow<List<DepartmentEntity>> = _departments.asStateFlow()

    // True while the user is actively dragging; avoids DB-flow overwriting local order.
    private var isDragging = false

    init {
        viewModelScope.launch {
            repo.observeDepartments().collect { list ->
                if (!isDragging) _departments.value = list
            }
        }
    }

    fun onDragStart() {
        isDragging = true
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = _departments.value.toMutableList()
        current.add(toIndex, current.removeAt(fromIndex))
        _departments.value = current
    }

    fun onDragEnd() {
        isDragging = false
        persistOrder()
    }

    fun addDepartment(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (_departments.value.any { it.name.equals(trimmed, ignoreCase = true) }) return
        viewModelScope.launch { repo.addDepartment(trimmed) }
    }

    fun renameDepartment(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        if (_departments.value.any { it.id != id && it.name.equals(trimmed, ignoreCase = true) }) return
        viewModelScope.launch { repo.renameDepartment(id, trimmed) }
    }

    fun deleteDepartment(id: String) {
        viewModelScope.launch { repo.deleteDepartment(id) }
    }

    fun isDuplicateName(name: String, excludeId: String? = null): Boolean =
        _departments.value.any {
            it.id != excludeId && it.name.equals(name.trim(), ignoreCase = true)
        }

    private fun persistOrder() {
        val ids = _departments.value.map { it.id }
        viewModelScope.launch { repo.reorderDepartments(ids) }
    }
}
