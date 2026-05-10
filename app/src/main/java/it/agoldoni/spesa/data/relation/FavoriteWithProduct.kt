package it.agoldoni.spesa.data.relation

data class FavoriteWithProduct(
    val favoriteId: String,
    val productId: String,
    val productName: String,
    val ordering: Int
)
