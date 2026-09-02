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
desce cavando **em escada** com picareta de diamante, para poder subir de
volta. Dez blocos abaixo abre uma sala de 7×4; desce mais dez virando
para outro lado, abre outra sala; e do vigésimo bloco em diante segue
numa galeria sem fim, de dois blocos de altura. Barreira à frente — lava,
bedrock, a casa de alguém — e a galeria vira.

E ele **vê o minério**: carvão e ferro, comuns e de ardósia, e a veia é
seguida até acabar — o minério colado na parede vem antes da parede.

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
jogo, e devolve. Ele para quando metade do estoque da colônia é tábua,
para que o lenhador sempre tenha onde pôr mais. Também **descasca
tronco**, monta **tocha** e monta **vidraça**.

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
cliente e em servidor dedicado, e a cadeia de madeira — cortar, fabricar,
construir — já foi vista funcionando numa vila de verdade, do começo ao
fim. A mineração e as profissões mais novas ainda estão sendo acertadas.

Use num mundo de teste antes de usar no seu mundo de sempre. Ele mexe no
mundo: derruba árvore, cava pedra e levanta casa.

---

<div align="center">

**Licença MIT** · Feito para Minecraft 1.21.1 com Fabric

</div>
