# ADB Tips — App Spesa

## Inserire un articolo nella lista

**Risoluzione dispositivo:** 1220x2712

### Coordinate UI (pixel device)
- Campo "Aggiungi prodotto..." — bounds `[45,371][1019,527]`, center `(532, 449)`
- Bottone "+" — center `(1119, 449)` (non rilevato da `get_uilayout`, usare coordinate dirette)

### Procedura
```bash
# 1. Focus sul campo testo
input tap 532 449

# 2. Digita il testo — usa %s al posto degli spazi
input text "nome%sprodotto"

# 3. Tap sul bottone "+"
input tap 1119 449
```

### Note
- `input text` non accetta spazi letterali né `\ `: usare `%s` per ogni spazio.
- Per svuotare il campo: `for i in $(seq 1 30); do input keyevent KEYCODE_DEL; done`
- `KEYCODE_CTRL_A` + `KEYCODE_DEL` non seleziona tutto in modo affidabile su questo dispositivo.
