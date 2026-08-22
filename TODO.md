# TODO

**Atualizado:** 2026-08-22, ao fim do ciclo das nove decisões.

O enunciado de cada regra está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md) §18;
a lista longa, com a razão de cada item, em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md). Onde os dois
discordarem, **vale este arquivo** — o Backlog parou em 08-15 e ainda
conta 375 testes.

**A distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas.

```text
458 testes unitários  ·  154 testes de jogo  ·  29 regras do autor
7 trabalhadores com código  ·  6 arquivos acima do teto de 500 linhas
bateria: 1 instável, 2 falhas em 12 rodadas — ver a seção seguinte
```

---

## ✅ As duas correções, provadas em jogo — 2026-08-22, 03:45

Terceira sessão do dia, treze minutos, e ela mediu exatamente o que
precisava ser medido.

**O construtor chega — provado**

```text
builders: 1 working, BUILDING at ColonyPos[x=-5656, y=64, z=-798]
builders: 1 working, WAITING_RESOURCES ...        ← só se chega aqui TOCANDO o bloco
Builder 8a0988a5 stopped — no minecraft:smooth_sandstone in the colony chests
```

`WAITING_RESOURCES` só é alcançado depois de `isWithinReach` ser verdade e
`takeMaterial` falhar. `could not reach` = **0** na sessão inteira, e o
maior travamento foi de **117 ticks** — seis segundos, contra os 2400 de
duas sessões atrás.

**A conta de pedra abre tarefa — provado**

`no miner work: no task open for it` apareceu **uma vez em treze
minutos**. Antes era toda passagem de ciclo.

---

## 🔴 E parou no elo que estava previsto

```text
no minecraft:smooth_sandstone in the colony chests    ×7
```

A colônia cava **arenito** e a casa pede o **liso**. O fundidor conhece
duas linhas — areia→vidro e ferro cru→lingote — e pedra não é uma delas.

**A decisão que vem junto, e ela é do autor.** Copiar o vidro é
mecânico: `SMOOTH_SANDSTONE` vira `ResourceType`, a meta desce para
`SANDSTONE` como o vidro desce para areia, e `SmelterWork.rawFor` ganha a
terceira linha. O que não é mecânico é o **grupo**: hoje `SANDSTONE` e
`COBBLESTONE` estão os dois em `ResourceGroup.STONE`, e o déficit conta
por grupo — então uma vila de deserto com pedregulho no baú **já acha
que a meta de arenito está cumprida**, e o fundidor queimaria pedregulho
achando que faz arenito liso. Separar os dois em grupos próprios
conserta, e mexe numa tabela de decisão.

---

## 🏜️ A primeira sessão em 36 commits — 2026-08-22

Vinte e oito minutos, 01:04 → 01:32. O jar novo foi o que rodou: a linha
`TEST BARRIER` só existe nele.

**O que ela provou**

| | |
|---|---|
| **A vila de deserto planejou uma casa do catálogo** | `desert_small_house_1`, 113 blocos. Inédito — ela nunca tinha passado de contratar |
| **A Emenda 4 segura o centro** | 4 movimentos em 28 min, todos com a contagem subindo (9 → 17 → 23 camas) e ≤16 blocos. **23 varreduras do jogador recusadas sem mover nada.** Nada parecido com o salto de 65 de 08-15 |
| **O guarda de travamento do lenhador dispara em jogo** | 4 vezes, `made no progress ... returned to the queue` |
| **7 colônias, 122 trabalhadores, 6 construções** | e a persistência aguentou |

**O que ela quebrou, e os dois foram corrigidos no mesmo dia**

**1 — O construtor não chegava ao bloco.** Oito minutos, três voltas do
guarda de dois minutos, zero blocos. `footOf` mandava o aldeão ao pé da
coluna na altura da origem da obra — e no deserto essa altura está
**dentro da duna**. Andar para dentro de bloco sólido é pedir um caminho
que não existe. `standingSpotNear` procura onde um aldeão cabe de pé, e
o guarda passou a dizer por que não chegou.

