# Terraplanagem da vila — o terreno que a colônia conserta

**Pesquisa de 2026-09-11.** Modo FEATURE. Pedido do autor, na sessão em que
ele voltou do jogo dizendo *"vi dificuldades, a vila não cresce"*:

> alguns aldeões devem ser capazes de corrigir o terreno que pertence à vila,
> visando primeiramente nivelar o terreno, fechando buracos, construindo ruas
> (caminhos), cavando e colocando bloco visando que a diferença de alturas
> entre um caminho e sua continuação seja de um bloco de altura. Os espaços de
> construção, se não encostarem em outras estruturas, podem ser corrigidos
> também para que uma construção possa nascer ali.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mappings | Yarn 1.21.1+build.3 |
| Conferido em | 2026-09-11, via `gradle.properties` e `javap` no jar de merge |
| Objetivo | adicionar feature |

---

## A resposta curta

**O Vanilla não tem nada reaproveitável**, e a razão é de arquitetura, não de
falta: ele adapta terreno **enquanto o terreno ainda é ruído**, durante a
geração do chunk. A vila do mod cresce em mundo já gerado, onde não há mais
ruído — só bloco.

**Mas metade da regra que o autor pediu já existe no mod**, escrita em 08-21 e
nunca ligada a quem pudesse cumpri-la. O que falta não é a regra: é alguém que,
em vez de **recusar** terreno ruim, **conserte** o terreno.

---

## 1. Vanilla — o jogo já faz isso?

**Faz, e só na geração.** `[FATO]`

| símbolo | evidência |
|---|---|
| `net.minecraft.world.gen.StructureTerrainAdaptation` | `javap`: enum com `NONE`, `BURY`, `BEARD_THIN`, `BEARD_BOX`, `ENCAPSULATE` |
| `net.minecraft.structure.pool.StructurePool$Projection` | `javap`: `TERRAIN_MATCHING`, `RIGID`, com `getProcessors()` |
| `net.minecraft.world.gen.StructureWeightSampler` | `javap`: `implements DensityFunctionTypes$Beardifying`, com `sample(DensityFunction$NoisePos)`, `minValue()`, `maxValue()` |

**A linha que decide a pesquisa inteira é a terceira.** O
`StructureWeightSampler` é uma **função de densidade**: ele participa do cálculo
que diz, para cada ponto do espaço, se ali vai haver pedra ou ar. É assim que a
aldeia do Vanilla ganha o platô debaixo dela — o gerador *inventa* o platô antes
de o primeiro bloco existir.

`createStructureWeightSampler(StructureAccessor, ChunkPos)` é por chunk, e roda
uma vez: quando o chunk termina de gerar, o beard já aconteceu e não há função
de densidade nenhuma para consultar depois.

**`[INFERÊNCIA]` Um chunk já gerado não pode ser re-beardificado.** Para o mod,
que trabalha em mundo carregado e vila existente, isso significa que as cinco
adaptações do Vanilla são inúteis — não por serem ruins, mas por pertencerem a
outra fase da vida do mundo.

**`[FATO]`** As ruas de aldeia do Vanilla (`village/plains/streets/*`) são peças
jigsaw com projeção `TERRAIN_MATCHING`: elas acompanham o relevo em vez de o
aplainar. É por isso que aldeia gerada em morro tem rua em degrau — e por isso
que imitar o Vanilla aqui **não daria** o que o autor pediu.

## 2. Fabric API — existe evento ou API pronta?

**Não.** `[FATO]` A Fabric API expõe eventos de mundo, bloco e chunk, e nenhum
deles terraplana. Não há utilitário de nivelamento: quem quer mover terra em
runtime usa `world.setBlockState`, que é o que o próprio mod já faz em seis
lugares.

## 3. O que o mod já tem — e é mais do que parece

### A regra do degrau **já está escrita**, e vale só para recusar `[FATO]`

`RoadExtension.MAX_STEP = 1`, com o javadoc:

> *Um. Rua que sobe mais que isso por passo não é rua, é escada — e o aldeão que
> a percorre fica preso no degrau.*

