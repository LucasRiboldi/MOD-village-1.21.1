# ADR-010 — A mão emprestada: o trabalhador troca de foco quando o dele não anda

**Data:** 2026-09-02
**Estado:** proposto
**Minecraft:** 1.21.1 · **Fabric Loader:** 0.17.2
**Skill que orientou:** `minecraft-villager-systems`

## Objetivo

Trabalhador cuja profissão não está rendendo passa a ajudar em outra coisa, em
vez de repetir a mesma parede até o fim da sessão.

## A pergunta de escopo

A skill manda decidir isto antes de escrever qualquer linha:

> **Isto é identidade nova, ou capacidade nova?**

```text
[ ] identidade  → profissão + POI + task        (7 peças)
[x] capacidade  → quem pode pegar qual tarefa   (1 peça)
```

**E aqui a resposta é mais barata do que a skill supõe**, por um fato do projeto
que vale registrar:

```text
grep -rn "setVillagerData|VillagerProfession" src/main
  → VillagerScanner.java:265  ... != VillagerProfession.NITWIT
```

Uma ocorrência, e é **leitura**. O mod nunca escreve profissão vanilla. O
`ProfessionType` é camada própria, em `core`, e não se liga a `VillagerProfession`,
a POI, a `JOB_SITE`, a trades nem à `Schedule`. As sete peças que a skill cobra de
uma profissão nova **não existem neste mod**.

O que existe é um aparato próprio, e ele tem custo real:

| peça do mod | o que custa trocar |
|---|---|
| baú do trabalhador (`WorkerStorage`) | reivindicação; a Regra 11 proíbe dispensar sem substituto de verdade |
| ferramenta (`WorkerEquipment`) | a mão combina com a profissão, conferida toda passagem |
| plaquinha (`WorkerNameplate`) | o jogador lê a vila pelo nome que está flutuando |
| relatório | linha por profissão, e ela não pode mentir |

Trocar a profissão de um mineiro toda vez que a mina fica sem pedra por um ciclo
churnaria baú, ferramenta e nome — e brigaria com a Regra 11. **Ele continua
mineiro. Só pega, por ora, tarefa de outro tipo.**

## Camada afetada

```text
[ ] SENSOR  [ ] MEMORY  [ ] ACTIVITY  [ ] TASK
[ ] POI     [ ] PROFESSION  [ ] SCHEDULE  [ ] TRADES
```

Nenhuma. **Nenhuma camada do Vanilla é tocada** — nem Brain, nem memória, nem
Activity. A mudança inteira mora em `core/coordination/WorkAssignment`, que é o
que já decide quem pega qual tarefa.

Isso é resultado, e não sorte: é a ADR-005 (isolamento do Core) rendendo. Vale
anotar porque a pergunta "vamos precisar de Mixin?" tem resposta **não**, e ela
costuma custar uma sessão para ser descoberta.

## O que a sessão de 2026-09-02 mostrou, e que muda o desenho

O primeiro desenho — "trabalhador **ocioso** ajuda em outra coisa" — **não teria
ajudado em nenhum dos dois casos medidos.** É a descoberta que justifica esta ADR
existir antes do código.

`WorkAssignment.idleWorkers` define ocioso assim:

> *Ocioso é quem não tem tarefa aberta, e não quem está parado: um trabalhador a
> caminho da árvore continua ocupado.*

Os dois travados da sessão **tinham tarefa aberta**:

| quem | estado | tempo |
|---|---|---|
| `45617d43` | tarefa de pedra aberta, `looking for stone`, `stall 0/2400` | 16 min |
| `fad43afc` | tarefa de madeira aberta, `chopping` a 4 blocos da árvore | 2 min × 2 |

Nenhum dos dois aparece em `idleWorkers`. **Travado não é ocioso** — é ter
trabalho e não conseguir fazê-lo. O sinal que sabe disso já existe e é o guarda
de travamento, que devolve a tarefa depois de 2.400 tiques de expediente sem
progresso.

## A decisão

Duas peças, e as duas são necessárias — uma sozinha não faz nada.

### 1. A capacidade que acabou de falhar descansa

Quando o guarda devolve a tarefa de um trabalhador, a **capacidade** daquela
tarefa fica de molho para ele por um prazo. Não é a árvore nem a pedra que
descansa — disso já cuidam `TreeMarks` e o cursor da mina —, é *este trabalhador
tentando este tipo de trabalho*.

Mesma forma das duas memórias de recusa que o projeto já tem, e de propósito:
`RoadExtension.REFUSED` e `TreeMarks.UNREACHABLE`. Prazo, teto de tamanho, e
esquecimento sozinho.

### 2. Sem tarefa da sua capacidade, ele pega a de outro

