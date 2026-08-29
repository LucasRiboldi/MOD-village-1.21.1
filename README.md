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

E **quando a mina não tem onde nascer** — vila cercada de água, encosta,
nenhuma das vinte e quatro colunas servindo —, ele não fica parado: passa
a raspar afloramento de pedra na superfície, ao alcance de uma caminhada.
Rende menos e não traz minério, e é justamente por isso que é a segunda
opção.

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

🌾 **O fazendeiro** colhe a lavoura madura, **replanta com a semente da
própria colheita** e guarda a comida no baú dele. Ele procura do centro
da vila para fora, e só toca no que está pronto — colher verde devolve a
semente e mais nada.

Quem diz se está madura é o **bloco**, e não uma lista escrita no mod:
vale para trigo, cenoura, batata e beterraba, e para o que um datapack
plantar depois.

Cada um ganha um nome sobre a cabeça e um quadro pregado no baú, para
você saber de relance quem é quem.

---

### As sete profissões, o que fazem e o que falta

**Todas as sete buscam recurso e guardam no próprio baú** desde
2026-08-27 — o fazendeiro era a última sem trabalho nenhum.

| Profissão | O que já faz | O que falta |
|---|---|---|
| 🪓 **Lenhador** | Acha árvore, derruba bloco a bloco no tempo do machado de ferro, replanta a muda, guarda no seu baú. Reconhece árvore grande | Não escolhe espécie por necessidade; não corta a copa que fica pendurada |
| ⛏️ **Mineiro** | Abre a boca da mina, mobilia com baú e lanterna, desce em escada de três blocos por degrau, abre duas salas de 7×4 e uma galeria sem fim. Segue veio de minério e abre degrau ao descer. Vira a galeria em barreira. Corrige sozinho a fronteira adiantada do save. Raspa afloramento quando a mina não tem onde nascer. **A escada é de um mineiro só**, e o segundo espera a vez dizendo que espera. Acende a galeria com tocha de parede a cada oito posições, atrás de onde está cavando | **Nunca foi visto cavando em jogo** — fechado na bateria em 08-28. Sem teto de inventário |
| 🐑 **Pastor** | Acha ovelha adulta e lanosa, tosquia, guarda a lã da cor do rebanho | Não cria rebanho, não alimenta, não separa por cor |
| 🔥 **Fundidor** | Funde pela receita do próprio jogo, sem forno no mundo: areia vira vidro, ferro cru vira lingote, arenito vira arenito liso | **Depende de a cadeia da areia começar** — o elo que ainda não fecha |
| 🪚 **Fabricante** | Tira tronco do baú e faz tábua pela receita do jogo, até metade do estoque. Descasca tronco, monta tocha e monta vidraça | Porta, cama, janela e baú por estoque, sem depender de haver obra (Regra 10) |
| 🏠 **Construtor** | Levanta a casa do jogo um bloco por segundo, na beira de rua existente, porta virada para a rua, piso no nível dela. Arranca o mato, tira cada peça do baú, espera o que falta | Uma casa por bioma enquanto a Regra 28 valer. Não desiste de obra travada |
| 🌾 **Fazendeiro** | Acha lavoura madura no raio da vila, colhe, replanta com a semente da colheita, guarda no seu baú | **Não ara terra nova nem planta em campo vazio.** Não faz pão, não escolhe cultura |

