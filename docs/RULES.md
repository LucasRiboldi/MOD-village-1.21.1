
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
| 14 | O construtor alcança o alto da obra — desde a ADR-037 B1, de qualquer lugar da zona da obra (pegada + 4) | 08-18 | ✅ feita | `BuilderApproach.isWithinReach`, `BuilderApproach.isInsideZone` |
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
| 28 | ~~Barreira de teste: casa pequena, mobília dispensada~~ | 08-20 | ❌ **retirada** em 10-06 (ADR-036 item 6) | peça que falta não é mais riscada: aparece na 4ª tentativa (Regra 51; na obra, 1 minuto (2 ciclos) sem avanço desde 11-10); só a 5ª falha ao pôr pula a peça |
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
| 49 | **A ordem das obras (consolida 55 e 60, 1A de 13-10):** faltando cama, casa; senão a oficina de cada ofício que ainda não tem (uma de cada tipo); senão o rodízio casa ↔ outro tipo, que levanta casa enquanto as camas forem menos que adultos + 4 (`HOUSE_SURPLUS`). O tipo sem lote numa volta inteira da varredura sai da vez por 30 min, e com todos recusados a casa entra no lugar | 10-02 (1A 10-13) | ✅ feita | `ConstructionPriority.buildsHouse/roomForAHouse`, `ConstructionTurn`, `ConstructionOrder`, `LotTypeRefusals`, `HousePlans.plansForNext` |
| 50 | Toda verificação mede o tempo dos aldeões — trabalhando, andando, bloqueado, ocioso, encalhado — por profissão, e usa a proporção como critério de melhoria e de correção | 10-02 | ✅ feita | `WorkTime` (linha `VC_TIME`), `scripts/time_ledger.py`, `CLAUDE.md` §0.4 |
| 45 | Os baús da colônia são todos os da vila, e só eles: com a vila medida, todo baú livre dentro da caixa (na janela de altura das camas) conta, de dentro ou de fora de casa; fora da caixa está fora de alcance, mesmo o de trabalhador; baú de trabalhador de qualquer colônia nunca é livre | 10-01 | ✅ feita | `ColonyChests.nearestFirst`, `VillageChests` (`VillageChestReachGameTest`) |
| 51 | Peça que falta aparece na **4ª** tentativa sem sucesso, no baú da profissão que tentou (coletor; artesão recebe o ingrediente sem rota; construtor se nenhuma profissão faz), com `VC_SUPPLY_ERROR` no log. A peça só é pulada se o construtor falhar **5 vezes ao pô-la** | 10-06 | ✅ feita (ADR-036 item 6) | `LocateFallback`, `BiomeConstructionSupply.ATTEMPTS_BEFORE_STOCKING`, `ConstructionProject.failsForTheLastTime` |
| 54 | **Mina em rede** (ADR-042): passada a escadaria e o salão, cada nível é uma grade de nós a cada 4 blocos (corredor 1 × 2, raio 6), um mineiro por nó; o minério ao alcance do braço a partir de um túnel aberto vem antes do túnel; piso, fluido encostado e beirada de queda garantidos com o enchimento dos baús (ou da parede do túnel quando falta); caminho só pelo que está pronto; nível esgotado desce; no fundo, caminhos laterais e volta pela rampa. O retorno subindo nunca quebra as 5 camadas sob a vila | 10-12 | ✅ feita (0.3.40) | `MineNetwork`, `NetworkDigging`, `NetworkSafety`, `NetworkVeins`, `MineReturn` |
| 54 | Emenda (10-12, 0.3.36) | Túnel de ramal 1 × 2 a cada 4 blocos (raio 6); minério ao alcance do braço vem antes do túnel; segurança só de piso, fluido e beirada; retorno nunca quebra as 5 camadas sob a vila (`MineReturn.SHELL_DEPTH`) |
| 55 | Roça grande primeiro; fundidor ocioso adianta o cru; lenhador ocioso planta mudas até 10 fora da caixa; pastor alimenta com qualquer comida; grama e flores surgem sozinhas (a ordem das obras saiu para a Regra 49, consolidada em 13-10) | 10-12 | ✅ feita (0.3.36) | `FarmPlans`, `SmelterAdvance`, `OutskirtsPlanting`, `ShepherdFlock`, `BlockShaping.growsByItself` |
| 56 | **Expansão do terreno:** a caixa cresce **só com rua e obra**, nunca com faixa vazia anexada (0.3.44). A vila cheia (uma volta da varredura sem lote) escolhe o melhor lado alcançável por rua — mais chão plano e seco, outra colônia veta — e puxa a rua para fora por ele, no máximo uma vez a cada 30 min; as construções da borda crescem a caixa. Casa reivindica 16 em volta, oficina e roça 24. Sem teto de tamanho (Emenda 7) | 10-12 (10-13) | ✅ feita (0.3.44) | `VillageExpansion`, `VillageExpansionSurvey`, `StripRoad`, `VillageGrowth.byPiece/byRoad` |
| 57 | O pastor põe no máximo um poste por casa e um por cercado (nenhum poste novo a até 6 blocos de um poste solto) | 10-12 | ✅ feita (0.3.38) | `ShepherdPens.postBesideHome`, `hasLonePostNear` |
| 58 | Madeira e pedra cruas podem passar dos limites do baú do ofício e do teto por tipo: o excedente vai para os baús públicos da vila (sem ofício, fora a boca da mina) em vez de cair no chão ou se perder. Árvore nunca fica pela metade | 10-12 | ✅ feita (0.3.38) | `PublicChests`, `TreeFelling.finishTheTrunk`, `MinerHaul.deposit` |
| 55 | Emenda (10-12, 0.3.38) | Grama, relva alta e flores da planta surgem sozinhas na obra, sem o construtor ir até elas (`GrowingPieces`) |
| 56 | Emenda (10-12, 0.3.39) | Uma em cada 5 obras novas procura lote só na faixa de 16 blocos da borda da caixa (dentro ou fora); sem lote, a vez cai e não conta para anexar (`VillageExpansion.buildOpened/allowsLotAt`) |
| 58 | Emenda (10-12, 0.3.39) | Sem baú público, a vila põe um numa casa da colônia, encostado na parede e a mais de 2 blocos de porta (`PublicChestPlacement`); baú público é só o de dentro da caixa |
| 59 | Ajudam a obra só carpinteiro e pedreiro (fazendeiro e pastor não); o carpinteiro guarda um pouco de toda peça de madeira e, sem tarefa, calça a rua como o construtor | 10-12 | ✅ feita (0.3.39) | `BuildHelper.HELPS`, `ItemStock.isWoodPiece`, `BuilderPaving.PAVERS` |
| 55 | Emenda (10-12, 0.3.42) | O lenhador ocioso planta uma muda fora da vila por vila a cada 5 minutos (`OutskirtsPlanting.EVERY`) |
| 60 | O tipo de obra que passa uma volta inteira da varredura sem lote fica 30 min fora da vez; com todas as oficinas que faltam recusadas, a vila constrói casa no lugar | 10-12 | ✅ feita (0.3.42) | `LotTypeRefusals`, `HousePlans.plansForNext`, `RoadGrowthPlanning.extendTheRoad` |
| 56 | Emenda (10-13) | Consolidada no texto principal da Regra 56 (caixa do modelo 2 é medida de novo, `VillageBoundsSave.CURRENT_MODEL` = 3; anexo no máximo a cada 30 min, `VillageExpansionSurvey.ANNEX_EVERY`) |
| 62 | A vila trabalha com o jogador dentro da caixa ou a até 48 blocos dela (`VillageFocus.PERIMETER`, emenda a ADR-003 Emenda 6, que valia só dentro da caixa); lote pode ficar encostado na rua ou 3 blocos atrás, com caminho calçado até ela (`RoadsideSites.SETBACKS`) | 10-13 | ✅ feita (0.3.45) | `VillageFocus`, `RoadsideSites.siteAt`, `SiteOpening.pavePathTo` |
| 61 | A colônia só é dada como abandonada sem vila à vista do centro **nem** dentro da caixa, e com menos de 3 trabalhadores vivos dentro da caixa | 10-12 | ✅ feita (0.3.43) | `ColonyAbandonment.sees`, `VillageAdoption.peopleStillHome` |
| 63 | **Socorro contra parada com prazo único:** o ofício que anda sem chegar ao alvo há 1 minuto de expediente é socorrido — a ação é a de cada ofício, o prazo é um só (era seis cópias de 2 min) | 10-13 | ✅ feita (0.3.47) | `WorkRescue.LIMIT` |
| 64 | As ruas da vila são medidas como **rede de caminhos conectados** a partir do centro (de bloco em bloco, degrau de 1, até 32 além da caixa); a busca de lote anda por ela em vez de varrer o raio em anéis | 10-13 | ✅ feita (0.3.46) | `RoadNetwork` |
| 53 | A boca da mina só nasce na faixa da borda da caixa da vila (até 8 blocos da linha, dentro ou fora); com a mina aberta, o mineiro sem ramal livre não cava pedra exposta na superfície | 10-11 | ✅ feita (0.3.32) | `MineEdge.nearTheEdge`, `MineDigging.mineOf`, `MineTrouble`, `MineDigging.exposedStoneFallback` |

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
| 1, 5 | Emenda (10-11) | **Teto geral do estoque:** nenhuma meta passa de 64 para bloco fabricado (bancada ou fornalha) nem de 256 para bloco cru, a não ser o que a obra aberta ainda pede (`StockCeiling`). Artesão sem tarefa ajuda a obra (`BuildHelper`) |
| 7 | Emenda (10-11) | A árvore cortada longe do bosque não é replantada lá: a muda vai para o bosque perto do baú do lenhador, que cresce até o dobro (`VillageGrove.takesTheSapling`) |
| 31 | Emenda (10-11) | Com nada maduro e nenhum canteiro vazio, o fazendeiro ara a terra **ao lado da roça existente**, com água a até 4 blocos, longe da rua e de obra, com semente nos baús (`FarmerTill`). Arar em qualquer lugar continua proibido |
| 26 | Emenda (10-11) | **Divisão por material:** madeira → carpinteiro; pedra, barro, terracota, fornalha → pedreiro; metal, vidro, gelo, neve → fundidor; tecido e corante → pastor; papel, livro, farinha de osso, feno → fazendeiro (`CraftFamily`). Carpinteiro e fundidor guardam 8 de cada item das plantas (`ItemStock`); flores, grama, bambu, cana (fazendeiro) e gelo, neve (fundidor) têm coleta (`PlantPatch`) |
| 51 | Emenda (10-11) | A peça que surge no baú surge **na quantidade toda** que a obra ainda pede (`BiomeConstructionSupply.askedByTheBuild`) |
| — | Decisão (10-11) | **Troca de ofícios desligada:** ninguém troca de ofício por ócio nem por desistência; a Regra 51 (15%) fica só como medida |
| 43 | Emenda (10-11) | **Pastor:** cercado com lugar primeiro (≤ 5 animais; com mais, outro cercado), pelo portão; sem cercado, a cerca mais perto; sem cerca, um poste novo ao lado da casa, com cerca do baú (`ShepherdPens`) |
| 14 | Emenda (10-11, 0.3.32) | A vaga do ajudante não é a tarefa da obra: com o construtor fora, o planejador abre outra para ele (`ConstructionPlanner.ensureTask`) |
| 51 | Emenda (10-11, 0.3.32) | ~~O fazendeiro com a roça descansando ajuda a obra~~ — **revogada em 13-10 pela Regra 59**: fazendeiro e pastor não ajudam a obra |
| 1, 5 | Emenda (10-11, 0.3.32) | Sem obra pedindo, a tora só vira tábua até o teto de 64 tábuas (`StockRules.logsToConvert`) |
| 51 | Emenda (10-11, 0.3.32) | Antes de surgir, o que a vila já tem em outro baú é **levado** ao baú da profissão (`ChestTransfer`); só surge o que não há em baú nenhum |
| 14 | Emenda (10-11, 0.3.32) | Obra que só tem até 4 peças sem apoio, depois de duas janelas de paciência, larga essas peças e fecha (`UnsupportedRelease`, linha `VC_UNSUPPORTED`) |
| 14 | Emenda (10-11, 0.3.32) | A obra que volta do save volta no giro em que foi levantada: das quatro orientações, a que mais casa com o que está de pé (`BlueprintPlacement.resumedBlueprintOf`) |
| 26 | Emenda (10-12, 0.3.33) | A cadeia da obra desce a receita também da peça sem recurso declarado (tijolos → tijolo assado → argila), até 3 degraus (`WorkMaterialRecipeNeeds`) |
| 7 | Emenda 2 (10-11) | O lenhador corta a árvore **mais perto dele** dentro do alcance da vila (a ordem centro → borda de 10-08 saiu) |
| 14 | Emenda (10-11) | **O construtor sempre começa a obra**, podendo receber ajuda ou não: a tarefa que o planejador abre é só dele; ajudante entra pela vaga do `BuildHelper`, a até 32 blocos (`HelperReach.mayStart`) |
| 29 | Emenda (10-11) | Aberta a escada, os ramais 1 a 3 abrem já e pulam a sala comum, que fica com o ramal zero (`Mine.skipTheSharedHall`) |

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

Nenhuma. A última, P0.7 (pedra como solo de lote), foi aceita pelo autor em 07-10 (ADR-039 F1):
o código já tratava todo bloco sólido como solo desde 15-09 (`LotGround.isLotGround`).

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
