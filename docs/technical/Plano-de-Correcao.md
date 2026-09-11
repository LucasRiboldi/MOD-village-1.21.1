# Plano de Correção — 2026-09-11

**Autor do plano: o autor do projeto.** Registrado aqui na íntegra em
2026-09-11, sobre o commit base `9385db6`, para que a execução não dependa
de ninguém lembrar do que foi combinado.

Este arquivo é o **roteiro**; a lista canônica do estado continua sendo o
[`TODO.md`](../../TODO.md). Onde os dois discordarem sobre *o que já foi
feito*, vale o TODO; sobre *o que fazer e em que ordem*, vale este.

---

## A régua (não negociável)

- **Ter teste verde ≠ estar pronto.** Todo conserto precisa ser visto em jogo.
- **Não tratar sintomas isoladamente.** Corrigir a cadeia:
  `descobrir → planejar → atribuir → executar → entregar → atualizar estado`.
- Cada etapa precisa de **motivo explícito de falha**, **cooldown quando
  necessário**, e **teste que reproduza o comportamento real**.
- **Nunca** aumentar raio, limite ou `tickLimit` só para fazer teste passar.
- Uma falha de **um** lote não significa "não existe lote".
- Uma falha de **uma** tarefa não significa "trabalhador burro".
- Não abrir profissão nova enquanto P0 não estiver verde.
- Não escrever teste de unidade onde o defeito é de integração — o
  `gauntlet-verifier` já reprovou duas entregas "prontas" que passavam na
  bateria e falhavam em jogo.

**Antes de começar:** rodar a bateria. Verde? seguir. Vermelha? parar e reportar.

---

## P0 — Bloqueadores

Um por vez, com teste antes de seguir.

### P0.1 — O planejador não acha lote

**Sintoma:** `no building work: nothing to work on in the whole radius`.
281 de 517 ciclos com `assigned 0 tasks`. Motivo nº 1:
`no carpenter work: no task open for it`, 67 vezes.

- Instrumentar a busca por obra: logar, **por candidato no raio**, o motivo
  da rejeição — fora de alcance, ocupado, terreno, recursos, planta, estado
  da obra, estado interno.
- O planejador deve avaliar **vários** candidatos, não encerrar no primeiro
  conjunto inválido.
- Transformar `nothing to work on` em *"rejeitei 12 lotes por X, Y, Z;
  candidato C válido"*.
- **Não aumentar o raio.** Primeiro descobrir por que rejeita.

**Teste:** colônia com recursos e área livre → pelo menos 1 lote válido.
**Critério:** log com motivo por candidato; nenhum `nothing to work on`
quando há lote válido.

### P0.2 — A colônia lê 1 de 8 baús

**Sintoma:** `in 5 of 16 chests read` (09-10) → `in 1 of 8 chests read`
(11 set). **Piorou.**

- Criar `ColonyChestScanReport`: `chestsInRadius`, `chestsScanned`,
  `chestsSkipped`, `chestsFailed`, `itemsObserved`, `scanStartTick`,
  `scanEndTick`, `scanCompleted`.
- `scanCompleted=false` é o alarme. Envolver a varredura em `try/finally`.
- `ColonyChestCache` com invalidação **por evento** — bloco quebrado ou
  colocado, baú aberto ou fechado, trabalhador move item —, não por tique.
  `MAX_STALENESS_TICKS = 200–400` como válvula.
- Varredura em fila: `CHESTS_PER_TICK = 4`. Não ler tudo num tique.
- Log por ciclo:
  `[Colony/chest] cycle=N in=X scanned=Y skipped=Z failed=W items=V duration=Tt completed=B`
- **Não aumentar a frequência de leitura.** Se lê 1 de 8, ler 20×/min
  continua 1 de 8.

**Teste:** `chestsScanned == chestsInRadius` quando `scanCompleted=true`;
nenhuma linha `completed=false` com `scanned < in`.
**Critério:** 8 encontrados → 8 válidos → 8 lidos → 8 contabilizados.

### P0.3 — A cadeia mineiro → armazenamento → fundidor

**Sintoma:** fundidor `nothing in the colony chests to smelt` (34×),
mineiro entregando 1 pedra em 30 minutos.

