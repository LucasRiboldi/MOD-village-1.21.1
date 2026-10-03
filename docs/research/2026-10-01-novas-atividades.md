# Novas atividades e ações para os aldeões — 2026-10-01

> Pedido do autor: *"pesquise melhorias que sejam criação de novas atividades
> ou ação que possam ser criadas sem estragar o projeto já criado"*. Estudo,
> nada implementado. Esforço: **P** (horas), **M** (um dia), **G** (vários
> dias, ADR).

---

## 1. O que existe hoje

Sete profissões (`ProfessionType`): mineiro, lenhador, pedreiro, fundidor,
carpinteiro, fazendeiro, pastor — mais o construtor. Tarefas (`TaskType`):
madeira, pedra, recurso de superfície, solo, lã, comida, fundir, fabricar
madeira e pedra, construir. Também: refeição do fim do expediente
(`VillageMeals`), pão do fazendeiro, viveiro, procriação de ovelhas, luz na
mina (`MineLighting`), fuga do curral e escalada (01-10).

**O que não existe** (conferido no código): defesa contra monstros e
ataques, pesca, compostagem, abelhas, vaca/galinha/porco (só como drop
automático), cana, abóbora, melancia, sino, comércio, iluminação das ruas,
recolher item caído no chão.

---

## 2. A regra para não estragar nada

Há dois tamanhos de mudança, e o risco é muito diferente.

**Ação** — algo que um aldeão que já existe faz **no tempo ocioso** (sem
tarefa reservada, a mesma pergunta que o `IdleHands` faz a cada segundo).
Não toca em `ProfessionType`, na Regra 11 ("uma de cada profissão"), nos
sete titulares da BigHouseMOD, no save nem na distribuição de tarefas.
Quem trabalha continua trabalhando; a ação só ocupa quem estava parado.

**Profissão nova** — mexe em tudo isso: a Regra 11 passa a exigir mais um
aldeão por vila (tirado de outro ofício), a BigHouseMOD tem 7 titulares
fixos, o `ProfessionAssigner` muda de ordem, o nome vai para o save (versão
antiga do mod não lê o novo) e os `switch` de telemetria e de log precisam do
caso novo. Pede ADR.

**Costuras que já existem e devem ser reusadas** (é o que mantém o projeto
inteiro): `ColonyChests.nearestFirst` (baús, Regra 45), `WorkTargets` +
`GoToWorkTargetTask` (andar), `WorkStall` e o encalhado (não travar),
`VillageFocus` (só com jogador), `BlockProtection` (Regra 3),
`ColonyEdits` (o que a colônia pôs), `ActivityTraces` e `/vc log`
(explicar), `VillageBounds` (onde é a vila).

**Duas armadilhas medidas nesta sessão:**

- código novo em `fabric.work` que chame `fabric.integration` ou
  `fabric.event` muda o texto do ciclo congelado do ArchUnit e reprova o
  build — ou se escreve a ação só com core, Minecraft e as fachadas que o
  `work` já usa, ou se recongela provando que o conjunto de ciclos é o mesmo;
- não há chave para desligar uma atividade: cada uma deveria nascer com
  uma (regra no `RULES.md` + constante), para o autor tirar sem reverter
  código.

---

## 3. Ações no tempo ocioso (menor risco)

| # | Ação | Quem faz | Por que vale | Inspiração | Esforço |
|---|---|---|---|---|---|
| A1 | **Recolher item caído** dentro da vila e guardar no baú | qualquer ocioso | ataca um defeito medido: 365 arenitos no chão com a obra esperando arenito (memória "baú cheio destrói o que a obra espera"); item no chão some em 5 min | "Nada se perde" do próprio projeto | **P–M** |
| A2 | **Iluminar a vila à noite**: tocha ou lanterna onde a luz das ruas está baixa | construtor ou ocioso | menos monstro nascendo dentro da vila, menos aldeão morto | Regrowth, *all villager work* | **M** |
| A3 | **Manter as ruas**: tapar buraco, tirar grama e folha do caminho | construtor ocioso | a ponta de rua já é calçada pelo mod; manter usa o mesmo código | Regrowth | **M** |
| A4 | **Compostar**: semente e muda que sobram viram farinha de osso, e a farinha acelera a roça | fazendeiro ocioso | fecha um ciclo de recurso que hoje termina no baú | MineColonies (compostador) | **P–M** |
| A5 | **Colher cana, abóbora e melancia** que existirem na vila | fazendeiro | cana dá papel (livro, mapa); abóbora e melancia, comida | — | **P** |
| A6 | **Reunião no sino** no fim do expediente | todos | só aparência e vida da vila; o `VillageMeals` já marca a hora | Vanilla (ponto de encontro) | **P** |
| A7 | **Entrar em casa quando há monstro perto** à noite | todos | o pânico Vanilla já foge; abrigo reduz morte | Millénaire (abrigo na guerra) | **M** |