**É exatamente a regra que o autor pediu**, de 2026-08-21. O `isRoadNear`
procura calçamento numa janela de `±MAX_STEP` em torno do Y, e o `pave` para no
primeiro bloco que recusa. Hoje ela só sabe dizer *não*.

### O desnível do lote **recusa**, e é por onde a vila para `[FATO]`

`BuildSiteScanner.MAX_SLOPE = 2`, consultado por `flatGroundAt`. Lote com
desnível maior que dois blocos é descartado, e a varredura segue para o anel
seguinte. O javadoc já admite a metade que falta:

> *a casa assenta no mais baixo para que nenhuma parte dela nasça enterrada; o
> que ficar acima é degrau que a preparação resolve*

### A preparação existe e **não move terra** `[FATO]`

`SitePreparation`, 103 linhas. Tira grama alta, samambaia, flor, camada de neve
e muda solta — `BlockTags.REPLACEABLE`. Bloco sólido não entra na lista, **de
propósito**: a Regra 3 manda não tocar no que é do jogador.

---

## 4. Por que a vila não cresce — a inferência que fecha o caso

`[INFERÊNCIA]` **O mod só sabe procurar terreno bom; nunca sabe fazer terreno
bom.**

A cadeia é direta, e as três portas são as de cima:

```text
varredura procura lote plano  →  MAX_SLOPE recusa o irregular
rua procura continuação plana →  MAX_STEP recusa o degrau alto
preparação limpa o canteiro   →  e não mexe em uma pá de terra
```

Em planície isso funciona: a oferta de terreno plano é grande. Em terreno
acidentado a oferta se esgota, e **quando ela se esgota a vila para para
sempre** — porque nada no mod muda o terreno, e o terreno é a única coisa que
está impedindo.

`[VALIDAÇÃO NECESSÁRIA]` Medir, numa sessão, quantos lotes a varredura recusa
por desnível contra quantos recusa por estarem ocupados. Se o desnível
dominar, esta inferência vira fato. **Hoje o log não distingue** — as duas
recusas viram o mesmo `IdleReason.NO_TARGET`, e é o mesmo defeito de
instrumentação que o P0.1 do plano manda consertar.

---

## 5. Escada de extensão — onde esta feature cai

| degrau | serve? | por quê |
|---|---|---|
| 1. Sistema Vanilla | ❌ | só na geração de chunk, §1 |
| 2. Registro Vanilla | ❌ | não há registro de terraplanagem |
| 3. Data-driven | ⚠️ parcial | a *forma* do degrau pode ser dado; a decisão não |
| 4. Fabric API | ❌ | não existe, §2 |
| 5. Fabric Events | ❌ | nada a ouvir |
| 6. Composição | ✅ | **é aqui** |
| 9–11. Mixin | ❌ | desnecessário |

**`[DECISÃO]` Degrau 6 — composição, sem Mixin nenhum.** A feature é lógica do
próprio mod, usando `world.setBlockState` como as seis integrações que já
existem. Isso é o melhor resultado possível para uma frente nova: nada que ela
faça pode quebrar com outro mod por conflito de Mixin.

---

## 6. O desenho proposto

### A ideia central: o terreno vira **obra**, e não caso especial

O mod já sabe abrir obra, reservar trabalhador, esperar material e fechar
projeto — é o `ConstructionProject`. **Terraplanagem é obra como outra
qualquer**, e entrar por essa porta reaproveita o ciclo inteiro em vez de criar
um caminho paralelo que precisa aprender tudo de novo.

```text
varredura acha terreno irregular  →  abre OBRA DE TERRENO
                                        ↓
                       o aldeão corta o que sobra e preenche o que falta
                                        ↓
                          terreno plano  →  lote válido  →  casa
```

### As três frentes, na ordem em que o autor as pediu

**1. Nivelar o lote.** Onde hoje o `flatGroundAt` recusa por `MAX_SLOPE`, passa
a haver uma segunda resposta: *"não serve agora, e serve depois de N blocos de
trabalho"*. O lote vira candidato a terraplanagem em vez de sumir.

**2. Fechar buracos.** É o mesmo mecanismo, com o sinal trocado: coluna sem
chão ganha bloco em vez de perder. O material sai do que a colônia já tem —
terra, pedregulho — e a regra de estoque já existe.

