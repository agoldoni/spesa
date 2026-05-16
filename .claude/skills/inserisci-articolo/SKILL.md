---
description: Inserisce un articolo nell'app Spesa tramite ADB
allowed-tools: mcp__android__execute_adb_shell_command, mcp__android__get_screenshot
argument-hint: <nome articolo>
---

Inserisci l'articolo "$ARGUMENTS" nell'app Spesa tramite ADB seguendo la procedura in @references/adb-tips.md.

Se il nome contiene spazi, sostituisci ogni spazio con `%s` nel comando `input text`.
Dopo aver tappato "+", fai uno screenshot per confermare che l'articolo è apparso nella lista.
