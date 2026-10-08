# Erros atuais, causas e correções possíveis (2026-10-08)

Fonte: playtests de 07-10 e 08-10 (`docs/reports/Playtest-2026-10-08.md`), `time_ledger.py`,
`activity_report.py`, Spark `bpHFsttvlp` e `iX0dliasLV` (mod = 3,2% da thread), e leitura do código.
Corrigido nesta rodada está marcado ✅; o resto é proposta, por prioridade.

## Já corrigido em 07/08-10 (branch `claude/obra-lote-e-trava`)

| | Defeito | Correção |
|---|---|---|
| ✅ | Roça retomada do save relida com trigo e camada da rua: trava de fundação recusava, obra "AVAILABLE with nobody" 37 min | `FarmBlueprint.asBuilt` |
| ✅ | Recusa da trava de reserva muda no log | `BuildSiteGate` + `refused: <motivo>` na linha `builders:` |
| ✅ | Obra abandonada sem base guardava o lote para sempre | reparo e retomada soltam o lote |
| ✅ | Funil: meta de ferro só via lampião; minério nunca com prioridade de obra | `WorkMaterials.iron` genérico, `RAW_IRON` em `constructionMaterials` |
| ✅ | Obra inteira parada por uma peça sem material | `SkipReason.WAITING_MATERIAL`: a peça fica de lado e a obra segue |
| ✅ | Nome da profissão invisível no painel (escala espelhada) | escala da plaquinha Vanilla + mixin de cliente |

## 🔴 Abertos que travam profissão

### E1. Mineiro preso a pedra cujo lugar de pé não se alcança
- **Sintoma:** 3 pedras em 10 min; 93% das amostras "andando até a pedra"; desvio, desistência, a
  mesma pedra de novo.
- **Causa provável:** `MinerApproach.approachTo(world, target, villager)` devolve um lugar de pé
  **alto demais para subir** (`tooHigh`) quando não há outro; a pedra escolhida não encosta na
  galeria. No log: mineiro em y29–30, lugar de pé em y33.
- **Correção:** quando só houver `tooHigh`, recusar a pedra e marcá-la (`MineMarks`), em vez de
  andar até lá; e na escolha do alvo, preferir pedra **exposta à galeria** (vizinha de ar alcançável).
- **Prova:** GameTest com pedra só alcançável por um bolsão 3 acima; o mineiro deve escolher outra.

### E2. Segundo mineiro sem alvo, relatório errado
- **Sintoma:** `waiting for a branch — 1 of 4 taken` por minutos com 3 ramais livres; depois pega
  pedra a 102 blocos e encalha.
- **Causa:** `MinerReport.waitingFor` diz "esperando ramal" sempre que há outro mineiro cavando; a
  busca dele volta vazia por outro motivo (ramais não abertos ou reservados — parente do E44).
- **Correção:** (a) o relatório diz o motivo real da busca vazia; (b) abrir o próximo ramal quando
  um mineiro fica sem alvo com ramais livres; (c) teto de distância para pedra fora da mina.

### E3. Varredura do fazendeiro sem fim
- **Sintoma:** `no farmer work: still sweeping — the budget ran out before an answer — nothing
  ripe and no empty plot within 32`; 60% de espera.
- **Causa:** `RingSweep` com orçamento por tique não termina o raio 32 inteiro antes de recomeçar.
- **Correção:** lembrar as colunas de roça já achadas (como as colunas de coleta da ADR-039 C) e
  varrer só elas; o anel completo só quando a lista esvazia. Ou filtro barato (`worth`) por
  coluna sem farmland conhecida.

### E4. Construtor parado pela peça em resolução — ✅ decidido e corrigido em 08-10.

## 🟠 Abertos que enganam o diagnóstico

### E5. Chat `[OBRA] … em resolução: uma profissão consegue no bioma; falta entregar`
- Dito para o funil quando **nenhuma profissão tinha o pedido**. `MaterialRequest.reason` deduz
  pela fonte (`PROFESSION`), sem conferir se existe tarefa aberta na cadeia.
- **Correção:** a frase nomeia o elo que falta, descendo a cadeia (funil → lingote → minério) e
  dizendo quem tem tarefa e quem não tem (`cadeia-de-producao-recursiva`).

### E6. `could not make iron_ingot — needs iron_block`
- O fabricante tenta o lingote pela receita do bloco de ferro. Só ruído, mas confunde: filtrar
  receita que desfaz bloco de armazenamento (`sameFamily` já faz isso em `WorkMaterials.through`).

### E7. Pastor devolve a tarefa de lã sem ovelha tosquiável
- Funciona como desenhado (terceira busca vazia estoca a lã — `EmptyFlock`). Melhoria: antes de
  esperar, o pastor procria ovelha (já faz com galinha) ou traz ovelha solta (ADR-038 P2b).

## 🟡 Melhorias

| # | Melhoria | Por quê | Referência |
|---|---|---|---|
| M1 | **Diário de ações estruturado** por aldeão (`VC_ACTION`, JSONL) + `scripts/action_report.py` | hoje o diagnóstico lê frases livres com regex; pedido do autor 08-10 | implementado nesta rodada |
| M2 | **Mapa de alcançabilidade da vila** (BFS do sino, degrau ≤ 1) e `VC_REACH` | a maioria das travas é degrau/buraco; mede antes de mexer | `2026-10-08-nivelamento-do-solo-da-vila.md` |
| M3 | Desempregados (~9) ociosos 100% | ofício novo (nivelador), ou carregador entre baús | plano do nivelador |
| M4 | Pico de 420 ms no Spark | não é do mod (3,2%), mas confirmar se coincide com autosave/geração de chunk | Spark |
| M5 | Overlay: `/vc log` e painel mostram a cadeia que trava a obra | mesma causa do E5 | — |

## Ordem sugerida

E1 → E2 (mineiro é a raiz do ferro, da pedra e do fundidor ocioso) → E5 (diagnóstico honesto) →
E3 → M2 (fase 1 do nivelador, só medir) → E7.
