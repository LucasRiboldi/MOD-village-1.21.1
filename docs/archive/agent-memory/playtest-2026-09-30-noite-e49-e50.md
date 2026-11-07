---
name: playtest-2026-09-30-noite-e49-e50
description: Playtest 30-09 18:09 (JAR 8f500a2) + Spark YUm45D9Sw4 — E49 e E50 em commits locais sem push; JAR AA3FDED3 (de3af60) instalado em mods, downloads/ não atualizado
metadata:
  node_type: memory
  type: project
  modified: 2026-09-30T22:14:20.413Z
---

Sessão de 2026-09-30 à noite, branch `claude/analise-atualizacoes-projeto-9b8886`
(worktree inspiring-torvalds, adiantado por fast-forward até 8f500a2).
Commits **locais, sem push**: `5e9a6e7` (E49), `00b27f3` (E50), `de3af60` (STATE).

- **E49:** `IdleHands` (F10, 1×/s) devolvia a tarefa recém-solta ao mesmo aldeão;
  6 fundidores sem areia = 6.324 paradas. Correção: `IdleHandouts` (par
  aldeão+tarefa espera 1 ciclo). A fila só oferece a tarefa da frente de cada
  capacidade, então quem tem a sua bloqueada espera o ciclo.
- **E50:** farmland sobre grama caía no "is in the way" antes da regra de moldar
  o chão; `BlockShaping.tillsTheGround` sem `BlockProtection.mayBreak` (ela protege
  a caixa da obra aberta e a vila original — o próprio lugar do canteiro).
- Spark: TPS 20, mod ~3,3% do servidor; lenhador (TreeScanner) é o próximo custo.

**Armadilhas:** (1) log de debug com `VillageColonyMod.LOGGER` em `BuilderWork.step`
não apareceu no log da bateria; `printStackTrace(System.out)` em `markPlaced`
apareceu e resolveu. (2) Rodar `gradlew build` com teste do ArchUnit falhando
**reescreve** `src/test/resources/archunit_store/*` — sempre `git checkout` nele.
(3) Dependência nova `work → integration` reprova o ArchUnit de ciclos.

**Why:** a próxima sessão precisa saber que há 3 commits locais e nada visto em jogo.
**How to apply:** JAR SHA-256 AA3FDED3…9AC7 (de3af60) instalado em mods em 30-09 19:1x, antigo 9CE02219 salvo no scratchpad; downloads/ ainda tem o antigo. Pedir aval para push; no jogo, recarregar o save e contar
`Smelter .* stopped` (deve cair ~30×) e ver a roça `fdc5698e` terminar.
Ver [[sessao-de-jogo-destrava-o-plano]], [[spark-log-decodificar]].