Sem isto a peça 1 não produz nada: quase toda profissão do mod tem **uma
capacidade só** — `MINER` tem `COLLECT_STONE` e nada mais. Pôr a única
capacidade dele para descansar o deixaria parado, que é o defeito com outro nome.

`takeOneTask` ganha uma segunda passagem: esgotadas as capacidades da profissão,
tenta as **emprestáveis** de outras.

```text
1ª passagem   capacidades da profissão, menos as que estão descansando
2ª passagem   capacidades emprestáveis de qualquer profissão
3ª passagem   as que estão descansando — antes de deixá-lo parado
```

A terceira passagem é o que impede a regra de virar o problema que ela conserta:
**nunca deixar o trabalhador parado para honrar um descanso.**

## O que é emprestável

Recomendação — só a coleta:

```text
emprestável       COLLECT_WOOD · COLLECT_STONE · COLLECT_WOOL
não emprestável   SMELT_ITEMS · CRAFT_ITEMS · MAINTAIN_FOOD · BUILD_STRUCTURE
```

Coleta é andar até um bloco e trazê-lo; qualquer trabalhador com baú faz. Obra é
outra coisa — tem projeto, cursor e barreira de teste, e um pedreiro emprestado
entrando no meio de uma casa é defeito, não ajuda. Fundir e fabricar dependem do
baú e do fogão certos.

O filtro de baú (`needsOwnStorage`) continua valendo em todas as passagens.

## O que não muda, e é decisão

- **A ferramenta.** O mineiro corta lenha com picareta de diamante na mão. A
  Regra 2 fixou que ferramenta não muda a velocidade do trabalho, então isso é
  cosmético — e trocá-la brigaria com a invariante *a mão combina com a
  profissão*, que acabou de ser consertada em 2026-09-02.
- **O nome e o baú.** Ele continua mineiro para todos os efeitos.

## O relatório precisa dizer

É a lição do E31, e ela vale aqui inteira: **relatório que afirma o que não mediu
é pior que relatório que cala.** Um mineiro cortando lenha não pode aparecer como
`looking for stone` — o autor investigaria a busca da mina, que está certa.

A linha precisa dizer que é mão emprestada, e de qual profissão.

## Escopo de estado

```text
[x] individual — por trabalhador, e por capacidade
[ ] por POI  [ ] por vila  [ ] por mundo
```

**Não persiste.** Mesmo argumento do `blocked` da `Mine`: é a contagem de uma
sessão, não um fato sobre o trabalhador. Reabrir o mundo custa, no pior caso,
uma tentativa a mais na parede — e um descanso gravado faria o trabalhador
reabrir o mundo já se recusando a fazer o próprio trabalho.

## O que pode dar errado

| risco | o que o segura |
|---|---|
| todos migram para o mesmo tipo de tarefa | tarefa é reservada por um só; quem não pega volta a perguntar no ciclo seguinte |
| o especialista some da especialidade | a 1ª passagem sempre prefere a profissão dele; empréstimo só quando não há nada dela |
| descanso deixa a colônia parada | a 3ª passagem pega a capacidade em descanso antes de deixar alguém ocioso |
| o jogador não entende por que o mineiro corta lenha | a linha do relatório diz |

## Non goals

- Trocar `ProfessionType` de verdade — ver a pergunta de escopo.
- Tocar em Brain, memória, sensor ou Activity do Vanilla.
- Mexer em `dismissExtraWorkers` ou na Regra 11.
- Emprestar obra, fundição ou fabricação.

## Como se prova

Unitário, em `core` — `WorkAssignmentTest`, sem Minecraft:

1. trabalhador com a capacidade descansando e tarefa dela aberta, havendo outra
   emprestável → pega a emprestada;
2. trabalhador com a capacidade descansando e **nada mais** aberto → pega a
   própria mesmo assim (a 3ª passagem);
3. obra aberta e mineiro ocioso → **não** pega;
4. sem descanso nenhum, a escolha não muda — a 1ª passagem continua sendo a
   profissão dele.

Gametest só para a linha do relatório.

## As 19 perguntas — as que têm resposta não óbvia

| # | pergunta | resposta |
|---|---|---|
| 3 | onde o estado mora | `core/coordination`, memória de sessão; nenhuma memória do Brain |
| 12 | o que persiste | nada, e é decisão — ver acima |
| 13 | o que acontece se falhar | ele pega a própria capacidade de volta; o pior caso é o comportamento de hoje |
| 14 | e com vários aldeões | a reserva de tarefa já serializa; nada novo é compartilhado |
| 15 | custo | uma passagem a mais na lista de capacidades, por trabalhador ocioso, por ciclo |
| 16/17 | vanilla ou nosso | inteiramente nosso |
| 18 | precisa de Mixin | **não** |
| 19 | menor implementação correta | um descanso por trabalhador+capacidade, e uma segunda passagem em `takeOneTask` |
