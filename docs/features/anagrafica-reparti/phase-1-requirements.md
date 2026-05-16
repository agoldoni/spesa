# Feature: Anagrafica Reparti

**Slug:** `anagrafica-reparti`
**Data:** 2026-05-16
**Stato:** Bozza — in attesa di approvazione

---

## 1. Obiettivo e motivazione

Permettere all'utente di organizzare i prodotti della lista della spesa in **reparti** (es. Frutta e Verdura, Latticini, Macelleria, Pane, Surgelati…), con un ordinamento personalizzabile che rispecchi il percorso fisico nel supermercato. Ogni prodotto può essere opzionalmente associato a un reparto.

**Problema risolto:** oggi i prodotti compaiono in ordine di aggiunta; non c'è alcun raggruppamento logico. L'utente deve mentalmente riordinare la lista mentre fa la spesa, perdendo tempo e rischiando di dimenticare articoli.

---

## 2. Scope

### Incluso

- CRUD completo per i reparti (crea, rinomina, elimina).
- Riordinamento manuale dei reparti tramite drag & drop (l'utente definisce l'ordine, tipicamente il percorso nel supermercato).
- Associazione opzionale prodotto → reparto (un prodotto può non avere reparto).
- Visualizzazione della lista della spesa raggruppata per reparto, nell'ordine definito. I prodotti senza reparto compaiono in una sezione "Senza reparto" alla fine.
- Sincronizzazione dei reparti via MQTT (stessa logica last-write-wins già usata per gli altri entity).
- Persistenza dell'ordinamento dei reparti (colonna `position` su Room).

### Escluso (out of scope)

