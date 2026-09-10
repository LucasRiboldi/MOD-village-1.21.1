# Responsabilidade das profissões — quem responde por qual material

**Criado em 2026-09-09**, ao avaliar uma proposta externa de reestruturação
das profissões. Este arquivo é a **fonte única** da pergunta *"qual profissão
é responsável por este material?"* — que até hoje só podia ser respondida
lendo quatro arquivos em sequência.

Onde ele discordar do código, **vale o código**: a matriz abaixo é derivada
dele, e `ProfessionResponsibilityTest` a mantém honesta.

---

## A corrente, e onde cada elo mora

```text
ResourceType.production()      de onde o material vem
        ↓                      core/type/ResourceType.java
ColonyCycle.typeFor()          traduz produção em tarefa
        ↓                      core/coordination/ColonyCycle.java:213
TaskType.required()            que capacidade a tarefa exige
        ↓                      core/task/model/TaskType.java
ProfessionRegistry             que profissão tem essa capacidade
                               core/worker/service/ProfessionRegistry.java:35
```

**Os dois primeiros elos o compilador garante.** O `switch` de `typeFor` é
exaustivo sobre `Production` e não tem `default`: produção nova não compila
sem uma resposta.

**Os dois últimos não tinham guarda nenhuma até 2026-09-09.** Quando nenhuma
profissão registrada tem a capacidade exigida, `ColonyCycle.requestMissing`
pula o pedido — e pular está certo: tarefa sem executor possível ficaria na
fila para sempre. O que estava errado é que ele pulava **calado**, e o que se
via em jogo era `assigned 0 tasks (0 open)` sem causa aparente — o mesmo
sintoma que custou uma hora de sessão em 09-09 pela roça que travava a vila.

**As duas metades foram fechadas no mesmo dia.** `ProfessionResponsibilityTest`
garante que a corrente não tem elo aberto *no registro*; `ProductionHands`
quebra o silêncio *na sessão*, para o caso que o registro não alcança — a
colônia que simplesmente não tem aldeão daquela profissão. A linha:

```text
Colony 020ad427 — no collect_stone work: no worker in the village
                  can do it — COBBLESTONE needs COLLECT_STONE
```

---

## A matriz de hoje — 7 profissões

| profissão | ferramenta | capacidade | produção que a convoca | materiais |
|---|---|---|---|---|
| **Lenhador** | machado de ferro | `COLLECT_WOOD` | `HARVESTED` | os 8 troncos |
| **Mineiro** | picareta de ferro | `COLLECT_STONE` | `MINED` | pedregulho, arenito, areia, carvão, ferro cru |
| **Pastor** | tesoura | `COLLECT_WOOL` | `SHEARED` | lã branca |
| **Fazendeiro** | enxada de ferro | `MAINTAIN_FOOD` | `FARMED` | trigo, cenoura, batata, beterraba |
| **Fabricante** | nenhuma | `CRAFT_ITEMS` | `CRAFTED` | as 8 tábuas |
| **Fundidor** | nenhuma | `SMELT_ITEMS` | `SMELTED` | vidro, lingote de ferro, arenito liso |
| **Construtor** | nenhuma | `BUILD_STRUCTURE` | — *(consome, não produz)* | — |

**O construtor é a exceção declarada.** O trabalho dele não nasce de uma meta
de recurso: nasce do `ConstructionPlanner`. Por isso ele não aparece em
`typeFor`, e o teste que exige "toda profissão responde por algum material"
o isenta por nome.

### O que a matriz **não** diz, e é de propósito

- **Qual espécie**, quando há família. A meta de pedra é posta pelo nome que a
  `VillagePalette` do bioma escolhe — pedregulho onde há rocha, arenito no
  deserto —, e não pelo grupo. Ver a seção seguinte.
- **Onde**. Mina, árvore, lote e lavoura são decididos por cada `*Work`.
- **Quando**. É do `WorkHours` e do `Schedule`.

---

## A avaliação da proposta externa de 2026-09-09

Uma proposta chegou com três partes: a tabela das 13 profissões do vanilla
com bloco de trabalho e produtos, as listas de blocos das 5 vilas por bioma,
e uma reatribuição de responsabilidades por profissão.

**Nota de aceitabilidade: 4/10 como plano de implementação; 7/10 como dado
de referência.** O diagnóstico da parte final está certo e é o mesmo que este
projeto adotou. O que ela recomenda **fazer** já está feito, e o único item
que ela manda **mudar** desfaria uma decisão do autor.

