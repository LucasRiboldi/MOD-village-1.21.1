---
name: simulacao-taiga-10-dias
description: "Simulação de 10 dias (taiga, semente 4522030530979676935, vila 1488,128) com mapas antes/depois — 8 defeitos achados, nenhum corrigido ainda"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T12:56:20.415Z
---

2026-10-28, branch fix/playtest-0102 (código até 46a1ce60). `biome_sim.py taiga --days 10 --seed 4522030530979676935 --near 2563,-42 --snapshot --keep-world`; mapas por `scripts/village_map.py` (e56153cc). Relatório e regiões em `run/biome-sim/reports/taiga/`. Nota 7,0; 15 obras planejadas e 14 concluídas; 7→14 aldeões; sem exceção.

Defeitos achados (nenhum corrigido):
1. Curral sem lote → faixa anexada a 98 blocos → caixa cresce, centro desliza 40 blocos a oeste, obras seguintes vão atrás (4 obras a 80–137 do centro). 4 currais em 15 obras.
2. `NightHome`: 114 teleportes, ~10 por aldeão (todas as noites); 66 de mais de 48 blocos (o alcance do findPathTo); todos na 1ª conferência da noite.
3. Camas oscilam 15→7→18→10 com a deriva do centro.
4. Fazendeiro 0% trabalho: 0 de 119 tarefas; COLLECT_SOIL num alvo fora de alcance (1370,68,96) repetido 79×; SWEEP_INCOMPLETE 68×.
5. Mina: 1 nível em 10 dias, ferro 2; 3 mineiros num poço (2 "waiting for a branch"); NO_PATH 32×.
6. Fundidor 80% e pedreiro 64% ociosos.
7. "chest had no room mid-harvest" 27× com 38% dos baús ocupados (o baú privado enche).
8. Log "finished ... 0 blocks placed" com a casa construída (contagem errada); `/vc bioma nota` do console grava no spawn ("forest").

Limite da bancada: o forceload é ±96 do centro, e a vila cresceu até x=1351 — a parte oeste fica fora dos chunks simulados, o que agrava 1, 2 e 4.

**Why:** retrato de referência para comparar depois das correções.
**How to apply:** repetir com a mesma semente e o --near para medir o efeito; ver [[ruas-naturais-progresso]] e [[abandono-por-deriva-do-centro]].

**Correções (f29458ba):** Regras 141 (centro fixo + faixa a 48 +10/volta), 142 (noite: 128 blocos, só o parado 2×), 143 (público sem teto), 144 (alvo inalcançável 1 dia fora). Bancada: SimulatedObserver.tickForcedChunks (o trigo não crescia sem jogador — o 0% do fazendeiro era da bancada).

**Escolhas do autor 10-28 (a fazer):** camas = camas da caixa (POI home dentro da caixa, a cada ciclo); fundidor/pedreiro = mais produtos + teto 512 (ferro do cru, carne assada, tijolo, pedra lisa; escadas/lajes/muros, arenito cortado/liso, tijolo musgoso/entalhado) E ajudantes (pedreiro aplaina em volta das casas e conserta rua; fundidor leva minério da boca aos baús e acende rua); logs = diário da vila (eventos com blocos contados no mundo, dia/hora, versão) + auditoria diária contra o mundo (VC_AUDIT) + resumo diário por profissão. Mina: seguir o plano do doc (MineRoute primeiro).

**Feito depois (dcb446f9, e0667dcb):** Regras 145 (BoxBeds), 146 (ValueGoods teto 512 + receitas com proporção), 147 (VillageDiary: VC_EVENT/VC_DAY/VC_AUDIT; state_report lê); bancada com tique aleatório; `scripts/road_map.py` (só ruas: cinza velha, laranja nova, vermelho solta). 2ª simulação (código de f29458ba): obras todas ≤51 do centro, teleporte 125→43, encalhe 117→35, mas construtor assentou 1.196 vs 1.573 e 7 de 10 obras.

**Pendente (não feito):** ajudantes (pedreiro aplaina/conserta rua; fundidor leva minério e acende rua); mina (MineRoute → MinePlan, doc 2026-10-28-mina-natural); impasse "0 blocks left, no build task" — só peças adiadas (cortador de pedra precisa ferro) e a obra é largada após 12.200 tiques meio pronta; anel de terra em volta da árvore de outra espécie que o lenhador planta; rodar a simulação com dcb446f9 (diário ainda não exercido em jogo); bump de versão/JAR/PR.

**Ciclo seguinte (NÃO commitado até a bateria passar):** Regra 148 BuildClosing (obra com 0 restantes fecha pelo ciclo; parada no fim → tudo surge, END_OF_BUILD no supply-gaps.jsonl; só obra sem nenhuma peça é largada) + BuildClosingGameTest; Regra 149 StreetGaps (falha de até 2 blocos entre ruas vira caminho) e VillageHelpers (pedreiro tapa falhas/nivela em volta da casa pronta via SiteSurroundings.level(Building); fundidor leva minério da boca aos públicos e põe lanterna na rua escura). Falta: RULES 148–149, GameTest do StreetGaps/VillageHelpers, simulação nova. Anel de terra das árvores = podzol do pinheiro gigante (vanilla), não é defeito. Mina: gargalo = HALL_SIDE 17 (MineShaft) ~4.875 blocos/nível; próximo passo proposto: salão menor (decisão "salões grandes saem") — afeta MineShaft/Mine/MineArm/MineEntrance/MineOreHeading e 6 testes.
