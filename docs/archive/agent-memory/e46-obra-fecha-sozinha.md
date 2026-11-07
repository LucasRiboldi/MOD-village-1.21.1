---
name: e46-obra-fecha-sozinha
description: E46 — o índice de ruas não tem teto de raio; a estrada leva o lote para fora e o guarda de alcance o mata
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-17T01:50:50.365Z
---

Playtest de 2026-09-16, 21:41. A biblioteca foi planejada duas vezes e as
duas morreram em ~30 s com **628 blocos restantes de 628**.

**A causa raiz é o índice de ruas sem teto de raio.** `BuildSiteScanner.find`
bifurca: o caminho dos **anéis** para em `ring <= radius`; o caminho
`findAmongRoads` percorre o índice **inteiro** e não consulta o raio (nem o
recebia). E `remember` acrescenta ao índice **qualquer** coluna de rua nova.
A vila calça estrada para fora do raio, o índice a absorve, e **o lote nasce
de onde a estrada chegou, não de onde o centro alcança**.

**A prova estava num log que já existia e ninguém lia** — o relatório de fim
de sessão do `SweepLog`: `40 planner runs, 40 answered by the index — 0
restarts (0 by drift)`. Todas as consultas pelo índice, zero deriva de
centro.

**A divergência de réguas é real, mas não matou estas obras.** Quem acha
mede em quadrado (Chebyshev), quem larga mede em círculo (`isOutOfReach`,
euclidiana), e a faixa entre as formas é **21%** da área varrida — armadilha
latente, registrada como C1. Mas as duas obras estavam a 67 e 71 blocos **em
quadrado**: fora das duas réguas. Unificar as contas não teria salvado
nenhuma.

**Errei a causa na primeira volta** por parar na primeira explicação
aritmeticamente coerente. O que a desfez foi perguntar *qual caminho do
código rodou*, e não *qual conta explica o número*.

**Descartados (pelo teste da frase ausente):** `isSupersededBy`
(`drops the untouched` = 0); `PatienceClock` (`WAITING_RESOURCES` não aparece
no log, e a paciência é 10 min contra 29 s); `ConstructionService.forget`
(único chamador é `WaitingWork:190`); o scanner partir de outro centro
(parte de `colony.center()`, e `0 by drift`).

**Armadilha que me custou tempo:** `WaitingWork.giveUp` é o ponto comum de
**duas** saídas com frases diferentes — `gives up on` (paciência) e
`lets go of` (distância). Contei a primeira = 2 e quase concluí paciência; a
certa era a segunda, também 2.

**Why:** C4 (teto do índice) é o que destrava a construção; C1 (régua) é
armadilha latente; C3 (centro a 77 blocos do aglomerado de camas, que a
detecção recusa mover) não causou isto e é investigação à parte.

**How to apply:** o C2 já instrumentou — a linha `planned …` diz o centro e
as duas distâncias, e o scanner emite `WARN` ao servir lote de fora do raio.
O próximo playtest confirma. Ver `docs/technical/E46-obra-nasce-condenada.md`.
Relacionado: [[obra-orfa-por-deriva-do-centro]],
[[indice-de-ruas-vira-rua-candidata]] (o índice de ruas já mordeu antes),
[[frase-ausente-refuta-mecanismo]].
