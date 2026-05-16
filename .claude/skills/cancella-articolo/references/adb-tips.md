# ADB Tips — Cancellare un articolo dall'app Spesa

## Struttura XML nel dump UIAutomator

Ogni prodotto nella lista genera questa struttura nell'albero XML:

```
<node text="nome prodotto" bounds="[x1,y1][x2,y2]" ... />          ← testo del prodotto
...
<node content-desc="Rimuovi" bounds="[x1,y1][x2,y2]" ... />        ← icona cestino
```

Il nodo con `content-desc="Rimuovi"` è l'icona interna al contenitore clickable.
Tappare il suo centro funziona perché è dentro l'area cliccabile del genitore.

## Come trovare il "Rimuovi" corretto

Ci sono tanti nodi `content-desc="Rimuovi"` quanti sono i prodotti in lista.
Per associare il cestino al prodotto giusto, usa la sovrapposizione del range y:

1. Dal testo prodotto estrai `y1` e `y2` (dal campo `bounds`).
2. Tra i nodi `content-desc="Rimuovi"`, scegli quello il cui `y1`–`y2` si sovrappone.

**Esempio reale — rimozione di "succo di pera":**

| Nodo | bounds | y_mid |
|---|---|---|
| Testo "succo di pera" | `[133,1541][400,1593]` | 1567 |
| Icona Rimuovi (corretta) | `[837,1559][904,1626]` | 1592 ✓ |
| Icona Rimuovi (altro prodotto) | `[837,1402][904,1469]` | 1435 ✗ |

Centro da tappare: `cx = (837+904)/2 = 870`, `cy = (1559+1626)/2 = 1592`
→ `input tap 870 1592`

## Risoluzione schermo

Il display è **1220×2712 px** fisici. Le coordinate del dump UIAutomator e di `input tap` sono nello stesso spazio di coordinate.

## Come ottenere l'XML

```bash
uiautomator dump /sdcard/ui.xml && cat /sdcard/ui.xml
```

L'output è un unico lungo XML su una riga. Cerca il testo del prodotto con una ricerca manuale nella stringa.
