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
| O construtor fabrica o que falta para a obra | 🧪 coberto por teste, nunca visto em jogo |
| Expediente do dia claro inteiro | 🧪 coberto por teste, nunca visto em jogo |
| Porta virada para a rua, lote no nível dela | 🧪 coberto por teste, nunca visto em jogo |
| Madeira conforme o bioma da vila | 🧪 coberto por teste, nunca visto em jogo |
| Cama, baú e lampião dentro de cada casa | 🧪 coberto por teste, nunca visto em jogo |
| Baú criado ao lado da cama quando não há nenhum | 🧪 coberto por teste, nunca visto em jogo |
| Registro de construções e proteção | 🧪 coberto por teste, nunca visto em jogo |
| Estrada crescendo com a vila | ⬜ não começado |
| Agricultura, mineração, ferraria, defesa | ⬜ não começado |

```text
402 testes unitários  ·  119 testes de jogo  ·  ./gradlew build
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
| **1** | **Rodar em jogo as sete regras de 08-19.** Fabricação, alcance, porta na rua, expediente, nível da rua, bioma e mobília. Todas têm teste e **nenhuma foi vista funcionando** | 🔒 exige sessão de jogo |
| **2** | **Regra 15 — estender a estrada.** A vila só constrói em beira de rua que já existe; quando ela acabar, a colônia para de crescer. Ficou mais urgente com a Regra 19 | 🔨 pronto para fazer |
| **3** | **Regra 16 — espaço em volta da casa.** Distância mínima e máxima entre construções, e o lote conferido como volume | 🔨 decidido em 2026-08-18 |
| **4** | **Regra 10, metade do fabricante.** Porta, janela, cama e baú por estoque, sem depender de haver obra | 🔨 depende do `ItemRequest` |
| **5** | **Regra 11 — uma de cada profissão por vila.** O mecanismo existe; falta a garantia e o teste | 🔨 pronto para fazer |
| **6** | **Envelhecimento de tarefa**, para que a mais antiga não seja esquecida | 🔨 pronto para fazer |
| **7** | **A proteção estrutural, e o lado do cliente.** Perguntar ao jogo quais blocos são de vila gerada; e nome, rachadura e braço na tela | 🔨 pronto para fazer |
| **8** | **O trabalhador pedir o que lhe falta**, em vez de travar | ⏸️ toca o centro do sistema |

**Duas decisões esperam o autor:** o que fazer com o centro da colônia,
que troca de âncora e volta a cada 30 segundos; e se a casa do jogo —
cujo schema já está no mod — vira a obra padrão, o que exigiria você
guardar pedregulho, vidro e tronco descascado no baú.

<details>
<summary>Etapas fechadas nos ciclos anteriores</summary>

| Etapa | |
|---|---|
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

**Limites de hoje.** A colônia produz tábua, e a partir dela fabrica o
que a obra pedir — porta, baú, escada. O que ela **não** produz é o que
pede minerar, fundir, tosquiar ou descascar: pedra, vidro, lã e ferro.
Por isso a obra dela é uma cabana de tábua, e não a casa de vila do
jogo. O que falta, **você guarda no baú**: o construtor tira de qualquer
baú da colônia sem perguntar de onde veio, e o log diz o que está
faltando.

A cama e o lampião de cada casa caem nessa conta — a casa **não espera**
por eles, termina sem, e eles entram sozinhos quando aparecer lã e ferro
num baú.

As casas sobem ao lado de ruas que já existem; a colônia ainda não
pavimenta, e essa é a próxima etapa. A escada ainda sai no estado padrão
da planta; a porta, não — ela dá na rua.

As pendências por prioridade estão em [`TODO.md`](TODO.md), com o que já
foi feito, o que falta e o que espera decisão sua. A lista longa está em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md), e o estado
sempre atual — o enunciado das 21 regras, uma a uma — em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

---
## Último ciclo de desenvolvimento

**2026-08-19** — o ciclo em que a casa fechou. Começou com o relatório
repetindo `waiting for minecraft:oak_door` com 154 tábuas guardadas, e
terminou com sete regras novas e uma cabana de pé em jogo.

**Implementado**

- **Regra 10** — o construtor fabrica o que a obra pede. Faltava a
  pergunta invertida: o mod sabia "o que sai deste tronco?" e não "o que
  faz uma porta?". Junto vieram as duas metades decididas em 08-15 e
  nunca escritas — os baús percorridos **por distância** até a obra, e a
  retirada **somando entre eles**.
- **Regra 14** — o construtor alcança o alto da obra. O alcance era uma
  esfera de raio 5, e o bloco do telhado ficava fora dela com ele de pé
  dentro do lote.
- **Regra 17** — a porta dá na rua. A direção sempre foi conhecida e era
  descartada depois de calcular o canto.
- **Regra 18** — o dia claro inteiro é expediente. A janela era a do
  Vanilla: WORK das 2.000 às 9.000, sete mil tiques num dia de vinte e
  quatro mil, com **três mil tiques de sol** parados.
- **Regra 19** — o lote fica no nível da rua, para se entrar e sair a pé.
- **Regra 20** — cada vila constrói na madeira do seu bioma, e o mod
  deixa de aceitar só planície.
- **Regra 21** — toda casa nasce com cama, baú e lampião. A casa **não
  espera** por lã e ferro: termina sem, e a peça entra depois.
- **Uma pasta de schemas** em `data/villagecolony/structure/`, com a
  casa pequena de planície e um README de como fazer outra.

**Corrigido**

- **O teto de colheita decidia o que é árvore.** A copa era procurada a
  partir do grupo de troncos já cortado em 24, então abeto gigante e
  carvalho-escuro viravam "não é árvore" — e a recusa é permanente. O
  log dizia `24 logs without a living canopy`, e 24 é o teto: quando o
  número da recusa é exatamente o limite, o limite é a causa.
- **A obra dormia por falta de mobília.** `hasMaterialForNextBlock`
  passou a contar a fabricação e a ignorar mobília.
- **Dois testes afirmavam sobre o servidor inteiro.** A bateria roda
  concorrente, e `blocksInProgress` era global — um teste contava os
  lenhadores do vizinho.

**Melhorado**

- A retirada de material saiu de `BuilderWork` para `ColonySupply`, que
  a obra e a mobília compartilham.
- 383 → 395 testes unitários, 99 → 115 testes de jogo.
- O README deixou de afirmar coisas que tinham deixado de ser verdade —
  o horário de trabalho do Vanilla, e a contagem de testes.

**Verificado em jogo, 2026-08-19**

- **A cabana subiu do começo ao fim** e virou infraestrutura da colônia,
  às 02:12. Outra foi planejada 26 segundos depois. É o sexto e último
  passo do MVP.
- **O guarda de travamento fechou o ciclo** pela primeira vez: os dois
  lenhadores devolveram a tarefa e a árvore inalcançável saiu da escolha.

**Pendente**

- **As sete regras deste ciclo não rodaram em jogo.** A sessão que
  fechou a casa rodou um jar de quatro dias antes — o defeito dos
  dezesseis blocos pulados naquela casa é justamente o que a Regra 19
  passou a recusar.
- **Um teste instável** (`theStallGuardReturnsTheTaskAndForgetsTheTree`),
  cerca de 1 falha a cada 4 execuções, anterior a este ciclo.
- **A metade do fabricante da Regra 10**, que depende do `ItemRequest`.

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
