<div align="center">

<img src="src/main/resources/assets/villagecolony/icon.png" width="180" alt="Village Colony">

# Village Colony

### Suas vilas param de esperar por você.

*Um mod Fabric que transforma vilas do Minecraft Vanilla em colônias que
trabalham, produzem e crescem sozinhas.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Ambiente](https://img.shields.io/badge/Lado-Servidor%20%7C%20Singleplayer-lightgrey)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.3.0%20alpha-orange)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-informational)
[![CI](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml/badge.svg)](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml)

### [⬇️ Baixar village-colony-0.3.0.jar](downloads/village-colony-0.3.0.jar?raw=1)

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
desce cavando **em escada dupla** — duas colunas lado a lado, para quem
sobe não esbarrar em quem desce —, de picareta de ferro, como todo
trabalhador começa. A boca ganha um **arco de pedra com lanterna
pendurada**, que é o que a faz ser vista de longe.

A descida é um **caracol**: quatro lances de cinco degraus, cada um
virando à direita do anterior, e a volta fecha **debaixo da própria
boca** vinte blocos abaixo. É o que faz a mina não ter rastro — o fundo
do nível fica sob a entrada.

Lá embaixo a galeria **espirala**: dois anéis que se abrem em volta do
poço, com um **bolsão** pendurado no meio de cada — mais parede exposta
é mais minério à vista. Barreira à frente — bedrock, a casa de alguém,
pedra sem onde pisar — e ela vira. Fechada a volta do nível, a mina
**desce mais um**. Túnel e bolsões abrem **três blocos de altura**.

**E o que você constrói dentro da mina fica de pé.** Escada, laje,
tocha, trilho, porta: a picareta dele reconhece que aquilo não é rocha e
passa ao lado.

**Saiu água?** Ele tapa a nascente com pedregulho na hora e desvia a
galeria.

E ele **vê o minério** — todo tipo, pela etiqueta do próprio jogo —, e
vai **no mais raro primeiro**: entre carvão no chão e diamante na
parede, ele escolhe o diamante. A veia é seguida até acabar.

Ele nunca cava vila gerada nem casa da colônia. A galeria vai sendo
**acesa** com tocha enquanto ele avança.

🐑 **O pastor** tosquia a ovelha e traz a lã. A ovelha continua viva e a
lã volta a crescer.

🔥 **O fundidor** transforma areia em vidro, ferro cru em lingote e
arenito em arenito liso, pela receita de fornalha do próprio jogo.

🪚 **O carpinteiro** tira tronco do baú, faz tábua pela receita do
próprio jogo, e devolve. **Converte cerca de metade da madeira e deixa o
resto em tora**. Também **descasca tronco**, monta **tocha** e monta
**vidraça**.

🧱 **O pedreiro** faz o mesmo do lado da pedra: lavra o que a obra pede
em alvenaria — tijolo, escada, laje, arenito cortado. A cadeia dele tem
três donos e é toda vanilla: o **mineiro** traz pedregulho, o
**fundidor** o assa em pedra, e o pedreiro a lavra.

🏠 **O construtor** levanta a casa um bloco por segundo, na beira de uma
rua que já existe, com a **porta virada para a rua** e o piso no nível
dela. **A colônia nunca inventa material**: o que falta, ele fabrica do
que houver nos baús; o que não dá para fabricar, ele espera.

**As casas são as do próprio Minecraft, e só elas.** O mod não inventa
casa: cada vila levanta o que a pasta de estruturas do jogo tem para o
bioma dela — planície, savana, taiga, nevada e deserto.

**E não repete a mesma casa.** O catálogo do jogo tem dezenas de peças por
bioma — 36 na planície — e muitas dividem a mesma pegada: oito casas
pequenas diferentes ocupam o mesmo retângulo. Achado o lote, a colônia
sorteia entre **todas** as que cabem ali, e não só a primeira delas.

**Ela levanta moradia.** A pasta de estruturas guarda junto tudo que um
lote da vila pode receber — cerca de bicho, ponto de encontro, templo,
estábulo. A colônia constrói casa; a roça é decidida à parte, pela
população.

🌾 **O fazendeiro** colhe a lavoura madura, **replanta com a semente da
própria colheita**, **semeia o canteiro vazio** com o que houver no baú
dele, e guarda a comida. Quem diz se está madura é o **bloco**, e não
uma lista escrita no mod.

Cada um ganha um nome sobre a cabeça e um quadro pregado no baú, para
você saber de relance quem é quem.

---

## As oito profissões, em tabela

### Quem é quem

| | profissão | ferramenta inicial | o que ela faz | tarefa |
|---|---|---|---|---|
| 🪓 | **Lenhador** | machado de ferro | derruba a árvore um bloco por vez e replanta a muda | `COLLECT_WOOD` |
| ⛏️ | **Mineiro** | picareta de ferro | cava a mina em caracol, espirala a galeria e segue o veio | `COLLECT_STONE` |
| 🐑 | **Pastor** | tesoura | tosquia a ovelha, que continua viva | `COLLECT_WOOL` |
| 🌾 | **Fazendeiro** | enxada de ferro | colhe, replanta e semeia canteiro vazio | `COLLECT_FOOD` |
| 🔥 | **Fundidor** | mãos livres | funde pela receita de fornalha do jogo | `SMELT_MATERIAL` |
| 🪚 | **Carpinteiro** | mãos livres | tora → tábua, descasca viga, monta tocha e vidraça | `CRAFT_WOOD_MATERIAL` |
| 🧱 | **Pedreiro** | mãos livres | pedra → tijolo, e a alvenaria que a obra pedir | `CRAFT_STONE_MATERIAL` |
| 🏠 | **Construtor** | mãos livres | levanta a casa e a roça | `BUILD` |

A ferramenta é a de **ferro**, e é onde ele começa: havendo uma melhor
dentro do baú dele, o trabalhador troca. Quem julga "melhor" é o jogo,
medindo a velocidade contra o bloco que aquela profissão quebra o dia
inteiro — nenhuma escada de material está escrita no mod.

### O que cada um guarda, e onde

| profissão | onde guarda | o que entra no baú |
|---|---|---|
| **Lenhador** | o dele, e transborda para os da colônia | tora, muda, vara, maçã |
| **Mineiro** | o baú da boca da mina primeiro, o dele com a sobra | pedregulho, arenito, carvão, ferro cru, cobre |
| **Pastor** | só o dele | lã, na cor do rebanho |
| **Fazendeiro** | só o dele | trigo, cenoura, batata, beterraba e as sementes |
| **Fundidor** | de volta no baú de onde a matéria crua saiu | vidro, lingote de ferro, arenito liso |
| **Carpinteiro** | o da tora primeiro, os da colônia se não couber | tábua, tora descascada, tocha, vidraça |
| **Pedreiro** | o da pedra primeiro, os da colônia se não couber | tijolo e a alvenaria da obra |
| **Construtor** | não guarda — ele só retira | — |

### Quanto a colônia quer de cada coisa

| o quê | quanto | de onde sai a conta |
|---|---|---|
| **comida** | 8 por cama, nunca menos que 64 | a vila come todo dia |
| **roça** | 1 a cada **15 aldeões** | uma zona de plantio por quinze moradores |
| **pedra** | 64, ou o que a obra pedir se for mais | piso de estoque para a casa seguinte |
| **madeira** | metade em tábua, metade em tora | a casa pede viga descascada, e viga não sai de tábua |
| **lã** | o que as camas da obra pedirem | sem cama não nasce aldeão |
| **ferramenta** | a melhor que houver no baú do trabalhador | trocada pela velocidade que o jogo mede |

### A cadeia de produção, ponta a ponta

**Toda peça que uma casa de vila pede tem dono** — e não só a peça da
planta: a cadeia inteira até a folha. A estante pede tábua **e livro**; o
livro pede papel **e couro**; o papel pede cana. Se um degrau não tiver
quem o faça, a obra espera para sempre, e foi assim que uma biblioteca
passou oito minutos parada esperando uma escada de pedregulho com 69
pedregulhos no baú.

Levantamento sobre as **189 peças distintas** das cinco vilas, seguindo
cada receita até o fim:

| quem | peças | o que cai aqui |
|---|---|---|
| 🪚 **carpinteiro** | 104 | tudo de madeira e o acabamento: tábua, porta, cerca, escada, cama, **livro, papel**, vidraça, tocha |
| 🧱 **pedreiro** | 28 | alvenaria: pedra, tijolo, laje, muro, terracota, e as escadas de pedra |
| 🔥 **fundidor** | 12 | o que sai da fornalha: vidro, lingote, pedra lisa, arenito liso |
| ⛏️ **superfície** | 8 | areia, cascalho, argila, terra, neve, **sílex** |
| 🪓 **lenhador** | 10 | tronco, tronco descascado, muda |
| 🌾 **fazendeiro** | 7 | cana, bambu, cacto, terra arada, **flor de jarro e flor-de-tocha** |
| 🐑 **pastor** | 3 | **fio, saco de tinta, pele de coelho** — a matéria do couro |
| ⛏️ **mineiro** | 1 | pedregulho, de onde desce quase toda a alvenaria |
| 🌍 **o mundo** | 16 | água, lava, flor silvestre, capim — o que já está lá |

**Nenhuma peça fica órfã.** Isso é verificado a cada bateria de testes por
`StructureCoverageGameTest`, que relê as plantas do jogo, desce cada
receita e **falha** se aparecer peça sem dono. A tabela acima sai do
relatório dele, em `build/gametest/structure-coverage.txt` — para regerá-la,
rode `gradlew runGametest`.

**Onde o pastor entrou.** Ele não tinha nenhuma peça da cadeia até
2026-09-18: fio, saco de tinta e pele de coelho eram órfãos, e a pele é
matéria do couro que o livro pede. Foram para ele por serem de bicho, que é
o mundo dele, e porque era quem tinha menos — a mesma razão levou a flor de
jarro ao fazendeiro e o sílex à superfície.

---

## As regras do jogo dele

**Vanilla primeiro.** Os aldeões são aldeões comuns. Os baús são baús
comuns. As receitas são as do jogo, perguntadas em tempo de execução —
não copiadas para dentro do mod. **As casas são as do jogo.**

**Nada é inventado.** Sem inventário virtual, sem contador abstrato de
recurso, sem economia paralela. Se a colônia tem 40 tábuas, há 40 tábuas
num baú que você pode abrir. Tire-as, e a colônia percebe.

**A sua construção está segura.** A única coisa que um trabalhador
quebra é árvore, e ele precisa provar que a árvore é árvore: tronco sem
folha viva acima conta como construção, não como floresta.

**Ele para sozinho.** A colheita acaba quando os baús enchem e recomeça
quando você tira alguma coisa.

**A casa nasce mobiliada, no estilo da vila.** Porta virada para a rua,
piso no nível dela, e dentro uma cama, um baú e um lampião.

**O lote é escolhido pelo volume, não pelo chão.** Se houver qualquer
bloco dentro do espaço onde a casa vai, aquele lote não serve.

**Nada é recusado para sempre.** O que o mod olhou e rejeitou volta a
ser olhado depois de um tempo.

**A colônia fabrica o que a obra pede.** Se falta a porta e sobra tábua,
o construtor faz a porta.

**O dia inteiro é dia de trabalho.** Enquanto houver sol, os
trabalhadores estão buscando recurso ou trabalhando. A última hora de
luz é deles para voltar para casa, e a noite é para dormir.

---

## O ciclo

```text
   vila achada  →  aldeões contratados  →  madeira cortada  →  tábua feita  →  casa erguida
        ↑                                                                          │
        └──────────────────────  a casa nova tem camas  ←──────────────────────────┘

O que já funciona

✅ Oito profissões	lenhador, mineiro, pastor, fundidor, carpinteiro, pedreiro, construtor e fazendeiro
✅ A cadeia da madeira, ponta a ponta	cortar → fabricar tábua → levantar casa
✅ A mina	descida em caracol, galeria em espiral, bolsões, veio de minério, água tapada, arco de pedra com lanterna
✅ A roça	a colônia levanta a mesma roça que vem na vila, e o fazendeiro semeia o canteiro
✅ O que você constrói fica de pé	escada, laje e tocha que você puser dentro da mina não viram picareta
✅ Cada profissão com sua cor	nome sobre a cabeça, oito cores distintas
✅ Ferramenta de ferro para todos	e quem tiver melhor no baú troca por ela
✅ Metade da madeira fica em tora	o carpinteiro não moe o estoque inteiro
✅ Casas do próprio Minecraft	planície, savana, taiga, nevada e deserto
✅ A colônia nunca inventa material	o que falta é fabricado; o que não dá, ela espera
✅ Regra 3	vila gerada e construção da colônia são intocáveis
✅ Sem menu nenhum	nada de GUI, nada de item de configuração
✅ Servidor dedicado	quem entra não precisa do mod no cliente
O que ainda não está fechado

A lista viva e datada está em STATE.md, e a lista completa
em TODO.md. O que segue é o resumo.

Defeitos abertos, em ordem de dor:
	o quê	gravidade
E44	A escada de recusas da mina existe e tem testes; falta a validação integrada em jogo	⬜ aguarda playtest
E43	O descanso de quatro ciclos é anulado no ciclo seguinte	🟠 aguarda decisão do autor
E41	Nada mede degradação ao longo de muitos ciclos	🟠 maior lacuna de cobertura
E42	Nenhum teste de impasse entre profissões	🔴 tentativa retirada pelo gauntlet
E38	O baú do trabalhador assoreia	⚙️ metade fechada
KF-001	A falha instável do teste foi corrigida; a vazão global só volta a ser assunto com nova evidência	✅ teste fechado

P0.7 foi entregue em 15-09:

    Todo piso sólido disponível pode receber lote; materiais de estrada só
    bloqueiam dentro de ROAD_AREA, e a seleção não terraplana o mundo.
    A regra está na ADR-017 e passou 327/327 GameTests. O mesmo JAR também
    libera no mesmo tique a claim de um ramal quando o job do mineiro encerra;
    `MinerWorkLifecycleTest` cobre esse contrato. O JAR 0.3.0 distribuído tem
    SHA-256 C5D0790F996082CE3B7D2AA55CED93936DF04063568A03B0B521F50245A0BA1A.
    Falta o playtest no mundo do autor.

E o que já está entregue mas ainda não foi visto em jogo — uma lista
que cresce mais rápido do que drena. As linhas a procurar estão em
docs/proxima-sessao.md.

    Profissões que o modelo econômico prevê e ninguém escreveu — a lista
    inteira, com as razões, está em
    Village-Economy.md:
    Profissão	Por quê	Prioridade
    Pecuarista	Couro, carne, ovo, leite — nenhuma entra na vila hoje	★★★★
    Transportador	Hoje cada um guarda no próprio baú	★★★★
    Armazenista	Estoque central e tarefa criada por escassez	★★★★
    Guarda	A defesa, que o modelo prevê	★★★
    Explorador	Define a área de expansão	★★

Instalação

Requisitos

Minecraft	1.21.1 (Java Edition)
Loader	Fabric
Dependência	Fabric API

Passos

    Instale o Fabric Loader para 1.21.1.

    Ponha a Fabric API na pasta mods.

    Baixe o village-colony-0.3.0.jar, confira o SHA-256 publicado no
    STATE.md e ponha-o ao lado dela.

    Abra o jogo, carregue um mundo, e ache uma vila.

Funciona em singleplayer e em servidor dedicado. Quem entra num servidor
que tem o mod não precisa instalá-lo no cliente.

    Atualizando de uma versão anterior: apague o jar antigo da pasta
    mods. O Fabric recusa carregar dois jars do mesmo mod.

Onde olhar

A colônia conta o que está fazendo no log do servidor. Os trabalhadores
trabalham o dia claro inteiro e param na última hora de luz para voltar
para casa — se você chegar de noite, use /time set noon e eles começam.
Antes de instalar

Isto é um alpha, e o número da versão diz a verdade. O mod carrega em
cliente e em servidor dedicado. A cadeia da madeira, a mina e a roça já
foram vistas funcionando numa vila de verdade. As profissões mais novas
ainda estão sendo acertadas.

Use num mundo de teste antes de usar no seu mundo de sempre. Ele mexe no
mundo: derruba árvore, cava pedra e levanta casa.
Desenvolvimento

Se você chegou aqui para contribuir ou entender como o mod é feito:

    CLAUDE.md — o ponto de entrada para quem vai trabalhar no código

    STATE.md — o estado vivo, o que está aberto agora

    TODO.md — a lista de pendências

    TODO-archive.md — o histórico

    PROJECT_CONSTITUTION.md — os princípios

    docs/decisions/ — as ADRs de arquitetura

<div align="center">

Licença MIT · Feito para Minecraft 1.21.1 com Fabric
</div>
