<div align="center">

<img src="src/main/resources/assets/villagecolony/icon.png" width="180" alt="Village Colony">

# Village Colony

### Suas vilas param de esperar por você.

*Um mod Fabric que transforma vilas do Minecraft Vanilla em colônias que
trabalham, produzem e crescem sozinhas.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Ambiente](https://img.shields.io/badge/Lado-Servidor%20%7C%20Singleplayer-lightgrey)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.1.0%20alpha-orange)
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

🪚 **O fabricante** tira tronco do baú, faz tábua pela receita do próprio
jogo, e devolve. Ele para quando metade do estoque da colônia é tábua,
para que o lenhador sempre tenha onde pôr mais.

🏠 **O construtor** levanta uma cabana de tábua, um bloco por segundo, na
beira de uma rua que já existe. Cada peça sai do baú da colônia antes de
entrar no mundo — **a colônia nunca inventa material**.

A cabana é de propósito modesta: 71 tábuas e uma porta, tudo o que sai de
tronco. A casa de vila do próprio jogo pede 43 pedregulhos, 3 vidros e
uma cama de lã, e nenhum aldeão deste mod minera, funde ou tosquia — a
obra parava no primeiro pedregulho e ficava parada para sempre. A casa
bonita volta a ser o alvo no dia em que essas cadeias existirem.

🌾 **O fazendeiro** tem nome, enxada e baú — e nenhum trabalho ainda.

Cada um ganha um nome sobre a cabeça e um quadro pregado no baú, para
você saber de relance quem é quem.

---

## As regras do jogo dele

**Vanilla primeiro.** Os aldeões são aldeões comuns. Os baús são baús
comuns. As receitas são as do jogo, perguntadas em tempo de execução —
não copiadas para dentro do mod. O leitor de estruturas do jogo continua
no código e é o que carrega obra antiga de save; a obra que a colônia
abre hoje é a cabana do mod, pelo motivo explicado acima.

**Nada é inventado.** Sem inventário virtual, sem contador abstrato de
recurso, sem economia paralela. Se a colônia tem 40 tábuas, há 40 tábuas
num baú que você pode abrir. Tire-as, e a colônia percebe.

**A sua construção está segura.** A única coisa que um trabalhador quebra
é árvore, e ele precisa provar que a árvore é árvore: tronco sem folha
viva acima conta como construção, não como floresta. Peças de vila
gerada são perguntadas diretamente ao jogo e deixadas em paz.

**Ele para sozinho.** A colheita acaba quando os baús enchem e recomeça
quando você tira alguma coisa. Nada cresce sem limite.

**A casa nasce mobiliada, no estilo da vila.** Ela é feita da madeira do
bioma — carvalho na planície, pinheiro na taiga, acácia na savana —, tem
a porta virada para a rua e assenta no nível dela, para você entrar e
sair a pé. Dentro vão uma cama, um baú e um lampião. O baú a colônia faz;
a cama e o lampião pedem lã e ferro, que ela não produz — guarde num baú
e eles entram sozinhos. **A casa não fica esperando por eles**: termina
sem, e a vila continua crescendo.

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
3. Ponha o `village-colony-0.1.0.jar` ao lado dela.
4. Abra o jogo, carregue um mundo, e ache uma vila de planície.

Funciona em singleplayer e em servidor dedicado. Quem entra num servidor
que tem o mod **não** precisa instalá-lo no cliente.

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
| Construção — o construtor põe bloco no mundo | 🟡 **visto em jogo**, 2 blocos; nenhuma casa terminada ainda |
| Baú criado ao lado da cama quando não há nenhum | 🧪 coberto por teste, nunca visto em jogo |
| Registro de construções e proteção | 🧪 coberto por teste, nunca visto em jogo |
| Agricultura, mineração, ferraria, defesa | ⬜ não começado |

```text
395 testes unitários  ·  112 testes de jogo  ·  ./gradlew build
```

---

## As etapas

**Cinco dos seis passos do MVP estão verificados em jogo.** Falta um, e
é o único que bloqueia.

```text
✅  detectar vila             verificado em jogo
✅  registrar aldeões         verificado em jogo
✅  organizar trabalhadores   verificado em jogo
✅  coletar recursos          verificado em jogo
✅  produzir materiais        verificado em jogo
🟡  construir expansão        o construtor põe bloco; casa nenhuma terminada
```

### O que falta, na ordem

| | Etapa | Estado |
|---|---|---|
| **1** | **Ver a casa inteira subir.** Os dois primeiros blocos foram vistos em 2026-08-15, e parte da casa em 2026-08-18 — foi essa sessão que expôs a **Regra 14**: o alcance do construtor era uma esfera de raio 5, e o bloco alto ficava fora dela com ele de pé dentro do lote. Corrigido e coberto por teste de jogo; falta a cabana terminada, virando infraestrutura da colônia | 🔒 exige sessão de jogo |
| ~~**2**~~ | ~~**O rodízio de profissão.**~~ A colônia só dispensa quem não tem baú quando existe baú livre de verdade para o substituto — contar candidatos não era contar baús | ✅ **feito em 2026-08-15** |
| ~~**3**~~ | ~~**O motivo de não trabalhar como valor.**~~ As três profissões dizem por que não trabalharam, no mesmo vocabulário, e o log registra a mudança em vez do estado | ✅ **feito em 2026-08-15** |
| ~~**4**~~ | ~~**Blocos de duas partes.**~~ A porta é uma porta e a cama é uma cama. Sobra a **orientação**: escada e porta saem no padrão | ✅ **feito em 2026-08-15** |
| **5** | **Regra 15 — estender a estrada.** Hoje a vila só constrói em beira de rua que já existe; quando ela acabar, a colônia para de crescer | 🔨 pronto para fazer |
| **5b** | **Regras 16 e 17 — o lote com espaço em volta, e a casa com uma lateral na rua.** Distância mínima e máxima entre construções, lote conferido como volume, e a planta girando para a porta dar na rua | 🔨 decidido em 2026-08-18 |
| **6** | **Envelhecimento de tarefa.** Para que a tarefa mais antiga não seja esquecida para sempre | 🔨 pronto para fazer |
| **7** | **A proteção estrutural, e o lado do cliente.** Perguntar ao jogo quais blocos são de vila gerada; e nome, rachadura e braço na tela | 🔨 pronto para fazer |
| **8** | **O trabalhador pedir o que lhe falta**, em vez de travar | ⏸️ só depois do MVP fechar |

**Limites de hoje.** A colônia só produz tábua. A obra dela é por isso
uma cabana de tábua, e não a casa de vila do jogo — essa pede pedra,
vidro e lã, e a colônia não minera, não funde e não tosquia. O que ela
não produz, **você guarda no baú**: o construtor tira de qualquer baú da
colônia sem perguntar de onde veio, e o log diz o que está faltando.

As casas sobem ao lado de ruas que já existem; a colônia ainda não
pavimenta. Porta e cama já saem inteiras; o que ainda sai no padrão é a
orientação — escada apontando para o mesmo lado, cabeceira de cama onde
o jogo a puser.

As pendências por prioridade estão em [`TODO.md`](TODO.md). A lista
completa — **34 itens abertos em 9 grupos**, com a razão de cada um —
está em [`docs/technical/Backlog.md`](docs/technical/Backlog.md).
O estado sempre atual está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

---

## Último ciclo de desenvolvimento

**2026-08-15** — cinco sessões em jogo, cerca de duas horas, e as travas
que elas expuseram. O ciclo começou com a casa parada em `151 blocks
left` e terminou com o construtor pondo bloco no mundo.

**Implementado**

- **Regra 7** — o lenhador replanta em toda saída do trabalho, e não só
  ao ir buscar a próxima árvore. *Visto em jogo: 15 mudas em 15 árvores.*
- **Regra 8** — um baú nasce ao lado da cama quando não há nenhum ao
  alcance, com cinco recusas para não estragar a casa de ninguém.
- **Regra 9** — árvore que o lenhador não alcança sai da escolha por
  6.000 ticks, e quem a marca é o guarda de travamento.
- **Regra 12** — o centro da colônia deixa de ser a média das camas — um
  ponto que podia cair no ar — e passa a ser a cama mais próxima dela.
- **Regra 13** — a obra do MVP passa a ser uma cabana de 71 tábuas e uma
  porta, que a colônia produz inteira.
- O relatório do construtor diz o estado da tarefa de obra e **de que
  material a obra está à espera**.
- Um teste que lê qualquer planta e escreve a lista de compras no log.

**Corrigido**

- Tarefa que exige baú deixa de ir para quem não tem baú — antes ela era
  entregue e devolvida a cada ciclo, sem nada ser produzido.
- `WAITING_RESOURCES` deixa de ser estado terminal. A obra que uma vez
  ficasse sem material não era tentada nunca mais, ainda que o baú
  enchesse no minuto seguinte.
- Obra de planta antiga e sem um bloco de pé sai da frente, em vez de
  prender a colônia numa meta impossível gravada no save.
- A contagem de tarefas abertas volta ao log — ela calava exatamente
  quando a distribuição parava, que é quando fazia falta.
- O relógio de travamento do lenhador deixa de ser invisível.

**Melhorado**

- O guarda de travamento ganhou teste depois de existir sem nenhum desde
  a TASK-050 — o limite passou a ser ajustável para a bateria alcançá-lo.
- 375 → 383 testes unitários, 85 → 99 testes de jogo.

**Pendente**

- **A cabana inteira nunca foi vista subindo.** Dois blocos foram postos;
  nenhuma casa terminou.
- **Regra 10** — o construtor fabricando o que a obra pede. Decidida,
  não começada; é a maior das que restam.
- Cinco correções deste ciclo **têm teste mas nunca rodaram em jogo**: o
  baú ao lado da cama, o despertar de `WAITING_RESOURCES`, a árvore fora
  de alcance, a cabana e o descarte da obra antiga.

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
