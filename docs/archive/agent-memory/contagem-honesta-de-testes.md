---
name: contagem-honesta-de-testes
description: As contagens de teste do TODO.md ficam obsoletas; como obter o número real de unitários e de gametests
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-03T03:22:37.122Z
---

As contagens no bloco de status do `TODO.md` erram para os dois lados e
já foram recontadas três vezes (08-27, 09-02, 09-03). Em 2026-09-03 ele
dizia 218 de jogo e 565 unitários; os números reais eram 222 e 607.

Como contar de verdade:

- **Gametests, baseline antes de mexer:** `@GameTest` no `HEAD`, não no
  working tree —
  `for f in $(git ls-tree -r HEAD --name-only | grep "src/gametest.*\.java$"); do git show HEAD:$f | grep -c "@GameTest"; done | awk '{s+=$1} END {print s}'`
- **Gametests, depois:** a linha `All N required tests passed` do
  `./gradlew runGametest`. Sem `--rerun-tasks` o Gradle pula a task como
  up-to-date e **não imprime linha nenhuma**, e o `BUILD SUCCESSFUL` sai
  igual — o mesmo silêncio de [[gametest-registro-silencioso]].
- **Unitários:** somar `tests` dos XML em `build/test-results/test/*.xml`.
  Um `build` incremental só reporta as classes que rodaram, e foi isso que
  produziu o 565.

**Why:** o projeto separa por regra *"tem teste"* de *"foi visto rodando"*,
e um relatório que cita contagem errada corrói justamente essa distinção.

**How to apply:** ao encerrar ciclo, rode `runGametest --rerun-tasks` e
tire o número da linha do runner; nunca copie a contagem do `TODO.md`.
