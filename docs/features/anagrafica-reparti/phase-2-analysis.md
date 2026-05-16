# Fase 2 — Analisi Tecnica: Anagrafica Reparti

**Data:** 2026-05-16
**Stato:** Completata

---

## A. File coinvolti

### Nuovi file

| Percorso | Tipo | Motivazione |
|---|---|---|
| `app/src/main/java/.../data/entity/DepartmentEntity.kt` | Nuovo | Entity Room per reparti (id, name, position, updatedAt) |
| `app/src/main/java/.../data/dao/DepartmentDao.kt` | Nuovo | CRUD + riordinamento reparti |
| `app/src/main/java/.../ui/reparti/RepartiActivity.kt` | Nuovo | Activity CRUD reparti con drag & drop (pattern MqttConfigActivity) |
| `app/src/main/java/.../ui/reparti/RepartiViewModel.kt` | Nuovo | ViewModel per RepartiActivity, iniettato con Hilt |

### File modificati

| Percorso | Tipo | Motivazione |
|---|---|---|
| `data/entity/ProductEntity.kt` | Modifica | Aggiunta colonna `departmentId: String?` + FK su `departments` |
| `data/AppDatabase.kt` | Modifica | version 2→3, aggiunte `DepartmentEntity` e `MIGRATION_2_3`, `DepartmentDao` |
| `sync/SyncSource.kt` | Modifica | Aggiunta `pushDepartment`, `deleteDepartment` |
| `sync/MqttSyncSource.kt` | Modifica | KIND `departments`, `handleDepartment`, `publishAll` aggiornato |
| `data/repository/SpesaRepository.kt` | Modifica | `observeDepartments`, `addDepartment`, `renameDepartment`, `deleteDepartment`, `reorderDepartments`, `setProductDepartment` |
| `di/AppModule.kt` | Modifica | Aggiunta `provideDepartmentDao`, switch a migration esplicita |
| `ui/shopping/ShoppingScreen.kt` | Modifica | Header: secondo IconButton per aprire `RepartiActivity`; `ItemsList` refactoring con `stickyHeader` per raggruppamento |
| `ui/shopping/ShoppingViewModel.kt` | Modifica | Aggiunta `departments: StateFlow`, logica raggruppamento items per reparto |
| `app/src/main/AndroidManifest.xml` | Modifica | Registrazione `RepartiActivity` |
| `gradle/libs.versions.toml` | Modifica | Aggiunta `reorderable = "2.4.3"` e libreria `sh.calvin.reorderable` |
| `app/build.gradle.kts` | Modifica | Aggiunta dipendenza `libs.reorderable` |

---

## B. Contratti e interfacce da modificare

### 1. `DepartmentEntity` (nuovo)

```kotlin
@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 2. `ProductEntity` — aggiunta colonna nullable (breaking: migrazione Room)

```kotlin
// Prima: nessun riferimento a department
// Dopo: aggiungere campo
val departmentId: String? = null
```

FK opzionale: si gestisce come `onDelete = ForeignKey.SET_NULL` (vedi sotto).

```kotlin
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["nameKey"], unique = true),
        Index("departmentId")  // nuovo
    ],
    foreignKeys = [            // nuovo
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameKey: String,
    val addedAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val departmentId: String? = null   // ← aggiunto
)
```

### 3. `DepartmentDao` (nuovo)

```kotlin
@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY position ASC")
    fun observeAll(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments ORDER BY position ASC")
    suspend fun getAll(): List<DepartmentEntity>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getById(id: String): DepartmentEntity?

    @Upsert
    suspend fun upsert(entity: DepartmentEntity)

    @Query("DELETE FROM departments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            updatePosition(id, index)
        }
    }

    @Query("UPDATE departments SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)
}
```

### 4. `SyncSource` — aggiunta di 2 metodi (breaking: impl concreta)

```kotlin
// Aggiungere a SyncSource.kt:
suspend fun pushDepartment(department: DepartmentEntity)
suspend fun deleteDepartment(id: String)
```

### 5. `AppDatabase` — migrazione esplicita v2→v3

```kotlin
@Database(
    entities = [MemberEntity::class, ProductEntity::class, ListItemEntity::class,
                FavoriteEntity::class, DepartmentEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // ...
    abstract fun departmentDao(): DepartmentDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS departments (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL(
                    "ALTER TABLE products ADD COLUMN departmentId TEXT DEFAULT NULL REFERENCES departments(id) ON DELETE SET NULL"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_products_departmentId ON products(departmentId)"
                )
            }
        }
    }
}
```

### 6. `AppModule` — switch da `fallbackToDestructiveMigration` a migration esplicita

```kotlin
fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "spesa.db")
        .addMigrations(AppDatabase.MIGRATION_2_3)   // ← sostituisce fallbackToDestructiveMigration
        .build()

