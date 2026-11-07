---
name: playtest-0110-mina-caminhos-estruturas
description: "Jogo da 0.3.110 (29-10 21:28 → 30-10 10:02, \"Novo mundo\", planície/rio) analisado em 30-10; achados de mina, caminhos e estruturas, nada corrigido"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-31T01:21:22.869Z
---

Sessão de 12,3 h, colônia a2254518, 15→19 aldeões, 38 obras prontas, 25 ms/tique. Logs: `2026-10-29-5.log.gz` + `latest.log`; diário `villagecolony-actions-20261029-212946.jsonl`.

- **Mina**: chega ao fundo do mundo (y≈-17) em ~2,5 h; 3× "moved to opposite mouth" com bocas que andam (-736/-726/-794, a última fora da caixa); depois 8× "keeps its stair — no first mouth". 135× "closed its stair before the hall — turning the helix". 2533× "every open branch is taken or finished" → mineiro 36% ocioso. 628 NO_PATH; raw_iron solto 107×; 20× "below its way and unable to climb".
- **Caminhos**: rede oscila (35 portas → 8 → 26; "set aside" até 33); 49× "dropped its road index"; 30 casas com "no path from the door". Dois animal_pen_1 colados em (-693,489) e (-702,487) com 24–26 de 51 peças: 221 encalhes "fenced in" ali (pastor, fundidor, carpinteiro). 127 teleportes, 47 num só ponto (-704,502).
- **Estruturas**: nenhuma fazenda planejada na sessão (fazendeiro 0% conclusão, 49% espera, "no empty plot"); small_house_4 ×5, _2 ×3, _1 ×3; 56 camas para 18 aldeões; 2 storage_majest; fletcher/tool_smith/shepherd "no lot in a whole sweep". Pedreiro 78% ocioso.
- Diagnóstico de estruturas (autor escolheu esta frente, 30-10; só lido, sem teste): casa repetida vem do fallback de `HousePlans.java:~294` (0.3.42, 4290aa87) — sem tipo não residencial com lote e `LotTypeRefusals.anyRefused`, levanta casa ignorando `roomForAHouse` (58 camas/19). Fazenda: Regra 67 (`ColonyFarms.owed`, 1 por 15 adultos) dá 2 para 19, com 3 fazendeiros, CROPS 20 no estoque e 1084 rações vindas de fora (Regra 117). Marca `WorkerHousingNeeds` presa: refutada (pastor resolvido em 2 min). Falta lote para trade_builder 63×, fletcher 15×.
- Extra: laço iron_block↔iron_ingot (~400× cada, "anvil needs it") — causa não verificada.

**Why:** base de comparação para a próxima correção; autor escolhe qual melhoria fazer.
**How to apply:** ao corrigir mina/caminhos/estruturas, compare com estes números no playtest seguinte (Regra 50). Ver [[jogo-0108-decisoes]].
- **Decisões do autor (30-10)**: casas → "os dois juntos" (teto de camas no fallback da casa + busca mais larga do lote para a oficina que falta); roças → "a maior das duas contas" = max(1 a cada 15 adultos, 1 por fazendeiro).
- **Feito (30-10, branch `fix/estruturas-0110`, commit 98dd26f4, sem push/PR)**: Regras 163 e 164 + defeito achado pela bateria: `FARM_DEFICIT` em `HousePlans.plansForNext` pedia `plansFor` (lista de CASAS) — a vez da roça abria casa; provável causa das casas repetidas acima de 15 adultos. Unit 1667, bateria 940/940, mutação da 164 morta. Não jogado, sem versão nova.
- **Pendente (parte 3, "busca mais larga")**: "trade_builder" recusado 63× é o `storage_majest` (604 peças, `ColonyModels` NAMED → BUILDER), não oficina faltando; a oficina que nunca saiu é a do flecheiro (15×). Faixa anexa: "504 doors, 0 lots fit, none with a path of at most 32 blocks to the road". Opções ainda não escolhidas pelo autor.
- **Feito (30-10, commit 422c17a4, mesma branch, sem push)**: autor escolheu "armazém menor" → galpão `storage_small` 7×7×5 gerado (`scripts/gen_storage_small.py`), "sempre o menor primeiro": grande só depois de um galpão terminado (Regra 165). Bateria 940/940, Python 155. Falta: GameTest da obra do galpão assentada no mundo; jogo; flecheiro (15 recusas) segue sem tratar.
- **JAR 0.3.111 (30-10, commit 54738535)**: SHA-256 4784E564…A93A08F6, instalado em .minecraft/mods (0.3.110 jogada → village-colony-0.3.110-jogado.jar.disabled). Não jogado; ao analisar o próximo log, confirmar "villagecolony 0.3.111".
