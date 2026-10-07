
---

# RULES.md

As regras do autor do Village Colony, em tabela. **Fonte única para
"esta regra existe, está ativa, e onde está implementada".**

Onde o código discordar de uma regra, é a regra que está certa — ou a
regra que foi emendada. O corpo de cada regra vive em
`docs/archive/technical/Project-State.md §18`; aqui é o índice.

---

## Regras ativas

| # | Enunciado | Data | Estado | Implementação |
|---|---|---|---|---|
| 1 | Colher até os baús encherem | 08-08 | ✅ feita | `ColonyGoals` (meta = guardado + espaço) |
| 2 | Colher no tempo de um jogador com ferro | 08-08 | ✅ feita | `BlockBreakTime`, `LumberjackWork.tick` |
| 2-e1 | **Emenda 1:** o aldeão trabalha na velocidade da ferramenta que tem — começa com ferro; havendo no baú dele uma melhor (mais rápida ou encantada), troca a de ferro por ela | 10-02 | ✅ feita | `ToolUpgrade` (desde 09-04) + `ActionTool.speedOf` com Eficiência e desempate por encantamento; troca pela pilha exata (`ChestWithdrawer.takeExact`, `ChestDepositor.depositExact`) — `BetterToolGameTest` |
| 3 | Nunca destruir construções da vila original, da colônia ou do jogador | 08-13 | ✅ feita, com limite de autoria manual | `BlockProtection`, `TreeHarvester` |
| 3-e1 | **Emenda 1:** lenhador respeita estruturas protegidas ao cortar árvores | 09-14 | ✅ feita | `TreeHarvester.plan`, `breakOne` |
| 4 | Dois trabalhadores por profissão | 08-13 | ⚠️ substituída na prática | a constante `MAX_PER_PROFESSION` não existe mais; a cota cresce com a população (`ProfessionAssigner.targetCount`, lotes de 15 adultos) e a demanda passa uma cabeça acima dela (`ProfessionDemand`, 09-30) |
| 5 | Fabricar até metade do armazém | 08-13 | ✅ feita | `ColonyGoals` (tábua) |
| 5-e1 | **Emenda 1:** a obra usa todos os recursos de todos os baús — a tora vira a tábua que a obra pede, além da metade; a reserva de metade em tora vale só para os outros ofícios. A meta e o fabricante fazem a mesma conta | 10-02 | ✅ feita | `StockRules.logsThatMayBeConverted`, `WorkDemand.rawLogs` |
| 6 | Estrada primeiro, casa ligada a ela | 08-14 | ✅ feita | `RoadExtension`, `BuildSiteScanner` |
| 7 | O lenhador planta onde cortou | 08-15 | ✅ feita | `LumberjackWork.closePlan` |
| 8 | Um baú ao lado de cada cama — **em toda casa** (da vila e construída), encostado na parede, nunca diante de porta ou escada (ADR-036 item 4) | 08-15, 10-06 | ✅ feita | `ChestPlacer` (porta, escada, parede), `VanillaBedChests.ensure` (vila e casas da colônia) |
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
| 18-e1 | **Emenda 1:** ninguém para de trabalhar por chuva, e não há pausa ao meio-dia (R-6 e R-7 do estudo de 10-02 recusadas) | 10-02 | ✅ é o que já acontece | `WorkHours` não olha o tempo |
| 19 | O lote fica no nível da estrada | 08-19 | ✅ feita | `LotLevel.flatGroundAt` |
| 20 | Cada vila constrói no estilo do bioma | 08-19 | ✅ feita | `VillagePalette`, `VillageBiomesGameTest` |
| 21 | Toda casa nasce com cama, baú e lampião | 08-19 | ⚠️ a decidir (C-02 da auditoria) | `BuilderWork.furnish` **não existe**; o código diz que a regra morreu e a cama vem da planta (`TestBarrier`, `WorkMaterials`) |
| 22 | O lote é livre no volume | 08-19 | ✅ feita | `BuildSiteScanner.isNothing` |
| 23 | O que já foi analisado se analisa de novo | 08-19 | ✅ feita | `TreeMarks` (prazos), `RingSweep` |
| 24 | A vila de planície levanta a casa do jogo | 08-19 | ✅ feita | `VillageStructures` |
| 25 | ~~A maior planta que couber no lote~~ | 08-20 | ❌ **desfeita** em 10-06 (ADR-036 item 5) | a ordem das plantas não favorece tamanho (`PlanOrdering.mixed`); a primeira casa continua a menor |
| 26 | Cadeia de produção, e paleta por bioma | 08-20 | ✅ feita | `ResourceType.production`, `VillagePalette` |
| 27 | Só o catálogo do jogo, e o construtor aguarda | 08-20 | 🔒 imutável | `VillageStructures`, `MaterialChoice` |
| 27-e1 | **Emenda 1:** abre para pedra só | 08-26 | ✅ feita | `Substitution.ALTERNATIVE` |
| 27-e2 | **Emenda 2:** e para a madeira junto | 08-26 | ✅ feita | `MaterialChoice.INTERCHANGEABLE_IN_THE_WALL` |
| 27-e3 | **Emenda 3:** os modelos próprios da colônia (`data/villagecolony/structure/colony/`, `.nbt`) têm prioridade sobre a estrutura do jogo quando existem — casa de cada ofício e troca de estrutura; sem modelo, vale o catálogo do jogo | 10-02 | ✅ feita | `ColonyModels`, `CATALOGO.md` (`scripts/structure_catalog.py`) |
| 28 | ~~Barreira de teste: casa pequena, mobília dispensada~~ | 08-20 | ❌ **retirada** em 10-06 (ADR-036 item 6) | peça que falta não é mais riscada: aparece na 4ª tentativa (Regra 51); só a 5ª falha ao pôr pula a peça |
| 29 | A mina em escada, duas salas, galeria sem fim | 08-20 | ✅ feita | `MineShaft` |
| 30 | O mineiro recolhe tudo, e a boca tem endereço | 08-22 | ✅ feita | `MineMouth`, `MinerHaul` |
| 30-e1 | **Emenda 1:** manter piso de carvão e ferro bruto mesmo sem obra ativa | 09-14 | ✅ feita | `ColonyGoals.MINERAL_FLOOR` |
| 31 | O fazendeiro planta o que tem e colhe o que está pronto | 08-26 | ✅ feita | `FarmerWork`, `CropPatch` |
| 32 | Móveis e cama entram depois da casa pronta | 08-29 | ⚠️ a decidir (C-02 da auditoria) | `BuilderWork.furnish` **não existe**; a segunda passada saiu com a Regra 21 (`TestBarrier`) |
| 33 | Pedido de commit+push também atualiza o JAR local | 09-14 | ✅ registrada | `build/libs/` → `downloads/` → `%APPDATA%/.minecraft/mods/`; conferir SHA-256 |
| 34 | A lava nunca surge automática | 09-30 | ✅ feita | `BlockShaping.isNeverPlaced`, `BuilderPlacement.placeOne` |
| 35 | Carpinteiro titular na BigHouseMOD, cama e baú à direita da porta, corredor livre até a escada | 09-30 | ✅ feita | `FOUNDATION_ORDER`, `big_house_mod.nbt` |
| 36 | O viveiro (terra enraizada + rebento) é do lenhador | 09-30 | ✅ feita | `LumberjackNursery` |
| 37 | Cada aldeão usa a ferramenta de ferro apropriada para a ação | 09-30 | ✅ feita | `ActionTool`, `BlockBreakTime` |
| 37-e1 | **Emenda 1:** a de ferro é a primeira, não a única — ver 2-e1 | 10-02 | ✅ feita | `ActionTool`, `ToolUpgrade` |
| 38 | Contratar primeiro a profissão de que a demanda depende, depois a lista | 09-30 | ✅ feita | `ProfessionDemand`, `ProfessionAssigner.demandedVacancy` |
| 39 | Comida é feita pelo fazendeiro (pão do trigo acima de 32) | 09-30 | ✅ feita | `FarmerBakery` |
| 40 | Piso de fundido só com cadeia do cru | 09-30 | ✅ feita | `StockRules.rawOf`, `ColonyGoals` |
| 41 | Corante, linha, pó de osso e drops de inimigo e animal aparecem automáticos | 09-30 | ✅ feita | `DropIngredients`, `ColonySupply` (ADR-028) |
| 42 | Mineiro para de guardar um tipo aos 256 no baú (fora o pedido) | 09-30 | ✅ feita | `MinerHaul.TYPE_CAP` |
| 43 | O pastor faz o rebanho procriar | 09-30 | ✅ feita | `ShepherdFlock` |
| 44 | Ofício do mod exclui ofício Vanilla | 09-30 | ✅ feita | `VanillaProfessionGuard`, `VillagerDataMixin` (ADR-029) |
| 46 | A fome não influencia o trabalho: a comida decide só a procriação (Vanilla) | 10-02 | ✅ é o que já acontece | `VillageMeals` |
| 47 | O aldeão não envelhece nem morre de velhice | 10-02 | ✅ é o que já acontece | — |
| 48 | Item caído no chão só é recolhido se for peça que falta à obra aberta | 10-02 | ✅ feita | `GroundPickup` (B-1: quem está à toa recolhe e guarda no baú mais perto da obra) |
| 49 | A ordem das obras: faltando cama, casa primeiro; não faltando, a casa de cada ofício que ainda não tem; só com todas de pé entram as demais (e a casa volta ao rodízio) | 10-02 | ✅ feita | `ConstructionPriority.WORKSHOP`, `ConstructionTurn`, `ConstructionOrder` (o mineiro ganhou o ferramenteiro; lenhador e construtor não têm casa de ofício no catálogo) |
| 50 | Toda verificação mede o tempo dos aldeões — trabalhando, andando, bloqueado, ocioso, encalhado — por profissão, e usa a proporção como critério de melhoria e de correção | 10-02 | ✅ feita | `WorkTime` (linha `VC_TIME`), `scripts/time_ledger.py`, `CLAUDE.md` §0.4 |
| 45 | Os baús da colônia são todos os da vila, e só eles: com a vila medida, todo baú livre dentro da caixa (na janela de altura das camas) conta, de dentro ou de fora de casa; fora da caixa está fora de alcance, mesmo o de trabalhador; baú de trabalhador de qualquer colônia nunca é livre | 10-01 | ✅ feita | `ColonyChests.nearestFirst`, `VillageChests` (`VillageChestReachGameTest`) |
| 51 | Peça que falta aparece na **4ª** tentativa sem sucesso, no baú da profissão que tentou (coletor; artesão recebe o ingrediente sem rota; construtor se nenhuma profissão faz), com `VC_SUPPLY_ERROR` no log. A peça só é pulada se o construtor falhar **5 vezes ao pô-la** | 10-06 | ✅ feita (ADR-036 item 6) | `LocateFallback`, `BiomeConstructionSupply.ATTEMPTS_BEFORE_STOCKING`, `ConstructionProject.failsForTheLastTime` |

