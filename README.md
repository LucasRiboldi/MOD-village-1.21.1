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

⛏️ **O mineiro** tira pedra do mundo e guarda no baú — pedregulho onde
há rocha, **arenito no deserto**, que ali é a própria parede. Ele só
toca pedra exposta e que não é de ninguém: peça de vila gerada e casa da
colônia ficam de pé, porque são feitas do mesmo material que ele procura.

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
bioma dela, e **a maior que couber no lote** é a que sobe.

| Vila | Casas disponíveis |
|---|---|
| Planície | 36 |
| Savana | 31 |
| Nevada | 30 |
| **Deserto** | **28** |
| Taiga | 27 |

As variantes em ruína ficam de fora — uma colônia que as levantasse
estaria construindo a própria decadência.

O deserto era o buraco do mod: a vila nascia, contratava, contava
recurso e nunca construía, por não haver árvore. Agora o mineiro tira
arenito da duna e ela levanta as casas de deserto do jogo.

**O construtor espera pelo bloco exato de que precisa.** Ele não
substitui e não pula: falta a cama, a obra para na cama. O que impede a
vila de morrer esperando é que a *obra* sai da frente depois de vinte
ciclos — a espera é dele, não dela.

A colônia produz tábua, pedra, lã e vidro, e fabrica com isso o que a
planta pedir. **O que ela ainda não faz — tocha, ferro e tronco
descascado — é seu**: guarde num baú da vila e a obra anda.

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
| A planta se adapta ao lote — a maior que couber | 🧪 coberto por teste, nunca visto em jogo |
| Obra parada sai da frente em vez de travar a vila | 🧪 coberto por teste, nunca visto em jogo |
| **Mineração** — pedregulho, e arenito no deserto | 🧪 coberto por teste, nunca visto em jogo |
| **Tosquia** — lã, e a ovelha fica viva | 🧪 coberto por teste, nunca visto em jogo |
| **Fundição** — areia vira vidro | 🧪 coberto por teste, nunca visto em jogo |
| **Vila de deserto constrói**, em arenito | 🧪 coberto por teste, nunca visto em jogo |
| Catálogo dos 1.180 ids de estrutura do jogo | 📋 lista pronta, escolha entre elas não começou |
| Estrada crescendo com a vila | ⬜ não começado |
| Descascar tronco | ⬜ não começado — o tronco descascado é seu |
| Agricultura, ferraria, defesa | ⬜ não começado |

