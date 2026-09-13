# Plano de Correção — 2026-09-11

**Autor do plano: o autor do projeto.** Registrado aqui na íntegra em
2026-09-11, sobre o commit base `9385db6`, para que a execução não dependa
de ninguém lembrar do que foi combinado.

Este arquivo é o **roteiro**; a lista canônica do estado continua sendo o
[`TODO.md`](../../TODO.md). Onde os dois discordarem sobre *o que já foi
feito*, vale o TODO; sobre *o que fazer e em que ordem*, vale este.

> **A varredura do plano contra o código** — feita em 2026-09-11 para
> descobrir quais itens já estavam vencidos antes de virar trabalho — está
> em [`varredura-do-plano-2026-09-11.md`](varredura-do-plano-2026-09-11.md).
> O placar final dela: **12 itens vencidos contra 7 entregues.** A lição
> mais útil do ciclo, e a razão de o plano ser relido antes de virar
> trabalho.


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

✅ **Entregue em 2026-09-11.** O índice de ruas voltou a valer para vila
grande; o `fits()` recusava índice acima de 1.024 colunas, e o preço era
pago pela vila que cresceu. Em lugar do teto, paginação com cursor.
**Visto em jogo na sessão de 02:03:** a colônia planejou o açougue e
abriu a obra.


### P0.1-b — O caminho de terra não sai de baú

**Sintoma:** `Builder stopped — no minecraft:dirt_path in the colony
chests`, 31 vezes na sessão de 02:03, com a obra em `WAITING_RESOURCES`
outras 30.

**Causa:** o `dirt_path` não tem item. No jogo ele nasce de uma pá batendo
na grama, e quebrado devolve terra. A obra esperava por ele para sempre.

**Correção:** uma linha no `isShapedFromTheGround`, que já cobria
`farmland` e `water` pelo mesmo motivo.

✅ **Entregue em 2026-09-11.** ⬜ **Espera sessão.**


### P0.1-c — A recusa de lote diz por quê

**Sintoma:** o `SweepLog` dizia "não há lote" quando o que ele sabia era
"não terminei de olhar".

**Correção:** `LotRefusals` conta os cinco motivos do `flatGroundAt`. É o
P0.1 ao pé da letra, e é o número que decide a terraplanagem.

✅ **Entregue em 2026-09-11.** O número chegou na sessão de 09-12: das
6.583 recusas, **4.578 (70%) são `NOT_NATURAL_GROUND`** — pedra não entra
como solo natural. ⬜ **Espera decisão do autor — ver P0.7.**


### P0.2 — A colônia lê 1 de 8 baús

❌ **VENCIDO NA PREMISSA — a varredura tinha lido os oito.** Conferido pelo
P0.0 em 2026-09-11, e é o achado que mais justifica a varredura.

A linha é montada assim, em `VillageDetectionHandler.logResources`:

