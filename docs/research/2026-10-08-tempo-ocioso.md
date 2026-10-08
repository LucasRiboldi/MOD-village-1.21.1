# Tempo ocioso das profissões — análise e alternativas (2026-10-08)

> Pedido do autor: *"analisar o tempo ocioso de todas as profissões e dar alternativas para
> nenhuma ficar mais de 15% do tempo ociosa ou bloqueada, como regra"*. Fontes: `time_ledger.py`
> das sessões de 10:01 (0.3.12) e 15:14 (0.3.16), `action_report.py` e o log. **Isto é análise e
> proposta; nada aqui foi implementado.**

## A régua proposta (Regra 51)

No expediente, **ocioso + bloqueado ≤ 15%** por profissão, medido pelo `time_ledger.py`.
- *ocioso* = sem tarefa; *bloqueado* = com destino e parado 3 amostras seguidas.
- *espera* (com tarefa, sem destino) fica fora da régua, mas acima de 40% vira alerta — é ela que
  esconde o defeito do fabricante abaixo.
- Hoje a Regra 50 avisa a partir de 40% sem trabalhar e 10% de bloqueio; a 51 aperta para 15% somados.
- **Quem fica sem tarefa entra numa cadeia de alternativas, nesta ordem:** (1) tarefa do próprio
  ofício; (2) manutenção do ofício (lista por profissão abaixo); (3) ajudar a obra aberta; (4)
  recolher do chão o que a obra espera (`GroundPickup`, já existe). Nunca "parado esperando".

## Medido (sessão de 15:14, 36 min)

| profissão | ocioso | bloqueado | soma | espera | régua 15% |
|---|---|---|---|---|---|
| Fundidor | 98% | 0% | **98%** | 0% | ❌ |
| Construtor | 82% | 14% | **96%** | 0% | ❌ (parte é a troca de ofício do construtor) |
| Pastor | 61% | 0% | **61%** | 18% | ❌ |
| Lenhador | 15% | 31% | **46%** | 4% | ❌ |
| Fazendeiro | 19% | 0% | **19%** | 39% | ❌ |
| Mineiro | 9% | 0% | 9% | 35% | ✅ (mas 0 pedras) |
| Pedreiro | 9% | 1% | 10% | **51%** | ✅ (espera alta) |
| Carpinteiro | 8% | 1% | 9% | **52%** | ✅ (espera alta) |
| Sem ofício (9) | 100% | — | 100% | — | fora da régua (M3) |

## Causa e alternativas, por profissão

### Carpinteiro e pedreiro — espera 51–52% (defeito, não desenho)
- **Causa:** a tarefa seguinte é entregue na hora (`TaskChain` + `IdleHands`), e a entrega chama
  `runOngoingWork` para ele começar a andar — mas essa lista não tem o `CraftingWork`. O trabalho de
  fabricar só nasce no ciclo da colônia, até 30 s depois: a mediana por tarefa é 30 s.
- **Alternativa A1 (recomendada):** pôr `CraftingWork.run` no `runOngoingWork`. Uma linha; a espera
  deve cair para perto de zero e a produção subir.

### Fundidor — 98% ocioso
- **Causa:** sem minério de ferro (o mineiro não chegava à pedra). A trava `SMELT_INPUT` acabou com
  a roda, mas sem cru não há o que fundir.
- **A2:** manutenção do ofício — fundir o que a vila tem de sobra e as obras usam: pedregulho → pedra
  (havia 339 pedregulhos), pedra → pedra lisa, areia → vidro, e **tronco → carvão vegetal** quando o
  carvão está baixo (tochas e combustível).
- **A3:** ajudar a obra (generalizar o `BuildHelper` do pastor para todo ofício produtor ocioso).

### Construtor — 82% ocioso + 14% bloqueado
- **Causa:** a obra parou 20 min esperando lampião e corrente (resolvido em 0.3.18) e, entre obras,
  ele espera a busca de lote. O registro mistura a troca de ofício do construtor 9353ad2c.
- **A4:** obra esperando material → ele prepara o próximo lote (limpar o canteiro, aterrar a base,
  `SitePreparation`/`FoundationPreparation`) ou calça a rua (`RoadPaving`).
- **A5:** investigar por que o construtor da obra trocou de ofício no meio da sessão.

### Pastor — 61% ocioso
- **Causa:** pedidos de lã curtos e o rebanho cheio (E7, por desenho). A ajuda na obra só abre com o
  construtor trabalhando e obra com 8+ peças.
