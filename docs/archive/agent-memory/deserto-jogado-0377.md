---
name: deserto-jogado-0377
description: "20/21-10 — deserto jogado (0.3.77, Spark IgpFCEon8m) com 0 obras; reproduzido na bancada pela semente do autor; 0.3.78/0.3.79 corrigem rua de arenito, janela de rua, corte de duna, fazendeiro, animais e grama no poste"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-21T03:46:08.806Z
---

Mundo do autor "Novo mundo" (bioma único deserto), semente -7040026764766232482, vila em -460,601 (caixa 96x139, y62..90, 36% água). Reproduz com `python scripts/biome_sim.py fixed-desert --seed=-7040026764766232482 --near=-460,601 --days 2 --keep-world`.

Causas medidas (0.3.78, 6472ffeb): rua de arenito liso da vila gerada nunca era rua (0.3.72 a deixou só no índice, e o índice só se enche de rua) → `BlockProtection.isVillageStreet` (peça `/streets/`); busca de rua pela janela do centro +2 → `RoadsideSites.roadGroundAt` pela superfície na altura da caixa (também StripRoad); corte de duna até 5 (`LotTerraform.MAX_CUT`). 0 → 4 casas, nota 5,1 → 8,8.

0.3.79 (84ae294c): FarmerStand (lugar de pé ao lado da planta, chegada 0), `MineApproachPath.legToward` + WalkLegs para fazendeiro e construtor longe; PostGrass (grama ao pé do poste da ovelha amarrada, volta na tosquia); animais da fundação 2×4 espécies (VillageFarmAnimals). JAR 0.3.79 instalado (50133FEB…), não jogado.

Armadilhas: a bancada varia muito (nota 3,9–8,8 com o mesmo código) — compare com 2+ rodadas; mundo do GameTest persiste (sobras de entidades); barreira da arena só é vazio acima da janela +2 (paredes da arena são barreira). Pendente: grama do poste não exercida na bancada; pastor 75% ocioso; lenhador/carpinteiro ócio alto; cacto/cana contam como chão.

Relacionado: [[bancada-simulacao-biomas]], [[plano-terraplanagem-vila]].

**0.3.80 (984f321f, 21-10):** jogado na 0.3.79 (nota 9,1, mundo novo semente 6191407967267685558, vila 153,-494). Correções aprovadas: StrandedWorkers.strandBelow (sobe até a altura do destino), PostGrass.inPen, LumberjackNursery.spotNearTheChest, AppearingPieces (terracota/feno/argila surgem no lugar; regra do autor substitui o 4º despacho para elas). 813/813 ×2; KF-005 (teste do carvão do mineiro cai 3/7 na bateria inteira). JAR 0.3.80 instalado (E34EA6C2…), não jogado. O curral da vila do deserto já tem chão de grama.

**0.3.81 (71abe3ce, 21-10):** jogado na 0.3.80 (nota 8,2, Spark ETMetpWVS9): vila em duas alturas (y63/y80+). Feito: WorkerStrikes.gaveUp(id, task, destino) → StrandedWorkers.strandThoseBelowTheirWay (todas as profissões sobem; ClimbPolicy libera pôr/cavar só abaixo do caminho; prazo 2400 t, descanso 6000 t); VillageStairs (escada fixa); viveiro do anel aceita areia da vila a céu aberto (só vila em duas alturas, dentro da caixa); FarmerTill na areia. Bancada sem ganho claro (4,4–9,0). **Armadilha de teste:** acrescentar classe de GameTest muda o arranjo das arenas e expõe interferência latente — runCycleNow da detecção conserta a fundação de colônia vizinha; isolar fora da arena (+1_000_000). KF-006 Crafting cycle. JAR 0.3.81 instalado (A3F2B32F…), não jogado.

**0.3.84 (84c920a6, 21-10):** SmelterFallback.waitedFor (fundidor atende já a peça de fornalha que a obra espera), IronRest (ferro descansa 1 dia após 32 blocos sem ferro), MineNetwork raio por nível (1º=2, demais=3; antes 6), DesertGround (2 camadas abaixo da superfície de areia viram arenito; decisão do autor "areia em cima, arenito embaixo"). 823/823. **Achado:** a variação da bancada (notas 5 vs 9) vem do lote 148,78,-507 cortado na duna (MAX_CUT 5) — casa num buraco, telhado sem lugar de pé; quando o planejador o pega primeiro, 0 obras. Pendente: decisão do autor. Pergunta aberta do autor: alterar a geração do mundo para a vila nascer em terreno com degraus de no máximo 1 bloco. JAR 0.3.84 instalado (E845E2D9…), não jogado.

**0.3.85 (9f4672fc):** autor escolheu "opção 1" para o lote-buraco: LotTerraform.cutRing corta corredor de 2 blocos em volta da casa até a base (só chão do bioma; nunca rua, peça da vila não-natural, obra, beira d'água; não aterra). Bancada 9,4/9,1/8,9, a casa 148,78,-507 fecha. JAR 0.3.85 instalado (0AF9D355…), não jogado. Pergunta da geração do mundo (degrau ≤1 na vila) ainda sem decisão do autor.

**Merge (21-10):** PR #30 (feat/biomas-simulacao → main, 28 commits, 0.3.67→0.3.85) integrado em 1923c6aa a pedido do autor; CI do PR ainda na fila no momento do merge. JAR 0.3.85 da main instalado (mesmo hash 0AF9D355…).