**2 — A conta de pedra pedia o bloco errado.** O mineiro ficou parado a
sessão inteira com os baús vazios. Lendo a casa no jar do jogo:

```text
 60  smooth_sandstone
 27  sandstone_slab
  5  sandstone            ← o único que a conta enxergava
  1  smooth_sandstone_stairs
```

93 na família, 5 no bloco puro. É **a terceira vez** que este defeito
aparece e sempre no deserto — vidro/vidraça, rua de terra/arenito liso,
e agora parede de arenito/arenito liso. A conta passou a ser por
família, e isso conserta a taiga junto (`cobblestone_stairs` e
`cobblestone_wall` também estavam de fora).

**O elo seguinte, e ele está aberto:** a colônia vai **cavar** arenito, e
a casa pede o **liso**. Quem funde pedra ainda não existe — o fundidor só
tem meta de vidro e de ferro. A obra vai esperar, que é o estado certo, e
agora com material no baú em vez de baú vazio.

---

## ⚠️ A bateria quase fechou, e o que a abriu era defeito de jogo

Ela ficou vermelha por sete commits, e a causa não era de teste.

`VillagerScanner.scan` partia sempre de `colony.center()`. Isso valia
enquanto o centro perseguia a última observação: adotar um aglomerado
movia o centro para perto dele, e a caixa de busca ia junto. **A Emenda
4 parou o centro**, e a caixa ficou para trás.

O efeito, em jogo: uma colônia que adote um aglomerado a dezenas de
blocos do próprio centro fica dona dele **e não enxerga um aldeão sequer
ali**. A vila cresce para um lado e a colônia procura gente no outro.
Ninguém vira trabalhador, nenhuma profissão é atribuída, e o log não diz
nada — `Registered 0 villagers` é silencioso por construção.

**A correção:** quem adota diz **de onde** veio a observação, e o
registro parte dali. O centro continua parado, que é a decisão; o
registro segue as camas, que é onde a gente está.

**O que ele fechou, medido em doze rodadas depois da correção:**

```text
villagersbecomeworkerswithaprofession    0 falhas em 12   ✅ fechado
thestallguardreturns...forgetsthetree    2 falhas em 12   ⚠️ melhorou
```

**A ressalva, e ela corrige o que eu escrevi antes.** As cinco primeiras
rodadas depois da correção vieram verdes, e daí saíram duas frases
otimistas demais — no commit `7832b10` e na primeira versão desta seção
— dizendo que a instabilidade do lenhador tinha a mesma causa e estava
fechada. **Não está.** Ela caiu de 3 em 10 para 2 em 12, o que é uma
melhora real e não uma explicação. A causa dela continua sem
diagnóstico.

---

## ✅ O ciclo das nove decisões — 2026-08-21

O autor respondeu as nove perguntas em aberto de uma vez, e todas foram
aplicadas.

**1 — ADR-003 Emenda 4: o centro é da sonda, e de mais ninguém.**
A contagem de camas e a posição do centro eram a mesma decisão em
`Colony.observe`, e nunca foram a mesma pergunta. O portão tinha três
portas — empate, prova geométrica e sino — e todas moviam o centro. Agora
a contagem continua entrando por qualquer observação; a posição, só numa
leitura da sonda. Fecha o sintoma de 08-18 a 08-20 e o salto de 65 blocos
de 08-15.

**2 — Regra 28: a barreira de teste passou a gritar.**
A justificativa dela caducou — as sete cadeias fecharam entre 08-20 e
08-21. Ela fica até a primeira sessão de jogo, mas cada peça riscada sai
como `WARN` dizendo **qual cadeia** deveria tê-la produzido, e a sessão
termina com a soma. Nasceu `TestBarrier`, e apagá-la devolve a Regra 27
sem exceção.

**3 — Regra 21: morreu.**
`HouseFurnishing` fazia duas coisas: repunha mobília riscada — isso
morreu — e declarava a demanda de lã e ferro, que era a **única** origem
dessas metas. Essa parte passou para `WorkMaterials`, lendo a obra
aberta, como o vidro e o carvão já faziam. **Cama e lampião saíram da
barreira** por consequência: sem quem os reponha, riscá-los deixaria a
casa sem eles para sempre.

