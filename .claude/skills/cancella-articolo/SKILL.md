---
description: Cancella un articolo dalla lista nell'app Spesa tramite ADB
allowed-tools: mcp__android__execute_adb_shell_command, mcp__android__get_screenshot
argument-hint: <nome articolo>
---

Cancella l'articolo "$ARGUMENTS" dalla lista dell'app Spesa:

1. Esegui `input keyevent 111` per chiudere la tastiera se aperta.
2. Esegui `uiautomator dump /sdcard/ui.xml && cat /sdcard/ui.xml` per ottenere la gerarchia UI.
3. Nell'XML, trova il nodo con `text="$ARGUMENTS"` (confronto case-insensitive) ed estrai i suoi `bounds` per ricavare il range y del prodotto.
4. Tra tutti i nodi con `content-desc="Rimuovi"`, individua quello il cui range y si sovrappone al range y del prodotto trovato al passo 3.
5. Calcola il centro del nodo Rimuovi: `cx = (x1+x2)/2`, `cy = (y1+y2)/2`.
6. Esegui `input tap <cx> <cy>`.
7. Chiama `get_screenshot` per confermare la rimozione.

Se il prodotto non è visibile nella lista, rispondi che l'articolo non è presente e non eseguire nessun tap.

Consulta @references/adb-tips.md per dettagli sull'XML e le coordinate.
