# ADB Tips — Assegnare un reparto a un articolo nell'app Spesa

## Struttura XML nel dump UIAutomator

Ogni prodotto nella lista genera questa struttura nell'albero XML:

```
<node text="nome prodotto" bounds="[x1,y1][x2,y2]" ... />          ← testo del prodotto
...
<node clickable="true" bounds="[x1,y1][x2,y2]" ...>                ← contenitore reparto (clickable)
  <node text="—" ... />                                             ← reparto corrente (o nome reparto)
  <node content-desc="Cambia reparto" bounds="[x1,y1][x2,y2]" />  ← icona freccia dropdown
</node>
```

Il nodo clickable che contiene `content-desc="Cambia reparto"` è quello da tappare per aprire il dropdown.

## Come trovare il selettore reparto corretto

Ci sono tanti nodi `content-desc="Cambia reparto"` quanti sono i prodotti in lista.
Per associare il selettore al prodotto giusto, usa la sovrapposizione del range y:

1. Dal testo prodotto estrai `y1` e `y2` (dal campo `bounds`).
2. Tra i nodi `content-desc="Cambia reparto"`, scegli quello il cui `y1`–`y2` si sovrappone.
3. Usa i bounds del suo nodo **padre clickable** per calcolare il centro da tappare.

**Esempio reale — assegnazione reparto "frutta" a "pomodori":**

| Nodo | bounds | y_mid |
|---|---|---|
| Testo "pomodori" | `[133,1384][324,1436]` | 1410 |
| Contenitore reparto clickable | `[114,1395][248,1528]` | 1461 ✓ |
| "Cambia reparto" (icona interna) | `[173,1442][212,1481]` | 1461 |

Centro da tappare: `cx = (114+248)/2 = 181`, `cy = (1395+1528)/2 = 1461`
→ `input tap 181 1461`

## Struttura del dropdown dopo il tap

Dopo aver tappato il selettore reparto, il dropdown appare con le opzioni:

```
<node text="— Nessun reparto" ... />     ← rimuove il reparto
<node text="frutta" ... />               ← nome reparto disponibile
<node text="latticini" ... />            ← altro reparto
...
```

Trova il nodo con il testo del reparto desiderato e tappa il suo centro.

**Esempio — selezione "frutta" nel dropdown:**

| Nodo | bounds |
|---|---|
| Testo "frutta" nel dropdown | `[166,1682][266,1738]` |

Centro: `cx = (166+266)/2 = 216`, `cy = (1682+1738)/2 = 1710`
→ `input tap 216 1710`

## Risoluzione schermo

Il display è **1220×2712 px** fisici. Le coordinate del dump UIAutomator e di `input tap` sono nello stesso spazio di coordinate.

## Come ottenere l'XML

```bash
uiautomator dump /sdcard/ui.xml && cat /sdcard/ui.xml
```

L'output è un unico lungo XML su una riga. Cerca il testo del prodotto con una ricerca manuale nella stringa.