**O que nenhuma delas faz ainda:** pedir o item que lhe falta em vez de
travar (o `ItemRequest`), e defender a vila. As duas estão na lista de
etapas.

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
3. Baixe o `village-colony-0.3.0.jar` na
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
| A mina abre, é mobiliada e o mineiro cava | ✅ **verificado em jogo** em 2026-08-26 |
| A casa termina sem o jogador encher baú | ✅ verificado em 2026-08-26 — **com 19 peças da barreira de teste** |
| O pastor tosquia | ✅ verificado em jogo em 2026-08-26 |
| Árvore grande reconhecida como árvore | ✅ **corrigido**, e a causa veio de jogo |
| O índice de ruas e a varredura pela metade atravessam o fechar do mundo | ✅ **verificado em jogo** em 2026-08-27 — `1 road indexes` gravado, 17 passagens, uma volta completa |
| A varredura de lote acaba num ciclo, não em dezessete | ✅ **verificado em jogo** em 2026-08-27 |
| O baú marcado é de quem a marca diz | 🧪 coberto por teste, nunca visto em jogo |
| O fazendeiro colhe, replanta e guarda | 🧪 coberto por teste, **a vila não tinha lavoura madura** na sessão |
| O mineiro entra na mina em vez de ficar na superfície | 🟡 **meio visto**: em 08-28 ele saiu de `y=66` e chegou ao degrau 7 |
| **O mineiro cavando a escada dentro de rocha** | 🧪 **fechado na bateria em 08-28** — três testes em rocha maciça. Em jogo: sete sessões, zero blocos |
| O fundidor assando — ele espera **areia**, e a cadeia dela nunca começou | ⛔ elo sem entrada |
| A casa de planície é a casa do jogo | 🧪 coberto por teste, nunca visto em jogo |
| O construtor fabrica o que falta para a obra | 🧪 coberto por teste, nunca visto em jogo |
| Expediente do dia claro inteiro | 🧪 coberto por teste, nunca visto em jogo |
| Porta virada para a rua, lote no nível dela | 🧪 coberto por teste, nunca visto em jogo |
| Lote conferido no volume, e mato arrancado | 🧪 coberto por teste, nunca visto em jogo |
| Recusa que envelhece, em vez de valer para sempre | 🧪 coberto por teste, nunca visto em jogo |
| Madeira conforme o bioma da vila | 🧪 coberto por teste, nunca visto em jogo |
| Cama e lampião são esperados pela obra; a lã e o ferro viram meta da colônia | 🧪 coberto por teste, nunca visto em jogo |
| Baú criado ao lado da cama quando não há nenhum | 🧪 coberto por teste, nunca visto em jogo |
| Registro de construções e proteção | 🧪 coberto por teste, nunca visto em jogo |
| **Uma de cada profissão antes de dobrar qualquer uma**, e a troca por baú nunca esvazia a última | ✅ coberto por teste unitário |
| **As casas são as do jogo**, e só elas | 🧪 coberto por teste, nunca visto em jogo |
| **A mina** — escada, duas salas e galeria sem fim | 🧪 coberto por teste, nunca visto em jogo |
| **A mina sobrevive ao fechar do mundo** — a boca, o lado da galeria e até onde a picareta chegou | 🧪 coberto por teste, e um reinício de verdade nunca foi feito |
| **Tosquia** — lã, e a ovelha fica viva | 🧪 coberto por teste, nunca visto em jogo |
| **Fundição** — areia vira vidro | 🧪 coberto por teste, nunca visto em jogo |
| **Descascar tronco**, e montar tocha e vidraça | 🧪 coberto por teste, nunca visto em jogo |
| **Vila de deserto constrói** | 🧪 coberto por teste, nunca visto em jogo |
| **A mina reconhece minério** — carvão e ferro, e a veia é seguida | 🧪 coberto por teste, nunca visto em jogo |
| **O mineiro diz o que está fazendo**, uma linha por ciclo | 🧪 coberto por teste, nunca visto em jogo |
| **A fornalha assa pedra** — e quem diz o que entra nela é o livro de receitas do jogo, não uma tabela | 🧪 coberto por teste, **é o elo que faltava para o deserto** |
| **A mina tem onde nascer** — doze colunas em vez de uma, e voz quando nenhuma serve | 🧪 coberto por teste, **e é o que a próxima sessão precisa ver** |
| **A boca da mina ganha lanterna e baú** — e o minério fica lá até lotar | 🧪 coberto por teste, nunca visto em jogo |
| **O mineiro reconhece os oito minérios** do jogo, e não dois | 🧪 coberto por teste, nunca visto em jogo |
| **Grupo de recurso classifica, e não substitui** — pedregulho deixou de responder por arenito | ✅ coberto por teste unitário |
| Obra parada sai da frente em vez de travar a vila | 🧪 coberto por teste, nunca visto em jogo |
| A planta se adapta ao lote — a maior que couber | 🧪 pronta, e inerte enquanto for uma casa por bioma |
| **Colher areia**, e o vidro virando meta pela receita da vidraça | 🧪 coberto por teste, nunca visto em jogo |
| **A bancada faz o que falta** — o graveto da tocha sai da tábua | 🧪 coberto por teste, nunca visto em jogo |
| **O lampião pede ferro**, e o pedido chega à fornalha e à mina | 🧪 coberto por teste, nunca visto em jogo |
| **Carvão e ferro na mina**, com a veia seguida até acabar | 🧪 coberto por teste, nunca visto em jogo |
| **A rua cresce com a vila** — sem beira livre, a colônia calça o trecho seguinte | 🧪 coberto por teste, nunca visto em jogo |
| **A rua do deserto é reconhecida** — é de arenito liso, e a vila de lá nunca achou lote | 🧪 coberto por teste, nunca visto em jogo |
| **O centro da colônia só anda pela sonda** — varredura do jogador não o arrasta mais | ✅ **verificado em jogo** em 2026-08-22 |
| **O registro de aldeão segue as camas vistas**, e não o centro antigo | 🧪 coberto por teste, nunca visto em jogo |
| **A barreira de teste grita** — cada peça riscada nomeia a cadeia que falhou | 🧪 coberto por teste, nunca visto em jogo |
| **A lavoura** — o fazendeiro colhe, replanta e guarda | 🧪 coberto por teste, a vila não tinha lavoura madura na sessão |
| Defesa | ⬜ não começado |

```text
556 testes unitários  ·  211 testes de jogo  ·  0 falhas  ·  ./gradlew build
```

**O que 🧪 quer dizer aqui.** A bateria roda o caso e ele passa. Não quer
dizer que alguém viu acontecer numa vila de verdade — e este projeto já
aprendeu duas vezes que as duas coisas são diferentes: foi em jogo, e
não na bateria, que apareceram o alcance esférico do construtor e a
porta que ninguém fabricava.

---

## Para onde ele vai

O MVP é *a vila cresce sozinha*. Depois dele, o alvo é **a economia
inteira**: as 13 profissões vanilla como agentes produtivos, mais as que
o Vanilla não tem — Pedreiro, Pecuarista, Armazenista, Transportador,
Guarda —, ligadas numa cadeia que vai da natureza à casa e de volta ao
aldeão novo.

O plano está escrito em
[`docs/technical/Village-Economy.md`](docs/technical/Village-Economy.md):
as famílias de material de cada bioma, o que cada profissão consome e
produz, a cadeia produtiva, e as cinco fases — **acampamento → vila →
vila desenvolvida → cidade → cidade autônoma**, com o que destrava cada
uma.

> **A vila de planície é um perfil, e não a regra.** Carvalho, terracota
> branca e cascalho são o que aquele bioma oferece; o deserto constrói de
> arenito liso. O motor é o mesmo — é a decisão da
> [ADR-009](docs/decisions/ADR-009-Autonomous-Village-Evolution.md), e
> este plano a respeita.