**Recomendação:** A1 primeiro — é a única que corrige um defeito já visto
em jogo, e é a mais isolada (lê entidade de item, anda, deposita pelo
`ColonyChests`). Depois A2, que tem o maior efeito na sobrevivência.

---

## 4. Ofícios estendidos (risco médio)

Ação nova dentro de uma profissão que já existe — sem mexer na Regra 11.

| # | Extensão | Profissão | O que dá | Inspiração | Esforço |
|---|---|---|---|---|---|
| E1 | **Vaca, galinha e porco** procriados como as ovelhas (`ShepherdFlock`) | pastor | couro, ovo, carne, pena — hoje só existem como drop automático | MineColonies (cinco pastores) | **M** |
| E2 | **Pesca** num ponto de água da vila, pela tabela de pesca do jogo | fazendeiro ocioso | comida sem roça | MineColonies (pescador) | **M** |
| E3 | **Colmeia**: colher mel e favo sem irritar as abelhas (fogueira embaixo) | fazendeiro | vela, colmeia, comida | MineColonies (apicultor) | **M** |
| E4 | **Carvão vegetal** da tora sobrando | fundidor | combustível sem mina — e responde a pendência "combustível fora da economia" (ADR-028) | — | **P–M** |

---

## 5. Profissões novas (maior risco, ADR)

| # | Profissão | O que faz | Risco principal | Inspiração | Esforço |
|---|---|---|---|---|---|
| N1 | **Guarda** | combate zumbi e saqueador perto da vila, com espada e armadura do baú | aldeão Vanilla não ataca: precisa de comportamento de combate no Brain, e a ADR-004 restringe mexer em task Vanilla | Guard Villagers, MineColonies | **G** |
| N2 | **Entregador** | leva o que transbordou de um baú para outro, e o que a obra espera para perto dela | o `ChestRelief` já move entre baús sem ninguém andar — vira personagem, não ganho novo | MineColonies (entregador) | **M–G** |
| N3 | **Mercador** | baú de mercado: o jogador deixa item e recebe esmeralda (ou o contrário), pela demanda da vila | é o primeiro canal de troca com o jogador — muda o desenho "o jogador acha a vila e vai embora" | Millénaire (comércio) | **G** |

**Recomendação:** nenhuma agora. Se uma entrar, o **guarda** é o que mais
protege o que já existe (aldeão morto é profissão perdida e obra parada);
ele pede a ADR antes do código.

---

## 6. Ordem sugerida

| Ordem | Item | Esforço | Por quê |
|---|---|---|---|
| 1 | A1 recolher item caído | P–M | corrige defeito medido, isolado |
| 2 | A2 iluminar a vila | M | sobrevivência |
| 3 | E4 carvão vegetal | P–M | fecha a pendência do combustível |
| 4 | E1 vaca/galinha/porco | M | reusa o `ShepherdFlock` |
| 5 | A4 compostar + A5 cana/abóbora/melancia | P–M | cadeia do fazendeiro |
| 6 | A3 manter ruas | M | reusa a calçada da ponta de rua |
| 7 | N1 guarda (com ADR) | G | só depois das ações |

Cada item nasce com: regra no `RULES.md` (com a chave para desligar),
GameTest que falha sem ele, teste de mutação, e a linha de log que o prova
em jogo — o mesmo padrão da sessão de 01-10.

---

## Fontes

- MineColonies: [lista de ofícios (CurseForge)](https://www.curseforge.com/minecraft/mc-mods/minecolonies), [informação dos trabalhadores](https://www.minecolonieswiki.orionminecraft.com/source/systems/workerinfo), [o compostador](https://steemit.com/utopian-io/@fireruner/minecolonies-new-ai-worker-the-composter)
- Guard Villagers: [guia do mod](https://craftdownunder.co/guides/mods/guard-villagers), [Modrinth / BoxToPlay](https://www.boxtoplay.com/en/minecraft-hosting/minecraft-server/modrinth-mods/guard-villagers-633)
- Millénaire: [site oficial](https://www.millenaire.org/indexen.html), [comércio](https://technicpack.fandom.com/wiki/Trading_(Millenaire)), [aldeões](https://technicpack.fandom.com/wiki/Villagers_(Millenaire))
- Ações autônomas: [Regrowth](https://www.curseforge.com/minecraft/mc-mods/regrowth), [all villager work](https://www.curseforge.com/minecraft/mc-mods/all-villager-work), [Helpful Villagers](https://www.curseforge.com/minecraft/mc-mods/helpful-villagers), [Productive Villagers](https://www.curseforge.com/minecraft/mc-mods/productive-villagers)
