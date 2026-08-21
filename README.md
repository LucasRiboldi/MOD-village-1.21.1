<div align="center">

<img src="src/main/resources/assets/villagecolony/icon.png" width="180" alt="Village Colony">

# Village Colony

### Suas vilas param de esperar por você.

*Um mod Fabric que transforma vilas do Minecraft Vanilla em colônias que
trabalham, produzem e crescem sozinhas.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Ambiente](https://img.shields.io/badge/Lado-Servidor%20%7C%20Singleplayer-lightgrey)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.2.0%20alpha-orange)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-informational)

</div>

---

## O que ele faz

Você acha uma vila de planície. Você vai embora.

Quando volta, alguém andou cortando madeira. As toras estão num baú
marcado com um machado. Outro alguém as transformou em tábuas. E onde
havia grama na beira da rua, há uma casa que não estava lá antes.

Ninguém mandou. **Você não abriu um único menu.**

---

## O que os seus aldeões fazem

🪓 **O lenhador** anda até a árvore, derruba um bloco por vez — no tempo
de um jogador com machado de ferro —, não carrega nada para casa porque
a madeira vai direto para o baú dele, e replanta a muda antes de sair.

⛏️ **O mineiro abre uma mina de verdade.** Ele anda até o fim da vila e
desce cavando **em escada** com picareta de diamante, para poder subir de
volta. Dez blocos abaixo abre uma sala de 7×4; desce mais dez virando
para outro lado, abre outra sala; e do vigésimo bloco em diante segue
numa galeria sem fim, de dois blocos de altura. Barreira à frente — lava,
bedrock, a casa de alguém — e a galeria vira.

E ele **vê o minério**: carvão e ferro, comuns e de ardósia, e a veia é
seguida até acabar — o minério colado na parede vem antes da parede. Só
esses dois, porque só esses duas receitas da colônia consomem; ouro e
cobre encheriam o baú do que ninguém usa.

Ele nunca cava vila gerada nem casa da colônia: a boca da mina não se
abre sobre elas, e cada bloco é conferido de novo antes da picareta.

🐑 **O pastor** tosquia a ovelha e traz a lã. A ovelha continua viva e a
lã volta a crescer — é a colheita que se repete. A lã sai da cor do
rebanho, e não branca sempre.

🔥 **O fundidor** transforma areia em vidro, pela receita de fornalha do
próprio jogo. Sem forno no mundo: ele transforma o que está no baú, como
o fabricante faz com o tronco.

🪚 **O fabricante** tira tronco do baú, faz tábua pela receita do próprio
jogo, e devolve. Ele para quando metade do estoque da colônia é tábua,
para que o lenhador sempre tenha onde pôr mais.

🏠 **O construtor** levanta a casa um bloco por segundo, na beira de uma
rua que já existe, com a **porta virada para a rua** e o piso no nível
dela. Ele escolhe um lote livre — livre no volume inteiro, não só no
chão —, arranca o mato que estiver ali, e tira cada peça do baú da
colônia antes de pôr no mundo. **A colônia nunca inventa material**: o
que falta, ele fabrica do que houver nos baús; o que não dá para
fabricar, ele espera.

**As casas são as do próprio Minecraft, e só elas.** O mod não inventa
casa: cada vila levanta o que a pasta de estruturas do jogo tem para o
bioma dela.

> **Enquanto o mod está em alpha, é uma casa por bioma** — a casa
> pequena, `plains_small_house_1` e as equivalentes. É barreira de teste
> deliberada: uma casa só por bioma torna cada sessão comparável com a
> anterior. Quando ela sair, são 36 casas em planície, 31 em savana, 30
> em nevada, 28 em deserto e 27 em taiga, e a **maior que couber no
> lote** é a que sobe.

O deserto era o buraco do mod: a vila nascia, contratava, contava
recurso e nunca construía, por não haver árvore. Agora o mineiro tira
arenito da duna e ela levanta as casas de deserto do jogo.

**O construtor espera pelo bloco exato de que precisa.** Ele não
substitui e não pula — com uma lista de exceções enquanto o mod está em
alpha: **porta, cama, lampião, baú, tronco descascado, tocha e vidraça**
são dispensados se não houver nenhum num baú da vila, para que a casa
consiga terminar durante os testes. Pedra e tábua não entram na lista:
sem elas a casa fica com furo de parede.

E o fabricante passou a fazer três dessas: ele **descasca tronco**,
monta **tocha** e monta **vidraça**, quando os materiais estiverem nos
baús da vila.

🌾 **O fazendeiro** tem nome, enxada e baú — e nenhum trabalho ainda.

Cada um ganha um nome sobre a cabeça e um quadro pregado no baú, para
você saber de relance quem é quem.

---

## As regras do jogo dele

**Vanilla primeiro.** Os aldeões são aldeões comuns. Os baús são baús
comuns. As receitas são as do jogo, perguntadas em tempo de execução —
não copiadas para dentro do mod. **As casas são as do jogo**: em vila de
planície a colônia levanta a mesma casa pequena que o gerador do
Minecraft levanta, lida do arquivo de estrutura dele.

**Nada é inventado.** Sem inventário virtual, sem contador abstrato de
recurso, sem economia paralela. Se a colônia tem 40 tábuas, há 40 tábuas
num baú que você pode abrir. Tire-as, e a colônia percebe.

**A sua construção está segura.** A única coisa que um trabalhador quebra
é árvore, e ele precisa provar que a árvore é árvore: tronco sem folha
viva acima conta como construção, não como floresta. Peças de vila
gerada são perguntadas diretamente ao jogo e deixadas em paz.

**Ele para sozinho.** A colheita acaba quando os baús enchem e recomeça
quando você tira alguma coisa. Nada cresce sem limite.

**A casa nasce mobiliada, no estilo da vila.** Porta virada para a rua,
piso no nível dela, e dentro uma cama, um baú e um lampião. O baú a
colônia faz; a cama e o lampião pedem lã e ferro — guarde num baú e eles
entram sozinhos. **A casa não fica esperando por eles**: termina sem, e a
vila continua crescendo.

**O lote é escolhido pelo volume, não pelo chão.** Se houver qualquer
bloco dentro do espaço onde a casa vai, aquele lote não serve — nada de
casa nascendo em volta de um tronco. Flor e mato não contam: o construtor
os arranca antes de começar.

**Nada é recusado para sempre.** O que o mod olhou e rejeitou volta a ser
olhado depois de um tempo. Você derruba uma parede, planta uma muda,
aplaina um barranco — e ele muda de ideia na mesma sessão, sem precisar
reiniciar o servidor.

**A colônia fabrica o que a obra pede.** Se falta a porta e sobra tábua,
o construtor faz a porta: ele junta o material de todos os baús da vila,
do mais perto da obra para o mais longe, somando entre eles até dar. Só
fabrica com a quantidade inteira em mãos, e só quando o resultado tem
onde ser guardado.

**O dia inteiro é dia de trabalho.** Enquanto houver sol, os
trabalhadores estão buscando recurso ou trabalhando. A última hora de
luz é deles para voltar para casa, e a noite é para dormir — trabalhador
no mato depois do escuro é trabalhador que os monstros pegam.

---

## O ciclo

```text
   vila achada  →  aldeões contratados  →  madeira cortada  →  tábua feita  →  casa erguida
        ↑                                                                          │
        └──────────────────────  a casa nova tem camas  ←──────────────────────────┘
```

---

## Instalação

**Requisitos**

| | |
|---|---|
| Minecraft | 1.21.1 (Java Edition) |
| Loader | Fabric |
| Dependência | Fabric API |

**Passos**

1. Instale o [Fabric Loader](https://fabricmc.net/use/) para 1.21.1.
2. Ponha a [Fabric API](https://modrinth.com/mod/fabric-api) na pasta `mods`.
3. Baixe o `village-colony-0.2.0.jar` na
   [página de releases](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases)
   e ponha ao lado dela.
4. Abra o jogo, carregue um mundo, e ache uma vila de planície.

Funciona em singleplayer e em servidor dedicado. Quem entra num servidor
que tem o mod **não** precisa instalá-lo no cliente.

> **Atualizando de uma versão anterior:** apague o jar antigo da pasta
> `mods`. O Fabric recusa carregar dois jars do mesmo mod, e o nome do
> arquivo muda a cada versão.

**Onde olhar**

A colônia conta o que está fazendo no log do servidor. Os trabalhadores
trabalham o dia claro inteiro e param na última hora de luz para voltar
para casa — se você chegar de noite, use `/time set day` e eles começam.

---

## Estado do desenvolvimento

> **Isto é um alpha, e é honesto sobre isso.**

O mod compila, carrega e roda em cliente e em servidor dedicado.

| Sistema | Estado |
|---|---|
| Detecção de vila, identidade estável da colônia | ✅ verificado em jogo |
| Trabalhadores, profissões, ferramentas, posse do baú | ✅ verificado em jogo |
| Contagem de recursos, déficit, atribuição de tarefas | ✅ verificado em jogo |
| Colheita de madeira e replantio | ✅ verificado em jogo |
| Fabricação — tronco vira tábua | ✅ verificado em jogo |
| Construção — a casa sobe do começo ao fim | ✅ **verificado em jogo** em 2026-08-19 |
| Árvore grande reconhecida como árvore | ✅ **corrigido**, e a causa veio de jogo |
| A casa de planície é a casa do jogo | 🧪 coberto por teste, nunca visto em jogo |
| O construtor fabrica o que falta para a obra | 🧪 coberto por teste, nunca visto em jogo |
| Expediente do dia claro inteiro | 🧪 coberto por teste, nunca visto em jogo |
| Porta virada para a rua, lote no nível dela | 🧪 coberto por teste, nunca visto em jogo |
| Lote conferido no volume, e mato arrancado | 🧪 coberto por teste, nunca visto em jogo |
| Recusa que envelhece, em vez de valer para sempre | 🧪 coberto por teste, nunca visto em jogo |
| Madeira conforme o bioma da vila | 🧪 coberto por teste, nunca visto em jogo |
| Cama, baú e lampião dentro de cada casa | 🧪 coberto por teste, nunca visto em jogo |
| Baú criado ao lado da cama quando não há nenhum | 🧪 coberto por teste, nunca visto em jogo |
| Registro de construções e proteção | 🧪 coberto por teste, nunca visto em jogo |
| **As casas são as do jogo**, e só elas | 🧪 coberto por teste, nunca visto em jogo |
| **A mina** — escada, duas salas e galeria sem fim | 🧪 coberto por teste, nunca visto em jogo |
| **A mina sobrevive ao fechar do mundo** — a boca, o lado da galeria e até onde a picareta chegou | 🧪 coberto por teste, e um reinício de verdade nunca foi feito |
| **Tosquia** — lã, e a ovelha fica viva | 🧪 coberto por teste, nunca visto em jogo |
| **Fundição** — areia vira vidro | 🧪 coberto por teste, nunca visto em jogo |
| **Descascar tronco**, e montar tocha e vidraça | 🧪 coberto por teste, nunca visto em jogo |
| **Vila de deserto constrói** | 🧪 coberto por teste, nunca visto em jogo |
| Obra parada sai da frente em vez de travar a vila | 🧪 coberto por teste, nunca visto em jogo |
| A planta se adapta ao lote — a maior que couber | 🧪 pronta, e inerte enquanto for uma casa por bioma |
| **Colher areia**, e o vidro virando meta pela receita da vidraça | 🧪 coberto por teste, nunca visto em jogo |
| **Carvão e ferro na mina**, com a veia seguida até acabar | 🧪 coberto por teste, nunca visto em jogo |
| Estrada crescendo com a vila | ⬜ não começado |
| Agricultura e defesa | ⬜ não começado |

```text
431 testes unitários  ·  134 testes de jogo  ·  ./gradlew build
```

**O que 🧪 quer dizer aqui.** A bateria roda o caso e ele passa. Não quer
dizer que alguém viu acontecer numa vila de verdade — e este projeto já
aprendeu duas vezes que as duas coisas são diferentes: foi em jogo, e
não na bateria, que apareceram o alcance esférico do construtor e a
porta que ninguém fabricava.

---

## As etapas

**Os seis passos do MVP estão verificados em jogo.** O último fechou em
2026-08-19, às 02:12, quando uma cabana subiu do começo ao fim e virou
infraestrutura da colônia — e outra foi planejada 26 segundos depois.

```text
✅  detectar vila             verificado em jogo
✅  registrar aldeões         verificado em jogo
✅  organizar trabalhadores   verificado em jogo
✅  coletar recursos          verificado em jogo
✅  produzir materiais        verificado em jogo
✅  construir expansão        verificado em jogo, 2026-08-19
```

A ressalva honesta: aquela casa subiu com dezesseis blocos pulados,
porque o lote tinha morro dentro. É o defeito que a **Regra 19** passou
a recusar, e a correção ainda não foi vista em jogo.

### O que falta, na ordem

| | Etapa | Estado |
|---|---|---|
| **1** | **Rodar em jogo a cadeia de produção.** Mineiro, pastor e fundidor entraram em 2026-08-20 e **nenhum foi visto trabalhando numa vila de verdade**. Junto vêm a paleta por bioma, a cabana de arenito do deserto e a planta que se adapta ao lote | 🔒 exige sessão de jogo |
| **2** | **Um passo de "faça primeiro o que falta".** O fabricante só monta com todos os ingredientes no baú. A tocha pede graveto, o lampião pede pepita, e nenhum dos dois se faz sozinho | 🔨 pronto para fazer |
| **3** | **Quem peça o ferro.** O fundidor funde ferro cru e nada lhe abre tarefa: o lampião é mobília e não passa pelas metas | 🔨 pronto para fazer |
| **4** | **Regra 15 — estender a estrada.** A vila só constrói em beira de rua que já existe; quando ela acabar, a colônia para de crescer | 🔨 pronto para fazer |
| **5** | **Regra 16 — espaço em volta da casa.** A metade da altura está feita; falta a distância mínima e máxima | 🔨 meia feita |
| **6** | **Escolher entre as 1.180 estruturas do catálogo.** A lista está no mod; falta o critério e a conta de materiais de cada uma | 🔨 base pronta |
| **7** | **Regra 10, metade do fabricante.** Porta, janela, cama e baú por estoque, sem depender de haver obra | 🔨 depende do `ItemRequest` |
| **8** | **Regra 11 — uma de cada profissão por vila.** O mecanismo existe; falta a garantia e o teste. Ficou maior: são sete profissões agora, e catorze vagas por colônia | 🔨 pronto para fazer |
| **9** | **Envelhecimento de tarefa**, para que a mais antiga não seja esquecida | 🔨 pronto para fazer |
| **10** | **O fazendeiro e a defesa.** Duas profissões que o modelo prevê e ninguém escreveu | ⬜ não começado |
| **11** | **O trabalhador pedir o que lhe falta**, em vez de travar | ⏸️ toca o centro do sistema |

**Uma decisão espera o autor:** o que fazer com o centro da colônia, que
troca de âncora e volta a cada 30 segundos — visto nos logs de 08-18 e
08-19. É comportamento da ADR-003.

<details>
<summary>Etapas fechadas nos ciclos anteriores</summary>

| Etapa | |
|---|---|
| **Mineiro, pastor e fundidor — a cadeia de produção** | ✅ 2026-08-20 |
| **A paleta por bioma, e a vila de deserto construindo** | ✅ 2026-08-20 |
| **A planta se adapta ao lote — a maior que couber** | ✅ 2026-08-20 |
| **Obra parada sai da frente em vez de travar a vila** | ✅ 2026-08-20 |
| **Peça de mobília destruída não volta** | ✅ 2026-08-20 |
| **A casa do jogo em vila de planície, girada para a rua** | ✅ 2026-08-19 |
| **O lote conferido no volume, e o mato arrancado** | ✅ 2026-08-19 |
| **Recusa que envelhece — nada é rejeitado para sempre** | ✅ 2026-08-19 |
| **Árvore grande deixa de ser recusada como "não é árvore"** | ✅ 2026-08-19 |
| **Ver a casa inteira subir** | ✅ 2026-08-19 |
| **Regras 17 e 19 — porta na rua, lote no nível dela** | ✅ 2026-08-19 |
| **Regra 20 — madeira do bioma, e vila fora da planície** | ✅ 2026-08-19 |
| **Regra 21 — cama, baú e lampião em cada casa** | ✅ 2026-08-19 |
| **Regra 14 — o construtor alcança o alto da obra** | ✅ 2026-08-18 |
| **Regra 18 — o dia claro inteiro é expediente** | ✅ 2026-08-19 |
| **Regra 10 — o construtor fabrica o que a obra pede** | ✅ 2026-08-19 (metade do construtor) |
| **O rodízio de profissão** | ✅ 2026-08-15 |
| **O motivo de não trabalhar como valor** | ✅ 2026-08-15 |
| **Blocos de duas partes — porta e cama inteiras** | ✅ 2026-08-15 |

</details>

**Limites de hoje.** A colônia produz tábua, pedra, lã e vidro, e a
partir deles fabrica o que a obra pedir — porta, baú, escada, cama. O
que ela ainda **não** produz:

| Falta | Por quê |
|---|---|
| **Profundidade de receita** | O fabricante só monta o que puder montar com **todos** os ingredientes já no baú. A tocha pede carvão — que a mina agora dá — e **graveto**, que só chega por sorteio das folhas |
| **Quem peça o ferro** | O fundidor sabe fundir ferro cru desde 2026-08-21, e nada abre tarefa para ele: o lampião é mobília e não passa pelas metas da colônia |

A casa de planície pede 43 pedregulhos, 16 troncos descascados, 3
vidraças e 3 tochas. **As matérias-primas estão todas resolvidas** — o
mineiro traz pedra, areia, carvão e ferro; o fundidor faz vidro e
lingote; o fabricante descasca e monta. O que falta não é material: é a
receita fundo demais para um passo só. Enquanto for assim, **guarde três
tochas num baú da vila** nos dias em que o graveto não cair. Onde a casa grande não cabe,
a colônia levanta a cabana, que ela produz inteira sozinha.

A cama e o lampião não entram nessa conta — a casa **não espera** por
eles, termina sem, e eles entram sozinhos quando houver material. E
**peça destruída não volta**: se você tirar o lampião de propósito, a
colônia não o repõe.

As casas sobem ao lado de ruas que já existem; a colônia ainda não
pavimenta, e essa é a próxima etapa — e ficou mais apertada, porque o
lote agora precisa estar no nível da rua e livre no volume inteiro. A
escada ainda sai no estado padrão da planta; a porta, não — ela dá na
rua.

As pendências por prioridade estão em [`TODO.md`](TODO.md), com o que já
foi feito, o que falta e o que espera decisão sua. A lista longa está em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md), e o estado
sempre atual — o enunciado das 29 regras, uma a uma — em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

---
## Último ciclo de desenvolvimento

**2026-08-20** — dezenove commits, e o ciclo em que a colônia deixou de
depender de você para os materiais. Começou com três defeitos vindos de
sessões de jogo e terminou com **três profissões novas**, uma mina de
verdade, e as casas do próprio Minecraft no lugar da cabana que o mod
inventava.

**Quatro regras novas do autor**

| | |
|---|---|
| **Regra 25** | A colônia levanta a maior planta que couber no lote |
| **Regra 26** | A cadeia de produção, e a paleta de cada tipo de vila |
| **Regra 27** | 🔒 **Imutável** — só o catálogo do jogo, e o construtor aguarda o bloco específico |
| **Regra 28** | 🧪 **Provisória** — uma casa por bioma, e a obra não espera por sete peças |
| **Regra 29** | A mina: escada, duas salas de 7×4, e galeria sem fim |

**Implementado**

- **O mineiro** abre uma mina de verdade — desce em escada com picareta
  de diamante, abre sala no nível −10, desce mais dez virando, abre
  outra no −20, e segue numa galeria sem fim. Nunca cava vila gerada
  nem casa da colônia, e há duas portas conferindo isso.
- **O pastor** tosquia, e a ovelha continua viva. Fecha o laço da vila:
  casa, cama, aldeão novo, trabalhador, casa.
- **O fundidor** faz vidro de areia, pela receita de fornalha do jogo.
- **O fabricante** passou a descascar tronco e montar tocha e vidraça.
- **As casas são as do jogo.** A cabana que o mod inventava foi
  aposentada — o que mudou não foi a casa, foi a colônia aprender a
  minerar.
- **A obra que espera demais sai da frente**, depois de vinte ciclos. É
  o que torna a espera do construtor suportável.
- **Peça de mobília destruída não volta**, e a conta vive no save.

**Corrigido, e as três causas vieram de jogo**

- **O alvo da obra** era comparado com um id escrito no código, e a
  pergunta envelheceu duas vezes.
- **O cursor da busca de lote** era guardado pela posição do centro — e
  o centro troca de âncora a cada trinta segundos.
- **O miolo oco da cabana** era oferecido como lote: sem piso, o chão de
  dentro é grama no nível da rua com o volume livre.

**Achado no caminho**

- `TreeHarvester.isNaturalLeaf` não conferia chunk descarregado e
  **derrubava o servidor**.
- `ChestDepositor.deposit` devolve quantos **não** couberam, e o mineiro
  leu como quantos entraram.
- O teste da forma da mina pegou **duas sobreposições** de geometria que
  em jogo teriam passado por trabalho.

**Refatorado**

- `ConstructionPlanner` 703 → 414, em três arquivos.
- `LumberjackWork` 1232 → 455, em seis. Era o pior arquivo do projeto.
- 402 → 431 testes unitários, 119 → 134 testes de jogo.

**Pendente, e dito sem suavizar**

- **Nada deste ciclo foi visto em jogo.** Dezenove commits, quatro
  regras e três profissões cobertos por teste e nunca rodados numa vila
  de verdade.
- **A tocha depende de haver graveto no baú.** O carvão a mina passou a
  dar em 2026-08-21; o graveto cai das folhas por sorteio, e fazê-lo de
  tábua exige um passo de recursão que o fabricante ainda não tem.
- **A mina é gravada desde 2026-08-20**, mas **fechar e reabrir o mundo
  de verdade nunca foi feito**: a bateria roda um servidor só.
- **Seis conflitos internos** estão listados em [`TODO.md`](TODO.md) —
  regras que se contradizem, código que ficou sem uso, e um arquivo da
  Mojang que continua no repositório e não é mais lido.

---

## Compilando do código-fonte

```bash
git clone https://github.com/LucasRiboldi/MOD-village-1.21.1.git
cd MOD-village-1.21.1
./gradlew build
```

O jar cai em `build/libs/`. Precisa de um JDK 21.

```bash
./gradlew runGametest    # a bateria de testes de jogo, sem cliente
./gradlew runServer      # um servidor dedicado com o mod carregado
```

---

## Para quem desenvolve

O mod é dividido de modo que o cérebro da colônia nunca toque no
Minecraft:

```text
core/     o que uma colônia é e como ela decide — nenhum tipo do Minecraft
fabric/   a fronteira: adaptadores, varredura do mundo, blocos, mixins
data/     persistência
```

Toda decisão de arquitetura está escrita e datada em
[`docs/decisions/`](docs/decisions) — de por que as vilas são detectadas
por aglomerado de camas em vez de perguntar por estruturas, até por que
a superfície do mixin é um método só.

Neste projeto os documentos de projeto vêm primeiro e o código os segue.
Onde os dois discordam, a discordância é registrada em vez de escondida
— veja as seções de "ressalvas" do
[`Project-State.md`](docs/technical/Project-State.md).

Há também uma engenharia reversa de um mod concorrente em
[`docs/workers-analysis/`](docs/workers-analysis), feita como fonte de
conhecimento técnico. Nenhuma linha de código foi copiada de lá, e o
§11 daquela pasta explica por que não poderia ser.

**Contribuindo:** leia os documentos de arquitetura primeiro, mantenha o
core livre de imports do Minecraft, e acrescente um teste na fronteira —
todo defeito sério da história deste projeto morou lá.

---

## Licença

MIT — veja [LICENSE](LICENSE).
