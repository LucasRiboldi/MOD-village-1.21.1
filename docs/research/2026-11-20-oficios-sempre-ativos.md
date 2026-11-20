# Ofícios sempre ativos — fundidor e carpinteiro (2026-11-20)

Pedido do autor, 2026-11-20: opções que mantenham as profissões trabalhando, ou numa atividade importante do próprio
ofício, com menos tempo parado e sem laço de erro e bloqueio. Base: jogo da 0.4.93 em planície (colônia ccac8347,
~3 h; `2026-11-19-2.log.gz` e `villagecolony-actions-20261119-140317.jsonl`).

## 1. O que o jogo mostrou

`time_ledger.py`, expediente:

| Ofício | Trabalho | Andando | Espera | Ocioso |
|---|---|---|---|---|
| Fundidor | 15% | 38% | 34% | 12% |
| Carpinteiro | 36% | 21% | 31% | 11% |

`WorkTime` conta espera quando o aldeão **tem tarefa** e não golpeia, não anda e não tem destino.

- **Fundidor.** O pedido de `iron_ingot` ficou aberto sem minério nos baús (o mineiro parado; Regra 336). O log tem 68
  linhas `stopped — none of N colony chests had … iron_ore …`, somando ~28 mil tiques parados. O código segura o
  pedido de propósito: "a espera por cru é transitória: manter a reserva" (`SmelterWork`). Enquanto segura, as
  alternativas de quem está sem tarefa não rodam.
- **Carpinteiro.** 170 tarefas, nenhuma solta, quase todas com 0 ou 1 peça: 68 param por "half of the colony's wood
  stays in logs" (reserva de tora, Regra 257) e 25 por "no logs left". O `TaskChain` reabre o mesmo pedido assim que
  o anterior faz 1 peça ("chained the next … — the shortage remains", 63 vezes), e o guarda de laço registra 15
  `VC_LOOP`. Entre pegar e fazer a primeira peça passam 260 a 580 tiques; a ida à bancada tem prazo de 200.

## 2. O que já existe e não roda

O ofício sem tarefa já tem o que fazer:

| Alternativa | Quem | O que faz |
|---|---|---|
| `ValueGoods` (Regras 133, 146) | os dois | carpinteiro: tábua, escada e laje da tora que sobra; fundidor: funde minério, assa carne, faz pedra, pedra lisa, vidro, vidraça, tijolo e terracota |
| `SmelterAdvance` | fundidor | adianta blocos intermediários até `ItemStock.EACH` |
| `SmelterGathers` | fundidor | busca a areia e a argila que faltam |
| `BuildingUpkeep` (Regra 221) | os dois | visita uma construção por vez e gasta material de manutenção |

Todas exigem o titular **sem tarefa**. Quem segura a tarefa impossível nunca chega a elas.

## 3. Opções

| # | Opção | O que muda | Risco |
|---|---|---|---|
| O1 | **Tarefa viável ou nenhuma.** A mesma pergunta que hoje faz o ofício parar passa a valer antes de dar a tarefa, antes de encadear e para segurar. Fundidor: pedido de fundição sem o cru (nem o cru do cru) em nenhum baú não é dado nem segurado; volta quando o cru aparecer. Carpinteiro: o `TaskChain` não encadeia quando a parada foi pela reserva de tora ou pela falta de tora, e o pedido de madeira que a reserva barra não é dado. | Livre, ele cai nas alternativas da seção 2. Nada novo para fazer, só para de travar. | O pedido de ferro fica sem dono enquanto não há minério; hoje também não anda, mas prende o fundidor. |
| O2 | **O1 mais uma atividade nova por ofício.** Carpinteiro: descasca tora para a obra (no jogo da montanha a tora descascada surgiu ×30 por falta dela) — continua sendo tora, então a reserva da Regra 257 fica intacta. Fundidor: assa a comida crua do baú primeiro (já está no `ValueGoods`; sobe na ordem quando a vila tem fome). | Mais trabalho útil quando o estoque de valor já está cheio. | Duas receitas a mais no `ValueGoods`; medir para não encher baú. |
| O3 | **Guarda de progresso geral.** Toda tarefa sem progresso por N tiques de expediente é solta, e o mesmo pedido não volta ao mesmo trabalhador até a causa mudar (o baú, o alvo). Vale para todos os ofícios. | Pega os laços que a O1 não conhece. | Castigo por pedido tende a virar rodízio (`castigo-por-oficio-vira-rodizio`); precisa da causa, não só do tempo. |
| O4 | **Medida.** A espera passa a sair dividida por motivo no `VC_TIME` (sem cru, reserva, ida à bancada, outro), e cada alternativa usada conta no `action_report`. | Prova a causa e o efeito no próximo jogo. | Só mede. |

## 4. Recomendação

O1 com a O4: corrige a raiz (a tarefa impossível que prende o ofício) e deixa entrar o que já foi feito para o tempo
livre; a medida prova no próximo jogo se a espera caiu e para onde o tempo foi. A O2 entra depois, se a medida mostrar
o titular livre e sem o que fazer. A O3 só depois de a O1 mostrar quais laços sobram.

Pela regra temporária (Regra 330), nenhuma opção põe um ofício no trabalho de outro.

Escolha do autor (2026-11-20): O1 + O4, fase 1 de 4. Implementada na 0.4.103 (Regra 339). As fases seguintes trocam
a reserva de blocos por classe (CRU até 256, FABRICADO 4, VILA 12, ESPECIAL 1; ADR-062), registram os blocos
(`VC_BLOCKS`, `/vc blocos`) e medem as travas pequenas (`VC_KEEP`, `stock_report.py`).
