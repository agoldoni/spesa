package it.agoldoni.spesa.data.relation

data class ListItemWithDetails(
    val itemId: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val addedAt: Long,
    val memberId: String?,
    val memberName: String?,
    val memberColor: Long?,
    val departmentId: String?
)
