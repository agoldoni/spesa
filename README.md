# Spesa

App Android per la lista della spesa condivisa tra più membri della famiglia.

## Funzionalità

- Singola schermata con lista prodotti, preferiti e barra di aggiunta
- Controllo quantità inline (− / valore / +) per ogni voce, minimo 1
- Aggiunta rapida tramite chip dei preferiti o autocomplete sullo storico
- Aggiungere un prodotto già presente incrementa la quantità invece di duplicarlo
- Avatar colorato che indica il membro della famiglia che ha aggiunto la voce
- Contatori live: numero di prodotti e somma dei pezzi totali
- Sincronizzazione opzionale tra dispositivi via Firebase Realtime Database

## Tech Stack

- **Kotlin** 2.0.21
- **Jetpack Compose** + Material 3
- **Architettura** MVVM + Repository pattern
- **Room** per la persistenza locale (SQLite)
- **Hilt** per la dependency injection
- **Coroutines + Flow** per la reattività
- **Firebase Realtime Database** (opzionale) per la sincronizzazione multi-device
- Android API 26+ (minSdk 26, targetSdk 34, compileSdk 35)

## Build

Requisiti: Android SDK, Java 17.

```bash
./build.sh debug      # Build debug
./build.sh release    # Build release (richiede keystore configurato)
./build.sh clean
```

Oppure direttamente con Gradle:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Installazione

```bash
./install-all.sh           # Installa l'APK debug su tutti i dispositivi connessi
./install-all.sh --build   # Build + installazione
./install-all.sh --run     # Installa e avvia l'app
```

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

## Sincronizzazione Firebase (opzionale)

L'app funziona out-of-the-box in modalità solo-locale. Per attivare la
sincronizzazione real-time tra dispositivi:

1. Crea un progetto Firebase su https://console.firebase.google.com
2. Aggiungi un'app Android con package `it.agoldoni.spesa` (o `it.agoldoni.spesa.debug` per il build debug)
3. Abilita Realtime Database nelle regole `read: true, write: true` (o regole più stringenti se autenticato)
4. Scarica `google-services.json` e copialo in `app/google-services.json`
5. Ricompila

Quando `app/google-services.json` esiste, il plugin Google Services viene applicato
automaticamente e `BuildConfig.FIREBASE_ENABLED` diventa `true`.
Il file è in `.gitignore` e non viene mai committato.

## Configurazione Release

| Variabile | Descrizione | Default |
|---|---|---|
| `KEYSTORE_FILE` | Percorso del keystore | `~/.android/release-key.jks` |
| `KEYSTORE_PASSWORD` | Password del keystore | — |
| `KEY_ALIAS` | Alias della chiave | `release` |
| `KEY_PASSWORD` | Password della chiave | — |

## Struttura del progetto

```
app/src/main/java/it/agoldoni/spesa/
├── data/        # Room: entità, DAO, database, repository
├── sync/        # Astrazione SyncSource (LocalOnly + Firebase RTDB)
├── di/          # Moduli Hilt
└── ui/          # Schermata Compose, ViewModel, theme
```

## Licenza

Copyright (c) Alberto Goldoni
