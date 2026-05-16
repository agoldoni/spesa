# Anagrafica Reparti — Implementation Plan

**Stato:** Bozza — in attesa di approvazione
**Autore:** Alberto Goldoni
**Data:** 2026-05-16
**Versione:** 1.0

---

## 1. Executive Summary

Viene introdotta l'anagrafica **Reparti** nell'app Android *Spesa*: l'utente crea e riordina (drag & drop) una lista di reparti che rispecchia il percorso fisico nel supermercato. Ogni prodotto può essere opzionalmente assegnato a un reparto. La lista della spesa viene quindi mostrata raggruppata per reparto (nell'ordine scelto), con gli articoli di ciascun gruppo ordinati alfabeticamente e i prodotti senza reparto in coda. I reparti si sincronizzano tra i dispositivi del gruppo via MQTT con la stessa logica last-write-wins già in uso.

La feature richiede una migrazione Room v2→v3 (esplicita, nessun dato perso), l'aggiunta di una dipendenza Compose per il drag & drop e la creazione di una nuova Activity `RepartiActivity`. Stima: **4.5 giorni/uomo**.

---

## 2. Obiettivo e motivazione

- **Problema che risolve:** i prodotti nella lista compaiono in ordine di inserimento senza alcun raggruppamento logico; l'utente deve mentalmente riordinare la lista durante la spesa, con il rischio di dover tornare indietro tra corsie.
- **Metriche di successo:**
  - [ ] L'utente può creare almeno un reparto e assegnare prodotti senza frizione (nessun passo obbligatorio aggiuntivo per aggiungere un prodotto alla lista).
  - [ ] La lista della spesa mostra intestazioni di reparto e ordinamento alfabetico intra-reparto.
  - [ ] La sincronizzazione MQTT propaga i reparti tra due device entro il normale ciclo di heartbeat MQTT.
- **Legame con obiettivi di prodotto:** migliorare l'usabilità della lista della spesa condivisa, riducendo il tempo speso a fare la spesa.

---

## 3. Scope

### Incluso