### O que ela acertou — e o projeto já tinha

| a recomendação | onde já está |
|---|---|
| "Separar quem coleta a matéria-prima de quem produz o bloco final" | Mineiro→Fundidor e Lenhador→Fabricante desde a Fase 9 |
| "Declarar no recurso de onde ele vem, em vez de lista de nomes" | `Production`, nascida da ADR-009 em 2026-08-22, com o javadoc explicando que veio de matar um `if (x == GLASS \|\| x == IRON_INGOT)` |
| "Profissão ← produção declarada" | `ColonyCycle.typeFor`, switch exaustivo |
| "Material novo entra sem código novo" | `WorkMaterials.smeltedNeeds` filtra por `Production.SMELTED`, e o javadoc promete exatamente isso |

A proposta descreve como novidade a arquitetura que o projeto adotou há três
semanas — e o projeto a leva um passo além: o `switch` sem `default` faz o
**compilador** cobrar a resposta, coisa que a proposta não previu.

### O que ela errou, e por quê

**1. A recomendação central — separar `SANDSTONE` de `COBBLESTONE` em grupos
diferentes — desfaria as Emendas 1 e 2 da Regra 27.**

A proposta descreve corretamente um defeito real, e ele **aconteceu**: em
2026-08-22 a colônia concluía que a meta de pedra estava cumprida, o mineiro
não ia cavar, e o construtor esperava pelo arenito. Mas o diagnóstico dela
para no sintoma.

O diagnóstico do projeto, escrito em `ResourceSubstitution:143-148`, é mais
fino: **o defeito era a discordância entre a conta e o construtor, e não a
substituição em si.** A correção foi fazer os dois lados concordarem — não
separar os grupos.

Separar hoje custaria o que o autor pediu com todas as letras em 2026-08-26:
*"alternativas de recursos para todas as construções dos biomas"*. Sem o
grupo, cada bioma volta a depender de a floresta ao lado ter a espécie exata
que a casa pede.

**E o grupo já não é usado para a pedra.** `amountOfGroup` é chamado em três
lugares, todos para `WOOD` e `PLANKS`; a meta de pedra é posta por nome
(`ColonyGoals:414`, `goals.put(stone, ...)`), com a paleta dizendo qual. O
problema que a proposta quer resolver não existe nesta base.

**2. As listas de blocos por bioma são dados escritos à mão para uma pergunta
que o projeto faz ao próprio jogo.** O `StructureBlueprintReader` lê o `.nbt`
da estrutura e pede exatamente o que ela lista. Uma paleta escrita à mão seria
menos exata, envelheceria a cada versão do jogo, e é o que a Regra 27 proíbe:
*o mod não inventa casa*.

A `VillagePalette` existe e tem três eixos só — estilo, pedra, vidro —, porque
é só isso que **não** dá para ler da estrutura: qual pedra o mineiro deve
cavar e qual vidro a vila usa.

**3. As 13 profissões do vanilla são o eixo errado.** Elas são *comerciais* —
o que o aldeão vende —, e as sete daqui são *funcionais* — quem faz o
trabalho. A própria proposta reconhece isso na segunda metade e converge para
uma lista funcional muito parecida com a que já existe.

### O que fica dela, e é útil

- **A tabela bloco de trabalho → profissão do vanilla** é referência boa para
  o dia em que a colônia precisar **assentar** o bloco de profissão numa casa
  que construiu, ou reconhecer o ofício que o jogo já atribuiu a um aldeão.
  Nenhuma das duas coisas existe hoje.
- **A divisão Carpinteiro / Pedreiro** é uma pergunta legítima: o
  `ManufacturerWork` faz os dois papéis e tem 639 linhas, acima do limite de
  500. Ver a lista futura.
- **A observação de que "produto de trade" ≠ "produto que o bloco fabrica"**
  está certa e vale registrar antes que alguém use a lista de trades como se
  fosse receita.

### Recusado de propósito, para não voltar como sugestão nova

Seguindo o hábito da lista do Nível 1 do `TODO.md`:

- **Separar os grupos de pedra por bioma** — desfaz as Emendas 1 e 2 da Regra
  27, e o defeito que ela quer evitar já foi corrigido por outro caminho.
- **Cadastrar paleta de blocos por bioma à mão** — o jogo já responde, e com
  mais exatidão.
- **Adotar as 13 profissões vanilla como as profissões do mod** — eixo
  comercial, não funcional.