**Onde ele está hoje:** sete profissões trabalhando, das quais duas — o
**Mineiro** e o **Construtor** — não existem no Vanilla e são as que a
autonomia exige.

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
| **1** | **Ver o mineiro cavar em jogo.** O E33 **fechou na bateria** em 08-28 — três testes em rocha maciça provam que ele cava a escada, desce cavando e conserta a fronteira adiantada do save. Em jogo continua sendo sete sessões e zero blocos | 🔒 exige sessão de jogo |
| ✅ | ~~Gravar o índice de ruas em disco~~ | Feito em 08-27, e **verificado em jogo**: `1 road indexes` gravado |
| ✅ | ~~O fazendeiro~~ | Feito em 08-27. As sete profissões buscam e guardam |
| **2** | **Implementar a ADR-008 — orientação de blocos.** É a que muda o que se vê: cama, escada e tocha param de sair todas para o mesmo lado. A decisão está escrita; falta atravessar o `Side` pelo `BlueprintBlock` | 🔨 decidido, por escrever |
| **3** | **Quebrar os seis arquivos acima de 500 linhas.** `VillageDetectionHandler` tem 982 e é o pior | 🔨 pronto para fazer |
| **4** | **Regra 16 — espaço em volta da casa.** A metade da altura está feita; falta a distância mínima e máxima | 🔨 meia feita |
| **5** | **O `ItemRequest`.** O trabalhador pedir o que lhe falta em vez de travar. Toca `Task`, que é o centro do sistema | ⏸️ decisão de arquitetura |
| **6** | **Escolher entre as 1.180 estruturas do catálogo.** A lista está no mod; falta o critério e a conta de materiais de cada uma | 🔨 base pronta |
| **7** | **Regra 10, metade do fabricante.** Porta, janela, cama e baú por estoque, sem depender de haver obra | 🔨 depende do `ItemRequest` |
| **8** | **Envelhecimento de tarefa**, para que a mais antiga não seja esquecida | 🔨 pronto para fazer |
| **9** | **Implementar a ADR-007 — fusão de colônias.** Decidida por escrito; nada dispara enquanto uma obra não encostar na outra | 🔨 decidido, por escrever |
| **10** | **A defesa.** A profissão que o modelo prevê e ninguém escreveu. O fazendeiro saiu desta linha em 08-27 | ⬜ não começado |
| **11** | **O fazendeiro arar e plantar.** Hoje ele só colhe o que já existe; campo vazio continua vazio | 🔨 base pronta |

### O que precisa ser arrumado

A lista completa — resolvidos, erros, incompatibilidades e decisões —
vive no [`TODO.md`](TODO.md), organizada por nível de progressão. Aqui
ficam os que mudam o que se vê no jogo.

| | O que | Por quê |
|---|---|---|
| 🟠 | **O mineiro desce, cava, e trava** | Em 08-28 ele estava **na galeria** com 108 pedras trazidas. A causa é aritmética e foi consertada em 08-29 — ele parava a dois blocos do lugar escolhido, e a folga somada ao alcance dava 4,2 num braço de 4. **Falta a sessão que confirme** |
| 🟠 | **O túnel que o jogador cava à mão confunde o mod** | Foi o que travou a mina do autor: um bolsão iluminado, desligado da escada, parecia frente de galeria. A frente passou a ser lida do mundo em 08-28, mas o mod ainda não distingue o que ele cavou do que o jogador cavou |
| 🔴 | **A vila fica presa numa obra só** | O planejador não sabe desistir. O catálogo do jogo já tem fazenda, curtume, ferraria — e a Regra 28 filtra tudo para uma casa por bioma |
| 🟠 | **Inventário do mineiro não tem teto nem retorno por lotação** | É onde mora o E3 — sobra de colheita vira perda de item |
| 🟠 | **Nove arquivos de código acima de 500 linhas** | Recontados em 08-26. `VillageDetectionHandler` é o pior com 983. `LumberjackWork` saiu da lista no corte do E19 (1149 → 455), e quatro entraram com a mina e a estrada |
| 🟠 | **`theStoneLeavesTheWorldAndReachesTheChest`** disse "a pedra não chegou ao baú" uma vez | Suspeita, não diagnóstico |
| 🟠 | **A arena da bateria tem bioma fixo** de planície | Escondeu **duas vezes** que o deserto estava quebrado |
| 🟡 | **O ícone tem 1,95 MB, e o jar 2,31 MB** | 84% do que se distribui é uma imagem mostrada a 64 pixels |
| 🟡 | **`furniture()` sem dono** e **Regra 25 inerte** | Lógica morta desde a Regra 21 e a 28 |
| 🟠 | **O `Development-Log` parou em 08-15** | **Oitenta e seis commits** e quatro dias de sessão — 08-21, 08-22, 08-25 e 08-26 — fora dele. Recontado em 08-26, e subiu de 🟡 porque o buraco dobrou |
| ✅ | **Nenhuma decisão esperando você** | As nove que travavam foram respondidas em 08-26, e a décima nasceu e fechou no mesmo dia. O que sobra é trabalho, não pergunta — a lista está no `TODO.md` |

<details>
<summary>Etapas fechadas nos ciclos anteriores</summary>

| Etapa | |
|---|---|
| **Nível 1 — a mina tem onde nascer, e voz quando não tem** | ✅ 2026-08-22 |
| **Regra 30 — o mineiro recolhe tudo, e a boca tem endereço** | ✅ 2026-08-22 |
| **ADR-009 — a vila evolui por bioma e recurso** | ✅ 2026-08-22 |
| **Grupo de recurso classifica, e não substitui** | ✅ 2026-08-22 |
| **ADR-003 Emenda 4 — o centro da colônia é da sonda** | ✅ 2026-08-21 |
| **A Regra 21 morre, e a demanda de lã e ferro passa para a obra** | ✅ 2026-08-21 |
| **A barreira de teste da Regra 28 passa a gritar** | ✅ 2026-08-21 |
| **A cabana do mod e a paleta de construção apagadas** | ✅ 2026-08-21 |
| **ADR-007 e ADR-008 — fusão e orientação, decididas por escrito** | ✅ 2026-08-21 |
| **O ícone com fundo transparente, e o `NOTICE` da Mojang** | ✅ 2026-08-21 |
| **Regra 11 — uma de cada profissão, e a garantia com nome** | ✅ 2026-08-21 |
| **Regra 15 — a rua cresce com a vila** | ✅ 2026-08-21 |
| **A cadeia de materiais fechada — areia, carvão e ferro** | ✅ 2026-08-21 |
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
| **Receita fundo demais** | A bancada e a conta de materiais descem **dois** degraus atrás do que falta. Cadeia mais longa que isso ainda não passa |
| **Comida, e a agricultura** | O fazendeiro tem nome, enxada e baú desde a Fase 4, e nenhum trabalho |

