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
458 testes unitários  ·  150 testes de jogo  ·  29 regras do autor
7 trabalhadores com código  ·  6 arquivos acima do teto de 500 linhas
a bateria de jogo NÃO fecha verde — ver a seção seguinte
```

---

## 🔴 O que está quebrado agora

**A bateria de jogo falha um teste por rodada**, alternando entre dois.
Cinco rodadas seguidas em 08-21, cinco falhas, nunca as duas juntas.

| | Teste | O que é |
|---|---|---|
| **1** | `thestallguardreturnsthetaskandforgetsthetree` | Instável desde antes deste ciclo — 3 falhas em 10 rodadas em 08-21. Já estava aqui |
| **2** | `villagersbecomeworkerswithaprofession` | **Regressão de arena deste ciclo**, e a causa está entendida |

**A causa do 2.** Com dois testes a menos — os da cabana, que saíram com
ela —, a bateria remaneja as estruturas, e as camas desse teste passaram
a cair a menos de `CLUSTER_DISTANCE` das de um vizinho. Os dois
aglomerados viram um, e a colônia que o adota fica a 73 blocos dali.
Antes da **Emenda 4 da ADR-003** ela arrastaria o centro para o meio do
aglomerado novo e alcançaria aqueles aldeões; agora o centro só anda
pela sonda, e não alcança.

**Não é defeito da regra** — é a contaminação entre gametests que o
próprio `ColonyDetectionGameTest` documenta em três lugares, aparecendo
por uma porta nova. Metade já foi corrigida (`colonyOwning` procura a
colônia pelo **dono** do aldeão em vez de pela distância) e não bastou:
quando ninguém os registra, não há dono a encontrar.

**O que provavelmente resolve:** dar a este teste camas que não possam
se fundir com as do vizinho, ou uma arena própria. Nenhuma das duas foi
tentada.

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

**Trinta e cinco commits desde a última sessão de verdade.** Em ordem do
que mais precisa ser visto:

| | O que | Como confirmar no log |
|---|---|---|
| **1** | **A mina** | `Miner ... opens a mine at ...`, depois a escada, a sala 7×4 e a galeria |
| **2** | **A casa do catálogo subindo** | `planned minecraft:village/<bioma>/houses/<bioma>_small_house_1` |
| **3** | **A vila de deserto construindo** | Pela primeira vez na história do mod |
| **4** | **A barreira de teste** | `TEST BARRIER covered for nothing this session` é a notícia boa. Qualquer `TEST BARRIER skipped` nomeia a cadeia que falhou |
| **5** | **O centro parado** | Nenhum salto de âncora. A obra não fica para trás |
| **6** | **A casa esperando a cama** | `WAITING_RESOURCES` por `white_bed`, e o pastor tosquiando por causa disso |
| **7** | **Pastor, fundidor e o fabricante descascando** | `Shepherd ... sheared`, `Smelter ... made minecraft:glass`, `stripped a oak_log` |
| **8** | **A cadeia da areia inteira** | meta de `SAND`, `Miner ... took` numa praia, vidro, e a vidraça saindo do fabricante |
| **9** | **O carvão da galeria** | `Miner ... took` de um `coal_ore` no −20, e a veia inteira num só ciclo |
| **10** | **A casa de planície terminando sozinha** | é a primeira vez que ela pode |
| **11** | **A vila de deserto achando lote**, e **a rua crescendo** | `extended the road 5 blocks` |
| **12** | **O E9** | `E9 — colony ... changed state N times`. Silêncio aqui fecha o erro |

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

**1 — Rodar em jogo.** Trinta e cinco commits, nove decisões e três
profissões sem uma única sessão de verdade. Nada mais deveria ser
construído antes disto.

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
