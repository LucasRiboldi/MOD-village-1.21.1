---
name: bancada-simulacao-biomas
description: "20-10 — bancada de simulação por bioma (scripts/biome_sim.py + -Dvillagecolony.simulate) achou que 3 das 5 vilas Vanilla nunca viravam colônia; corrigido em 0.3.69 (junção pela vila gerada, estilo pela vila gerada, casa fundadora com busca maior e aplainamento). Branch feat/biomas-simulacao, sem push, não jogado."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-21T01:31:21.585Z
---

**Bancada:** `python scripts/biome_sim.py <alvo> --days N` (`--list`). Sobe `gradlew runBiomeSim` sem jogador; `SimulatedObserver` (só com `-Dvillagecolony.simulate=true` E dev) faz o centro dos chunks forçados contar como jogador. Locate/place por RCON, `/forceload` 13x13, espera a área gerar, `/tick sprint`. Relatórios em `run/biome-sim/reports/<alvo>/`. Linha `VC_SIM` diz o que a varredura viu. 1 dia ≈ 20 s sem colônia, ~1,5 min com.

**Armadilhas da bancada:** `/forceload` só marca, a geração é assíncrona (sprint antes da vila existir = 0 colônias); `/place structure` recusa posição não carregada; arena de GameTest tem teto de barreira em y=9 (lote que exige 25 de ar falha — testar fora da arena, +1_000_000); heredoc com `python - <<'EOF'` grande quebrou no bash — usar arquivo no scratchpad.

**Achados (semente 20261020):** D3 vila espalhada partida em pedaços < 3 camas (deserto); D1 centro em outro bioma → ignorada (neve 19 camas em `forest`, taiga em `cold_ocean`); D4 casa fundadora 7x11 exigia 77 colunas planas a ≤32 do centro (deserto, taiga adiados 40/40). Commits 6a7d624e, 5c983a27, 5d56c083 (0.3.69). ADR-003 Emenda 9.

**Prova:** test 1550/0, runGametest 775/775. MinerDirectPathGameTest falha ~1/3 já no 690dee98 (pré-existente; chip de tarefa criado). Aplainamento só provado por GameTest+mutação, não exercido na bancada.

**Rodada 4 feita (3 dias, ver doc §5).** **Pendente:** deserto sem tarefa de pedreiro/fundidor (NO_TASK), fazendeiro/pastor sem alcance (deserto, neve) e biomas fixos (forest, jungle, swamp, badlands, cherry, ice_spikes, mushroom...); riscos da previsão por código (F1 roça nova exige grama/terra com ar em cima — neve e deserto; F2 viveiro barrado pela neve; F4 rua de arenito não reconhecida) ainda não medidos. Doc: `docs/research/2026-10-20-biomas-previsao.md`.

**Rodada 5 (notas, scripts/biome_score.py, commit 4946295a):** savana 9,3; meadow 9,0 (sem montanha); planície 8,1; taiga 7,8; neve 5,3; deserto 4,0; ermos 3,4; rio/pântano/picos de gelo 3,0; floresta/cerejeira/cogumelos/taiga nevada 0 por **D5** (casa fundadora sem lote: não corta árvore, micélio não é chão); selva/floresta escura 0. Artefato: /place structure não registra a vila gerada (sem junção nem estilo).

**G1 (cf97cb9e, 0.3.70):** casa fundadora limpa árvore natural (LotGrowth) e aceita #dirt; mata sai de 0 para 3,0. **G2 (6b159f12, 0.3.71):** vigia por peça que trava a obra (a da vez + postas de lado por material), 2 ciclos sem progresso com ou sem tarefa → tudo surge no baú do construtor; registro permanente villagecolony/supply-gaps.jsonl + scripts/supply_gaps.py; ADR-040 emenda 20-10. Planície 9,4, savana 9,6, neve 8,3, taiga 8,0, deserto 5,6. Pendente: obras comuns na mata não acham lote (árvores) — G1 só valeu para a casa fundadora; armadilha de teste: lã e lanterna do mar têm ingrediente automático (drop), use bancada de flecheiro para peça sem rota.

**0.3.72-0.3.74 (c74041cd, a2725c6d, 24448dbe):** caminho natural por chão (VillageRoad.pavingOn; caminhos de bioma só são rua no índice — arenito liso é parede das casas do deserto); obra comum limpa lote só em bioma de mata (LotGrowth.clearsTreesAt); proteção do viveiro por ColonyTrees (terra enraizada NÃO serve: a cerejeira nasce dela — regressão achada e corrigida); mineiro escora teto de areia em arenito (MineSettling.shoreUp, 79 escoramentos no deserto) e MineFill = pedra do bioma; ferramentas de teste por bioma: /vc bioma, /vc bioma nota, VC_BIOME, scripts/biome_report.py, BiomeCoverageGameTest (53 biomas), docs/testing/roteiro-por-bioma.md. JAR 0.3.74 instalado (473905BF…). Armadilha: -PgametestOnly com várias classes de mina reprovou StrandedEscape que passa na bateria inteira. Próximo: jogar um bioma pelo roteiro; terraplanagem barra mata/ermos (relevo).

**0.3.75-0.3.76 (db4bdd64, 39011c83, fcf199e9, push feito):** terraplanagem fase 1 (LotTerraform: lote ±2 da rua aceito e aplainado; lote medido pela rua; guarda de água; casa sem estrutura busca 48); rotas próprias (WorldEvidence madeira/argila/areia; STRIPPING; superfície de peça sem tipo vira pedido; hasRouteInBiome delega a SupplyRoutes). Pendente: casa trava no último bloco (taiga, 44 ciclos "1 blocks left"); STALLED do arenito/vidraça/terracota; NO_TASK de acácia na savana; melancia sem rota; fase 2 da terraplanagem.

**0.3.77 (119fad7e, push feito):** peça UNREACHABLE de lado 3× com nada mais a pôr surge no lugar (UnreachablePieces; bloco não substituível é mantido e só riscado) + villagecolony/unreachable-pieces.jsonl + VC_UNREACHABLE; biome_report aponta "acesso:". test 1552/0, gametest 800/800, mutação reprova 2/3. Decisões do autor 20-10: "surge + registro"; teste em jogo = "você joga, eu preparo" (instalar JAR, autor joga pelo roteiro, eu gero relatório). **Mas a bancada refutou a causa:** a última peça da taiga não era UNREACHABLE (0 VC_UNREACHABLE) — era tocha UNSUPPORTED porque 29 paredes foram puladas por "spruce_leaves is in the way" (copa de fora do lote). 593605b0: folha não persistente dá lugar à peça → taiga 0→2 casas, nota 7,6; gametest 802/802. JAR 0.3.77 instalado (EF825151…), não jogado. Pendente: tronco natural no volume ainda pulado; UNSUPPORTED sem apoio nunca volta.

Relacionado: [[bancadas-telas-e-biomas-plano]], [[varredura-2026-10-20-graves]], [[plano-terraplanagem-vila]].
