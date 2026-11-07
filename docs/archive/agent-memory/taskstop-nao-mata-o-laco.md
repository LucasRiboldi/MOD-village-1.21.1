---
name: taskstop-nao-mata-o-laco
description: TaskStop num script de N rodadas (check-tests-green) não mata o bash nem o Gradle no Windows; o laço segue abrindo rodadas e prende o session.lock
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-11T06:30:08.173Z
---

Em 2026-10-11, o `TaskStop` de `check-tests-green.sh --runs 8` parou só o invólucro. O `bash` do script continuou vivo e abriu a rodada seguinte. Ela segurou `build/gametest/world/session.lock` e deixou uma colônia salva no mundo de teste ("Loaded 1 colonies with 12 workers").

Antes disso, uma edição de comentário feita durante a rodada 2 quebrou a compilação dela: o javac leu o arquivo pela metade.

**Why:** rodada interrompida ou compilada com arquivo pela metade não conta, e a colônia que sobra no mundo de teste pode interferir na bateria seguinte (ver [[gametest-interferencia-global]]).

**How to apply:**
- Não edite nada em `src/` enquanto a bateria roda; só documentação.
- Para parar o script, procure `check-tests-green` e `fabric-api.gametest` no `Win32_Process` e mate a árvore inteira. Nunca o TLauncher.
- Depois de matar, apague `build/gametest/world` antes da próxima bateria.
