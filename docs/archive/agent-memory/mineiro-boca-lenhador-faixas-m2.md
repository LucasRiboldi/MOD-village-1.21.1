---
name: mineiro-boca-lenhador-faixas-m2
description: 0.3.14 (08-10) — MineEntrance (boca de superfície), VillageZone (lenhador centro→borda), ReachMap (M2 fase 1), /vc log; lições da dedução de nível
metadata:
  type: project
---

0.3.14, commits 8aa6f243..0f604e5c (enviados, branch claude/obra-lote-e-trava, sem PR). Não visto em jogo.

- `Mine.entry()` é a entrada do NÍVEL ATUAL, não a boca: cada "went one level deeper" a desce 10 blocos. `MineEntrance` guarda a boca de superfície e deduz nas minas antigas subindo pelo deslocamento fixo de `MineShaft.deepened` (só depende do sentido) enquanto o nível de cima fica dentro do terreno. "A entrada está a céu aberto?" foi frágil (algo sobre a boca) — o MineLeaseGameTest pegou um nível fantasma no ar.
- Lenhador: `VillageZone` (terços da caixa por eixo) + `TreeChoice.byZone`; substitui para ele o rodízio do ADR-036 item 18.
- M2 fase 1: `ReachMap` (BFS degrau ≤1, fatiado, só com jogador perto, VC_REACH + [ALCANCE]). Fases 2–4 do nivelador esperam o número.
- GroundPickupGameTest caiu uma vez só na rodada de mutação — vigiar ([[bateria-gametest-instavel]]).

**How to apply:** próximo playtest confere a lista "Playtest da 0.3.14" do TODO.md e mostra action_report ([[spark-com-action-report]]).

**Atualização 0.3.15 (playtest 12:11, Spark bal0IJeOGc):** a dedução da boca acertou, mas o mineiro ia e voltava entre a boca e o 1º lance — a entrada de nível fica 2 acima do chão onde o aldeão para, e a chegada em linha reta (3) não fechava. Regra nova: chegada por eixo (≤3), progresso = entrada mais funda alcançada, destino = lugar de pé da entrada. Abertos: feno da obra (hay_block 93×, trigo é gargalo), viveiro do lenhador perto da vila.

**Atualização 0.3.17 (playtest 15:14, Spark 3Oy2cLrBoT):** a descida funcionou até o nível 2, mas "entrada mais perto" puxava de volta para cima quem já tinha passado dela, e a descida estourava os 2.400 tiques. Regra nova: na escada a próxima entrada é a primeira ABAIXO dele; "na escada" = logo abaixo da boca ou debaixo da terra (heightmap) perto de uma entrada; descida zera job.stalled. Obra parou 20 min em lampião/corrente (ferro) — decisão pendente do autor se entram na lista do feno. GroundPickupGameTest cai só nas rodadas de mutação no worktree.
