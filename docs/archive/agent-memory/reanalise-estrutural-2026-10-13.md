---
name: reanalise-estrutural-2026-10-13
description: Produção por sessão 01-10..12-10 — work% antes da 0.3.7 era inflado; queda real = nenhuma obra desde 0.3.36, tetos de 11-10, mina refeita 4x; decisões do autor abertas
metadata:
  type: project
---

Reanálise em docs/research/2026-10-13-reanalise-estrutural-profissoes.md (846ecb23). Os scripts estão no scratchpad: `history.py` roda o `time_ledger` por log `.gz`, e `production.py` mede a produção por minuto.

- O "% de trabalho" antes da 0.3.7 era inflado (`0aceda49`: o braço levantado contava como trabalho). Compare épocas pela **produção**, não pelo `time_ledger`.
- Quedas reais:
  - zero obras desde a 0.3.36 (C1);
  - peças de 10–20/min para cerca de 2/min, por causa dos tetos do P1 de 11-10 amarrados à obra (C2);
  - mineração de 10–25/min para 0,1–5/min depois da ADR-042 (C3).
- O foco e a busca de lote lenta formam a causa C4.

**Why:** o autor disse "antes da auditoria funcionava, agora tudo ocioso". A causa é uma porta só (a obra) travada, não defeito em cada ofício.

**How to apply:** aguardando escolha do autor:
- obra: A rodízio, B casa na 1ª volta ou C manter;
- teto do artesanato com reserva fixa;
- congelar a mina;
- cadência no índice de ruas.

Ver [[sem-obra-desde-0336]] e [[abandono-por-deriva-do-centro]].

**Aplicado na 0.3.46 (5a999762):**
- 1A, rodízio de casa com teto de camas = adultos + 4;
- 2, reserva 32 e `ItemStock` 16;
- 4, `RoadNetwork` (BFS de caminhos conectados).

**Proposta aberta** em docs/research/2026-10-13-fluidez-mina-e-regras.md:
- **Mina M:** nível plano, cavar pela frente, socorro de 30 s. Toda mina travou por "out of reach", e não pela geometria.
- **F4:** socorro geral de 30 s para todos os ofícios.
- **Consolidação das regras:** conflitos 49/55/60, 59 contra a emenda da 51, texto da 54 e da 56, foco e tetos.

**Lição:** o commit 817bddbd afirmou uma mutação que não foi pega (teste vazio); 8e10dae5 corrigiu. Rode a mutação ANTES de escrever a mensagem.

**Melhorias aplicadas (13-10):**
- F4 em `WorkRescue.LIMIT` = 1200 (eb8d0121); mina M em `NetworkDigging` (104d204d): célula recusada é pulada, nó só falha com toda a pedra que falta recusada; item 4 em `RoadNetwork`; regras consolidadas em docs/RULES.md (49 absorve 55/60/1A; 54/56 reescritas; emenda da 51 revogada pela 59; regras 63/64).
- JAR corrente: 0.3.48 (D4ACA323). Nada jogado desde a 0.3.44.
- A mina M mexeu só na seleção de alvo (pular célula); a geometria 1×2/raio 6 já existia. Se o playtest ainda mostrar "out of reach", o próximo passo é o nível plano sem degrau dentro do nível (item 1 da proposta), que ainda NÃO foi feito.

**Playtest 0.3.48 → 0.3.49 (a7b0ac9b):** a obra VOLTOU (4 casas, rede de ruas 1324 colunas; construtor/carpinteiro/pedreiro com 85–97% de conclusão). O gargalo passou a ser o mineiro: 336 "looking for stone" contra 12 "digging", travado em "11 of 86" por 33 min, e isso fazia fundidor e obra esperarem cobblestone. Causa: `NetworkDigging.nextTarget` reclamava o nó a partir de `body.getBlockPos()` — o corpo sobe à superfície para depositar, então ele pegava o nó mais raso e cavava 1-2. Corrigido para `network.hub()` (o salão). Diagnóstico pela razão: contar "looking for stone" vs "digging" no log; `stall 0/1200 still 0/300` com `N of M` congelado = loop de busca que não devolve alvo, não travamento físico. JAR 0.3.49 (2325A59B), não jogado.
