package it.agoldoni.spesa.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.agoldoni.spesa.data.ActiveMemberStore
import it.agoldoni.spesa.data.entity.DepartmentEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity
import it.agoldoni.spesa.data.relation.FavoriteWithProduct
import it.agoldoni.spesa.data.relation.ListItemWithDetails
import it.agoldoni.spesa.data.repository.SpesaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingGroup(
    val departmentId: String?,
    val departmentName: String?,
    val items: List<ListItemWithDetails>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repo: SpesaRepository,
    private val activeMember: ActiveMemberStore
) : ViewModel() {

    val members: StateFlow<List<MemberEntity>> = repo.observeMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteWithProduct>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val departments: StateFlow<List<DepartmentEntity>> = repo.observeDepartments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val itemCount: StateFlow<Int> = repo.observeItemCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalQuantity: StateFlow<Int> = repo.observeTotalQuantity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeMemberId: StateFlow<String?> = activeMember.observeActiveMemberId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shoppingGroups: StateFlow<List<ShoppingGroup>> = combine(
        repo.observeListItems(),
        repo.observeDepartments()
    ) { items, depts ->
        buildGroups(items, depts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    val suggestions: StateFlow<List<ProductEntity>> = _input
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList<ProductEntity>())
            else repo.observeSuggestions(q.trim()) as Flow<List<ProductEntity>>
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onInputChange(value: String) {
        _input.value = value
    }

    fun selectMember(id: String) {
        activeMember.set(id)
    }

    fun submitInput() {
        val text = _input.value.trim()
        if (text.isEmpty()) return
        val active = activeMember.get()
        viewModelScope.launch {
            repo.addOrIncrementByName(text, active)
            _input.value = ""
        }
    }

    fun pickFavorite(productId: String) {
        val active = activeMember.get()
        viewModelScope.launch {
            repo.addOrIncrement(productId, active)
        }
    }

    fun increment(itemId: String) {
        viewModelScope.launch { repo.increment(itemId) }
    }

    fun decrement(itemId: String) {
        viewModelScope.launch { repo.decrement(itemId) }
    }

    fun remove(itemId: String) {
        viewModelScope.launch { repo.remove(itemId) }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch { repo.toggleFavorite(productId) }
    }

    fun reorderFavorites(orderedIds: List<String>) {
        viewModelScope.launch { repo.reorderFavorites(orderedIds) }
    }

    fun setProductDepartment(productId: String, departmentId: String?) {
        viewModelScope.launch { repo.setProductDepartment(productId, departmentId) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    private fun buildGroups(
        items: List<ListItemWithDetails>,
        departments: List<DepartmentEntity>
    ): List<ShoppingGroup> {
        val byDept = items.groupBy { it.departmentId }
        val result = mutableListOf<ShoppingGroup>()

        for (dept in departments) {
            val deptItems = (byDept[dept.id] ?: emptyList())
                .sortedBy { it.productName.lowercase() }
            if (deptItems.isNotEmpty()) {
                result.add(ShoppingGroup(dept.id, dept.name, deptItems))
            }
        }

        val unassigned = (byDept[null] ?: emptyList())
            .sortedBy { it.productName.lowercase() }
        if (unassigned.isNotEmpty()) {
            result.add(ShoppingGroup(null, null, unassigned))
        }

        return result
    }
}
