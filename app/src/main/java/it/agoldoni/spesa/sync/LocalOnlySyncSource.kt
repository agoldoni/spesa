package it.agoldoni.spesa.sync

import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalOnlySyncSource @Inject constructor() : SyncSource {
    override fun start() = Unit
    override fun stop() = Unit
    override suspend fun pushMember(member: MemberEntity) = Unit
    override suspend fun pushProduct(product: ProductEntity) = Unit
    override suspend fun pushListItem(item: ListItemEntity) = Unit
    override suspend fun deleteListItem(id: String) = Unit
    override suspend fun clearListItems() = Unit
    override suspend fun pushFavorite(favorite: FavoriteEntity) = Unit
    override suspend fun deleteFavorite(id: String) = Unit
    override suspend fun pushFavoriteOrder(orderedIds: List<String>) = Unit
}
