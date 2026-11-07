---
name: climb-out-nunca-desiste
description: "E47 revisto 01-10 (088846e): encalhado larga o ofício e ClimbOut sobe por escada/pilar/túnel sem desistir; frozen ArchUnit muda com qualquer dependência"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-01T15:30:14.142Z
---

Pedido do autor 2026-10-01: "o mineiro tem que conseguir quebrar ou adicionar
bloco para escalar e nunca ficar preso". Caso real: 199ad062 parou a y=56 após
os 32 degraus (surface ~71) — o ofício seguia rodando ao lado da fuga (dois
condutores), o degrau dependia da navegação Vanilla, e havia teto + desistência.

Commit 088846e: StrandedWorkers marca → WorkerJobs.dropAll no tique seguinte
(nunca dentro do laço do ofício: CME); ClimbOut STAIRS→PILLAR→TUNNEL a cada 6 s
sem ganho; pilar usa o bolso (blocos cavados), parede, depois pedregulho do baú
via EscapeBackfill.placeFromChest; StrandedDetours removido. JAR md5 95f120d7
instalado em mods (anterior 21d7af35 no scratchpad). Não visto em jogo.

**Why:** o limite real que sobra é material: poço de tijolo sem pedra nem baú
deixa o pilar sem bloco (o teste mostrou 9 de 10). Fechado em bloco protegido,
ele tenta para sempre e avisa a cada minuto (Regra 3 mantida).

**How to apply:** no log, `climbed to … by PILLAR`, `switches from`, `boxed in`.
ArchUnit congelado: QUALQUER dependência nova/removida num ciclo muda o texto
(contagem "N further dependencies omitted") e reprova; recongelar só depois de
provar que o conjunto de ciclos é o mesmo (awk nos cabeçalhos "Cycle detected").
**E o refreeze vale para TODAS as regras congeladas** (02-10): ele engoliu "RoadRefusals keeps a static
mutable collection" no store 9f6005cc. Depois de recongelar, só o store de ciclos (bd1afc07) pode ter mudado;
qualquer outro mudou = violação nova, desfazer e corrigir o código.
GameTest: limpar só o próprio aldeão — clearAll global derruba testes do mesmo
lote. Ver [[e52-curral-prende-aldeoes]], [[bateria-gametest-instavel]].
