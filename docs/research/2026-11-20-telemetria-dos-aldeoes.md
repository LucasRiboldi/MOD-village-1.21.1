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

## 4. Próximas telemetrias (pedido do autor, 2026-11-20; nenhuma implementada)

Em ordem de valor para a análise:

- **A. Movimento:** A1 eficiência da caminhada (andado contra a reta); A2 custo de logística (blocos andados por item
  entregue); A3 falhas de navegação (caminho nulo, parcial ou recalculado, por alvo e lugar); A4 mapa de calor das
  paradas e encalhes sobre o `village_map.py`.
- **B. Fila:** B1 latência da tarefa aberta até ser pega; B2 por que ninguém pegou (motivo de cada recusa na
  distribuição); B3 pedidos que nunca acabam.
- **C. Produção:** C1 produção por hora por ofício; C2 curva da vila por dia; C3 ritmo da obra; C4 comida produzida
  contra comida comida.
- **D. Mundo:** D1 edições do mod por tipo e área; D2 segurança (dano, monstros, ataques); D3 ferramentas.
- **E. A própria medida:** E1 manifesto do jogo (versão, semente, bioma, duração, presença do jogador, outros mods);
  E2 comparação com o jogo anterior no `game_report.py`; E3 contador de erros e avisos; E4 custo da telemetria.
- **F. Interferência do jogador** (o `PlayerWorldChangeHandler` e o `PlayerGround` já percebem as edições; falta medir):
  F1 edição por alvo (construção, obra, rua do grafo, chão do Zelador, mina, curral, plantação, bancada, baú, peça
  Vanilla, terreno natural), `VC_PLAYER_EDIT`; F2 o jogador desfaz o mod; F3 o mod desfaz o jogador; F4 intenção pela
  forma (caminho, cerca, plantio, luz, casa própria, limpeza); F5 o jogador nos baús e bancadas; F6 o jogador com os
  aldeões; F7 comandos, obras pedidas e cancelamentos; F8 notas cruzadas com as edições; F9 presença do jogador.

### F3b. A reafirmação do jogador (ponto de atenção)

Pedido do autor, 2026-11-20. É o lado 2 do conflito levado um passo adiante, e o sinal mais forte da intenção dele:

1. o **jogador** deixa um bloco num estado (põe, tira ou troca);
2. o **mod** muda esse bloco (reparo, calçada, rua, nivelamento, aterro, colheita, obra);
3. o **jogador** volta o bloco ao estado que ele tinha deixado.

O passo 3 é a **reafirmação**: o jogador insistiu. A edição dele é o desenho que ele quer, e o sistema do mod que a
desfez é o que precisa mudar.

- **Como medir.** Guardar, por posição, a última edição do jogador (estado, hora) e a última mudança do mod (sistema,
  hora). Quando o jogador repõe o estado que ele tinha deixado e, entre as duas edições dele, o mod mudou o bloco, sai
  `VC_PLAYER_INSIST` com: lugar, bloco, sistema do mod que desfez, tempo entre os passos e quantas vezes a mesma
  posição (ou o mesmo trecho de 5 × 5) já foi reafirmada. Vale dentro de um dia de jogo entre os passos.
- **Quem é "o mod".** Toda escrita de bloco do mod passa a levar o nome do sistema (obra, reparo, Zelador, rua,
  mina, fazenda, lenhador), num registro curto por posição; mudança sem dono conhecido sai como `mundo` (fogo,
  gravidade, crescimento), para não culpar o mod pelo que não fez.
- **Peso.** Uma reafirmação é atenção; duas no mesmo lugar, ou três do mesmo sistema no dia, é achado 🔴.
- **Destino.** O `game_report.py` põe as reafirmações no topo dos achados, numa seção **"Para corrigir — o jogador
  insistiu"**, com o sistema, o lugar, o bloco e a intenção provável (F4: janela aberta numa parede, caminho próprio,
  cerca, canteiro, luz). Na triagem, cada uma vira item do `TODO.md` com a evidência.
- **Teste.** O jogador quebra um bloco de parede de uma construção, o reparo o repõe, o jogador quebra de novo: sai uma
  reafirmação com o sistema `reparo`. O jogador quebra pedra natural duas vezes, sem o mod no meio: nada sai.

Escolhas do autor (2026-11-20): T1 a T6, implementadas na 0.4.108 (Regra 344); F1, F2, F3 e a reafirmação F3b, implementadas na 0.4.109 (Regra 345). As outras de §4 seguem abertas.
