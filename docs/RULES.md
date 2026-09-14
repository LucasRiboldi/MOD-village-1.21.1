
---

# RULES.md

As regras do autor do Village Colony, em tabela. **Fonte única para
"esta regra existe, está ativa, e onde está implementada".**

Onde o código discordar de uma regra, é a regra que está certa — ou a
regra que foi emendada. O corpo de cada regra vive em
`docs/technical/Project-State.md §18`; aqui é o índice.

---

## Regras ativas

| # | Enunciado | Data | Estado | Implementação |
|---|---|---|---|---|
| 1 | Colher até os baús encherem | 08-08 | ✅ feita | `ColonyGoals` (meta = guardado + espaço) |
| 2 | Colher no tempo de um jogador com ferro | 08-08 | ✅ feita | `BlockBreakTime`, `LumberjackWork.tick` |
| 3 | Nunca destruir construções da vila original, da colônia ou do jogador | 08-13 | ✅ feita, com limite de autoria manual | `BlockProtection`, `TreeHarvester` |
| 3-e1 | **Emenda 1:** lenhador respeita estruturas protegidas ao cortar árvores | 09-14 | ✅ feita | `TreeHarvester.plan`, `breakOne` |
| 4 | Dois trabalhadores por profissão | 08-13 | ✅ feita | `ProfessionAssigner`, `MAX_PER_PROFESSION` |
| 5 | Fabricar até metade do armazém | 08-13 | ✅ feita | `ColonyGoals` (tábua) |
| 6 | Estrada primeiro, casa ligada a ela | 08-14 | ✅ feita | `RoadExtension`, `BuildSiteScanner` |
| 7 | O lenhador planta onde cortou | 08-15 | ✅ feita | `LumberjackWork.closePlan` |
| 8 | Um baú ao lado de cada cama | 08-15 | ⚠️ metade | `ChestPlacer` (só para trabalhador) |
| 9 | Subir e descer para alcançar, e poder voltar | 08-15 | ✅ feita | `BuilderApproach`, `TreeMarks` |
| 10 | O construtor fabrica o que a expansão pede | 08-15 | ✅ feita | `CraftingLookup.billFor`, `ColonyChests` |
| 11 | Uma de cada profissão em cada vila | 08-15 | ✅ feita | `ProfessionAssigner`, `ProfessionFloorTest` |
| 12 | O centro fica em bloco que existe | 08-15 | ✅ feita | `VillageDetector.evaluate` |
| 13 | A obra é uma que a colônia consiga fazer | 08-15 | ✅ feita | `ColonyHut` (aposentada pela 27) |
| 14 | O construtor alcança o alto da obra | 08-18 | ✅ feita | `BuilderWork.isWithinReach` |
| 15 | A estrada cresce com a vila | 08-18 | ✅ feita | `RoadExtension` |
| 16 | Cada casa com espaço em volta | 08-18 | ⚠️ parcial | volume no `BuildSiteScanner` |
| 17 | A casa com uma lateral na estrada | 08-19 | ✅ feita | `Blueprint.doorSide`, `rotated` |
| 18 | O dia inteiro é expediente | 08-19 | ✅ feita | `WorkClock`, `WorkHours` |
| 19 | O lote fica no nível da estrada | 08-19 | ✅ feita | `BuildSiteScanner.flatGroundAt` |
| 20 | Cada vila constrói no estilo do bioma | 08-19 | ✅ feita | `VillagePalette`, `VillageBiomesGameTest` |
| 21 | Toda casa nasce com cama, baú e lampião | 08-19 | ✅ feita | `BuilderWork.furnish` |
| 22 | O lote é livre no volume | 08-19 | ✅ feita | `BuildSiteScanner.isNothing` |
| 23 | O que já foi analisado se analisa de novo | 08-19 | ✅ feita | `TreeMarks` (prazos), `RingSweep` |
| 24 | A vila de planície levanta a casa do jogo | 08-19 | ✅ feita | `VillageStructures` |
| 25 | A maior planta que couber no lote | 08-20 | ⚠️ inerte | bloqueada pela 28 |
| 26 | Cadeia de produção, e paleta por bioma | 08-20 | ✅ feita | `ResourceType.production`, `VillagePalette` |
| 27 | Só o catálogo do jogo, e o construtor aguarda | 08-20 | 🔒 imutável | `VillageStructures`, `MaterialChoice` |
| 27-e1 | **Emenda 1:** abre para pedra só | 08-26 | ✅ feita | `Substitution.ALTERNATIVE` |
| 27-e2 | **Emenda 2:** e para a madeira junto | 08-26 | ✅ feita | `MaterialChoice.INTERCHANGEABLE_IN_THE_WALL` |
| 28 | Barreira de teste: casa pequena, mobília dispensada | 08-20 | ⚠️ **provisória** | `VillageStructures.ONLY_WHILE_TESTING` |
| 29 | A mina em escada, duas salas, galeria sem fim | 08-20 | ✅ feita | `MineShaft` |
| 30 | O mineiro recolhe tudo, e a boca tem endereço | 08-22 | ✅ feita | `MineMouth`, `MinerHaul` |
| 30-e1 | **Emenda 1:** manter piso de carvão e ferro bruto mesmo sem obra ativa | 09-14 | ✅ feita | `ColonyGoals.MINERAL_FLOOR` |
| 31 | O fazendeiro planta o que tem e colhe o que está pronto | 08-26 | ✅ feita | `FarmerWork`, `CropPatch` |
| 32 | Móveis e cama entram depois da casa pronta | 08-29 | ✅ feita | `BuilderWork.furnish` (segunda passada) |
| 33 | Pedido de commit+push também atualiza o JAR local | 09-14 | ✅ registrada | `build/libs/` → `downloads/` → `%APPDATA%/.minecraft/mods/`; conferir SHA-256 |

