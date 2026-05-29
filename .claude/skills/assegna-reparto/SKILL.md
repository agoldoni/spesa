---
description: Assegna un reparto a un articolo nella lista dell'app Spesa tramite ADB
allowed-tools: mcp__android__execute_adb_shell_command, mcp__android__get_screenshot
argument-hint: <nome articolo> <nome reparto>
---

Assegna il reparto specificato all'articolo nell'app Spesa. Gli argomenti sono: "$ARGUMENTS" (formato: "nome_articolo nome_reparto", es. "pomodori frutta").

Estrai il nome articolo e il nome reparto dagli argomenti (l'ultimo token è il reparto, tutto il resto è il nome articolo).

1. Esegui `input keyevent 111` per chiudere la tastiera se aperta.
2. Esegui `uiautomator dump /sdcard/ui.xml && cat /sdcard/ui.xml` per ottenere la gerarchia UI.
3. Nell'XML, trova il nodo con `text="<nome articolo>"` (confronto case-insensitive) ed estrai i suoi `bounds` per ricavare il range y del prodotto.
4. Tra tutti i nodi con `content-desc="Cambia reparto"`, individua quello il cui range y si sovrappone al range y del prodotto trovato al passo 3.
5. Risali al nodo padre clickable (quello con `clickable="true"` che contiene il nodo "Cambia reparto") e calcola il suo centro: `cx = (x1+x2)/2`, `cy = (y1+y2)/2`.
6. Esegui `input tap <cx> <cy>` per aprire il dropdown del reparto.
7. Esegui nuovamente `uiautomator dump /sdcard/ui.xml && cat /sdcard/ui.xml` per ottenere il dropdown aperto.
8. Nell'XML del dropdown, trova il nodo con `text="<nome reparto>"` (confronto case-insensitive) e calcola il suo centro.
9. Esegui `input tap <cx> <cy>` per selezionare il reparto.
10. Chiama `get_screenshot` per confermare che l'articolo è stato spostato nella sezione del reparto corretto.

Se il prodotto non è presente nella lista, o il reparto specificato non esiste nel dropdown, rispondi spiegando il problema senza eseguire tap aggiuntivi.

Consulta @references/adb-tips.md per dettagli sull'XML e le coordinate.