A casa de planície pede 43 pedregulhos, 16 troncos descascados, 3
vidraças e 3 tochas. **Nenhum dos oito materiais dela depende mais de
você** — o mineiro traz pedra, areia e carvão; o fundidor faz vidro; o
fabricante descasca, monta a vidraça e faz o graveto que a tocha pede.
Isso é afirmação de teste: **a casa nunca foi vista terminando em
jogo**. Onde a casa grande não cabe,
a colônia levanta a cabana, que ela produz inteira sozinha.

A cama e o lampião não entram nessa conta — a casa **não espera** por
eles, termina sem, e eles entram sozinhos quando houver material. E
**peça destruída não volta**: se você tirar o lampião de propósito, a
colônia não o repõe.

As casas sobem ao lado da rua, e **desde 2026-08-21 a colônia faz rua**:
quando não sobra beira livre, ela calça cinco blocos a partir da ponta
mais distante do centro. A escada ainda sai no estado padrão da planta;
a porta, não — ela dá na rua.

As pendências por prioridade estão em [`TODO.md`](TODO.md), com o que já
foi feito, o que falta e o que espera decisão sua. A lista longa está em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md), e o estado
sempre atual — o enunciado das 29 regras, uma a uma — em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

---
## Último ciclo de desenvolvimento

**2026-08-29** — uma sessão de jogo respondeu quatro perguntas, e três
delas com a causa exata no log ou no arquivo do próprio Vanilla.

| | |
|---|---|
| **O mineiro descia — e parava dois blocos antes de chegar** | Pela primeira vez em nove sessões o log pegou um mineiro **dentro** da mina, com 108 pedras já trazidas. Ele ficou a `4,2 blocks away (out of reach, he is at 756, 44, 878, walking to 758, 44, 878)` por seiscentos tiques. **Dois** era a folga com que a navegação se dá por chegada: ela parou, o mod continuou dizendo "fora de alcance", e ele moeu os últimos dois blocos. Duas contas certas que não compunham — o lugar escolhido estava a 2,0 da pedra, e o braço é 4. A folga passou a ser **do destino**: o lenhador continua com dois, o mineiro pede um |
| **O bloco central do chão era um encaixe do gerador** | O piso da casa de planície é um quadrado de nove tábuas, e a **do meio** é um `jigsaw` no arquivo do Vanilla. O leitor o descartava como andaime — mas encaixe não é andaime: ele carrega o `final_state`, o bloco em que vira quando a vila é gerada. Do meio do piso sai tábua; da porta, o degrau da entrada, que também faltava |
| **A cama pela metade, e a Regra 32** | `Could not finish the two-part block — cobblestone is in the way`. A planta guarda o nome e não o estado, então a cama saía olhando para o norte, que na casa de planície é a parede. O autor pediu a regra junto com o defeito: **móveis e cama entram depois da casa pronta**. Ela conserta os dois — as três tochas de parede riscadas com `nothing holds it` vinham antes da parede que as segura |

```text
556 testes unitários     0 falhas
211 testes de jogo       0 falhas
```

**Fase vermelha conferida nos quatro consertos.** E dois testes que
passavam por acidente caíram no caminho: o da retomada, que media a
planta na orientação errada e só funcionava enquanto a casa era
quadrada; e a primeira versão do teste da cama, onde uma planta de um
bloco só fazia a regra da porta apontar a cama para longe do muro.

**O que nada disto prova:** nenhuma sessão viu os consertos. São quatro
causas com prova de código e **zero sessões**.

### O ciclo antes — 2026-08-28, à noite

Dois relatórios que afirmavam o que não tinham medido, e os dois
calaram. **Nenhuma sessão de jogo:** é trabalho de bateria.

| | |
|---|---|
| **A barreira de teste absolvia a Regra 28 sem ter medido nada** — o E31 | Numa sessão com zero obras ela dizia `covered for nothing — Rule 28 can go`. A soma só sabia contar o que **foi riscado**; numa sessão sem obra a barreira não é exercitada uma vez, e o silêncio dela não é notícia boa, é ausência de notícia. O veredito passou a ter três estados, e sessão sem obra sai como `NOTHING_BUILT` |
| **A escada da mina passou a ser de um mineiro só** | A reserva era da **tarefa**, e a colônia abre uma por recurso pedido. Mas o cursor da galeria mora no `Mine` e é um: os dois mineiros recebiam a mesma posição na mesma passagem e davam `could not reach the stone` no mesmo tique — e esse aviso **recua o cursor**, que recuava duas vezes por um bloco. Agora a mina tem dono, no molde do `TreeClaims` |
| **O mineiro barrado diz que está barrado** | A linha de quem não tem alvo era `looking for stone` nos dois casos. Uma sessão inteira do segundo "procurando" mandaria investigar a busca, que está certa |
| **A galeria passou a acender, e não só a boca** | Vinte blocos abaixo da lanterna da entrada a mina tem **luz zero**, que é a condição exata de criatura nascer — ao lado de um aldeão desarmado. Uma tocha de parede a cada oito posições, no alto do que já foi cavado e um espaçamento **atrás** do cursor. E o mineiro **não cava a própria luz**: posição com luz é espaço aberto, não rocha, senão a frente da galeria recuaria até a tocha para sempre |