- **A3** (ajuda na obra para todos) e **A6:** manutenção do ofício — levar animais soltos ao curral
  (`ShepherdHerding`, existe), procriar galinha/porco/vaca até o teto, tosquiar por estoque (lã de
  reserva até um teto) em vez de só por pedido.

### Lenhador — 31% bloqueado
- **Causa:** árvores a 48–134 blocos; a navegação Vanilla não traça caminho tão longo e para no meio.
  O bosque de 3 mudas (0.3.16) ainda não cresceu.
- **A7:** caminhada por pernas para alvos longe (pontos a cada ~24 blocos), como o mineiro já faz.
- **A8:** bosque maior (3 → 6 mudas) e farinha de osso nas mudas do bosque para crescerem logo.

### Fazendeiro — 19% ocioso (e 39% espera)
- **Causa:** a roça é pequena e a lavoura cresce devagar; sem maduro nem canteiro vazio, ele
  descansa. O feno da obra (0.3.16) já não depende dele.
- **A9:** manutenção do ofício — arar e semear canteiro novo ao lado da roça quando houver semente,
  até um teto (roça maior = mais trigo, mais feno, mais comida).
- **A3** (ajuda na obra).

### Mineiro — 0 pedras
- Não é ócio (anda 57%): é a descida, corrigida em 0.3.17. Medir de novo antes de mexer.

### Sem ofício — 9 aldeões 100% ociosos
- Fora da régua por decisão do M3 (não criar profissão só por ociosidade). **A10:** se o autor
  quiser, eles entram na cadeia como ajudantes de obra (A3) — sem criar ofício.

## Recomendação (ordem)
A1 (defeito, uma linha) → A3 (ajuda na obra para todo ofício ocioso, regra comum) → A2 (fundidor) →
A9 (fazendeiro) → A7/A8 (lenhador) → A4 (construtor) → A6 (pastor). Depois, a Regra 51 entra no
`time_ledger.py` como veredito, e o playtest seguinte prova o efeito.

## Feito em 0.3.19 (escolha do autor)

- **Regra 51** no `time_ledger.py` (veredito `REGRA 51 (N% ocioso+bloqueado)` e `ESPERA (N%)`) e no
  `CLAUDE.md` §0.4.
- **A7** caminhada por pernas (`WalkLegs`, 24 blocos na superfície) para o lenhador.
- **A8** bosque de 6 mudas com farinha de osso a cada conferência (`VillageGrove`).
- **A4** do jeito do autor: o construtor sem obra calça a rua — grama ou terra no meio do caminho
  vira caminho (`BuilderPaving`); conta como trabalho no `time_ledger`.

## Novas alternativas para escolher

| # | Profissão | Alternativa |
|---|---|---|
| A1 | Carpinteiro, pedreiro | **Defeito:** a entrega entre ciclos não liga o `CraftingWork`; espera de até 30 s por tarefa (51–52%). Uma linha. |
| A3 | Todos os produtores | Ajudar a obra aberta no tempo livre (o pastor já faz; vale para fundidor, fazendeiro, lenhador, mineiro sem ramal), com os 10 blocos mínimos. |
| A2 | Fundidor | Fundir sobra que a obra usa: pedregulho → pedra, pedra → pedra lisa, tronco → carvão vegetal quando o carvão está baixo. |
| B1 | Fundidor | Recolher areia e argila no tempo livre para estoque (até um teto), não só por pedido. |
| A9 | Fazendeiro | Arar e semear canteiro novo ao lado da roça quando houver semente, até um teto. |
| B2 | Fazendeiro | Farinha de osso na lavoura verde para o trigo amadurecer antes (feno e comida). |
| A6 | Pastor | Manutenção do rebanho: levar animais soltos ao curral, procriar até o teto, lã de reserva até um teto. |
| B3 | Pastor | Ajudar o fazendeiro a colher quando há maduro e o fazendeiro está longe. |
| B4 | Construtor | Preparar o lote da próxima obra (limpar o canteiro, aterrar a base) enquanto espera material. |
| B5 | Mineiro | Sem ramal livre: abrir o poço do nível seguinte, ou raspar pedra exposta perto da vila (já existe; dar prioridade). |
| A10 | Sem ofício (9) | Entram na cadeia como ajudantes de obra, sem criar profissão (M3). |
| B6 | Geral | Carregador: levar o que a obra pede dos baús profissionais ao baú da obra (o construtor anda menos). |