- **Não mexer na lógica do fundidor ainda.**
- Instrumentar a cadeia: quantos itens fundíveis existem nos baús no momento
  da falha?
- Correlacionar coleta, depósito, leitura do baú e decisão de fundição.
- Zero fundíveis → o fundidor está certo, e é sintoma do P0.2.
- Só alterar o fundidor se a cadeia anterior estiver comprovadamente correta.

**Critério:** prova de que a cadeia está correta, **ou** prova de onde ela quebra.

### P0.4 — O lenhador corta a parede da própria vila

**Sintoma:** `Not a tree — N logs without a living canopy` sobre troncos de
casa. 118 rejeições sobre 28 posições; redescobre a mesma parede 6×.

- Conjunto de posições rejeitadas **por trabalhador**.
- Consultar antes de tentar. Rejeitada N vezes → cooldown.
- Cooldown progressivo: 6.000 → 12.000 → 24.000 → 48.000 ticks.
- Durante o cooldown a posição **não é candidata**.
- Invalidação, não lista negra permanente: bloco mudou, ambiente mudou, ou
  cooldown expirou.
- **Não persistir entre sessões.**

**Teste:** a mesma posição não é reavaliada antes do cooldown; redescobertas ≤ 2.
**Critério:** 118 rejeições caem para poucas.

### P0.5 — E3: perda de item por inventário cheio

- Coleta → inventário cheio → tenta depósito → falha → procura armazenamento
  válido → deposita → continua.
- Sem armazenamento: retorna a um conhecido; impossível → estado de espera.
- **Nunca destruir o item.**

**Critério:** o recurso é preservado.

---

## P1 — Estabilizar comportamento

Depende de P0 verde.

