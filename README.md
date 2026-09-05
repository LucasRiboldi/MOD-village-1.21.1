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
desce cavando **em escada** — de picareta de madeira, como todo
trabalhador começa —, para poder subir de volta. Dez blocos abaixo abre uma sala de 7×4; desce mais dez virando
para outro lado, abre outra sala; e do vigésimo bloco em diante segue
numa galeria — que **não é um cano reto**: a cada oito colunas ela abre
um bolsão ao lado, o que dá mais parede exposta e mais minério à vista.
Barreira à frente — bedrock, a casa de alguém, pedra sem onde pisar — e a
galeria vira. Quatro curvas fecham a volta do nível, e a mina **desce mais
um**, atrás do que só existe fundo.

**Saiu água?** Ele tapa a nascente com pedregulho na hora e desvia a
galeria. Mina inundada não é mina difícil — é mina onde o aldeão não fica
de pé. Com lava, o mesmo, e ali é a vida dele.

E ele **vê o minério** — todo tipo, pela etiqueta do próprio jogo, com as
variantes de ardósia —, e vai **no mais raro primeiro**: entre carvão no
chão e diamante na parede, ele escolhe o diamante. A veia é seguida até
acabar, e o minério colado na parede vem antes da parede.

Ele nunca cava vila gerada nem casa da colônia: a boca da mina não se
abre sobre elas, e cada bloco é conferido de novo antes da picareta. A
galeria vai sendo **acesa** com tocha enquanto ele avança, porque mina
escura é mina com monstro nascendo ao lado de um aldeão desarmado.

E **quando a mina não tem onde nascer** — vila cercada de água, encosta,
nenhuma coluna servindo —, ele não fica parado: passa a raspar
afloramento de pedra na superfície.

🐑 **O pastor** tosquia a ovelha e traz a lã. A ovelha continua viva e a
lã volta a crescer — é a colheita que se repete. A lã sai da cor do
rebanho, e não branca sempre.

🔥 **O fundidor** transforma areia em vidro, ferro cru em lingote e
arenito em arenito liso, pela receita de fornalha do próprio jogo. Sem
forno no mundo: ele transforma o que está no baú.

🪚 **O fabricante** tira tronco do baú, faz tábua pela receita do próprio
jogo, e devolve. Ele **converte cerca de metade da madeira e deixa o
resto em tora** — vinte troncos viram dez tábuas e dez troncos —, porque
parte da casa é feita de tora direto: a de planície pede dezesseis vigas
descascadas, e elas não saem de tábua. Também **descasca tronco**, monta
**tocha** e monta **vidraça**.

🏠 **O construtor** levanta a casa um bloco por segundo, na beira de uma
rua que já existe, com a **porta virada para a rua** e o piso no nível
dela. Ele escolhe um lote livre — livre no volume inteiro, não só no
chão —, arranca o mato que estiver ali, e tira cada peça do baú da
colônia antes de pôr no mundo. **A colônia nunca inventa material**: o
que falta, ele fabrica do que houver nos baús; o que não dá para
fabricar, ele espera.

**As casas são as do próprio Minecraft, e só elas.** O mod não inventa
casa: cada vila levanta o que a pasta de estruturas do jogo tem para o
bioma dela — planície, savana, taiga, nevada e deserto.

🌾 **O fazendeiro** colhe a lavoura madura, **replanta com a semente da
própria colheita** e guarda a comida no baú dele. Ele procura do centro
da vila para fora, e só toca no que está pronto.

Quem diz se está madura é o **bloco**, e não uma lista escrita no mod:
vale para trigo, cenoura, batata e beterraba, e para o que um datapack
plantar depois.

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
entram sozinhos.

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
do mais perto da obra para o mais longe, somando entre eles até dar.

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

## O que já funciona

| | |
|---|---|
| ✅ **Sete profissões** | lenhador, mineiro, pastor, fundidor, fabricante, construtor e fazendeiro |
| ✅ **A cadeia da madeira, ponta a ponta** | cortar → fabricar tábua → levantar casa. Vista funcionando numa vila de verdade |
| ✅ **A mina** | escada, salas, galeria com bolsões, tocha atrás do mineiro, descida de nível, veio de minério, água tapada |
| ✅ **Casas do próprio Minecraft** | planície, savana, taiga, nevada e deserto. O mod não inventa casa |
| ✅ **A colônia nunca inventa material** | o que falta é fabricado do que houver nos baús; o que não dá, ela espera |
| ✅ **Regra 3** | vila gerada e construção da colônia são intocáveis, conferidas bloco a bloco |
| ✅ **Sem menu nenhum** | nada de GUI, nada de item de configuração. Você acha a vila e vai embora |
| ✅ **Servidor dedicado** | quem entra não precisa do mod no cliente |