**3. Ligar caminhos com degrau de um.** Onde hoje o `pave` para no bloco que
recusa, passa a haver a alternativa de **cavar um** ou **subir um** para que a
continuação caiba no `MAX_STEP` que já está escrito. A regra não muda; ganha
quem a cumpra.

### O que **não** muda, e é o que protege o mundo do autor

- **Regra 3 continua inteira.** Bloco que o jogador pôs não é tocado. O
  `BuildSiteScanner` já sabe distinguir peça de vila e coisa do jogador, e essa
  pergunta passa a valer também para a pá.
- **Só dentro da vila**, que é o que o autor escreveu. O raio já existe.
- **Nada de terraplanar a montanha.** Um teto de desnível separa *"terreno
  irregular"* de *"isso é um morro"* — passar disso continua sendo recusa, e a
  vila cresce para outro lado. Sem esse teto a colônia aplaina o mapa.

---

## 7. Riscos `[RISCO]`

| | o quê |
|---|---|
| 🔴 | **É a frente que mais mexe no mundo do jogador.** Todas as outras põem bloco onde a planta manda; esta **tira** bloco que já estava lá. Um defeito aqui é visível e irreversível — e o mundo do autor tem 20 colônias |
| 🔴 | **O plano de correção proíbe abrir frente nova antes do P0 fechar.** Esta é uma frente grande. A tensão é real e a decisão é do autor |
| 🟠 | **Custo por ciclo.** O ciclo da colônia já está em 122 ms contra o limite de 50, e terraplanagem é varredura de colunas. Ela precisa entrar no orçamento por passagem, como a busca de lote |
| 🟠 | **Terreno alterado invalida o índice de ruas e o cursor da varredura.** Os dois guardam altura; mover o chão embaixo deles é o mesmo caso do `drifted` |
| 🟡 | **Água e lava.** Cavar ao lado de um lago alaga o canteiro. O mod já tem `MineFlooding` para a mina, e a regra vale aqui |

---

## 8. As decisões — respondidas pelo autor em 2026-09-11

| pergunta | decisão |
|---|---|
| **Quem faz o trabalho** | **Capacidade do construtor.** Nada de profissão nova: o ofício que já existe ganha a tarefa de terreno e reusa o estado `PREPARING`. Respeita o plano, e o teto de dois por profissão não engole a novidade |
| **Quanto vale aplainar** | **Quatro blocos de desnível** — o dobro do `MAX_SLOPE` de hoje. Corrige ondulação e barranco pequeno; **morro continua sendo recusa**, e a vila cresce para outro lado. Conservador de propósito, porque esta é a frente que mais mexe no mundo do jogador |
| **A ordem** | **Medir primeiro.** A frente não abre com a inferência do §4 — abre com número |

### E a terceira decisão manda no que vem antes

O autor não aceitou a inferência como causa, e está certo: ela é `[INFERÊNCIA]`,
não `[FATO]`, e o §4 já dizia que o log de hoje não a distingue.

**O que vem antes, então:**

1. **Consertar a instrumentação do P0.1** — o `IdleLog` compara só o
   `IdleReason` e engole o detalhe, então *"o único lote livre está fora do
   alcance do fazendeiro"* e *"nenhuma ponta de rua pôde ser calçada"* são a
   mesma linha e só a primeira aparece.
2. **Contar a recusa de lote por motivo**, que é o que o P0.1 pede ao pé da
   letra: *"rejeitei 12 lotes por X, Y, Z"*.
3. **Uma sessão de jogo.** Se a recusa por desnível dominar, a inferência do §4
   vira fato e a frente abre. Se não dominar, a causa é outra e esta pesquisa
   evitou uma frente grande pelo motivo errado.

---

## 9. Handoff

`minecraft-code-research` → **`fabric-development`**, com:

- sistema Vanilla identificado e **descartado com razão** (§1)
- degrau da escada escolhido: **6, composição, sem Mixin** (§5)
- os três pontos de produção onde a mudança encosta: `BuildSiteScanner.flatGroundAt`,
  `RoadExtension.pave`, `SitePreparation`
- riscos (§7) e as três decisões pendentes (§8)