```text
421 testes unitários  ·  131 testes de jogo  ·  ./gradlew build
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
| **2** | **A areia não é colhida por ninguém.** O fundidor funde a areia que houver no baú, e nada a põe lá. Sem isso o vidro depende de você | 🔨 falta a meta de areia |
| **3** | **Descascar tronco.** O último material da casa de planície que a colônia não produz. Não é receita de bancada: é machado no tronco, e pede caminho próprio | 🔨 pronto para fazer |
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
| **Areia** | O fundidor funde a que houver no baú, e ninguém a colhe. Sem areia não há vidro |
| **Tronco descascado** | Não é receita de bancada — é machado no tronco, e pede caminho próprio |
| **Ferro** | O lampião pede ferro, e o ferro pede minerar fundo e fundir |

A casa de planície pede 43 pedregulhos, 16 troncos descascados e 3
vidraças. O mineiro resolve a pedra; o tronco descascado continua seu —
**guarde-o num baú da vila** e a obra anda. Onde a casa grande não cabe,
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
sempre atual — o enunciado das 25 regras, uma a uma — em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

---
## Último ciclo de desenvolvimento

**2026-08-20** — o ciclo em que a colônia deixou de depender de você para
os materiais. Começou com três correções vindas de sessões de jogo e
terminou com **três profissões novas** e a vila de deserto construindo
pela primeira vez.

**Implementado**

- **Regra 27, imutável** — o construtor só levanta o que está na pasta
  de estruturas do jogo, e **aguarda o bloco específico** de que
  precisa. Ela desfaz a cabana que a Regra 13 tinha inventado e a
  exceção de mobília da Regra 21. O que a tornou possível é do mesmo
  dia: a Regra 13 recusou a casa do jogo porque 66 dos 149 blocos dela
  pediam minerar, fundir, tosquiar e descascar — e agora a colônia
  aprendeu três dos quatro.
- **O mineiro** tira pedra do mundo: pedregulho onde há rocha, arenito
  no deserto. Ele só toca pedra **exposta e de ninguém** — peça de vila
  gerada e casa da colônia ficam de pé, e isso importa mais que para o
  lenhador, porque as duas são feitas do material que ele procura.
- **O pastor** tosquia, e a ovelha continua viva. É a colheita que se
  repete, e a Regra 7 de graça: quem replanta é o próprio jogo. A lã sai
  da cor do rebanho.
- **O fundidor** faz vidro de areia, pela receita de fornalha do jogo.
  Resolve a exceção que a Regra 10 tinha registrado como impossível.
- **A paleta por bioma** — a Regra 20 dita por inteiro. Antes o estilo do
  bioma era uma coisa só, a espécie da madeira, e por isso o deserto
  ficava de fora. Agora o bioma diz também de que é a parede e a qual
  pasta do catálogo a vila pertence.
- **A planta se adapta ao lote** (Regra 25). A vila varreu o raio inteiro
  sem achar lugar para a casa de planície, tendo três cabanas de pé
  dentro dela: 49 colunas no nível exato da rua pedem muito mais espaço
  que 25. Agora a colônia levanta a maior planta que couber **naquele
  lote**, e a casa grande continua subindo onde há espaço.
- **A obra que espera demais sai da frente** (`PatienceClock`). Nada
  tirava da frente uma obra parada, e a vila parava de crescer para
  sempre esperando um pedregulho. Vinte ciclos — mais que uma varredura
  inteira, que é o custo da alternativa.
- **A Regra 21 vale para qualquer casa**, e não só para a cabana. A casa
  que a colônia passou a preferir era justamente a que nunca ganhava
  cama, e casa sem cama não vira aldeão.
- **Peça destruída não volta**, e a conta disso vai para o save.

**Corrigido, e as três causas vieram de jogo**

- **O alvo da obra** era comparado com um id escrito no código. A
  pergunta envelheceu duas vezes: primeiro presa à casa de planície,
  depois à cabana. Agora quem responde é a colônia.
- **O cursor da busca de lote** era guardado pela posição do centro — e o
  centro troca de âncora a cada trinta segundos. A varredura recomeçava
  do zero todo ciclo e nunca passava das mil primeiras colunas.
- **O miolo oco da cabana** era oferecido como lote: sem piso, o chão de
  dentro é grama no nível da rua com o volume livre. Passava em todas as
  perguntas que se fazem ao mundo, porque nenhuma pergunta de quem é.

**Achado no caminho, e os dois são reais**

- `TreeHarvester.isNaturalLeaf` não conferia chunk descarregado e
  **derrubava o servidor**. A copa passou a ser procurada em até 256
  troncos em 08-19, e alcança longe o bastante para sair do carregado.
- `ChestDepositor.deposit` devolve quantos **não** couberam, e o mineiro
  leu como quantos entraram — todo pedregulho guardado virava uma linha
  de "baú cheio" com o baú vazio ao lado.

**Refatorado**

- `ConstructionPlanner` (703 linhas) partiu em três, e `LumberjackWork`
  (1232, o pior arquivo do projeto) em seis. Não por tamanho: eram
  perguntas independentes morando juntas. Nenhum arquivo acima de 500.
- `RingSweep` nasceu do mineiro: a espiral orçada com cursor já existia
  em dois lugares e ele seria a terceira cópia.
- 402 → 421 testes unitários, 119 → 131 testes de jogo.

**Pendente, e dito sem suavizar**

- **Nada disto foi visto em jogo.** As três profissões novas, a paleta, a
  cabana de arenito e as três correções estão cobertas por teste e nunca
  rodaram numa vila de verdade.
- **A areia não é colhida por ninguém.** O fundidor funde a que houver no
  baú; enquanto ninguém a traz, o vidro continua dependendo de você.
- **Descascar tronco** continua fora. É o último material da casa de
  planície que a colônia não faz.
- **Um teste instável** (`theStallGuardReturnsTheTaskAndForgetsTheTree`),
  cerca de uma falha em quatro execuções, anterior a este ciclo.
- **O centro da colônia** continua trocando de âncora a cada trinta
  segundos, e continua esperando decisão do autor.

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