```java
"Colony {} stores {} in {} of {} chests read{}",
    colony.id(), stock,
    resources.byChest().size(),   // baús COM CONTEÚDO
    survey.chestsRead(),          // baús LIDOS

Os dois números são baús com conteúdo e baús lidos — nunca uma
fração de cobertura. in 1 of 8 chests read quer dizer "um baú tinha
alguma coisa, dos oito que li". O javadoc de ChestSurvey diz isso em
tantas palavras: "chestsRead — baús alcançados, incluindo os que
estavam vazios".

O que ficava pendurado nessa premissa: ColonyChestScanReport,
ColonyChestCache com invalidação por evento, CHESTS_PER_TICK = 4,
MAX_STALENESS_TICKS e o log por ciclo. Nada disso tem defeito que o
justifique — o alarme que eles construiriam (scanCompleted=false) já
existe como ChestSurvey.isPartial().

✅ O que sobrou, e foi entregue: a frase enganava, e enganou. Foi ela
que custou um bloqueador inteiro de plano. ChestSurvey.coverage() monta
a cobertura num lugar só, e reserva a forma X of Y chests read para
cobertura de verdade — varredura completa sai como 8 chests read, 1 with items, que não se lê ao contrário. Cinco casos em
ChestSurveyCoverageTest.
P0.3 — A cadeia mineiro → armazenamento → fundidor

✅ CONFIRMADO, e a ruptura está achada — é estática.

A Regra 30 manda o minério que não é carvão para o baú da boca da
mina. Esse baú é achado por geometria — MineMouth.chestAt procura um
baú encostado na entrada do poço — e não é registro de ninguém: o
único lugar do mod que cria WorkerStorage é ChestScanner.scan, que
procura baú ao redor da cama do aldeão. Mina não tem cama ao lado.

E tudo o que conta o estoque da colônia percorre baús de trabalhador:
quem	de onde tira a lista
ColonyChests.nearestFirst	WORKERS.ofColony → STORAGES.of(...)
ChestInventoryReader.survey	os mesmos WorkerStorage
SmelterWork.smeltOne	percorre os trabalhadores direto

Provado por SmelterGameTest.theOreInTheMineMouthChestIsInvisibleToTheColony,
nas duas metades.

✅ Conserto entregue em 2026-09-11, pelo lado de quem lê. O
ColonyChests passou a ser a única resposta a "onde estão os baús
desta colônia", com o da boca da mina entre eles, e os três que montavam
a própria lista passaram a perguntar a ele. ⬜ Espera sessão.
P0.4 — O lenhador corta a parede da própria vila

❌ VENCIDO — o item pede o que entrou em 2026-09-09. Conferido pelo
P0.0 em 2026-09-11, item por item:
o que o plano pede	onde já está
conjunto de posições rejeitadas	TreeMarks.REJECTED, teto MAX_REJECTED = 4096
consultar antes de tentar	sim, e guarda o grupo inteiro
rejeitada N vezes → cooldown	Refusal(since, count)
6.000 → 12.000 → 24.000 → 48.000	memoryFor: UNREACHABLE_MEMORY = 10 × 600 = 6000, dobrando até × MAX_MEMORY_FACTOR = 8 — a escada exata
invalidação, não lista negra	Regra 23, a marca envelhece sozinha
não persistir entre sessões	mapa estático

Sobrou "por trabalhador", e ele não deve ser feito: o conjunto é
global de propósito, e por trabalhador seria pior — dois lenhadores
redescobririam a mesma parede em separado.
P0.5 — E3: perda de item por inventário cheio

✅ CONFIRMADO e ENTREGUE em 2026-09-11. Era a única das cinco
pendências de P0 que descrevia um defeito ainda vivo, e era literal.

O MinerHaul.deposit tentava o baú da boca da mina e depois o baú do
mineiro; o que sobrasse virava uma linha de WARN contando a perda.
Colônia com seis baús vazios a vinte blocos não mudava nada.

Agora o transbordo atravessa os baús da colônia pelo mesmo caminho do
lenhador — o baú do dono primeiro, o resto como transbordo.

Provado por MinerOverflowGameTest.theHaulThatDoesNotFitGoesToAnotherColonyChest.
⬜ Espera sessão.

**Revisão após playtest, 2026-09-13:** o transbordo entre profissões
contradizia o armazenamento pessoal definido na ADR-001 §7 e foi observado
em jogo. A saída agora vai ao baú de quem produziu; o insumo continua
retirável de qualquer baú da colônia. Se faltar espaço para o produto,
fundidor/carpinteiro devolvem a matéria-prima ao baú de origem; drops de
mina e árvore ficam no mundo em vez de migrar para outro ofício. Os
GameTests novos falharam antes da alteração e passaram depois. O tratamento
de resíduos que assoreiam o baú pessoal continua como E38; confirmação
visual deste contrato está pendente.

**Atualização do lote, 2026-09-13:** build e 313/313 GameTests passaram.
JAR atualizado para distribuição e launcher. Não houve novo playtest após
esta cópia; a validação visual segue pendente. O lote para aqui, antes de
investigar/alterar retomada de construção e navegação/mineração.

**Lote seguinte, 2026-09-13 — ADR-012:** alterações reais de blocos pelo
jogador invalidam os índices locais de construção; abrir espaço sobre braço
da mina já percorrido reabre a frente do primeiro ponto afetado. Build e
314/314 GameTests passaram. A sessão em jogo deve verificar se a obra e a
mina retomam; novos ramais e migração E45 continuam fora do lote.
P0.6 — A enxurrada da areia calou

✅ Entregue em 2026-09-11. Amortecedor de um ciclo no IdleLog, para
quem pergunta por tique: motivo que oscila deixa de virar quatro linhas
por segundo. O irmão da pedra de superfície foi junto. ⬜ Espera sessão.


### P0.7 — Setenta por cento dos lotes são recusados por serem pedra

⬜ **DECISÃO DO AUTOR, não correção automática.**

`isNaturalGround` aceita grama, terra, terra grossa, podzol e areia.
**Pedra não entra**, por decisão registrada (*"pedra à mostra é
montanha"*). A vila do autor é rochosa: **4.578 de 6.583 recusas**.

**É decisão do autor**, porque toca a Regra 3 e a Regra 19, e porque
separar grupos de pedra já desfez uma regra antes. Três caminhos, e o
custo muda muito entre eles:

| caminho | custo em recurso | risco |
|---|---|---|
| aceitar pedra como chão de lote | **zero** | casa nasce em afloramento, e pode ficar esquisita |
| terraplanar o lote antes de construir | terra ou o próprio bloco do chão; ~1 bloco por coluna fora de nível | mexe no mundo do jogador; mais código |
| ampliar o raio de busca de lote | zero em material, custo de tique | a casa nasce longe, e piora a logística |

**Recomendação registrada:** aceitar pedra é a intervenção menor que
resolve o gargalo — e é literalmente o que a especificação manda fazer
(*"qual é a menor e mais eficiente intervenção que resolve esse gargalo?"*).

**A pesquisa da terraplanagem** está represada em
[`docs/research/terraplanagem-da-vila.md`](../../docs/research/terraplanagem-da-vila.md),
e o Vanilla não serve de atalho: o `StructureWeightSampler` é função de
densidade, e o platô da aldeia é inventado enquanto o terreno ainda é
ruído — chunk gerado não pode ser re-beardificado. Metade da regra pedida
já existe no mod desde 08-21 (`RoadExtension.MAX_STEP`), e o que falta é
alguém que **conserte** em vez de **recusar**.


## P1 — Estabilizar comportamento

Depende de P0 verde.

| | o quê |
|---|---|
| **P1.1** | ❌ **VENCIDO — lido cláusula a cláusula em 2026-09-11.** Abandonar tarefa que não progride: os guardas de travamento e imobilidade, em todas as sete. Motivo de falha explícito: `giveUp(…, why)`. Cooldown: `MineMarks.refuse` na posição e `Worker.rest(capability)` no ofício. Outra tarefa: `task.release()` devolve à fila. Voltar depois: o prazo expira. E **as sete profissões passam pela mesma porta**, `WorkerStrikes.gaveUp`. A última cláusula, *"a Regra 28 vira diagnóstico, não inteligência definitiva"*, foi cumprida em 2026-08-21 |
| **P1.2** | ❌ **VENCIDO.** `Worker.REST_CYCLES = 4` é o descanso que o item pede, e o E43 está citado nominalmente ao lado dele. **A pergunta que o item mandava documentar já tem resposta escrita** |
| **P1.3** | ⚠️ **decidido por implementação, e o resto é de jogo.** O segundo caminho — *"devolver a tarefa e deixar a rotina Vanilla trazê-lo"* — está feito. ⬜ **O que falta é a outra metade:** se a rotina Vanilla de fato traz de volta o mineiro que já está longe |
| **P1.4** | ❌ **VENCIDO — as três cláusulas estão cumpridas.** `job.stall.stuck` e `TreeChoice.stallLimit`, e o lenhador passa pela mesma porta das outras seis |
| **P1.5** | ✅ **Entregue em 2026-09-11.** A busca do fazendeiro alcançava 22 dos 32 blocos prometidos; agora atravessa passagens pelo `RingSweep`, distingue "não achei" de "não terminei" e descansa depois de uma volta inteira. ⬜ **Espera sessão** |
| **P1.6** | ❌ já implementado — `INTERCHANGEABLE_IN_THE_WALL` |
| **P1.7** | ❌ **NÃO SE FAZ — refaria o defeito de 2026-09-10.** Separar `WOOD` por espécie recria a discordância entre conta e construtor. **E contradiz o P1.6 do próprio plano** |
| **P1.8** | ❌ **NÃO SE FAZ — o campo não está morto.** `furniture()` é o **primeiro critério de ordenação** da obra em `StructureBlueprintReader`. Removê-lo faria a cama e a mesa serem assentadas junto com a parede |
| **P1.9** | ⬜ **Regra 25: não mexer agora.** Só ganha sentido com múltiplas plantas, depois da §3.6 |
| **P1.10** | ✅ **Feito em 2026-09-11.** `theNeighbourhoodDeadEndIsNotTheTunnelCursorsQuestion`: três recusas em posições já cavadas enchem o cubo em volta de uma fronteira sem marca própria. A mutação que o Verifier provou que passava pela bateria inteira agora derruba este teste e só ele |
| **P1.11** | ✅ **Entregue em 2026-09-11, e não pelo caminho que a linha previa.** O gancho que encurtaria a hélice foi **medido antes de ser tomado**: torná-lo variável obriga três constantes a virarem método, e só `CARVED` tem **52 usos**. A segunda sala foi provada **onde ela é pura**, em `MineSecondLevelTest`. ⚠️ **E a lacuna era real, só que outra:** ninguém prendia o **x/z** do nível de baixo. Uma mina que descesse torta por nível passava por toda a bateria. Mutação conferida |
| **P1.12** | ✅ **Feito em 2026-09-11, com uma parte recusada.** `TestWorkers` dá os dois helpers com a diferença no nome, e um teste prova que ela é observável. Os 26 call-sites passaram a equipar. ❌ **A asserção defensiva no `assign()` não entra:** na produção o `assignMissing` roda antes do `equip`, no mesmo ciclo, então mão vazia ali é o desenho |
| **P1.13** | ✅ **Entregue em 2026-09-11.** `ColonyEnduranceGameTest`: 200 ciclos, um por tique. Amostra quatro contagens por ciclo e compara janela tardia com inicial. ⚠️ **A primeira versão passava com o `purgeClosed` desligado** — o caso só passou a medir depois que o teste fechou as tarefas distribuídas |

## P2 — Performance

**P2.1 — O ciclo da colônia em 112 ms** (limite: 50 ms).

✅ **Instrumentado em 2026-09-11, e só isso** — nenhuma otimização junto,
que é a ordem que este item dá. O aviso de ciclo lento passou a dizer
**onde**: `chests 61 ms, workers 28 ms, planner 14 ms, assign 5 ms,
detect 3 ms, lifecycle 0 ms, other 1 ms`, da fase mais cara para a mais
barata.

⚠️ **Uma das seis fases que o item lista não existe.** Persistência não
roda no ciclo: o `ColonySavedData.sync` é chamado **só** no
`SERVER_STOPPING`. Entraram no lugar as duas do topo que o item não
previa — a detecção em volta do jogador e o ciclo de vida das colônias.

**O instrumento acusa quando ele próprio mente.** `other` é o ciclo menos
tudo o que se sabe nomear, cortado em zero para não sair negativo; quando
o corte machuca, a linha diz `(phases overlap by N ms — the numbers above
are inflated)`.

**Nove casos em `CycleCostTest`**, e mutação conferida.

⬜ **Otimizar continua em aberto, e de propósito** — a régua do item é
passar de 100 ms ou o TPS cair, e quem responde isso é a sessão de jogo
com a linha nova na mão.

---

## P3 — Confiabilidade

| | o quê |
|---|---|
| **P3.1** | ❌ **NÃO SE FAZ — não há o que extrair, e o comando não tem para onde ir.** Não existem `VillageMigration`, `VillagerDataVersion`, `ColonyMigrationStats`, `LastProfession` nem `ColonyNeedsRehire` no projeto. E **o mod não registra comando nenhum** |
| **P3.2** | ❌ **NÃO SE FAZ — ele contradiz uma decisão posterior, e mira uma profissão que não existe mais.** O `MANUFACTURER` foi **dividido** em `CARPENTER` e `MASON` em 2026-09-10. As duas metades já têm teste. Construir a migração agora acrescentaria `DataVersion` e `LastProfession` ao formato de save para preservar uma atribuição que o projeto decidiu soltar |
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
  ✅ P0.0  validar o plano contra o código          ← 2026-09-11, feito
  ✅ P0.1  entregue (visto em jogo)
  ✅ P0.1-b, P0.1-c  entregues (esperam sessão)
  ✅ P0.2  vencido na premissa; a frase consertada
  ✅ P0.3  ruptura achada, conserto entregue (espera sessão)
  ✅ P0.4  vencido
  ✅ P0.5  entregue (espera sessão)
  ✅ P0.6  entregue (espera sessão)
  → P0.7  DECISÃO DO AUTOR: aceitar pedra como solo de lote?
  → PARAR E REPORTAR
  → P1.1 (vencido) → P1.2 (vencido) → P1.3 → P1.4 (vencido) → P1.5 (entregue)
  → P1.6 (implementado) → P1.7 (recusado) → P1.8 (recusado)
  → P1.10 (feito) → P1.11 (feito) → P1.12 (feito) → P1.13 (feito)
  → P2.1 (instrumentado, otimização aberta)
  → P3.1 (recusado) → P3.2 (recusado) → P3.3
  → gauntlet-verifier

Um item por commit, com mensagem no formato
P0.1: <o que mudou> (<teste que prova>). Não agrupar P0.1 com P0.2.

Regras finais

    Se um teste passa só por folga de tickLimit, o teste está errado.

    Se um teste mede mão nua, o teste está errado.

    Se um teste unitário cobre comportamento de integração, o teste está errado.

    Se foi preciso aumentar um limite para o teste passar, o teste está errado.

    Se o defeito some quando a frequência aumenta, o defeito não sumiu.

    Se não dá para reproduzir em jogo, não foi consertado.

Estado da execução

Atualizado a cada item entregue. ✅ só entra com bateria verde e
mutação conferida; a coluna em jogo é a que a régua cobra.
item	estado	em jogo
P0.0	✅ feito em 2026-09-11. A varredura derrubou dois dos quatro P0 abertos e achou a ruptura do terceiro sem sessão	—
P0.1	✅ entregue em 2026-09-11 — o índice de ruas voltou a valer para vila grande	✅ visto na sessão de 02:03
P0.1-b	✅ o caminho de terra não sai de baú. Uma linha no isShapedFromTheGround	⬜ espera sessão
P0.1-c	✅ a recusa de lote diz por quê. LotRefusals conta os cinco motivos	⬜ espera sessão
P0.2	❌ vencido na premissa — a varredura nunca pulou baú. ✅ O que sobrou foi a frase, e ela foi consertada	—
P0.3	✅ fechado em 2026-09-11 — o ColonyChests virou a única resposta a "onde estão os baús desta colônia"	⬜ espera sessão
P0.4	❌ vencido — a escada que o item pede entrou em 2026-09-09	—
P0.5	✅ entregue em 2026-09-11 — o transbordo do mineiro atravessa os baús da colônia	⬜ espera sessão
P0.6	✅ entregue em 2026-09-11 — a enxurrada da areia calou	⬜ espera sessão
P0.7	⬜ decisão do autor, não correção automática. O número chegou em 2026-09-12 e não era o esperado: 4.578 de 6.583 recusas foram NOT_NATURAL_GROUND	✅ número visto em sessão
P1.1	❌ vencido — lido cláusula a cláusula em 2026-09-11	—
P1.2	❌ vencido — Worker.REST_CYCLES = 4 é o descanso que o item pede	—
P1.3	⚠️ decidido por implementação. ⬜ o resto é de jogo	⬜ espera sessão
P1.4	❌ vencido — as três cláusulas estão cumpridas	—
P1.5	✅ entregue em 2026-09-11 — a busca do fazendeiro atravessa passagens	⬜ espera sessão
P1.6	❌ já implementado — INTERCHANGEABLE_IN_THE_WALL	—
P1.7	❌ não se faz — refaria o defeito de 2026-09-10	—
P1.8	❌ não se faz — o campo não está morto	—
P1.9	⬜	—
P1.10	✅ 2026-09-11	—
P1.11	✅ 2026-09-11 — a segunda sala provada em unitário; o gancho na hélice foi medido e recusado	—
P1.12	✅ 2026-09-11, com a asserção defensiva recusada	—
P1.13	✅ 2026-09-11 — 200 ciclos, falha por tendência; mutação conferida	⬜ espera sessão
P2.1	✅ instrumentado em 2026-09-11 — o aviso de ciclo lento reparte por fase. Otimizar segue em aberto	⬜ espera sessão
P3.1, P3.2	❌ não se fazem	—
P3.3	⬜ rodar o gauntlet-verifier, quando o autor pedir	—

Bateria no fim do ciclo de 2026-09-11: 753 unitários e 295 de gametest, zero falhas, medidos pelos XML de relatório e pelo runGametest.

Depois do P0.0, do que ele liberou, do P1.5, do P2.1, do conserto do P0.3, do P1.13 e do P1.11: 778 unitários e 299 de gametest, zero falhas, com os XML mais novos que o fonte.

Mutação conferida nos três. Com o MinerHaul voltando a depositar só no baú do mineiro, um teste cai e é o do P0.5. Com o coverage() voltando à forma ambígua, caem os cinco do P0.2 e nenhum outro. Com o CropPatch voltando a varrer sempre do centro, um teste cai e é o FarmerGameTest.theFieldSweepResumesWhereTheBudgetStoppedIt.
Nota sobre a manutenção deste arquivo

A varredura do plano contra o código (P0.0) mora em varredura-do-plano-2026-09-11.md.

A tabela de estado da execução é atualizada quando um item é entregue. A coluna em jogo só marca ✅ com sessão real.

Itens vencidos não saem do arquivo — ficam com o ❌ e a razão escrita. A lição mais útil do ciclo é justamente quais eram vencidos, e por quê.