| | o quê |
|---|---|
| **P1.1** | **Implementar a ADR-009 §3.6** — a decisão mais importante. Abandonar tarefa que não progride, trocar de objetivo, voltar depois. Arquitetura `Task → Attempt → Progress?` — sim continua, não → motivo de falha → cooldown → outra tarefa. A Regra 28 vira **diagnóstico**, não inteligência definitiva. Vem antes do P1.2, porque o descanso depende do abandono funcionar |
| **P1.2** | **Honrar o descanso de quatro ciclos (E43).** `giveUp()` = desistir daquela tarefa; não pode recebê-la de volta por 4 ciclos. Sem outra: ocioso, e **ociosidade é aceitável**. Documentar no `WorkStall` qual das duas regras vale — *"só se aplica quando há alternativa"* ou *"estende-se se a única tarefa é a mesma"* |
| **P1.3** | **Mineiro longe do corredor: devolver a tarefa.** Nada de linha reta — NPC preso, atravessa montanha, cai em buraco. Estado explícito de *perdido*, independente do planejador que o levou lá. Cooldown para aquela ordem/localização |
| **P1.4** | **Lenhador: consertar o travamento**, não ressuscitar a mão emprestada. Comparar `assign()` do mineiro e do lenhador lado a lado antes de portar. Criar gancho de teste equivalente ao `shortenStallLimitTo`. GameTest ponta a ponta: força stall, abandona, entra em cooldown, escolhe outro objetivo |
| **P1.5** | **Agricultor: busca progressiva + cooldown.** Não 32 → 64 direto: 0–16 → 16–32 → 32–48 → ocioso. Procurou, nada, cooldown, comportamento secundário |
| **P1.6** | **Aceitar espécies misturadas na casa.** Identidade de material vira **preferência**, não barreira |
| ~~**P1.7**~~ | ❌ **NÃO SE FAZ — refaria o defeito de 2026-09-10.** A premissa é que cem toras de cerejeira não deviam satisfazer "Oak Log × 20". Mas **elas satisfazem de verdade**: `INTERCHANGEABLE_IN_THE_WALL` já traz `WOOD`, `PLANKS`, `STONE` e `STRIPPED`, então a cerejeira substitui o carvalho na parede pela Regra 27, e o `MaterialChoice` descasca cerejeira para planta de carvalho desde 09-05. A conta por grupo e o construtor **concordam** — e é essa concordância que importa. O javadoc do `ResourceSubstitution` guarda a lição, que custou uma sessão: *"a colônia concluía que a meta estava cumprida e o mineiro não ia cavar, enquanto o construtor esperava pelo arenito. O defeito era a **discordância** entre a conta e o construtor, e não a substituição em si"*. Separar `WOOD` por espécie recria exatamente isso, do outro lado: a conta passaria a exigir carvalho enquanto a parede aceita cerejeira. **Este item também contradiz o P1.6** do próprio plano, que manda aceitar espécies misturadas — e o P1.6 é o que já está implementado |
| ~~**P1.8**~~ | ❌ **NÃO SE FAZ — o campo não está morto.** Conferido em 2026-09-11: `furniture()` é o **primeiro critério de ordenação** da obra em `StructureBlueprintReader`, e o javadoc de lá diz por quê — *"a ordem de baixo para cima garante o que está embaixo, e mobília depende do que está ao lado; só a casa inteira responde isso"*. Removê-lo faria a cama e a mesa serem assentadas junto com a parede. A premissa do item era que a morte da Regra 21 o tinha deixado sem dono; ele mudou de dono, e o dono novo é a ordem de construção |
| **P1.9** | **Regra 25: não mexer agora.** Só ganha sentido com múltiplas plantas, depois da §3.6 |
| **P1.10** | ✅ **feito em 2026-09-11.** `theNeighbourhoodDeadEndIsNotTheTunnelCursorsQuestion`: três recusas em posições **já cavadas** enchem o cubo em volta de uma fronteira **sem marca própria**. Duas asserções de montagem garantem que o cenário discrimina de verdade, e a mutação que o Verifier provou que passava pela bateria inteira agora derruba este teste e só ele |
| **P1.11** | ⚠️ **medido em 2026-09-11, e é maior que esta linha.** A arena não é só rasa: ela vive em **y=-58..-52** (medido no log da bateria), o mundo acaba em -64, e **`MineShaft.DEEPEST = -59`** já proíbe aprofundar nessa faixa. Um nível custa 20 blocos, então a sala 2 cairia em **-76** — fora do mundo, não só fora da arena. E a altura da arena é do runner de gametest, que o mod não controla: não há `vmArg` nem configuração no `build.gradle`. **O caminho que resta** é um gancho de teste que encurte a descida entre níveis, no molde do `sandRadius` e do `searchRadius` que o projeto já tem — com 4 blocos por nível a sala 2 cai em -60 e cabe. Não é afrouxar limite para passar; é encurtar geometria para caber. Mas mexe no `MineShaft`, que é Core e governa a forma da mina inteira, então é trabalho próprio e não uma linha |
| **P1.12** | ✅ **feito em 2026-09-11, com uma parte recusada.** `TestWorkers` dá os dois helpers com a diferença no nome, e um teste prova que ela é observável. Os 26 call-sites de mineiro e lenhador passaram a equipar — e a bateria seguiu verde, então **nenhum dependia da mão nua**. ❌ **A asserção defensiva no `assign()` não entra:** na produção o `assignMissing` roda antes do `equip`, no mesmo ciclo, então mão vazia ali é o desenho — recusá-la quebraria a contratação inteira |
| **P1.13** | **E41: teste de degradação longa.** 200+ ciclos, amostra por ciclo, falha **por tendência** e não por valor absoluto. Precisa de fonte de trabalho infinita (`TestWorkSource`), senão mede "nothing to work on" — que é o P0.1 — e não degradação. O `chestScanCompleted` vem do P0.2: as frentes andam juntas |

---

## P2 — Performance

**P2.1 — O ciclo da colônia em 112 ms** (limite: 50 ms).

Instrumentar **antes** de otimizar, por subsistema: planner, chest scan, task
assign, workers, persistence, other. A regra é *nem tudo em todo ciclo* —
planejamento de obras a cada X ciclos, scanner com cache incremental, seleção
de tarefas quando necessário, IA do trabalhador em frequência normal.

Só otimizar de verdade se passar de 100 ms ou o TPS cair.

---

## P3 — Confiabilidade

| | o quê |
|---|---|
| **P3.1** | Extrair `VillagerMigration`, `VillagerDataVersion` e `ColonyMigrationStats`. Comando `/colony debug migration [summary\|reset\|inspect\|migrate <radius>]` e `/colony debug chest scan` |
| **P3.2** | Teste de migração com NBT sintético: sem profissão + POI perto → `MANUFACTURER`; sem POI + `LastProfession` → usa a última; sem nada → `ColonyNeedsRehire`; já migrado → não toca; `DataVersion` gravada de volta. `PoiLookup` injetável |
| **P3.3** | Rodar o `gauntlet-verifier` depois de P0 e de P1 |