```text
549 testes unitários     0 falhas
208 testes de jogo       0 falhas
```

**Fase vermelha conferida nos três**, em rodadas separadas. Sem a
contagem da peça, a parede sobe e o teste de jogo da barreira cai; sem a
reserva, os dois testes de mina caem; sem o `isLight`, os dois da tocha.

**E a bateria não só passou: ela cobrou.** A iluminação quebrou o E33
duas vezes antes de ficar de pé — a tocha no bloco dos pés da escada, e
depois a tocha na camada da cabeça de uma coluna que o cursor dizia
pronta e a picareta ainda não tinha terminado. É a arena de rocha maciça
de 08-28 fazendo exatamente o que ela nasceu para fazer.

### Os dois dias antes — 2026-08-27 e 28

**Vinte e três commits, oito sessões de jogo**, e um defeito que precisou
de todas elas. As sete profissões passaram a trabalhar; o mineiro ainda
não cavou.

### O que ficou funcionando, e verificado em jogo

| | |
|---|---|
| **O índice de ruas atravessa o fechar do mundo** | A varredura de lote custava 16.641 colunas a cada entrada no mundo — dezessete ciclos, 8,5 minutos, e as sessões curtas morriam dentro dela. Índice **e** cursor vão para o disco. Confirmado em jogo: `17 passes over 16641 columns, 1 complete rounds`, `1 road indexes` gravado |
| **A pedra ganhou piso de estoque** | O mineiro só tinha tarefa quando havia obra aberta, e a obra dependia da varredura. Na prática ele quase nunca trabalhava — 19 ciclos com dois mineiros capazes e uma linha só: *"no task open for it"* |
| **O baú marcado é de quem a marca diz** | O quadro pregado no baú existia desde 08-12 e era **decoração**: o mod o escrevia e nunca o lia. Baú era escolhido pelo mais perto da cama, e madeira ia parar no baú do fazendeiro |
| **As sete profissões buscam e guardam** | O fazendeiro era só uma etiqueta — enxada, baú, placa e nenhum trabalho. Faltava a corrente inteira: recurso, produção, tarefa e meta |
| **A escada da mina dá para descer** | Ela abria dois blocos por degrau, que é quanto o aldeão ocupa **parado**. Descer é andar antes de cair, e a cabeça batia no teto do degrau seguinte |

### O defeito que levou oito sessões, e ainda está aberto

**O mineiro não cavou um bloco.** Cinco defeitos reais caíram no
caminho, e **nenhum era a causa sozinho**:

```text
a mobília da boca ficava no primeiro degrau  →  ele cavava a própria lanterna
o degrau seguinte é DIAGONAL                 →  a busca só olhava as seis faces
o cursor marchava por dentro da rocha        →  o mod dizia ter cavado, e não tinha
duas contas de distância                     →  o log dizia "chegou" sobre quem não chegou
duas contas de "cabe um aldeão"              →  o escolhedor e o relator discordavam
```

**O que os instrumentos foram descobrindo, sessão a sessão:** que a
varredura não reiniciava (ela só acabava antes); que ele estava a 4,7 e
não a 4; que ele estava **na superfície**, em `y=66`, vinte e um blocos
acima da galeria; e por fim que ele **entrou** — chegou ao degrau 7 — e
parou mirando uma lanterna que o próprio jogador tinha posto num túnel
cavado à mão.

Cada instrumento foi construído antes do conserto, e cada um encurtou a
busca. **Nenhum palpite sobreviveu ao log.**

### A pesquisa: o mesmo sintoma no MineColonies

