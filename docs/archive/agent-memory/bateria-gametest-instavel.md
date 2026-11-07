---
name: bateria-gametest-instavel
description: "A bateria de gametest falha ~25% das vezes num único teste — \"269/269 passou\" não é prova de nada sem repetição"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-17T02:08:55.332Z
---

Medido 2026-09-09, commit `3e367d5`, mesma máquina, mesma sessão: **12 execuções
de `runGametest --rerun-tasks`, 3 falhas**. Sempre o mesmo teste,
`MinerGameTest.aFrozenMinerGivesUpLongBeforeTheStallGuard`. Nenhum outro falhou.

**Consequência prática, e é a que importa:** uma bateria verde **não** prova que a
mudança está boa. Rodar uma vez e reportar "269/269" é reportar um sorteio. Para
afirmar que algo passou, **repita** — e quando falhar, diga qual teste, não só que
falhou.

**Causa até onde foi medida.** O contador de imobilidade só anda quando o mineiro
tem alvo; ganhar alvo custa uma busca; `MinerWork.SEARCHES_PER_TICK` é **1 para o
servidor inteiro**, e a bateria monta 18 cenários de mineiro. Mesmo aldeão
emparedado, duas execuções: `still 280/300` numa, `still 99/300` na outra.

**Hipótese refutada — não repetir:** aumentar a folga (360→700 tiques) piorou para
2 falhas em 6. O ciclo da colônia é de 600 tiques e **re-reserva** a tarefa que o
guarda liberou, então a asserção encontra `RESERVED` de novo. A folga não cabe
entre o guarda (300) e o ciclo (600) com a fila global no meio.

Registrado em `docs/behavioral-tests/known-failures.md` (KF-001).
Ver [[contagem-honesta-de-testes]] e [[gametest-interferencia-global]].

---

## Remedição em 2026-09-16 — mudou de teste, e são dois

**16 execuções de `runGametest --rerun-tasks`, 3 falhas (~19%).** O teste do
`MinerGameTest` acima **não falhou uma vez**; quem falha agora é outro par,
no mesmo arquivo:

- `SurfaceGatheringGameTest.smelterGathersDirtOutsideTheProtectedVillageRadius`
- `SurfaceGatheringGameTest.smelterGathersGrassOutsideTheProtectedVillageRadius`
- `LumberjackGameTest.aBlockReplacedMidHarvestIsLeftAlone` (uma vez)

**A mensagem é sempre a mesma:** `fundidor criado no setor não está
registrado no ServerWorld`. O cenário cria o aldeão com `spawnEntity` num
setor devolvido por `FarthestVillageSector.farthestLoadedSector` e confere
no `runAtTick(1)` se ele ainda existe. É o mesmo aldeão que some entre o
spawn e o tique 1 que o **AUD-001** do `TODO.md` já registra como sem causa
determinística.

**Provado pré-existente, e vale o método:** rodei 6 vezes em `e209e1f` (o
commit anterior, só documentação) e falhou 1. A instabilidade **não** vem de
mudança nova — para acusar ou inocentar um commit, rode a bateria no commit
anterior, não só no seu.

**Consequência que se mantém:** "335/335 passou" de uma rodada é sorteio.
Hoje foram 13 verdes em 16, e o número honesto a reportar é esse, com o nome
dos testes que caíram. Ver [[e46-obra-fecha-sozinha]] (a sessão em que isto
foi remedido).
