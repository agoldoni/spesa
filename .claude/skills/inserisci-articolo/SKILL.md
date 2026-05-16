---
description: Inserisce un articolo nell'app Spesa tramite ADB
allowed-tools: mcp__android__execute_adb_shell_command, mcp__android__get_screenshot, mcp__android__get_uilayout
argument-hint: <nome articolo>
---

Inserisci l'articolo "$ARGUMENTS" nell'app Spesa seguendo questa procedura:

1. Chiama `get_uilayout` per ottenere l'albero UI.
2. Trova il nodo con testTag `input_nome_prodotto`, calcola il centro dai suoi bounds e tappa.
3. Digita il testo con `input text` (sostituisci ogni spazio con `%s`).
4. Trova il nodo con testTag `btn_aggiungi_prodotto`, calcola il centro e tappa.
5. Chiama `get_screenshot` per confermare che l'articolo è apparso nella lista.

Consulta @references/adb-tips.md per dettagli su come estrarre i bounds e le coordinate di fallback.
