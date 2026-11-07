---
name: autossuficiencia-escolhas-2026-10-11
description: Branch claude/autossuficiencia (11-10, ee6c98ae, enviada, sem PR): P1,P2,P3,P5c,P6,P7,P8b,P9 feitos; P4b espera decisão de arquitetura; nada jogado, JAR não gerado
metadata:
  type: project
---

Pilha #20–#25 na main (38015460, merge feito pelo autor 11-10). Branch `claude/autossuficiencia` da main, com push a cada marco.
Doc: `docs/research/2026-10-11-autossuficiencia-e-ocio.md` (escolhas na tabela).

Feito e enviado (cada um com GameTest e bateria verde): 059f9238 (peça de lado por material volta à demanda = raiz da areia nunca coletada; hotspot SmelterWork; tarefa 0 peça não encadeia; semente de qualquer baú), 023779ea P1 teto do AdvanceStock (BiomePieces publica teto = maior casa + obra aberta), fe881630 P3 (cobblestone aceita andesito/granito/diorito; BuildReserve; MineMarks.REGION_MEMORY), 22c012a7 P2 (SandNearWater usa SandPatch; setor gira por varredura vazia; Crusher do pedreiro; WindowEquivalent cerca na frente sem rota de vidro), bb183700 P7c (WoodInPieces: tábua das escadas/cercas na demanda), 82613f49 P7a (bosque 6/lenhador, árvore madura do bosque primeiro), 12edeecf P7b (LumberjackDetours).

Depois: 464d748c P9 (DISPATCHES_BEFORE_STOCKING=10, emenda na ADR-040), 8ab7076e P8b (FarmerCompost), 24ff8e6e P5c (IdleYield: titular único cede a ofício em falta, construtor nunca), 9d50cf42 P6 (Mine.branchesOpenNow: zero acabado além do trecho comum mantém 4 ramais), ee6c98ae STATE/TODO. Verificação final: 1457 unit, 716/716 GameTests ×3 em mundo novo, 112 Python.

Pendente: P4b é decisão de arquitetura (planejador recusa lote com obra aberta, ALREADY_OPEN) — perguntado ao autor. P5a não feito: tosquia por estoque/curral já existem, procriação parou por falta de trigo. 2a coleta por estoque no ócio não feita. Próximo: decisão P4b, JAR 0.3.25, PR para a main, playtest único.

Achado: BuildSiteGameTest.indexedRoadFindsALotWithoutStartingFullSweep reprova quando um LOTE NOVO entra dois lotes antes (dependência de tique); pôr teste novo em batchId existente evita. `build/gametest/world` persiste entre execuções (apagar antes de medir). `-PgametestOnly=Trecho` roda só classes cujo nome contém o trecho.

**How to apply:** depois da decisão do P4b, gerar JAR, abrir PR, playtest único com time_ledger/action_report/cost_ledger. Ver [[playtest-2026-10-11-manha]].

**Atualização (11-10, fim):** construtor limpa a faixa de construção (6 blocos da rua) e planta tirada pelo jogador não volta (386c14f2); JAR 0.3.25 = 863C8BF8… instalado (78b41b72); PR LucasRiboldi/MOD-village-1.21.1#26 aberta para a main. Próximo: playtest único.

**Ciclo 2 (11-10 noite):** 5b0c9850 contagem completa+mina não fecha+1 min+britador pela obra; 8b56c4b1 SmelterGathers+reserva 16 areias; a379ca67 artesão sem andar ao baú; e545c328 sem contratar fundidor sem cru+HelperReach; JAR 0.3.26 = 5663237D instalado; não jogado. Próximo: playtest longo 30–60 min.