@Provides fun provideDepartmentDao(db: AppDatabase): DepartmentDao = db.departmentDao()
```

---

## C. Pattern da rispettare

### Naming conventions (da codice esistente)
- Entity: `DepartmentEntity` (suffisso `Entity`)
- DAO: `DepartmentDao` (suffisso `Dao`)
- UUID primary key come `String`
- Campo `updatedAt: Long = System.currentTimeMillis()` per LWW sync
- `nameKey` non serve per reparti (nomi non deduplicati a livello DB; la validazione duplicati è nel repository)

### Pattern Repository (da `SpesaRepository.kt:185`)
```kotlin
private fun mirror(block: suspend () -> Unit) {
    scope.launch { runCatching { block() } }
}
```
Ogni operazione di scrittura: aggiorna DAO → chiama `mirror { sync.push*() }`.

### Pattern MQTT KIND (da `MqttSyncSource.kt:241-246`)
```kotlin
private const val KIND_DEPARTMENTS = "departments"
private val KINDS = listOf(KIND_MEMBERS, KIND_PRODUCTS, KIND_LIST_ITEMS, KIND_FAVORITES, KIND_DEPARTMENTS)
```
Topic: `sync/{groupId}/departments/{id}`.

Delete signal: payload vuoto sul topic retained (già implementato in `publishDelete`).

Nel blocco `when (kind)` di `handleIncoming`: aggiungere `KIND_DEPARTMENTS -> handleDepartment(json)`.
Nel blocco delete `when (kind)`: aggiungere `KIND_DEPARTMENTS -> db.departmentDao().deleteById(id)`.

### Pattern Activity (da `MqttConfigActivity.kt`)
```kotlin
@AndroidEntryPoint
class RepartiActivity : ComponentActivity() {
    @Inject lateinit var repository: SpesaRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpesaTheme { /* RepartiScreen */ } }
    }
}
```
Stessa struttura: `@AndroidEntryPoint`, `enableEdgeToEdge()`, `SpesaTheme`, back via `finish()`.

### Pattern ViewModel (da `ShoppingViewModel.kt:31-47`)
```kotlin
val departments: StateFlow<List<DepartmentEntity>> = repo.observeDepartments()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### Drag & drop — libreria scelta: `sh.calvin.reorderable:reorderable:2.4.3`
Compatibile con Compose BOM 2024.12.01 (Compose UI 1.7.6). È la libreria più mantenuta per drag & drop in LazyColumn su Compose. Alternativa `org.burnoutcrew.composereorderable` è meno aggiornata.

```toml
# libs.versions.toml
reorderable = "2.4.3"
reorderable-lib = { group = "sh.calvin.reorderable", name = "reorderable", version.ref = "reorderable" }
```

Pattern di utilizzo in `RepartiScreen`:
```kotlin
val reorderState = rememberReorderableLazyListState(onMove = { from, to ->
    viewModel.moveItem(from.index, to.index)
})
LazyColumn(state = reorderState.listState) {
    items(departments, key = { it.id }) { dept ->
        ReorderableItem(reorderState, key = dept.id) { isDragging ->
            // row content + DragHandle
        }
    }
}
```