---

## Regras provisórias

Regras que o autor declarou **temporárias** e vão sair.

| # | Enunciado | Por que existe | O que destrava a saída |
|---|---|---|---|
| 28 | ~~Barreira de teste~~ (**retirada em 10-06**, ADR-036 item 6): ~~só `plains_small_house_1`~~ (caiu em 09-09) e peça dispensada | Tornar as sessões comparáveis entre si | O planejador saber desistir de um objetivo (P1.1) |

**Enquanto a metade que resta valer:**
- A casa sobe com peças da barreira (playtest de 02-10: 16 de 69).
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

A regra de arquitetura "a colônia não cria recurso" (`Construction-System.md`,
sem número nesta tabela) foi **retirada em 2026-09-30** — ADR-028. A 25 foi
**desfeita em 2026-10-06** — ADR-036 item 5.

---

## Decididas em 2026-10-02 (estudo de profissões)

Do `docs/research/2026-10-02-melhorias-profissoes-vida-natural.md`, o autor:
recusou a pausa e a chuva (18-e1); trocou a ferramenta fixa pela melhor do baú
(2-e1, 37-e1); fixou que a fome não pesa no trabalho (46) e que ninguém envelhece
(47); limitou o recolher do chão ao que a obra espera (48); deu a ordem das obras
(49); e fez do tempo dos aldeões o critério de toda verificação (50).

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
`docs/archive/technical/Project-State.md §18`. Aqui é o índice.

Quando o código divergir da tabela, **a tabela está errada** — atualize
aqui primeiro, e depois o código.