- **`Profession → inputs/processing/outputs` como estrutura de dados nova** —
  `Production` já responde à mesma pergunta com uma coluna, e a receita sai do
  livro do jogo (`ManufacturerWork`, `SmelterWork`), não de uma tabela do mod.

---

## O que a guarda de 2026-09-09 afirma

`src/test/java/com/villagecolony/core/coordination/ProfessionResponsibilityTest.java`,
5 casos:

| caso | o que afirma |
|---|---|
| `everyResourceHasAProfessionThatCanProduceIt` | todo `ResourceType` chega a uma capacidade que alguma profissão registrada tem |
| `everyDeclaredProductionHasAProfession` | idem, varrendo `Production` em vez dos recursos |
| `everyTaskCapabilityHasSomeoneWhoDeclaresIt` | nenhuma `TaskType` exige capacidade órfã |
| `everyProfessionAnswersForSomeMaterial` | nenhuma profissão fica sem material que a convoque — o estado do fazendeiro até 08-27 |
| `theMaterialGoesToTheProfessionThatOwnsIt` | dez pares nomeados: a lavoura é do fazendeiro e **não** do lenhador |

**Fase vermelha conferida, em duas mutações:**

1. Pastor removido do registro → caem 3 dos 5. *(O `ProfessionRegistryTest`
   também pega este caso, então ele não é prova do valor próprio.)*
2. `case FARMED -> TaskType.COLLECT_WOOD` — a lavoura vira trabalho de
   lenhador, que é o defeito exato que o valor `FARMED` existe para impedir →
   **caem só os 2 casos deste arquivo, de 686 unitários.** Sem ele a troca
   passaria calada.

O teste chama `typeFor` **de dentro do pacote**, e por isso a visibilidade
mudou de `private` para pacote. Reimplementar o `switch` no teste validaria a
cópia e não a regra — a lição de 2026-09-05 com o `footingIn` do mineiro.

---

## Lista para implementação futura

Ordenada por quanto dói, no formato do `TODO.md`.

| | o quê | por quê |
|---|---|---|
| ✅ | ~~**`requestMissing` pula calado quando ninguém sabe fazer.**~~ | **Fechado em 2026-09-09.** `ProductionHands` (`core.coordination`) leva o número de mãos até a camada Fabric, e `VillageDetectionHandler.reportHands` escreve a linha via `IdleLog` — a interface existe porque a ADR-006 §6 proíbe `core` de importar `fabric`, e é o mesmo caminho do `hasStorage`. Recebe o **número**, não só a ausência: sem o caso `hands > 0` o registrador nunca é mandado esquecer. Guardado por `ProductionHandsTest` (4 casos) e `IdleLogTest` (6 — o `IdleLog` não tinha nenhum). Visto na bateria: `no collect_stone work: no worker in the village can do it — COBBLESTONE needs COLLECT_STONE` |
| 🟠 | **Dividir o Fabricante em Carpinteiro e Pedreiro** | `ManufacturerWork` tem 639 linhas e faz dois ofícios: tábua/descascado (madeira) e o que vier de pedra. A proposta externa levantou isto, e o limite de 500 linhas concorda. **Decisão de projeto, e é do autor**: mais uma profissão é mais um aldeão a contratar numa vila pequena |
| 🟡 | **`MINED` responde por areia, carvão e ferro além da pedra** | O nome `COLLECT_STONE` mente um pouco: o mineiro traz cinco materiais. Não é defeito — a picareta é a mesma —, mas o dia em que a areia tiver origem própria (praia, não mina) vai pedir separação |
| 🟡 | **A colônia não assenta o bloco de profissão nas casas que constrói** | A casa do catálogo já traz o bloco quando a planta o tem; casa levantada pela colônia herda o que a planta disser. Não há código que escolha *qual* ofício aquela casa hospeda. É aqui que a tabela vanilla da proposta serve |
| 🟡 | **Nada lê a profissão que o jogo já atribuiu ao aldeão** | O mod atribui a sua própria por escassez (`ProfessionAssigner`). Um aldeão que já era ferreiro do vanilla vira lenhador sem cerimônia. Pode ser o certo — são sistemas paralelos —, mas nunca foi decidido por escrito |
| 🟢 | **Materiais que as vilas usam e a colônia não sabe produzir** | Terracota, podzol, neve, gelo, feno, cascalho. Hoje a Regra 28 risca o que falta; quando ela cair, cada um vira uma cadeia ou uma espera. Levantar a lista **lendo as estruturas do jogo**, não à mão |