**4 — O `.nbt` da Mojang: declarado.**
Nasceu o `NOTICE`. Diz o que o arquivo é, de onde veio, em que commit
entrou, que nenhum caminho de produção o abre desde a Regra 27, e que ele
não está coberto pelo MIT deste repositório.

**5 — E9: o dado antes da decisão.**
A TASK-048 continua sem código de propósito. O que entrou foi a medida:
a linha de transição diz de que estado veio e o que a sonda viu **nos
dois sentidos**, e a sessão termina nomeando toda colônia que trocou de
estado três vezes ou mais. `ColonyStateLog` some quando o E9 fechar.

**6 e 7 — as duas ADRs que faltavam.**
`ADR-007` (fusão): sobrevive o UUID da maior; o teto de profissão é
violado e permanece violado; o centro é o do sobrevivente. `ADR-008`
(orientação): forma (a) — `Side` de quatro direções em `BlueprintBlock`,
tradução na fronteira, e as duas formas recusadas ditas por escrito.
**Nenhuma das duas está implementada.**

**8 — O ícone: fundo transparente.**
Arte nova do autor, 1254×1254. O fundo saiu por **preenchimento a partir
da borda**, e não por chave de cor — que era a objeção de 08-07, e ela
continuava certa: a roupa do aldeão e o reboco da casa são brancos.
39,1% da imagem saiu, e nenhum branco de dentro do desenho foi tocado.

**9 e 10 — a cabana e a paleta: apagadas.**
`ColonyHut` saiu do código, e `VillagePalette` encolheu junto — parede,
porta e lã tinham um só leitor, que era ela. Ficaram estilo, pedra e
vidro.

**Um defeito de jogo veio junto, e foi corrigido.**
`hasMaterialForNextBlock` dizia "tem" para toda mobília, citando a Regra
21. Com a regra morta e a cama de volta à espera, a obra acordaria
dizendo que tem a cama, tentaria, falharia e dormiria — todo ciclo, para
sempre. A pergunta passou a ser a barreira.

**E um defeito que estava escondido há mais tempo.**
O jogo tem mais de uma receita para `white_bed`, e uma delas é **tingir**
uma cama preta. O livro devolvia essa, e a conta de lã dava **zero** — o
pastor sem tarefa e a casa sem cama, em silêncio. A regra que conserta:
quem decompõe uma peça não pode partir de outra peça da mesma família.

---

## 🧪 O que falta testar — nada disto foi visto em jogo

**A sessão de 2026-08-22 fechou os itens 5, 11 e parte do 2 e do 3.** O
resto continua sem ter sido visto:

| | O que | Como confirmar no log |
|---|---|---|
| **1** | **A mina** | `Miner ... opens a mine at ...`, depois a escada, a sala 7×4 e a galeria |
| **2** | **A casa do catálogo subindo** | `planned minecraft:village/<bioma>/houses/<bioma>_small_house_1` |
| **3** | **A vila de deserto construindo** | Pela primeira vez na história do mod |
| **4** | **A barreira de teste** | `TEST BARRIER covered for nothing this session` é a notícia boa. Qualquer `TEST BARRIER skipped` nomeia a cadeia que falhou |
| ~~5~~ | ~~**O centro parado**~~ | ✅ **visto em 2026-08-22** — 4 movimentos, todos convergindo |
| **6** | **A casa esperando a cama** | `WAITING_RESOURCES` por `white_bed`, e o pastor tosquiando por causa disso |
| **7** | **Pastor, fundidor e o fabricante descascando** | `Shepherd ... sheared`, `Smelter ... made minecraft:glass`, `stripped a oak_log` |
| **8** | **A cadeia da areia inteira** | meta de `SAND`, `Miner ... took` numa praia, vidro, e a vidraça saindo do fabricante |
| **9** | **O carvão da galeria** | `Miner ... took` de um `coal_ore` no −20, e a veia inteira num só ciclo |
| **10** | **A casa de planície terminando sozinha** | é a primeira vez que ela pode |
| ~~11~~ | ~~**A vila de deserto achando lote**~~ | ✅ **visto em 2026-08-22** — lote achado e casa planejada. A rua crescendo continua por ver |
| **12** | **O E9** | `E9 — colony ... changed state N times`. Silêncio aqui fecha o erro |
| **13** | **O registro seguindo as camas** | `Registered N villagers` numa colônia cujo centro esteja longe do aglomerado novo |

