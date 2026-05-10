package it.agoldoni.spesa.data

import androidx.room.Database
import androidx.room.RoomDatabase
import it.agoldoni.spesa.data.dao.FavoriteDao
import it.agoldoni.spesa.data.dao.ListItemDao
import it.agoldoni.spesa.data.dao.MemberDao
import it.agoldoni.spesa.data.dao.ProductDao
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity

@Database(
    entities = [
        MemberEntity::class,
        ProductEntity::class,
        ListItemEntity::class,
        FavoriteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun productDao(): ProductDao
    abstract fun listItemDao(): ListItemDao
    abstract fun favoriteDao(): FavoriteDao
}