### Raggruppamento lista spesa (da `ShoppingScreen.kt:329-344`)
La `LazyColumn` esistente itera su `items` con key = `itemId`. Va refactored per iterare su gruppi:

```kotlin
// Struttura dati di output dal ViewModel:
data class ShoppingGroup(
    val departmentId: String?,
    val departmentName: String?,   // null → "Senza reparto"
    val items: List<ListItemWithDetails>
)
```

Uso di `stickyHeader {}` della `LazyColumn` per intestazione reparto.

---

## D. Test da creare o aggiornare

> Nessun test automatico è configurato nel progetto. I test sono esclusivamente manuali.

**Checklist test manuale:**
- [ ] Creare reparto, verifica compare in lista
- [ ] Rinominare reparto, verifica aggiornamento
- [ ] Eliminare reparto con prodotti associati → prodotti passano a "Senza reparto"
- [ ] Riordinare reparti drag & drop → ordine persistito dopo riavvio app
- [ ] Associare prodotto a reparto → compare nel gruppo corretto in lista spesa
- [ ] Dissociare prodotto → va in "Senza reparto"
- [ ] Sync MQTT: aggiungere reparto su device A → compare su device B
- [ ] Sync MQTT: riordinamento device A → ordine recepito su device B (LWW su `updatedAt`)
- [ ] Eliminazione reparto device A → rimosso su device B, prodotti tornano senza reparto

---

## E. Rischi tecnici aggiornati

| # | Rischio | Evidenza da codice | Impatto | Mitigazione |
|---|---|---|---|---|
| 1 | **Migration esplicita** — `AppModule.kt:26` usa `fallbackToDestructiveMigration()` che va rimosso | Confermato | Alto | Sostituire con `addMigrations(MIGRATION_2_3)`. La migration aggiunge tabella + colonna nullable, nessun dato perso |
| 2 | **Drag & drop libreria** — nessuna dep per reordering in `libs.versions.toml` | Confermato (riga 1-42, nessun dep DnD) | Medio | Aggiungere `sh.calvin.reorderable:2.4.3`; compatibile con Compose BOM 2024.12.01 |
| 3 | **FK deferred su SQLite Android** — `ALTER TABLE` non supporta FK in SQLite; la FK va nella migration via DDL raw | Confermato | Medio | Nella migration SQL usare la sintassi `REFERENCES departments(id) ON DELETE SET NULL` nell'`ALTER TABLE`, oppure dichiarare la FK solo nell'annotation `@Entity` e gestirla a livello Kotlin (Room rispetta le FK solo a livello di annotation in scrittura) |
| 4 | **`publishAll` in MqttSyncSource.kt:181-191** — non include reparti oggi | Confermato | Basso | Aggiungere `db.departmentDao().getAll().forEach { publishEntity(KIND_DEPARTMENTS, it.id, it) }` |
| 5 | **Sync ordinamento reparti multi-device** — `position` è LWW per singola entity; due device riordinano → convergenza può richiedere più round-trip | Accettabile | Basso | Stesso comportamento già accettato per `FavoriteEntity.ordering`; documentato come known limitation MVP |

---

## F. Prerequisiti e task bloccanti

Nessun refactoring bloccante. L'ordine di implementazione consigliato (ogni step compila in autonomia):

1. **`DepartmentEntity` + `DepartmentDao`** — nessuna dipendenza
2. **Migration + `AppDatabase` v3** — dipende da (1)
3. **`AppModule` update** — dipende da (2)
4. **`SyncSource` + `MqttSyncSource` update** — dipende da (1)
5. **`ProductEntity` update** (`departmentId`) — dipende da (1); contestuale alla migration
6. **`SpesaRepository` update** — dipende da (1)(4)
7. **`RepartiViewModel` + `RepartiActivity`** — dipende da (6); aggiungere dep `reorderable`
8. **`ShoppingViewModel` update** (gruppi) — dipende da (6)
9. **`ShoppingScreen` update** (raggruppamento + link a `RepartiActivity`) — dipende da (7)(8)
10. **`AndroidManifest.xml`** — dipende da (7)