- `DepartmentEntity` (id, name, position, updatedAt) con CRUD completo.
- Riordinamento manuale dei reparti via drag & drop (`RepartiActivity`).
- Associazione opzionale prodotto → reparto (campo `departmentId: String?` su `ProductEntity`).
- Assegnazione del reparto dal campo di aggiunta articolo (dropdown inline nell'`AddBar`) oppure tramite selezione dalla schermata Reparti.
- Lista della spesa raggruppata: gruppi ordinati per `position` reparto; articoli all'interno del gruppo ordinati **alfabeticamente (A→Z, case-insensitive)**; sezione "Senza reparto" sempre in coda, anch'essa alfabetica.
- Intestazione sticky per ogni gruppo nella `LazyColumn`.
- Sincronizzazione MQTT dei reparti (topic `sync/{groupId}/departments/{id}`).
- Migrazione Room esplicita v2→v3 (nessuna perdita di dati).

### Escluso (out of scope)

- Colori o icone per i reparti — MVP testuale; riduce complessità UI.
- Assegnazione del reparto direttamente dalla riga dell'articolo nella lista — il reparto è proprietà del prodotto anagrafico, non dell'item di lista.
- Raggruppamento automatico basato su ML o categorie predefinite — fuori perimetro MVP.
- Reparti per-lista (i reparti sono globali al gruppo) — semplifica la sync.

### Decisioni aperte

Nessuna decisione bloccante rimasta aperta.

---

## 4. User Stories e criteri di accettazione

### US-001 · Gestione anagrafica reparti
**Priorità:** Must Have

Come utente voglio creare, rinominare ed eliminare reparti per organizzare i prodotti per area del supermercato.

**Criteri di accettazione:**
- [ ] Icona "Reparti" nell'header di `ShoppingScreen` apre `RepartiActivity`.
- [ ] L'utente può inserire un nome e salvare; il reparto appare in fondo alla lista con `position = size`.
- [ ] Nome obbligatorio; nomi duplicati (case-insensitive) rifiutati con messaggio di errore.
- [ ] Rinomina inline (long-press o icona matita) aggiorna `name` e `updatedAt`.
- [ ] Eliminazione con dialog di conferma; i prodotti associati tornano a `departmentId = null` tramite FK `ON DELETE SET NULL`.

### US-002 · Riordinamento reparti drag & drop
**Priorità:** Must Have

Come utente voglio riordinare i reparti tramite drag & drop per rispecchiare l'ordine fisico del mio supermercato.

**Criteri di accettazione:**
- [ ] Ogni riga in `RepartiActivity` mostra un drag handle (icona `DragHandle`).
- [ ] Long-press o press sull'handle avvia il drag; rilascio aggiorna la colonna `position`.
- [ ] Il nuovo ordine è persistito localmente e sincronizzato via MQTT (ogni entity aggiornata con nuovo `position` e `updatedAt`).
- [ ] Riordinamento non altera le associazioni prodotto-reparto.

### US-003 · Associazione prodotto-reparto
**Priorità:** Must Have

Come utente voglio associare un prodotto a un reparto (o lasciarlo senza) in modo che compaia nel gruppo corretto.

**Criteri di accettazione:**
- [ ] Il campo "Reparto" nell'`AddBar` (o in un dialog di dettaglio prodotto) mostra un dropdown con la lista dei reparti + voce "Nessun reparto".
- [ ] La selezione viene salvata in `ProductEntity.departmentId`.
- [ ] Il campo è opzionale; omettendolo il prodotto finisce in "Senza reparto".
- [ ] La modifica del reparto aggiorna `updatedAt` e viene sincronizzata via MQTT.

### US-004 · Lista spesa raggruppata e ordinata
**Priorità:** Must Have

Come utente voglio vedere la lista della spesa raggruppata per reparto nell'ordine che ho scelto, con gli articoli di ogni gruppo in ordine alfabetico, così risparmio tempo nel percorrere il supermercato.

**Criteri di accettazione:**
- [ ] I gruppi appaiono nell'ordine `position` definito nella schermata Reparti.
- [ ] All'interno di ogni gruppo gli articoli sono in ordine **alfabetico A→Z** (case-insensitive su `productName`).
- [ ] I prodotti senza reparto compaiono in un gruppo finale "Senza reparto", anch'esso ordinato alfabeticamente.
- [ ] Ogni gruppo ha un'intestazione sticky con il nome del reparto.
- [ ] Se non esistono reparti o tutti i prodotti sono senza reparto, la lista si comporta come oggi (nessun header).

### US-005 · Eliminazione reparto
**Priorità:** Must Have

Come utente voglio eliminare un reparto senza perdere i prodotti associati.

**Criteri di accettazione:**
- [ ] Dialog di conferma prima dell'eliminazione.
- [ ] Dopo l'eliminazione i prodotti associati hanno `departmentId = null` (FK `ON DELETE SET NULL`).
- [ ] Il topic MQTT del reparto eliminato viene svuotato (payload vuoto = delete signal).

### US-006 · Sincronizzazione MQTT reparti
**Priorità:** Must Have

Come utente voglio che i reparti si sincronizzino automaticamente tra i dispositivi del gruppo.

**Criteri di accettazione:**
- [ ] Topic: `sync/{groupId}/departments/{id}`, retained=true.
- [ ] Conflitti risolti last-write-wins via `updatedAt`.
- [ ] Al connect, `publishAll()` include anche i reparti locali.
- [ ] Delete signal: payload vuoto sul topic retained.

---

## 5. Architettura tecnica

### Flusso dati

```
RepartiActivity (Compose)
    └─ RepartiViewModel
          └─ SpesaRepository
                ├─ DepartmentDao  ──→  Room DB (departments table)
                └─ SyncSource.pushDepartment / deleteDepartment
                      └─ MqttSyncSource ──→ MQTT broker
                                               └─ altri device
                                                     └─ handleDepartment → DepartmentDao.upsert

ShoppingScreen (Compose)
    └─ ShoppingViewModel
          └─ SpesaRepository.observeDepartments() + observeListItems()
                └─ groupAndSortItems() → List<ShoppingGroup>
                      [reparto1(α), reparto2(α), ..., Senza reparto(α)]
```

### Modifiche al data model

| Tabella | Tipo modifica | Dettaglio |
|---|---|---|
| `departments` | Nuova | `id TEXT PK, name TEXT, position INT, updatedAt INT` |
| `products` | Modifica | Aggiunta colonna `departmentId TEXT DEFAULT NULL REFERENCES departments(id) ON DELETE SET NULL` + indice |
| Room schema | Modifica | Version 2 → 3, migration esplicita `MIGRATION_2_3` |

### Struttura dati di raggruppamento (Kotlin)

```kotlin
data class ShoppingGroup(
    val departmentId: String?,        // null = "Senza reparto"
    val departmentName: String?,      // null → mostra "Senza reparto"
    val items: List<ListItemWithDetails>   // già ordinati A→Z per productName
)
```

Logica di costruzione (nel ViewModel o nel Repository):
```
departments (ordinati per position)
  → per ogni dept: items con productId.departmentId == dept.id, sorted by productName ASC
  → in coda: items con departmentId == null, sorted by productName ASC
```

### Breaking changes

| Componente | Tipo | Piano di migrazione |
|---|---|---|
| `ProductEntity` | Aggiunta campo nullable | Migration Room aggiunge `ALTER TABLE products ADD COLUMN departmentId TEXT DEFAULT NULL`; tutti i record esistenti avranno `null` — nessun dato perso |
| `SyncSource` interface | Aggiunta metodi | `MqttSyncSource` (unica implementazione) aggiornata contestualmente |
| `AppModule` | Rimozione `fallbackToDestructiveMigration` | Sostituito con `addMigrations(MIGRATION_2_3)`; migration esplicita preserva i dati |

---

## 6. Piano di implementazione

| ID | Task | Area | Stima (gg) | Dipende da |
|---|---|---|---|---|
| T-01 | `DepartmentEntity.kt` — nuova entity con id/name/position/updatedAt | Data | 0.1 | — |
| T-02 | `DepartmentDao.kt` — observeAll, getAll, getById, upsert, deleteById, reorder (transaction) | Data | 0.2 | T-01 |
| T-03 | `AppDatabase.kt` — version 3, `MIGRATION_2_3` (CREATE departments + ALTER products), aggiungi `departmentDao()` | Data | 0.2 | T-01, T-02 |
| T-04 | `ProductEntity.kt` — aggiunta `departmentId: String? = null`, FK annotation, indice | Data | 0.1 | T-01 |
| T-05 | `AppModule.kt` — rimozione `fallbackToDestructiveMigration`, `addMigrations(MIGRATION_2_3)`, `provideDepartmentDao` | DI | 0.1 | T-03 |
| T-06 | `SyncSource.kt` — aggiunta `pushDepartment`, `deleteDepartment` | Sync | 0.1 | T-01 |
| T-07 | `MqttSyncSource.kt` — KIND_DEPARTMENTS, subscribe, handleDepartment (LWW), delete handler, publishAll aggiornato | Sync | 0.3 | T-06 |
| T-08 | `SpesaRepository.kt` — `observeDepartments`, `addDepartment`, `renameDepartment`, `deleteDepartment`, `reorderDepartments`, `setProductDepartment` | Data | 0.4 | T-02, T-07 |
| T-09 | `libs.versions.toml` + `build.gradle.kts` — aggiunta `sh.calvin.reorderable:reorderable:2.4.3` | Build | 0.1 | — |
| T-10 | `RepartiViewModel.kt` — StateFlow departments, addDepartment, rename, delete, reorder (move locale + debounced persist) | UI | 0.3 | T-08 |
| T-11 | `RepartiActivity.kt` — Activity Compose con LazyColumn drag & drop, dialog crea/rinomina, dialog conferma elimina; link da ShoppingScreen header | UI | 0.8 | T-09, T-10 |
| T-12 | `AndroidManifest.xml` — registrazione `RepartiActivity` | Build | 0.05 | T-11 |
| T-13 | `ShoppingViewModel.kt` — `observeDepartments()`, funzione `buildShoppingGroups()` che produce `List<ShoppingGroup>` (raggruppamento + sort alfabetico) | UI | 0.3 | T-08 |
| T-14 | `ShoppingScreen.kt` — `ItemsList` refactoring con `stickyHeader` per reparto, sezione "Senza reparto" in coda; secondo IconButton header per `RepartiActivity` | UI | 0.5 | T-13 |
| T-15 | QA manuale (checklist Fase 2 sezione D) | Test | 0.5 | T-12, T-14 |

**Stima totale:** 4.35 giorni/uomo  
**Breakdown:** Data/Sync 1.5gg · DI 0.1gg · UI 1.95gg · Build 0.15gg · Test 0.5gg · Doc 0.1gg

---

## 7. Piano di test

**Strategia:** test esclusivamente manuali (nessun test automatico nel progetto).

### Test cases critici

| ID | Tipo | Descrizione | Priorità |
|---|---|---|---|
| TC-01 | Funzionale | Creare reparto → appare nella lista con position = ultima | Alta |
| TC-02 | Funzionale | Drag & drop due reparti → ordine aggiornato dopo riavvio app | Alta |
| TC-03 | Funzionale | Eliminare reparto con prodotti associati → prodotti in "Senza reparto" | Alta |
| TC-04 | Funzionale | Aggiungere prodotto con reparto → appare nel gruppo corretto nella lista | Alta |
| TC-05 | Funzionale | Lista spesa: più reparti → ordine rispetta `position`; articoli A→Z dentro ogni gruppo | Alta |
| TC-06 | Funzionale | Lista spesa: prodotti misti (con e senza reparto) → "Senza reparto" sempre in coda | Alta |
| TC-07 | Sync | Creare reparto su device A → compare su device B entro pochi secondi | Alta |
| TC-08 | Sync | Riordinare reparti su device A → ordine aggiornato su device B (LWW) | Media |
| TC-09 | Sync | Eliminare reparto su device A → sparisce su device B; prodotti tornano senza reparto | Alta |
| TC-10 | Edge case | Nessun reparto creato → lista si comporta come prima (nessun header) | Media |
| TC-11 | Edge case | Nome reparto duplicato → messaggio di errore, nessun inserimento | Media |
| TC-12 | Migration | Aggiornare app con dati esistenti (v2→v3) → tutti i dati presenti, nessun crash | Alta |

### Definition of Done per QA

- [ ] Tutti i test case TC-01..TC-12 superati su almeno un device fisico
- [ ] Nessun crash in `adb logcat` durante i test case
- [ ] Test sync TC-07..TC-09 eseguiti con due device fisici sullo stesso broker
- [ ] Migration TC-12 testata installando l'APK sopra una versione con DB v2

---

## 8. Rischi e mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Migration v2→v3 non testata su device con DB esistente causa crash | Media | Alto | Testare TC-12 su device con versione precedente installata prima del rilascio |
| Libreria `sh.calvin.reorderable` introduce conflitti con Compose BOM 2024.12.01 | Bassa | Medio | Verificare compile a T-09; fallback: implementare drag & drop manuale con `detectDragGesturesAfterLongPress` (nessuna dep esterna) |
| Sync ordinamento multi-device: due device riordinano in contemporanea | Bassa | Basso | LWW per entity — same behavior di `FavoriteEntity.ordering`; documentato come known limitation |
| FK SQLite `ON DELETE SET NULL` non eseguita se Room non ha FK enforcement attivo | Media | Medio | Aggiungere `db.setForeignKeyConstraintsEnabled(true)` nel builder in `AppModule`, oppure gestire il SET_NULL a livello repository nel metodo `deleteDepartment` |

---

## 9. Rollout

**Strategia di rilascio:** deploy diretto (nessun feature flag — l'app è single-team, no staged rollout).

**Piano di rollback:**
1. Ripristinare la versione precedente dell'APK (nessuna distribuzione pubblica in corso).
2. Il DB v3 è backward-incompatible con il binario v2 (colonna extra + nuova tabella): se necessario, disinstallare e reinstallare l'app vecchia (i dati MQTT sono sul broker e vengono ricaricati al reconnect).

---

## 10. Checklist di approvazione

| Revisione | Responsabile | Stato | Data |
|---|---|---|---|
| Revisione tecnica | Alberto Goldoni | ⏳ In attesa | — |
| Stima accettata | Alberto Goldoni | ⏳ In attesa | — |
| Rischi accettati | Alberto Goldoni | ⏳ In attesa | — |
| Data di inizio confermata | Alberto Goldoni | ⏳ In attesa | — |

---

## Domande aperte

1. **FK enforcement in Room**: SQLite Android non attiva i vincoli di FK di default. Confermare se aggiungere `db.setForeignKeyConstraintsEnabled(true)` nel builder (opzione più robusta) oppure gestire il `SET_NULL` manualmente nel repository `deleteDepartment` (più esplicito, nessun side effect su altri vincoli). — *Risponde: Alberto Goldoni prima di T-03.*

2. **Dropdown reparto nell'AddBar**: il campo reparto durante l'aggiunta di un articolo richiede un'interazione extra. Confermare se mostrare sempre il dropdown reparto inline oppure nasconderlo dietro un'icona espandibile per non appesantire il flusso rapido di aggiunta. — *Risponde: Alberto Goldoni prima di T-11.*

---

*Documento generato con la skill `claude-code-feature`.*
