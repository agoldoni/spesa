package it.agoldoni.spesa.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.agoldoni.spesa.data.AppDatabase
import it.agoldoni.spesa.data.dao.DepartmentDao
import it.agoldoni.spesa.data.dao.FavoriteDao
import it.agoldoni.spesa.data.dao.ListItemDao
import it.agoldoni.spesa.data.dao.MemberDao
import it.agoldoni.spesa.data.dao.ProductDao
import it.agoldoni.spesa.sync.MqttSyncSource
import it.agoldoni.spesa.sync.SyncSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "spesa.db")
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()

    @Provides fun provideMemberDao(db: AppDatabase): MemberDao = db.memberDao()
    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun provideListItemDao(db: AppDatabase): ListItemDao = db.listItemDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideDepartmentDao(db: AppDatabase): DepartmentDao = db.departmentDao()

    @Provides
    @Singleton
    fun provideSyncSource(impl: MqttSyncSource): SyncSource = impl
}
