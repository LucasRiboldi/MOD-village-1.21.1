# Telemetria dos aldeões e dos itens — o que existe, o que falta, plano (2026-11-20)

Pedido do autor, 2026-11-20: saber se o mod acompanha todos os itens (quem cria, gasto e criação corretos, erros,
tentativas, esperas, bloqueios) e cada aldeão de profissão (local, tempo acordado, trabalhando, travado, parado, em
erro), e planejar o que falta para as correções ficarem mais certeiras. Nenhum código aqui.

## 1. O que já existe

| Fonte | O que dá | Limite |
|---|---|---|
| Diário de ações (`logs/villagecolony-actions-*.jsonl`, `action_report.py`) | Por aldeão e por ação, com tique, ofício, posição e motivo: `PLACED`, `MINED`, `CRAFTED`, `SMELTED`, `HARVESTED`, `FELLED_TREE`, `SHEARED`, `BUTCHERED`, `SET_ASIDE`, `TASK_TAKEN/STARTED/DONE/RELEASED`, `GAVE_UP`, `STRANDED`, códigos de bloqueio (Regra 112) | Só registra quando há ação: o aldeão parado não aparece. A tarefa vem pelo tipo, sem número: não dá para seguir uma tarefa do começo ao fim. A posição só existe no instante da ação |
| `VC_TIME` e `VC_TIME_WAIT` (`time_ledger.py`) | Por **profissão**, a cada 5 min: trabalho, caminhada, espera, bloqueio, ócio e encalhe no expediente; a espera por pedido | Soma por profissão, não por aldeão. Fora do expediente nada é medido (sono, noite) |
| `VC_STATE` (`state_report.py`) | Retrato a cada 5 min: aldeões, ofícios, camas, travados nomeados, estoque por grupo, obra, tarefas, mina | Retrato, não trajetória |
| `VC_SUPPLY` (`supply_report.py`) | O que surgiu sem ofício, por exceção, item e quantidade | — |
| `VC_BLOCKS`, `VC_KEEP` (`stock_report.py`) | Por bloco e dia: classe, reserva, estoque, posto pela obra, esperado, surgido; travas de cru | Só itens de bloco; o gasto conta só o que a obra pôs |
| `VC_LOOP`, `VC_TASK_PROGRESS`, `StopLog` | Pedido sem resultado, tarefa sem progresso, paradas repetidas | Espalhado em linhas de texto |
| `VC_REACH`, `VC_COST`, `VC_BIOME`, `VC_VANILLA_JOB` e outras ~40 linhas `VC_*` | Alcance, custo por fase, boletim do bioma, ofício Vanilla | — |

**Resposta curta:** o mod já sabe **quem fez o quê, onde e por que parou**, e **quanto tempo cada profissão** passou em
cada estado. Ele **não sabe** ainda:

1. o tempo de **cada aldeão** (só por profissão), nem onde ele estava enquanto parado;
2. a **noite**: sono, horas acordado, quem não dormiu, fome e vida;
3. o **balanço dos itens**: quanto entrou e saiu de cada item por origem e por destino, e se a conta fecha (o que sumiu
   sem explicação, o que foi destruído com baú cheio);
4. o **ciclo de vida de cada tarefa**: quanto tempo do pedido à primeira peça e ao fim, por tipo;
5. as **tentativas por item**: quantas vezes tentou fazer cada item e por que falhou, somado por item.

## 2. Plano — seis peças, cada uma pequena e medida

| # | Peça | Como | Saída | Custo |
|---|---|---|---|---|
| T1 | **Número da tarefa no diário** | Toda linha `TASK_*`, `CRAFTED`, `SMELTED`, `GAVE_UP` leva o id curto da tarefa | `task_report.py`: por tipo de tarefa, tempo até a 1ª peça, até o fim, quantas soltas e por quê (mediana e p95) | P |
| T2 | **Linha do tempo de cada aldeão** | No `WorkTime`, só quando o estado muda (trabalho → espera → bloqueio…), uma linha `STATE` no diário: estado anterior, novo, duração, posição e motivo (o pedido segurado, o código de bloqueio). Por mudança, e não por segundo: poucas linhas | `villager_timeline.py`: por aldeão, o dia em faixas; quem ficou mais tempo parado, onde e por quê; mapa dos pontos de parada | M |
| T3 | **Balanço dos itens** | Contador diário por item: **entrou** (fabricado, fundido, minerado, colhido, cortado, tosquiado, surgido, recolhido do chão) e **saiu** (ingrediente da bancada, cru da fornalha, posto na obra, comido, gasto na manutenção, **destruído por baú cheio**), mais o estoque no início e no fim do dia. Linha `VC_FLOW` | `flow_report.py`: a conta `estoque final − inicial = entrou − saiu`; a diferença é item que sumiu ou apareceu sem registro (jogador, defeito) | M |
| T4 | **Tentativas por item** | Só no script: o motivo de parada que o diário já tem (`CRAFTED`/`SMELTED` com `detail`) somado por item | Seção no `action_report.py`: por item, feitas, falhas por motivo | P |
| T5 | **Noite e saúde** | Linha diária por aldeão `VC_VILLAGER_DAY`: horas no expediente, dormindo, acordado fora do expediente, sem cama, vida mínima, refeições, morte e causa | Seção no `state_report.py` | P |
| T6 | **Um relatório só** | `game_report.py` roda todos os relatórios no log mais recente e entrega um resumo único, com os 10 achados mais graves ordenados (Regra 51, travas, balanço que não fecha, tarefas lentas) | Um arquivo por jogo em `docs/testing/relatorios/` | M |

### Cuidados

- **Volume:** T2 grava só na mudança de estado (estimativa: algumas dezenas de linhas por aldeão por hora). T3 e T5 são
  uma linha por dia. Medir no `VC_COST` antes e depois.
- **Desempenho:** nada lê o mundo a mais, salvo o estoque do início do dia em T3 (a leitura do `VC_BLOCKS` já faz).
- **Regras do projeto:** só leitura e registro; nenhum comportamento muda. Cada peça com teste (o formato da linha e a
  conta do script), como nas Regras 342 e 343.

## 3. Ordem recomendada

1. **T1 e T4** (pequenas, só diário e script): já dão o tempo de cada tarefa e as falhas por item.
2. **T2**: responde "quem ficou parado, onde e por quê", a pergunta mais comum das triagens.
3. **T3**: fecha a conta dos itens e mostra o que some (baú cheio destruindo material, Regra 52).
4. **T5** e **T6**: a noite e o relatório único.