Procurando projetos de aldeão que ajudassem a entender, o
[**MineColonies**](https://github.com/ldtteam/minecolonies) tem o
sintoma registrado com as mesmas palavras —
[issue #4297](https://github.com/ldtteam/minecolonies/issues/4297):

> *o mineiro fica parado na superfície acima do alvo, numa mina grande;
> sem bloqueio e sem falta de item. Cavar direto para baixo à mão
> resolve até ele precisar voltar.*

É exatamente o que aconteceu aqui, **incluindo o remendo manual** — o
autor deste mod cavou até a galeria para ver o que havia lá.

A resposta deles foi trocar a navegação do Minecraft por um **A\*
próprio, multi-thread, com cache de chunks**
([PerViamInvenire](https://github.com/ldtteam/PerViamInvenire)). Cedo
demais para este projeto. O que se aproveitou foi a **disciplina** que
aquilo impõe:

- **nunca mandar o trabalhador para um lugar de onde ele não consegue
  trabalhar** — virou a busca de apoio por distância, e a frente da
  galeria lida do mundo;
- **caminhar por pernas curtas** em vez de pedir um destino que a
  navegação não sabe traçar — virou a entrada pela boca da mina;
- **quem colhe, replanta** — virou a regra do fazendeiro.

### Os testes

```text
530 testes unitários     0 falhas
202 testes de jogo       0 falhas  ·  rodada três vezes
```

**O erro de teste que mais custou, e ele não era um teste falhando —
era a arena.** Todos os testes do mineiro montavam um **piso de terra
plano** e plantavam uma pedra nele:

```java
for (int x = 0; x <= 7; x++)
    for (int z = 0; z <= 7; z++)
        setBlockState(new BlockPos(x, 1, z), DIRT);   // um piso, e só
```

Numa arena assim não há escada, não há teto, não há degrau diagonal e
não há frente de galeria. **Todo defeito destes dois dias vivia
exatamente no que a arena não tinha** — por isso a bateria ficava verde
com o jogo quebrado. Os três testes novos montam rocha maciça, e um
deles reproduz a mina do autor: fronteira do save adiantada, nada
aberto.

**Falsos verdes e falsos vermelhos que a bateria pegou**, e valem tanto
quanto os consertos:

- um teste de aproximação **passou sem o conserto**, porque a arena não
  tinha teto e o aldeão achava lugar em cima da coluna — numa mina de
  verdade aquilo é rocha;
- dois testes de "não há onde ficar de pé" falharam **por estarem
  certos**: com alcance de 4, a superfície da rocha e o chão da arena
  entram na conta;
- um conserto do veio foi **revertido** por eu ter lido a diagonal da
  escada como um veio descendo — os testes daquela versão afirmavam uma
  geometria que o `OreVein` não produz;
- `theMineTheSaveBroughtIsNotDugAgain` **virou do avesso**: ele afirmava
  que a fronteira gravada era obedecida, e foi isso que quebrou a mina do
  autor. Passou a afirmar que ela é conferida contra o mundo.

### O que permanece falhando

| | |
|---|---|
| 🔴 | **O mineiro não cavou nenhum bloco em jogo.** Entra na mina desde 08-28, e para no fim da escada de verdade |
| 🟠 | **Túnel cavado pelo jogador confunde a frente da galeria.** O mod ainda não distingue o que ele abriu do que você abriu |
| 🟠 | **Dois mineiros dividem a mesma escada.** A reserva é por tarefa, não por mina |
| 🟠 | **O piso de pedra e de comida ignoram o espaço do armazém.** Vale para lã, vidro e carvão também |
| 🟠 | **`ColonySavedData.sync` tem sete parâmetros**, numa cadeia de sobrecargas 2→4→5→6→7 |
| 🟡 | **O fazendeiro não ara nem planta** — só colhe o que já existe |
| 🟡 | **Rua feita à mão pelo jogador fica invisível ao índice** até o centro andar mais de 20 blocos |

---

### Ciclo anterior — 2026-08-27, manhã

**A primeira mina da história do mod abriu**, a primeira casa terminou
sem o jogador encher baú, e **as duas coisas expuseram defeitos que só
apareceriam ali**.

**O que a sessão de jogo mostrou**

Dezoito minutos, e quatro itens da lista "falta ver em jogo" caíram
juntos: a mina abrindo, o mineiro cavando, a boca mobiliada com baú e
lanterna, e o pastor tosquiando. A casa subiu em 4 min 57 s — 149 blocos
planejados, 127 assentados.

**A ressalva que não se omite:** dezenove peças vieram da barreira de
teste, não da colônia. *Casa feita inteira com material da própria vila*
continua sem prova.

**Os defeitos que a mina acendeu**

| | |
|---|---|
| **O mineiro cavava a mina de pé lá em cima** | O alcance de braço media `dx` e `dz` e nunca `dy`. Ele furava o chão da superfície e **nunca entrava na própria escada** — que a Regra 29 desenhou com dois blocos de altura justamente para ele caber de pé. Funcionava enquanto a escada descia debaixo dele; morria quando a galeria corria na horizontal |
| **O destino do aldeão era dentro da rocha** | Bloco sólido nunca é alcançável: a navegação devolve caminho parcial e ele estaciona. Uma sonda mostrou o alvo com vizinho pisável **e ninguém apontando para ele** |
| **Três linhas de log nunca tinham falado** | O nome ofuscado `class_2338` voltou — 97 vezes, todas do mineiro. Não era regressão: a correção anterior arrumou os caminhos que **já tinham aparecido em log**, e os do mineiro nunca tinham rodado porque nenhuma mina tinha aberto |
| **O guarda contava a noite** | O mineiro era o único trabalhador sem porta de expediente. Metade do orçamento de dois minutos queimou com o aldeão dormindo — proibido de andar pela própria agenda |

**A varredura de lote, medida em vez de estimada**

Duas sessões morreram sem a vila construir nada, presas em *"ainda
varrendo"*. Lendo o save do mundo direto, das **16.641** colunas do
quadrado de raio 64 só **698** eram rua — 4,19%, e o mesmo em três
centros. O teto é 1.024 por passagem: as ruas **cabem numa passagem só**.

A ideia óbvia — seguir o traçado a partir de uma semente — foi medida e
**reprovada**: aquelas 698 colunas são catorze pedaços soltos, e um
alastramento acharia 60% delas. O que entrou foi um índice, que só nasce
de uma varredura completa e por isso não pode mentir sobre ter visto
tudo.

```text
477 unitários, 0 falhas   ·   176 de jogo, 4 rodadas sem vermelha
varredura: 17 ciclos → 1        ·   8,5 minutos → 30 segundos
```

**O que fica aberto, e está no [`TODO.md`](TODO.md):** o **E32** — em 20%
das geometrias o aldeão ainda não entra na própria escada — e a primeira
varredura de cada sessão, que continua custando os 17 ciclos porque o
índice não é gravado em disco.

### Ciclo anterior — 2026-08-26

**2026-08-26** — um checkpoint e **três defeitos fechados, nenhum deles
do mod.** O dia começou com treze commits parados no repositório local e
terminou com a bateria de testes consertada nos três lugares em que ela
mentia.

**O que se descobriu, e como**

| | |
|---|---|
| **A bateria mentia numa rodada só** | Rodada oito vezes, deu **duas vermelhas**. Uma execução teria dito "171 passaram" e o checkpoint teria mentido por omissão |
| **O guarda de travamento sempre funcionou** | O E20 acusava o mod há dias. A instrumentação mostrou a tarefa voltando à fila no tique 61 e ficando lá até o 240 — quem a rerreservava era o ciclo longo, e a afirmação era feita tarde demais |
| **A limpeza não rodava quando a afirmação caía** | `assertTrue` lança, e a limpeza vinha depois. **44 dos 46 pontos** deixavam trabalhador vivo e estático alterado para o resto da bateria |
| **O javadoc prometia duas provas e o teste fazia uma** | O nome do método já dizia `AndForgetsTheTree`, e ninguém conferia que o guarda **marca** a árvore |

**O padrão do dia:** os três defeitos eram de **como o mod era
observado**, não do mod. Duas das três correções não tocaram uma linha de
produção; a terceira mudou uma, e só a visibilidade de um método.

**Uma hipótese caiu no caminho, e fica registrada.** O checkpoint supôs
que o ciclo de 600 ticks rerreservava a tarefa, e a sonda que forçou o
ciclo no tique 200 deu três rodadas verdes. Era cedo demais: faltava a
outra metade — a busca achando **outra** árvore, na arena de um teste
vizinho.

```text
476 unitários, 0 falhas   ·   171 de jogo, 20 rodadas sem vermelha
0 afirmações dentro de finally   ·   0 linhas de comportamento alteradas
```

### Ciclo anterior — 2026-08-22

**2026-08-22** — quinze commits, **três sessões de jogo** e uma virada
de arquitetura. O dia começou com o autor respondendo nove perguntas em
aberto e terminou com a raiz de tudo achada: **a mina nunca tinha
aberto.**

**O que as sessões provaram**

| | |
|---|---|
| **A vila de deserto planejou uma casa do catálogo** | `desert_small_house_1`, 113 blocos. Inédito |
| **O construtor chega ao bloco** | A obra passou de `BUILDING` a `WAITING_RESOURCES`, que só se alcança tocando o bloco. Antes: oito minutos andando sem chegar |
| **O centro parou de saltar** | 4 movimentos convergindo, e 23 varreduras do jogador recusadas sem mover nada |
| **A conta de pedra abre tarefa** | `no miner work` caiu de toda passagem de ciclo para uma vez em treze minutos |

**A raiz, achada no fim do dia**

`MineDigging.mouthOf` procurava a boca em **uma coluna** — centro mais
quarenta blocos numa direção fixa — e desistia se ela não servisse. Sem
alternativa, sem nova tentativa e **sem uma linha de log**. Três sessões
terminaram com `0 mines` no save e mineiros mudos com tarefa aberta. E a
mina é a raiz de pedra, carvão e ferro: sem ela, a cadeia inteira de
material fica pendurada num elo que nunca produziu um bloco.

Agora são **doze colunas**, a boca é superfície e não miolo de morro, e
o fracasso tem voz.

**Três defeitos de jogo, corrigidos no mesmo dia**

- **O construtor pisava dentro da duna.** `footOf` mandava o aldeão ao
  pé da coluna na altura da obra — no deserto, debaixo da areia.
- **A conta de pedra pedia o bloco errado.** A casa de deserto tem 93
  blocos de arenito na família e 5 do bloco puro; a conta via 5.
- **A colônia procurava aldeão no centro antigo**, e não onde viu as
  camas — defeito que a Emenda 4 destapou.

**A virada:** a [`ADR-009`](docs/decisions/ADR-009-Autonomous-Village-Evolution.md)
tira o mod de `BIOMA → ESCOLHER ESTRUTURA` e o põe num **motor universal
de evolução** alimentado por perfis. A primeira peça dela já entrou:
grupo de recurso voltou a ser classificação, e a substituição passou a
ser declarada.

---

<details>
<summary>O ciclo das nove decisões — 2026-08-21</summary>

**As nove decisões**

| | |
|---|---|
| **O centro da colônia só anda pela sonda** | ADR-003 Emenda 4. Contagem de camas e posição eram a mesma decisão e nunca foram a mesma pergunta — o portão tinha três portas e todas moviam o centro |
| **A barreira de teste grita** | A justificativa dela caducou: as sete cadeias fecharam. Fica até a primeira sessão, mas cada peça riscada nomeia a cadeia que falhou, e a sessão termina com a soma |
| **A Regra 21 morre** | A mobília repunha o que a obra riscava, e declarava a demanda de lã e ferro. A primeira parte morreu; a segunda passou para a obra aberta |
| **O `.nbt` da Mojang, declarado** | Nasceu o `NOTICE`: o que é, de onde veio, em que commit entrou, e que não está coberto pelo MIT daqui |
| **O E9 ganha medida antes de decisão** | A transição de estado diz de onde veio e o que a sonda viu nos dois sentidos; a sessão nomeia quem trocou três vezes ou mais |
| **ADR-007 — fusão** | Sobrevive o UUID da maior; o teto de profissão é violado e permanece violado; o centro é o do sobrevivente |
| **ADR-008 — orientação** | `Side` de quatro direções no `BlueprintBlock`, tradução na fronteira. As duas formas recusadas estão ditas por escrito |
| **O ícone com fundo transparente** | Por preenchimento a partir da borda, e não por chave de cor — a roupa do aldeão e o reboco da casa também são brancos |
| **A cabana e a paleta, apagadas** | `ColonyHut` saiu; `VillagePalette` perdeu parede, porta e lã, que só ela lia |

**Três defeitos de jogo apareceram no caminho**

- **A colônia procurava aldeão onde era o centro, e não onde viu as
  camas.** Com o centro parado pela Emenda 4, uma colônia que adotasse um
  aglomerado a dezenas de blocos do próprio centro ficava dona dele **e
  não enxergava um aldeão sequer ali**. É o que fazia a bateria falhar, e
  **melhorou** `theStallGuardReturnsTheTaskAndForgetsTheTree` sem
  explicá-lo. Medido em doze rodadas depois da correção: o teste da
  detecção não falhou nenhuma vez, e o do lenhador falhou duas — era 3
  em 10 antes. É melhora real, e não diagnóstico.
- **A receita da cama devolvia zero.** O jogo tem mais de uma receita para
  `white_bed`, e uma delas é **tingir** uma cama preta. O livro achava
  essa primeiro, e a conta de lã dava zero — o pastor sem tarefa e a casa
  sem cama, sem uma linha de log. Quem decompõe uma peça não pode partir
  de outra peça da mesma família.
- **A obra dizia ter a mobília que não tinha.** `hasMaterialForNextBlock`
  respondia "tem" para toda peça de mobília, citando a Regra 21. Com a
  regra morta, a obra acordaria, tentaria, falharia e dormiria — todo
  ciclo, para sempre.

---

<details>
<summary>O ciclo de 2026-08-21, pela manhã — a cadeia de materiais</summary>

**Implementado**

| | |
|---|---|
| **A mina é da colônia, e é gravada** | A boca, o lado da galeria e até onde a picareta chegou. Uma por colônia, e não uma por mineiro |
| **Areia, e o vidro virando meta** | A casa pede **vidraça**, e não vidro. A conta decompõe pela receita do jogo |
| **Carvão e ferro na mina** | E a **veia é seguida**: o minério colado na parede vem antes da parede |
| **A bancada faz o que falta** | Dois degraus de receita. A tocha pedia graveto, e ninguém fazia graveto |
| **O lampião pede ferro** | A mobília relata o que lhe falta; o lingote vira meta da fornalha e o cru vira meta da mina |
| **Regra 15 — a rua cresce com a vila** | Sem beira livre, a colônia calça cinco blocos a partir da ponta mais distante do centro |
| **Regra 11 — o piso das profissões** | Sete testes onde não havia nenhum, e a garantia com nome próprio |

**Corrigido**

- **A vila de deserto nunca achou lote.** A rua de lá é de **arenito
  liso**, e a busca reconhecia rua por um nome escrito no código:
  `dirt_path`. A vila nascia, contratava, contava recurso, recebia
  arenito do mineiro — e terminava toda varredura dizendo que não havia
  lote. Não aparecia em teste porque a arena da bateria tem bioma fixo
  de planície. Agora quem responde de que bloco é a rua é o **catálogo
  do jogo**.
- **O fundidor queimaria a areia numa tarefa de ferro.** O que entra na
  fornalha estava escrito no código como areia; agora sai da tarefa.
- **Um javadoc mentiroso**: o registro de construções dizia não ser
  persistido, e é desde 08-14.

**Achado no caminho**

- Mover três testes para um arquivo novo derrubou a bateria de **143
  para 140 — dizendo "todos passaram"**. Classe de gametest não
  registrada em `fabric.mod.json` **some em silêncio**: os testes não
  falham, eles deixam de existir. Quem acusou foi a contagem, e é por
  isso que ela vai no relatório de todo ciclo.
- A dispensa por falta de baú **pode** esvaziar uma profissão. O que a
  impedia era um acidente do jeito de contar baús livres. Agora é uma
  frase com nome.

**Refatorado**

- `MinerWork` 690 → 459, em três arquivos: `MineDigging` desce,
  `SandGathering` varre, e ele ficou com o que os dois compartilham.
- `GlassDemand` virou `WorkMaterials` quando o carvão precisou da mesma
  conta; `WorkDemand` nasceu porque `ColonyGoals.of` chegou a nove
  parâmetros posicionais, quatro do mesmo tipo.
- 450 → 469 testes unitários, 134 → 151 testes de jogo.

**Pendente, e dito sem suavizar**

- **Nada deste ciclo foi visto em jogo**, e nada do anterior também.
  Eram **25 commits** desde a última sessão de verdade; hoje são 36.
- **A casa nunca foi vista terminando sozinha.** Que ela possa é
  afirmação de teste.
- **Fechar e reabrir o mundo de verdade nunca foi feito** — a bateria
  roda um servidor só, e é justamente o que a mina no save promete.
- **Um teste instável e uma falha não diagnosticada.** O instável foi
  diagnosticado em 08-21 e não era instável — ver o ciclo acima.

<details>
<summary>2026-08-20 — o ciclo anterior, em dezenove commits</summary>

O ciclo em que a colônia deixou de depender de você para os materiais, e
em que o mod parou de inventar casa.

**Cinco regras novas do autor**

| | |
|---|---|
| **Regra 25** | A colônia levanta a maior planta que couber no lote |
| **Regra 26** | A cadeia de produção, e a paleta de cada tipo de vila |
| **Regra 27** | 🔒 **Imutável** — só o catálogo do jogo, e o construtor aguarda o bloco específico |
| **Regra 28** | 🧪 **Provisória** — uma casa por bioma, e a obra não espera por sete peças |
| **Regra 29** | A mina: escada, duas salas de 7×4, e galeria sem fim |

- **O mineiro** abre uma mina de verdade — escada com picareta de
  diamante, sala no −10, outra no −20, e galeria sem fim.
- **O pastor** tosquia, e a ovelha continua viva.
- **O fundidor** faz vidro de areia, pela receita do jogo.
- **As casas são as do jogo.** A cabana que o mod inventava foi
  aposentada.
- **A obra que espera demais sai da frente**, depois de vinte ciclos.

**Corrigido, e as três causas vieram de jogo:** o alvo da obra comparado
com um id escrito no código; o cursor da busca de lote guardado pela
posição do centro, que troca de âncora a cada trinta segundos; e o miolo
oco da cabana oferecido como lote.

**Achado no caminho:** `TreeHarvester.isNaturalLeaf` não conferia chunk
descarregado e **derrubava o servidor**; `ChestDepositor.deposit` devolve
quantos **não** couberam, e o mineiro leu ao contrário.

**Refatorado:** `ConstructionPlanner` 703 → 414; `LumberjackWork` 1232 →
455, que era o pior arquivo do projeto.

</details>

</details>

</details>

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
