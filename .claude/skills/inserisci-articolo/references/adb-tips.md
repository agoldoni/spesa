# ADB Tips — App Spesa

## Inserire un articolo nella lista

### Strategia: elementi per testTag (preferita)

Usa `get_uilayout` per trovare gli elementi nell'albero UI. I componenti principali espongono i seguenti testTag come attributi semantici:

| testTag | Elemento |
|---|---|
| `input_nome_prodotto` | Campo testo "Aggiungi prodotto…" |
| `btn_aggiungi_prodotto` | Pulsante "+" |
| `chip_preferito_<Nome>` | Chip preferito (es. `chip_preferito_Latte`) |
| `item_<itemId>` | Riga prodotto nella lista |
| `btn_rimuovi_<itemId>` | Cestino |
| `<itemId>_stepper_plus` | Pulsante + dello stepper |
| `<itemId>_stepper_minus` | Pulsante − dello stepper |

### Procedura

```
1. Chiama get_uilayout per ottenere l'albero UI corrente.
2. Cerca il nodo con testTag "input_nome_prodotto" — estrai il campo "bounds"
   es. bounds="[45,371][1019,527]" → center = ((45+1019)/2, (371+527)/2) = (532, 449)
3. Tap sul centro calcolato:
     input tap <cx> <cy>
4. Digita il testo — usa %s al posto degli spazi:
     input text "nome%sprodotto"
5. Cerca il nodo con testTag "btn_aggiungi_prodotto" — estrai bounds e calcola center.
6. Tap sul centro:
     input tap <cx> <cy>
7. Chiama get_screenshot per verificare che l'articolo sia apparso nella lista.
```

### Note
- `input text` non accetta spazi letterali né `\ `: usare `%s` per ogni spazio.
- Se un elemento non è rilevato da `get_uilayout`, usa le coordinate di fallback (vedi sotto).
- Per svuotare il campo: `for i in $(seq 1 30); do input keyevent KEYCODE_DEL; done`

### Coordinate di fallback (risoluzione 1220×2712)

Se `get_uilayout` non restituisce i nodi cercati:
- Campo testo: `input tap 532 449`
- Pulsante "+": `input tap 1119 449`
