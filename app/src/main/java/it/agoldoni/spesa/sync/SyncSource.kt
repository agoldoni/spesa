package it.agoldoni.spesa.sync

import it.agoldoni.spesa.data.entity.DepartmentEntity
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity

interface SyncSource {
    fun start()
    fun stop()
    fun reconnectIfNeeded()
    fun isConnected(): Boolean

    suspend fun pushMember(member: MemberEntity)
    suspend fun pushProduct(product: ProductEntity)
    suspend fun pushListItem(item: ListItemEntity)
    suspend fun pushFavorite(favorite: FavoriteEntity)
    suspend fun pushDepartment(department: DepartmentEntity)
}