## O que falta

**Profissões que o modelo econômico prevê e ninguém escreveu** — a lista
inteira, com as razões, está em
[`Village-Economy.md`](docs/technical/Village-Economy.md):

| Profissão | Por quê | Prioridade |
|---|---|---|
| **Pedreiro** | Fecha `mineiro → pedra → pedreiro → construtor`. Hoje o construtor consome pedregulho cru e não tem escada, laje nem muro | ★★★★★ |
| **Pecuarista** | Couro, carne, ovo, leite — nenhuma entra na vila hoje | ★★★★ |
| **Transportador** | Hoje cada um guarda no **próprio** baú. Com mais profissões isso não escala | ★★★★ |
| **Armazenista** | Estoque central e tarefa criada por escassez | ★★★★ |
| **Guarda** | A defesa, que o modelo prevê | ★★★ |
| **Explorador** | Define a área de expansão | ★★ |

**E o que ainda não está fechado no que já existe:**

- 🔴 **A casa ainda sobe com a barreira de teste.** Na sessão de
  2026-09-04 foram **47 peças de 169** — 28% da obra. Todas
  `stripped_oak_log`, e a cadeia que deveria fazê-las não entregava
  porque o lenhador não entregava tronco. É o que falta para a Fase 2
  fechar.
- 🟡 **O detector de imobilidade era zerado a cada alvo novo** — o que
  deixou dois mineiros parados vinte e cinco minutos com os contadores em
  zero. **Fechado em 2026-09-04:** ele passou a recomeçar quando o
  trabalhador *anda* ou *trabalha*, e não quando pega alvo. Eram três
  profissões e não seis — construtor e fabricante já estavam certos. Tem
  teste, e **nenhuma sessão o viu rodar**.
- 🟠 **O lenhador é o único que cobra imobilidade enquanto trabalha.**
  Ele pergunta se o aldeão saiu do lugar *antes* de conferir se já está
  na árvore, então uma árvore grande pode devolvê-lo à fila por estar
  derrubando-a. Achado ao ler, e ainda não visto falhar.
- 🟠 **A navegação dentro da rocha.** Seis defeitos numerados (E30–E35)
  saíram todos da mesma raiz: a navegação do Vanilla não traça caminho
  por dentro de uma mina. O mod contorna andando um passo de cada vez
  pela ordem de cavar; os quatro mods de agente que resolveram isso de
  verdade trocaram a navegação por um A\* próprio. É a decisão em aberto.
- 🟠 **Mina de save antigo.** A galeria mudou de forma em 2026-09-03, e o
  cursor gravado aponta para o desenho velho. A leitura do mundo deve
  recuar sozinha — mas isso ainda não foi visto acontecendo.
- 🟡 **O detector de imobilidade** foi visto em jogo só no mineiro. Nas
  outras cinco profissões é a mesma peça, com teste, sem sessão.
- 🟠 **O baú do trabalhador assoreia.** Vara, maçã e muda não são
  recursos da colônia: nada as retira, e cada uma ocupa um slot para
  sempre. Desde 2026-09-04 a colheita transborda para os outros baús em
  vez de ser destruída — mas baú que só enche acaba cheio.
- 🟡 **Sem economia de moeda, sem comércio, sem crescimento populacional
  dirigido.** As fases 3, 4 e 5 do modelo não começaram.

> A lista canônica e datada do que está aberto vive em
> [`TODO.md`](TODO.md). Este resumo é a versão curta.

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
3. Baixe o **[village-colony-0.3.0.jar](downloads/village-colony-0.3.0.jar?raw=1)**
   e ponha ao lado dela.
4. Abra o jogo, carregue um mundo, e ache uma vila.

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

## Antes de instalar

**Isto é um alpha, e o número da versão diz a verdade.** O mod carrega em
cliente e em servidor dedicado. A cadeia de madeira — cortar, fabricar,
construir — e **a mineração** já foram vistas funcionando numa vila de
verdade. As profissões mais novas ainda estão sendo acertadas, e o que
falta está na lista acima.

Use num mundo de teste antes de usar no seu mundo de sempre. Ele mexe no
mundo: derruba árvore, cava pedra e levanta casa.

---

<div align="center">

**Licença MIT** · Feito para Minecraft 1.21.1 com Fabric

</div>
