# Spesa

App Android per la lista della spesa condivisa tra più membri della famiglia.

## Funzionalità

- Singola schermata con lista prodotti, preferiti e barra di aggiunta
- Controllo quantità inline (− / valore / +) per ogni voce, minimo 1
- Aggiunta rapida tramite chip dei preferiti o autocomplete sullo storico
- Aggiungere un prodotto già presente incrementa la quantità invece di duplicarlo
- Avatar colorato che indica il membro della famiglia che ha aggiunto la voce
- Contatori live: numero di prodotti e somma dei pezzi totali
- Sincronizzazione opzionale tra dispositivi via MQTT (broker configurabile a runtime)

## Tech Stack

- **Kotlin** 2.0 + **KSP** per la code generation
- **Jetpack Compose** + Material 3
- **Architettura** MVVM + Repository pattern
- **Room** per la persistenza locale (SQLite, schema v2)
- **Hilt** per la dependency injection
- **Coroutines + Flow** per la reattività
- **HiveMQ Mqtt3 Client** (opzionale) per la sincronizzazione multi-device
- Android API 26+ (minSdk 26, targetSdk 34, compileSdk 35), Java 17

## Build

Requisiti: Android SDK, Java 17.

### Tramite `build.sh` (consigliato)

```bash
./build.sh                       # Build debug (default)
./build.sh debug                 # Build debug APK
./build.sh release               # Build release APK (richiede variabili d'ambiente)
./build.sh clean                 # Pulisce gli artefatti
```

Per la build di release, esportare prima le variabili d'ambiente:

```bash
export KEYSTORE_FILE=~/.android/release-key.jks   # default, omettibile
export KEYSTORE_PASSWORD=<password>
export KEY_ALIAS=release                           # default, omettibile
export KEY_PASSWORD=<password>                     # default: KEYSTORE_PASSWORD, omettibile
./build.sh release
```

L'APK viene emesso in `app/build/outputs/apk/release/spesa.apk`.

### Tramite Gradle direttamente

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (richiede variabili d'ambiente)
./gradlew clean                  # Pulisce gli artefatti
```

## Installazione

```bash
./install-all.sh           # Installa l'APK debug su tutti i dispositivi connessi
./install-all.sh --build   # Build + installazione
```

## Configurazione Release

| Variabile | Descrizione | Default |
|---|---|---|
| `KEYSTORE_FILE` | Percorso del keystore | `~/.android/release-key.jks` |
| `KEYSTORE_PASSWORD` | Password del keystore | — |
| `KEY_ALIAS` | Alias della chiave | `release` |
| `KEY_PASSWORD` | Password della chiave | `KEYSTORE_PASSWORD` |

## Emulatore e dispositivi

```bash
# Elenca i dispositivi/emulatori connessi
adb devices -l

# Elenca gli AVD (Android Virtual Device) configurati
emulator -list-avds

# Avvia un emulatore per nome
emulator -avd <nome_avd>

# Avvia un emulatore in background, senza audio e con wipe dei dati
emulator -avd <nome_avd> -no-audio -wipe-data &

# Termina tutti gli emulatori in esecuzione
adb emu kill

# Disinstalla l'app debug da un dispositivo specifico
adb -s <device_id> uninstall it.agoldoni.spesa.debug
```

### Clipboard nell'emulatore

```bash
# Inietta testo nel campo focalizzato (occhio agli spazi: %s)
adb shell input text "broker.example.com"

# Imposta direttamente la clipboard dell'emulatore (poi long-press → Incolla)
adb shell cmd clipboard set-text "username-segreto"
```

In alternativa, dalla finestra dell'emulatore di Android Studio è attivo di default lo *clipboard sharing* tra host e VM (Ctrl+C / Ctrl+V). Se non funziona: `…` (Extended controls) → **Settings** → **Enable clipboard sharing**.

## Sincronizzazione MQTT (opzionale)

L'app funziona out-of-the-box in modalità solo-locale. Per attivare la sincronizzazione real-time tra dispositivi, tocca l'icona ingranaggio nell'header e compila i campi:

| Campo | Descrizione |
|---|---|
| Broker host | Hostname o IP del broker MQTT |
| Porta | Default `8883` |
| Group ID | Identificatore condiviso tra i dispositivi da sincronizzare |
| TLS | Abilita la connessione cifrata |
| Username / Password | Opzionali, dipende dalla configurazione del broker |

Attiva il toggle **Sincronizzazione attiva** e salva. Dopo la connessione, il client:
1. Si iscrive ai topic `sync/{groupId}/{members,products,list_items,favorites}/#`
2. Ripubblica lo stato locale completo (retained) per far convergere i peer già connessi
3. Applica la conflict resolution **last-write-wins** tramite il campo `updatedAt`

Tutti i dispositivi che condividono lo stesso `groupId` e puntano allo stesso broker si sincronizzano automaticamente in tempo reale.

## Struttura del progetto

```
app/src/main/java/it/agoldoni/spesa/
├── data/   # Room: entità, DAO, database, SpesaRepository
├── sync/   # SyncSource interface + MqttSyncSource (HiveMQ)
├── di/     # Moduli Hilt
└── ui/     # ShoppingScreen, ShoppingViewModel, MqttConfigActivity, theme
```

## Licenza

Copyright (c) Alberto Goldoni
