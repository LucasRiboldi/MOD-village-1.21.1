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

🏠 **O construtor** lê uma casa de vila de verdade dos arquivos do jogo e
a levanta, um bloco por segundo, na beira de uma rua que já existe. Cada
peça sai do baú da colônia antes de entrar no mundo — **a colônia nunca
inventa material**.

🌾 **O fazendeiro** tem nome, enxada e baú — e nenhum trabalho ainda.

Cada um ganha um nome sobre a cabeça e um quadro pregado no baú, para
você saber de relance quem é quem.

---

## As regras do jogo dele

**Vanilla primeiro.** Os aldeões são aldeões comuns. Os baús são baús
comuns. As receitas são as do jogo, perguntadas em tempo de execução —
não copiadas para dentro do mod. A casa é literalmente o mesmo arquivo
que o gerador de mundo usa.

**Nada é inventado.** Sem inventário virtual, sem contador abstrato de
recurso, sem economia paralela. Se a colônia tem 40 tábuas, há 40 tábuas
num baú que você pode abrir. Tire-as, e a colônia percebe.

**A sua construção está segura.** A única coisa que um trabalhador quebra
é árvore, e ele precisa provar que a árvore é árvore: tronco sem folha
viva acima conta como construção, não como floresta. Peças de vila
gerada são perguntadas diretamente ao jogo e deixadas em paz.

**Ele para sozinho.** A colheita acaba quando os baús enchem e recomeça
quando você tira alguma coisa. Nada cresce sem limite.

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

A colônia conta o que está fazendo no log do servidor. Os aldeões só
trabalham no horário de trabalho do Vanilla — use `/time set noon` se
não quiser esperar, e note que `/time set day` é **antes** de a janela
de trabalho abrir.

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
| Construção — casas e escolha de lote | 🧪 coberto por teste, nunca visto em jogo |
| Registro de construções e proteção | 🧪 coberto por teste, nunca visto em jogo |
| Agricultura, mineração, ferraria, defesa | ⬜ não começado |

```text
373 testes unitários  ·  80 testes de jogo  ·  ./gradlew build
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
🧪  construir expansão        coberto por teste, nunca visto em jogo
```

### O que falta, na ordem

| | Etapa | Estado |
|---|---|---|
| **1** | **Ver a casa subir.** A obra bloco a bloco, a casa virando infraestrutura da colônia, e o lote seguinte não caindo em cima dela | 🔒 exige sessão de jogo |
| **2** | **Decidir o rodízio de profissão.** A colônia dispensa quem não conseguiu baú "em favor de quem consegue", e o substituto também não consegue. Corrigir mexe numa regra, não no código | 👤 exige decisão |
| ~~**3**~~ | ~~**O motivo de não trabalhar como valor.**~~ As três profissões dizem por que não trabalharam, no mesmo vocabulário, e o log registra a mudança em vez do estado | ✅ **feito em 2026-08-15** |
| **4** | **Blocos de duas partes.** Porta e cama são postas como duas metades soltas. A saída provável é pô-las numa segunda passada, com o par junto | 🔨 pronto para fazer |
| **5** | **Estender a estrada.** Hoje a vila só constrói em beira de rua que já existe; quando ela acabar, a colônia para de crescer | 🔨 pronto para fazer |
| **6** | **Envelhecimento de tarefa.** Para que a tarefa mais antiga não seja esquecida para sempre | 🔨 pronto para fazer |
| **7** | **A proteção estrutural, e o lado do cliente.** Perguntar ao jogo quais blocos são de vila gerada; e nome, rachadura e braço na tela | 🔨 pronto para fazer |
| **8** | **O trabalhador pedir o que lhe falta**, em vez de travar | ⏸️ só depois do MVP fechar |

**Limites de hoje.** A colônia só produz tábua — uma casa de vila também
quer pedra, vidro e cama, e essas precisam já estar nos baús. As casas
sobem ao lado de ruas que já existem; a colônia ainda não pavimenta.
Porta e cama saem como metades soltas, então a casa fica um pouco tosca.

A lista completa — **32 itens abertos em 8 grupos**, com o que já foi
feito — está em [`docs/technical/Backlog.md`](docs/technical/Backlog.md).
O estado sempre atual está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md).

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
