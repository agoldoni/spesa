package it.agoldoni.spesa.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.agoldoni.spesa.data.dao.DepartmentDao
import it.agoldoni.spesa.data.dao.FavoriteDao
import it.agoldoni.spesa.data.dao.ListItemDao
import it.agoldoni.spesa.data.dao.MemberDao
import it.agoldoni.spesa.data.dao.ProductDao
import it.agoldoni.spesa.data.entity.DepartmentEntity
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity

@Database(
    entities = [
        MemberEntity::class,
        ProductEntity::class,
        ListItemEntity::class,
        FavoriteEntity::class,
        DepartmentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun productDao(): ProductDao
    abstract fun listItemDao(): ListItemDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun departmentDao(): DepartmentDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS departments (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN departmentId TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_products_departmentId ON products(departmentId)"
                )
            }
        }
    }
}