- Colori o icone per i reparti (MVP testuale).
- Assegnazione del reparto direttamente dalla lista della spesa inline (si fa tramite la scheda prodotto o l'anagrafica prodotti).
- Raggruppamento automatico basato su ML o categorie predefinite.
- Più liste con reparti diversi (il set di reparti è globale/condiviso nel gruppo).
- Statistiche per reparto.

---

## 3. User Stories

1. **Come utente** voglio creare un reparto con un nome (es. "Frutta e Verdura") per poter organizzare i prodotti per area del supermercato.

2. **Come utente** voglio riordinare i reparti tramite drag & drop per rispecchiare l'ordine fisico del mio supermercato e velocizzare la spesa.

3. **Come utente** voglio associare un prodotto a un reparto (o lasciarlo senza) quando lo aggiungo o lo modifico nell'anagrafica, per fare in modo che compaia nel gruppo corretto nella lista.

4. **Come utente** voglio vedere la lista della spesa raggruppata per reparto nell'ordine che ho scelto, così evito di girare avanti e indietro nel supermercato.

5. **Come utente** voglio eliminare un reparto; i prodotti associati tornano a "Senza reparto", senza perdere dati.

6. **Come utente** voglio che i reparti si sincronizzino automaticamente tra i dispositivi del gruppo (stesso comportamento degli altri dati).

---

## 4. Criteri di accettazione

### US1 — Creazione reparto
- [ ] L'utente può aprire una schermata/dialog "Gestione Reparti" dall'UI principale.
- [ ] Può inserire un nome e salvare; il reparto appare in fondo alla lista.
- [ ] Il nome è obbligatorio e non può essere vuoto.
- [ ] Nomi duplicati (case-insensitive) sono rifiutati con un messaggio di errore.

### US2 — Riordinamento
- [ ] L'elenco reparti mostra un'icona drag handle su ogni riga.
- [ ] Trascinando una riga l'utente può cambiare la posizione.
- [ ] L'ordine è persistito localmente (colonna `position`) e sincronizzato via MQTT.
- [ ] Il riordinamento non causa perdita di associazioni prodotto-reparto.

### US3 — Associazione prodotto-reparto
- [ ] Nel dialog di aggiunta/modifica prodotto è presente un campo opzionale "Reparto" (dropdown/chip selector).
- [ ] "Nessun reparto" è sempre un'opzione valida.
- [ ] L'associazione è salvata in `ProductEntity` (FK verso `DepartmentEntity`).

### US4 — Lista raggruppata e ordinata
- [ ] Nella schermata principale (`ShoppingScreen`) i prodotti in lista sono raggruppati per reparto.
- [ ] L'ordine dei gruppi corrisponde all'ordine `position` definito dall'utente nella schermata Reparti.
- [ ] All'interno di ogni gruppo gli articoli sono ordinati **alfabeticamente per nome prodotto** (A→Z, case-insensitive).
- [ ] I prodotti senza reparto compaiono in una sezione finale "Senza reparto", anch'essa ordinata alfabeticamente.
- [ ] Ogni gruppo mostra il nome del reparto come intestazione sticky.

### US5 — Eliminazione reparto
- [ ] Prima dell'eliminazione compare un dialog di conferma.
- [ ] Dopo l'eliminazione, i prodotti già associati a quel reparto passano a `departmentId = null`.
- [ ] Il topic MQTT del reparto eliminato viene svuotato (payload vuoto = delete signal).

### US6 — Sincronizzazione
- [ ] `DepartmentEntity` segue lo stesso pattern MQTT delle entità esistenti.
- [ ] Topic: `sync/{groupId}/departments/{id}`.
- [ ] Conflitti risolti last-write-wins via `updatedAt`.

---

## 5. Rischi e dipendenze

| # | Rischio | Probabilità | Impatto | Mitigazione |
|---|---------|-------------|---------|-------------|
| 1 | Drag & drop in Compose non è nativo (serve libreria o implementazione custom) | Alta | Medio | Usare `reorderable` (Burnett's Compose Reorderable) o `foundation` `detectReorderAfterLongPress` — valutare in Fase 2 |
| 2 | Aggiunta colonna `departmentId` a `ProductEntity` richiede migrazione Room | Alta | Alto | Bump schema version + migrazione con default NULL; `fallbackToDestructiveMigration` è attivo ma si preferisce una migration esplicita per non perdere dati utente |
| 3 | Il raggruppamento nella lista modifica il layout corrente di `ShoppingScreen` | Media | Medio | Refactoring del `LazyColumn` per supportare `stickyHeader` |
| 4 | Ordinamento dei reparti in sync multi-device: due device riordinano contemporaneamente | Bassa | Basso | Last-write-wins su `updatedAt` — accettabile per MVP |

---

## 6. Stima effort

| Area | Giorni/uomo |
|------|-------------|
| Dati (Room entity, DAO, migrazione, Repository) | 0.5 |
| Sync MQTT (DepartmentEntity) | 0.5 |
| UI — Gestione Reparti (schermata CRUD + drag & drop) | 1.5 |
| UI — Associazione prodotto-reparto (dropdown nel dialog) | 0.5 |
| UI — Lista raggruppata per reparto in ShoppingScreen | 1.0 |
| DI (Hilt) + ViewModel update | 0.5 |
| **Totale** | **4.5** |

> Nessun test automatico è configurato nel progetto (come da CLAUDE.md), pertanto il test è manuale.

---

## 7. Milestones

1. **M1 — Data layer** — `DepartmentEntity` (id, name, position, updatedAt) + `DepartmentDao` + migrazione Room v2→v3 + `SpesaRepository` aggiornato + colonna `departmentId` nullable su `ProductEntity`.
2. **M2 — Sync** — Topic MQTT per `departments` in `MqttSyncSource`; publish/subscribe/delete per `DepartmentEntity`.
3. **M3 — Schermata Gestione Reparti** — Nuovo `RepartiScreen` (o Activity) con lista riordinabile drag & drop, dialog crea/rinomina, conferma eliminazione.
4. **M4 — Associazione prodotto-reparto** — Campo reparto nel dialog prodotto (Aggiungi / Modifica).
5. **M5 — Lista raggruppata** — `ShoppingScreen` refactoring: `stickyHeader` per reparto, sezione "Senza reparto" in fondo; accesso a `RepartiScreen` dall'header (icona).
6. **M6 — QA manuale** — Test su singolo device + test sync multi-device.