---

## Baixo — gatilhos, não frentes

1. Save antigo perde `MANUFACTURER` → só com bug report real.
2. Achados *medium* em `test_gauntlet.py` → só se o gauntlet for reescrito.
3. Ciclo de 81 ms → já coberto pelo P2.
4. Escada solta/retomada → é o E44, tratar junto com o P1.4.
5. Picareta de ouro vs diamante → documentar, não corrigir.
6. Falta de lint → só se a dívida passar de 20 arquivos acima de 500 linhas.

---

## Ordem de execução

```text
bateria verde
  → P0.1 → P0.2 → P0.3 (investigação) → P0.4 → P0.5
  → PARAR E REPORTAR
  → P1.1 → P1.2 → P1.3 → P1.4 → P1.5 → P1.6 → P1.7 → P1.8
  → P1.10 → P1.11 → P1.12 → P1.13
  → P2.1
  → P3.1 → P3.2 → P3.3
  → gauntlet-verifier
```

**Um item por commit**, com mensagem no formato
`P0.1: <o que mudou> (<teste que prova>)`. Não agrupar P0.1 com P0.2.

---

## Regras finais

- Se um teste passa só por folga de `tickLimit`, **o teste está errado**.
- Se um teste mede mão nua, **o teste está errado**.
- Se um teste unitário cobre comportamento de integração, **o teste está errado**.
- Se foi preciso aumentar um limite para o teste passar, **o teste está errado**.
- Se o defeito some quando a frequência aumenta, **o defeito não sumiu**.
- Se não dá para reproduzir em jogo, **não foi consertado**.

---

## Estado da execução

Atualizado a cada item entregue. `✅` só entra com bateria verde **e**
mutação conferida; a coluna *em jogo* é a que a régua cobra.

| item | estado | em jogo |
|---|---|---|
| P0.1 | ✅ entregue em 2026-09-11 — o índice de ruas voltou a valer para vila grande | ✅ **visto na sessão de 02:03**: a colônia planejou o açougue e abriu a obra |
| **P0.1-b** | ✅ **o caminho de terra não sai de baú.** A obra do P0.1 abriu e travou em `waiting for minecraft:dirt_path`, 30 vezes — o bloco não tem item, nasce de pá na grama. Uma linha no `isShapedFromTheGround`, que já cobria `farmland` e `water` pelo mesmo motivo | ❌ |
| **P0.1-c** | ✅ **a recusa de lote diz por quê.** `LotRefusals` conta os cinco motivos do `flatGroundAt` e o relatório de sessão os diz junto do `SweepLog`. É o P0.1 ao pé da letra, e é o número que decide a terraplanagem | ⬜ **espera sessão** |
| **P0.7** | ⬜ **terraplanagem da vila** — pesquisada em [`terraplanagem-da-vila.md`](../research/terraplanagem-da-vila.md), decidida (capacidade do construtor, teto de 4 blocos) e **represada de propósito**: abre com o número do P0.1-c, não com inferência | — |
| **P0.6** | ✅ **a enxurrada da areia calou.** Amortecedor de um ciclo no `IdleLog`, para quem pergunta por tique: motivo que oscila deixa de virar quatro linhas por segundo. O irmão da pedra de superfície foi junto | ⬜ **espera sessão** |
| ~~P0.6~~ | 🔴 ~~**novo, e é bloqueador de diagnóstico:** a busca de areia oscila entre *"ainda varrendo"* e *"varri tudo"* a cada passagem, e o `IdleLog` registra as duas. **4.389 linhas de 6.117 na sessão de 02:03** — 72% do log. A próxima sessão fica cega~~ | — |
| P0.2 | ⬜ | — |
| P0.3 | ⬜ | — |
| P0.4 | ⬜ | — |
| P0.5 | ⬜ | — |
| P1.1 – P1.13 | ⬜ | — |
| P2.1 | ⬜ | — |
| P3.1 – P3.3 | ⬜ | — |