**Coberto por teste, com limite conhecido:**

- **A arena da bateria tem bioma fixo.** Taiga, savana, nevada e deserto
  nunca rodaram.
- **A retomada da mina entre sessões** — fechar e reabrir o mundo de
  verdade continua sendo a dívida E4.

---

## ⚠️ Conflitos — o que ainda se contradiz

**Quatro dos seis fecharam neste ciclo.** Ficaram estes:

**1. A Regra 25 continua inerte enquanto a 28 valer.**
"A maior planta que couber" precisa de mais de uma planta. Com uma casa
por bioma, ela não escolhe nada. Volta a valer quando a barreira sair.

**2. O `furniture()` do `BlueprintBlock` perdeu o dono.**
Ele marcava o que a Regra 21 repunha. A regra morreu, e quem decide hoje
o que não segura a obra é a barreira da Regra 28. A marca continua sendo
lida do arquivo e afirmada por teste, e **não governa mais nada** — some
junto com a barreira, ou ganha outro uso.

**3. O ícone tem 1,95 MB, e o jar tem 2,29 MB.**
85% do que se distribui é uma imagem mostrada a 64 pixels na lista de
mods. A 256×256 daria cerca de 100 KB. É escolha de arte, e por isso não
foi mexida.

**4. O `Development-Log.md` parou em 2026-08-15.**
Trinta e cinco commits, sete dias e duas sessões de jogo não estão nele,
e `logs/latest.log` tem 0 bytes. O "49 camas e 7" que originou a Emenda
4 **não tem registro primário em lugar nenhum do repositório** — existe
só como frase aqui e no README.

---

## 🗂️ Próximas atividades, por importância

**1 — Decidir o grupo do arenito, e escrever quem funde pedra.** É o
único elo entre a obra de deserto e a casa de pé, e a sessão de 03:45
parou exatamente nele. A decisão está na seção vermelha acima.

**2 — Fechar a bateria.** Os dois testes da seção 🔴. O da arena é
regressão deste ciclo; o do lenhador atrapalha todo ciclo desde antes.

**3 — Implementar a ADR-008 (orientação).** É a que muda o que se vê:
cama, escada e tocha param de sair todas para o mesmo lado. `Side` já
existe; o que falta é atravessá-lo pelo `BlueprintBlock`.

**4 — Quebrar os arquivos acima de 500 linhas.**

```text
982  VillageDetectionHandler      621  BuildSiteScanner
729  BuilderWork                  566  ColonySavedData
724  TreeHarvester                527  ConstructionPlanner
639  ManufacturerWork
```

`ConstructionPlanner` **voltou** à lista: foi de 703 para 414 em 08-20 e
já está em 527. Nos testes há mais quatro acima do teto:
`LumberjackGameTest` 1571, `BuilderGameTest` 904, `BuildSiteGameTest` 670
e `MinerGameTest` 510.

**5 — Regra 16**, distância mínima e máxima entre construções.

**6 — O `ItemRequest`.** O trabalhador pedir o que lhe falta em vez de
travar. `WorkMaterials` já é meio dele — um passo de decomposição, dois
onde o lampião pede. O que falta é a profundidade qualquer.

**7 — Implementar a ADR-007 (fusão).** Depende de a construção rodar em
jogo: nada dispara enquanto uma obra não encostar na outra.

---

## 🟢 Depois disso

- **O fazendeiro.** Tem nome, enxada e baú desde a Fase 4, e nenhum
  trabalho. É a última profissão do modelo sem código.
- **O buraco que o mineiro deixa** na superfície e a mina aberta.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
- **Defesa.** Nada no modelo ainda.