---

## Regras provisórias

Regras que o autor declarou **temporárias** e vão sair.

| # | Enunciado | Por que existe | O que destrava a saída |
|---|---|---|---|
| 28 | Barreira de teste: só `plains_small_house_1` e mobília dispensada | Tornar as sessões comparáveis entre si | O planejador saber desistir de um objetivo (P1.1) |

**Enquanto a 28 valer:**
- A 25 fica inerte (não há escolha entre plantas).
- A casa sobe com peças da barreira (última medição: 19 de 169).
- `TEST BARRIER covered for N of M pieces` é a linha que mede.

---

## Regras emendadas

| # | Emenda | O que mudou |
|---|---|---|
| 27 | Emenda 1 (08-26) | Abriu substituição **só para pedra** na parede |
| 27 | Emenda 2 (08-26) | Estendeu para **madeira, tábua e pedra** |
| 3 | Emenda 1 (09-14) | A árvore não é exceção: o lenhador não corta bloco que pertença a estrutura protegida; consulta no plano e em cada quebra |
| 30 | Emenda 1 (09-14) | A colônia mantém 64 carvão e 64 minério de ferro bruto; demanda de obra soma ao piso |

**As emendas nasceram do mesmo defeito:** a conta e o construtor
discordavam. A conta aceitava o substituto, a parede exigia o exato.
Duas correções foram feitas em 08-22 (a conta deixou de aceitar) e em
08-26 (a parede passou a aceitar). **A lição:** a conta e o consumidor
precisam **concordar**.

---

## Regras revogadas

Nenhuma revogada até hoje. A 25 está **inerte**, não revogada — acorda
quando a 28 sair.

---

## Regras que faltam decidir

| # | Pergunta | Trava |
|---|---|---|
| E43 | O descanso de 4 ciclos deve valer mesmo quando não há outra tarefa? | anulado pela 2ª passagem do `takeOneTask` |
| P0.7 | Aceitar pedra como solo de lote? | toca a Regra 3 e a Regra 19 |

---

## Como ler esta tabela

- **#** é o número da regra como o autor a enunciou. Uma regra não é
  renumerada quando emendada — a emenda é sufixada (`27-e1`, `27-e2`).
- **Data** é a data do enunciado pelo autor, não da implementação.
- **Estado:**
  - ✅ **feita** — implementada e coberta por teste
  - ⚠️ **parcial / metade / inerte / provisória** — implementada em parte
  - 🔒 **imutável** — o autor marcou como não-desfazível
  - ⬜ **não iniciada**
- **Implementação** aponta para a classe ou arquivo principal.

---

## Onde estão os enunciados completos

Cada regra tem o texto integral (o que o autor disse, na data) em
`docs/technical/Project-State.md §18`. Aqui é o índice.

Quando o código divergir da tabela, **a tabela está errada** — atualize
aqui primeiro, e depois o código.
