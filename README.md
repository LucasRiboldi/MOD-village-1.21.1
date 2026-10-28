# Village Colony

Mod Fabric para Minecraft 1.21.1 que transforma vilas Vanilla em colônias
autônomas. Os aldeões recebem profissões, usam baús reais, produzem recursos,
plantam, mineram e constroem estruturas do próprio jogo sem exigir menu ou
mod no cliente.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Versão](https://img.shields.io/badge/Vers%C3%A3o-0.3.102%20alpha-orange)
![Licença](https://img.shields.io/badge/Licen%C3%A7a-MIT-informational)
[![CI](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml/badge.svg)](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/workflows/ci.yml)

Download: na [página de versões](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases)
do GitHub — o JAR saiu do repositório (ADR-036, item 26). Cada versão traz o
SHA-256 do JAR na descrição.

## O que o mod faz

- Adota vilas Vanilla e mantém o estado no mundo, nos aldeões, baús e blocos.
- Atribui profissões e garante camas e baús distintos para a fundação.
- Usa receitas e blocos Vanilla em vez de inventário ou economia virtual.
- Busca madeira, pedra, lã, comida, areia, terra, relva e minérios quando há
  demanda real.
- Planta e mantém até dez árvores da madeira do bioma, fora do centro da vila,
  usando muda e terra enraizada.
- Cria uma `BigHouseMOD` automaticamente para a fundação de cada vila.
- Escolhe estruturas Vanilla permitidas pelo bioma e constrói uma por vez.
- Revarre construções incompletas ciclicamente antes de abrir uma obra nova.
- Cancela uma obra profissional quando o jogador coloca uma Tocha das Almas
  dentro dela, liberando a fila; a `BigHouseMOD` é protegida dessa regra.
- Evita sobreposição com estruturas existentes, projetos pendentes e blocos
  físicos dentro da área vertical protegida.
- Põe o aldeão ocioso para recolher do chão só o item que falta à obra aberta
  (Regra 48) e mostra no `/vc log` quem espera o quê há mais de dois minutos e,
  por ofício, a última janela de cinco minutos do expediente (trabalho,
  caminhada, espera, bloqueio, ócio e encalhe), marcando quem passa de 15% na
  regra do ócio. O comando é aberto a todo jogador.
- Regras de profissão por mundo, editáveis pelo [Mod Menu](https://modrinth.com/mod/modmenu)
  (opcional): ativar ou desativar a profissão, limite de trabalhadores, raio de
  busca do lenhador, fazendeiro e pastor, e ordem de contratação (ADR-030).
- Com o mod no cliente, mostra a profissão sobre o aldeão, na cor do que ele
  faz agora — laranja no ofício, azul ajudando, vermelho travado há 30 s,
  branco à toa, cinza fora do expediente (Regra 129) — e o progresso da obra,
  com o primeiro material que falta. Sem o mod no cliente, a profissão
  não aparece: desde 2026-10-10 (P0.8) o servidor não escreve mais o nome da
  profissão no aldeão.
- Liga a porta de cada construção pronta à rede de caminhos da vila: caminho
  de até 3 blocos de largura, **rente ao chão** — troca só o bloco de cima,
  cobre só o buraco de 1 bloco, corta no máximo 1 e nunca põe bloco acima do
  terreno; sobe no máximo 1 bloco por passo, estreita onde o lado não cabe,
  contorna o desnível maior, nunca passa por construção nem pela boca da mina e
  se junta aos caminhos que já existem; vai ao chafariz por atalho quando a
  rede dá mais de 2 vezes a volta (Regras 123 a 125). Quem assenta é o
  pedreiro, o carpinteiro ou o fundidor sem tarefa, um por vez.
- No deserto usa o bloco próprio "caminho de areia" (Regra 73).
- A mina desce em caracol de 4 lances curtos, abre um salão de 17×17 e desce de
  novo pelos 4 cantos; a escada de descida é o caminho de volta, com no máximo
  1 bloco por degrau, e nunca é fechada; a luz é de tocha no chão dos salões
  (Regra 119).
- O pastor mantém um curral por espécie, lota pelo tamanho do curral, cria até
  8 e abate acima de 6, e pede um curral novo quando todos estão cheios ou
  quando há 5 animais amarrados no mesmo poste (Regras 68, 72 e 130).
- Antes de cada obra nova, nivela o chão em volta na altura da rua: um anel de
  2 blocos com no máximo 1 de diferença e uma rampa de 3 até o terreno natural
  (Regra 131).
- Nenhuma profissão espera mais de 5 minutos: quem fica parado com tarefa a
  larga e vai ajudar a vila; se a mesma tarefa trava de novo em 10 minutos, o
  produto surge no baú (Regra 128).
- Quem está sem tarefa ajuda: o pastor colhe a roça, o lenhador cuida do
  bosque (1 em cada 10 mudas é de outra espécie), o carpinteiro põe a tocha da
  colônia nos caminhos escuros, o pedreiro recolhe neve com a pá (Regra 127).
- Cada aldeão dorme na cama mais perto de onde trabalha: uma vez por dia, só
  com ganho de 16 blocos de caminhada, levando o baú junto, como uma transação
  (Regra 74).
- Cama, porta e baú são peças obrigatórias da obra; sem apoio, ganham o chão
  embaixo (Regra 21).
- Diagnóstico: tudo o que surge sem uma profissão obter está numa lista
  declarada e deixa a linha `VC_SUPPLY` no log (Regra 111); toda desistência
  no diário de ações leva um código único de bloqueio (Regra 112).

O mod é alpha. A 0.3.100 (2026-10-28) passou na bateria automática completa
(ver "Desenvolvimento e verificação"). O último jogo foi o da 0.3.99 na
savana (Spark `eVkPhNEtZT`, 76 min): 20 TPS sem travão e o caminho rente ao
chão; o tempo dos ofícios mostrou vila madura parada (estoque cheio), tarefas
que abriam sem poder render, roça fora do alcance e o mineiro preso na boca.
A 0.3.100 trata cada um (Regras 133 a 137) e ainda não foi jogada. O estado vivo está no
[`STATE.md`](STATE.md). O que mudou em cada
publicação está no [`CHANGELOG.md`](CHANGELOG.md); a avaliação técnica mais
recente, em
[`docs/technical/avaliacao/`](docs/technical/avaliacao/).

## Profissões e funções

Estas são as oito funções operacionais atuais (revistas em 2026-09-30). `BREEDER` foi encerrado;
saves antigos com esse código são carregados como `SHEPHERD`.
`MANUFACTURER` não existe no código atual; documentos que usam esse nome estão
desatualizados.

| Código | Nome | Ferramenta inicial | Responsabilidade | Executor |
|---|---|---|---|---|
| `MINER` | Mineiro | Picareta de ferro | Abre e amplia a mina, coleta pedra e minério; guarda até 256 de cada tipo que a tarefa não pediu | `MinerWork` |
| `LUMBERJACK` | Lenhador | Machado de ferro | Derruba árvores, replanta e mantém o viveiro da borda | `LumberjackWork` |
| `MASON` | Pedreiro, equivalente ao ferreiro do catálogo | Nenhuma | Produz alvenaria e peças de pedra exigidas pelas obras | `CraftingWork` |
| `SMELTER` | Fundidor | Pá de ferro com Silk Touch | Funde materiais e coleta areia, relva, cacto e argila sob demanda | `SmelterWork` |
| `CARPENTER` | Carpinteiro | Nenhuma | Artesão geral: madeira e toda peça que não é alvenaria (vidraça, tear, cama, lampião, tocha) | `CraftingWork` |
| `FARMER` | Agricultor/Fazendeiro | Enxada de ferro | Mantém lavouras, cava terra para obra e faz pão do trigo acima da reserva | `FarmerWork` |
| `SHEPHERD` | Pastor | Tesoura | Tosquia, faz o rebanho procriar (até 8 adultos por espécie) e abate acima de 6, um curral por espécie | `ShepherdWork` |
| `BUILDER` | Construtor | Nenhuma | Reserva lotes, repara e assenta estruturas | `BuilderWork` |

Cada ofício tem a sua bancada do mod (estação de trabalho, produção em dobro e
passivas na vila); ver [Blocos do mod](#blocos-do-mod).

Compatibilidade: `BREEDER` é convertido para `SHEPHERD` ao ler saves antigos.
O agricultor continua profissão completa, sem cama ou baú fundacional dentro
da `BigHouseMOD`. O carpinteiro é titular da fundação desde 2026-09-30.

Regras transversais de 2026-09-30:

- Cada bloco é quebrado com a ferramenta de ferro certa para ele
  (`ActionTool`); a da mão fica quando é tão boa ou melhor.
- A contratação atende primeiro a profissão de que a obra depende agora
  (`ProfessionDemand`), depois a ordem fixa.
- Aldeão com profissão do mod não recebe profissão Vanilla (ADR-029).
- A regra "a colônia não cria recurso" foi retirada (ADR-028): corante,
  linha, pó de osso e drops de inimigo e animal aparecem no baú quando a
  receita do artesão pede; bloco sem item é montado no local; a **lava nunca**
  é assentada.

## Regras do mod

Todas as regras do autor, numeradas, com a data da decisão, o estado e as classes que as aplicam. A fonte é
o [`docs/RULES.md`](docs/RULES.md): esta seção é gerada por `python scripts/rules_to_readme.py` e não se
edita à mão (`--check` acusa quando ela ficou para trás).

<!-- REGRAS:INICIO (gerado por scripts/rules_to_readme.py a partir de docs/RULES.md; não editar à mão) -->

### Regras ativas

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
| 21 | Toda casa nasce com cama, baú e lampião — **resolvida em 10-22 (C-02, autor):** cama, porta e baú vêm da planta e são **obrigatórios**; sem apoio, ganham o chão do bioma embaixo e são postos; só quando o apoio é impossível (algo protegido embaixo) caem nas saídas de sempre, com `VC_FUNCTIONAL_MISSING`. As demais peças (lampião inclusive) mantêm as saídas atuais, sem obra eterna | 08-19 | ✅ feita (0.3.93) | `FunctionalPieces`, `BuilderPlacement`, `UnsupportedClosing` |
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
| 32 | ~~Móveis e cama entram depois da casa pronta~~ — **substituída em 10-22 pela 21 (C-02, autor):** não há segunda passada de mobília; cama, porta e baú vêm da planta, como peças obrigatórias | 08-29 | ❌ substituída | — |
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
| 54 | **Mina em rede** (ADR-042): passada a escadaria e o salão, cada nível é uma grade de nós a cada 4 blocos (corredor 1 × 2, raio 6), um mineiro por nó; o alvo é sempre a frente do próprio túnel, alcançável andando pelo que já está aberto, e a célula que ele não alcança é pulada (o nó só falha com toda a pedra que falta recusada — mina M, 0.3.48); o minério ao alcance do braço a partir de um túnel aberto vem antes do túnel; piso, fluido encostado e beirada de queda garantidos com enchimento que **surge** (a mina não gasta recurso da vila — 0.3.51, como o `MineFlooding` já fazia para o fluido; antes vinha dos baús e travava quando faltava, playtest 0.3.50); caminho só pelo que está pronto; nível esgotado desce; no fundo, caminhos laterais e volta pela rampa. O retorno subindo nunca quebra as 5 camadas sob a vila | 10-12 | ✅ feita (0.3.40) | `MineNetwork`, `NetworkDigging`, `NetworkSafety`, `NetworkVeins`, `MineReturn` |
| 54 | Emenda (10-12, 0.3.36) | Túnel de ramal 1 × 2 a cada 4 blocos (raio 6); minério ao alcance do braço vem antes do túnel; segurança só de piso, fluido e beirada; retorno nunca quebra as 5 camadas sob a vila (`MineReturn.SHELL_DEPTH`) |
| 55 | Roça grande primeiro; fundidor ocioso adianta o cru; lenhador ocioso planta mudas até 10 fora da caixa; pastor alimenta com qualquer comida; grama e flores surgem sozinhas (a ordem das obras saiu para a Regra 49, consolidada em 13-10) | 10-12 | ✅ feita (0.3.36) | `FarmPlans`, `SmelterAdvance`, `OutskirtsPlanting`, `ShepherdFlock`, `BlockShaping.growsByItself` |
| 56 | **Expansão do terreno:** a caixa cresce **só com rua e obra**, nunca com faixa vazia anexada (0.3.44). A vila cheia (uma volta da varredura sem lote) escolhe o melhor lado alcançável por rua — mais chão plano e seco, outra colônia veta — e puxa a rua para fora por ele, no máximo uma vez a cada 30 min; as construções da borda crescem a caixa. Casa reivindica 16 em volta, oficina e roça 24. Sem teto de tamanho (Emenda 7). **Emenda A5 (autor, 2026-10-22):** se a rua não entra na faixa (duna na entrada), o lote nasce **dentro da faixa, o mais fundo que couber**, ligado à rua por um caminho a pé que segue o terreno (degrau ≤1, sem cortar nem aterrar, até 32 blocos), calçado na abertura da obra | 10-12 (10-13, 10-22) | ✅ feita (0.3.44; A5 0.3.87) | `VillageExpansion`, `VillageExpansionSurvey`, `StripRoad`, `StripLot`, `VillageGrowth.byPiece/byRoad` |
| 57 | O pastor põe no máximo um poste por casa e um por cercado (nenhum poste novo a até 6 blocos de um poste solto) | 10-12 | ✅ feita (0.3.38) | `ShepherdPens.postBesideHome`, `hasLonePostNear` |
| 58 | Madeira e pedra cruas podem passar dos limites do baú do ofício e do teto por tipo: o excedente vai para os baús públicos da vila (sem ofício, fora a boca da mina) em vez de cair no chão ou se perder. Árvore nunca fica pela metade | 10-12 | ✅ feita (0.3.38) | `PublicChests`, `TreeFelling.finishTheTrunk`, `MinerHaul.deposit` |
| 55 | Emenda (10-12, 0.3.38) | Grama, relva alta e flores da planta surgem sozinhas na obra, sem o construtor ir até elas (`GrowingPieces`) |
| 56 | Emenda (10-12, 0.3.39) | Uma em cada 5 obras novas procura lote só na faixa de 16 blocos da borda da caixa (dentro ou fora); sem lote, a vez cai e não conta para anexar (`VillageExpansion.buildOpened/allowsLotAt`) |
| 58 | Emenda (10-12, 0.3.39) | Sem baú público, a vila põe um numa casa da colônia, encostado na parede e a mais de 2 blocos de porta (`PublicChestPlacement`); baú público é só o de dentro da caixa |
| 59 | Ajudam a obra só carpinteiro e pedreiro (fazendeiro e pastor não); o carpinteiro guarda um pouco de toda peça de madeira e, sem tarefa, calça a rua como o construtor | 10-12 | ✅ feita (0.3.39) | `BuildHelper.HELPS`, `ItemStock.isWoodPiece`, `BuilderPaving.PAVERS` |
| 55 | Emenda (10-12, 0.3.42) | O lenhador ocioso planta uma muda fora da vila por vila a cada 5 minutos (`OutskirtsPlanting.EVERY`) |
| 60 | O tipo de obra que passa uma volta inteira da varredura sem lote fica 30 min fora da vez; com todas as oficinas que faltam recusadas, a vila constrói casa no lugar | 10-12 | ✅ feita (0.3.42) | `LotTypeRefusals`, `HousePlans.plansForNext`, `RoadGrowthPlanning.extendTheRoad` |
| 56 | Emenda (10-13) | Consolidada no texto principal da Regra 56 (caixa do modelo 2 é medida de novo, `VillageBoundsSave.CURRENT_MODEL` = 3; anexo no máximo a cada 30 min, `VillageExpansionSurvey.ANNEX_EVERY`) |
| 56 | Emenda (10-14, 0.3.58) | A vila com uma faixa anexada que não rendeu lote anexa de novo já na volta seguinte sem lote (`FULL_AGAIN_AFTER = FULL_AFTER`, era 3×): crescia devagar demais e ficava ociosa entre anexos (playtest `iALIIoOwEK`: produtores 70–98% ociosos com estoque cheio). O teto de 1 anexo a cada 30 min (`ANNEX_EVERY`) continua — é a proteção contra a deriva do centro que soltou 27 aldeões na 0.3.42 |
| 62 | A vila trabalha com o jogador dentro da caixa ou a até 48 blocos dela (`VillageFocus.PERIMETER`, emenda a ADR-003 Emenda 6, que valia só dentro da caixa); lote pode ficar encostado na rua ou 3 blocos atrás, com caminho calçado até ela (`RoadsideSites.SETBACKS`) | 10-13 | ✅ feita (0.3.45) | `VillageFocus`, `RoadsideSites.siteAt`, `SiteOpening.pavePathTo` |
| 61 | A colônia só é dada como abandonada sem vila à vista do centro **nem** dentro da caixa, e com menos de 3 trabalhadores vivos dentro da caixa | 10-12 | ✅ feita (0.3.43) | `ColonyAbandonment.sees`, `VillageAdoption.peopleStillHome` |
| 63 | **Socorro contra parada com prazo único:** o ofício que anda sem chegar ao alvo há 1 minuto de expediente é socorrido — a ação é a de cada ofício, o prazo é um só (era seis cópias de 2 min) | 10-13 | ✅ feita (0.3.47) | `WorkRescue.LIMIT` |
| 64 | As ruas da vila são medidas como **rede de caminhos conectados** a partir do centro (de bloco em bloco, degrau de 1, até 32 além da caixa); a busca de lote anda por ela em vez de varrer o raio em anéis | 10-13 | ✅ feita (0.3.46) | `RoadNetwork` |
| 65 | **Bancadas exclusivas do mod**: três blocos próprios (`carpenter_bench`, `mason_cutter`, `blacksmith_forge`), réplicas das estações do carpinteiro/pedreiro/ferreiro com luz 10. Só o **player** as fabrica (receita = estação Vanilla + 4× ouro, sempre disponível); a colônia nunca as faz nem coloca. Vínculo por POI próprio do mod (sem profissão Vanilla); o titular que trabalha com a bancada da sua profissão perto **dobra a produção por ciclo**. Aparecem na aba do criativo "Village Colony — Bancadas" (0.3.53) | 10-13 | ✅ feita (0.3.52) | `ModContent`, `ModBenchBonus`, `CraftStation.modBench`, `CraftingSteps`, `SmelterWork` |
| 66 | **Cercado no nível da rua**: estrutura cuja camada y=0 é só solo natural (grama, terra, areia) tem essa camada tratada como a camada da rua — o chão de pisar fica no nível da rua e o que sobe dela (a cerca do `animal_pen`) acima, em vez de o recinto afundar um bloco (bug 0.3.52) | 10-13 | ✅ feita (0.3.53) | `StreetLayer.soilBase` |
| 67 | **Plantação por população**: uma roça a cada 15 adultos, no mínimo uma desde a fundação; ao passar de 15 adultos (o 16º), a obra de fazenda vira prioridade e fura a fila antes de oficinas e do rodízio (só a falta de cama vem antes). Substitui a Regra 52 (uma por fazendeiro) | 10-13 | ✅ feita (0.3.54) | `ConstructionPriority.FARM_DEFICIT`, `ColonyFarms.owed` |
| 68 | **O pastor aproveita o rebanho** (manutenção do ofício, não toca a obra): com mais de 6 adultos de uma espécie (a criação enche até 8; eram 8 e 12 até 10-22, autor), abate 1 por ciclo o excedente deixando sempre ≥6 vivos, e deposita o drop Vanilla no baú (couro/carne); a carne crua acima de 16 é cozida sem combustível; o ovo que a galinha larga no chão é recolhido. Reserva viva (6 adultos) e reserva de item (16) são limites distintos | 10-14 | ✅ feita (0.3.56; 8/6 em 0.3.89) | `FlockHarvest`, `ShepherdFlock.tend`, `CraftingLookup.smelted` |
| 69 | **A horta da casa surge sem gastar recurso**: os blocos de chão da planta que não têm item nem receita (farmland, água, caminho, cultivo) são moldados no local, com o terreno garantido embaixo, em vez de passar pela cadeia de material (que falhava, "could not make farmland", travando a obra nos últimos blocos — playtest 0.3.56). Só sobre lugar livre; bloco posto não é sobrescrito (Regra 3) | 10-14 | ✅ feita (0.3.57) | `BuilderPlacement.placeOne`, `BlockShaping.isShapedFromTheGround`/`groundFor` |
| 70 | **Corredor de caminho:** todo caminho que a colônia traça até uma construção tem um bloco de largura, sai da porta em linha reta na direção dela, vira só em ângulo reto (no máximo 2 vezes, nunca antes de 2 blocos retos) e para no primeiro caminho que encostar, de frente ou de lado; o construtor sem obra só fecha o buraco de um bloco no meio de uma reta (caminho dos dois lados opostos e nada dos outros), nunca canto — para não virar tudo caminho. **Degrau cortado (autor, 10-22; emenda de 10-28, opção 3):** o corredor do lote tenta primeiro a **serpentina** pelo terreno, até 6 curvas, degrau de no máximo um e sem cortar; só sem volta por ela desce a encosta cortando o chão do bioma, **até 3 de fundo** (era 12); a abertura da obra corta a coluna do caminho e desce os lados em **talude** — a coluna a k blocos fica k acima do piso, um degrau por bloco, e o degrau exposto ganha a grama de cima —, em vez do vale de paredes retas; guarda o que sai nos baús públicos. Nunca aterra; só corta o que o lote cortaria | 10-22 | ↪️ substituída pela 123 (10-27) nos caminhos da rede; o corredor do lote segue, com serpentina e talude (0.3.101) | `Corridor`, `WorkPath`, `StripLot`, `BuilderPaving.isGap`, `LotTerraform.cutAbove`, `CorridorCut` |
| 71 | **O mineiro não constrói para fora da montanha:** o desvio dele não põe bloco ao céu aberto (só dentro da montanha) e, se precisar construir para andar, desce antes de ir de lado (cavar descendo custa o de sempre, de lado o dobro, subindo o triplo; pôr bloco o dobro); com a entrada da mina inalcançável a pé, ele cava um acesso só cavando, descendo da vila até o nível; debaixo da terra não entra na água (ADR-025), ao ar livre nada pelo custo do Vanilla — a margem de lago cercada por paredão só se deixa nadando (autor, 10-22) | 10-22 | ✅ feita (0.3.88; água 0.3.89) | `MinerTerrain`, `DetourMoves.Costs.MINER`, `MineAccess`, `MinerCaution` |
| 72 | **Um curral por espécie:** animais iguais ficam no mesmo curral; com mais de um curral na vila, cada tipo vai para o seu — o pastor leva o animal para o curral só daquela espécie, senão um vazio (que passa a ser dela), senão um misto que já a tem, senão qualquer um com lugar; sem animal solto a recolher, tira de um curral misto o animal que tem curral próprio ou vazio com lugar e o leva até lá, para que, com a vila crescendo, cada espécie acabe no seu. **Lotação pelo tamanho** (autor, 10-22): um animal a cada 4 blocos de chão do curral (eram 5 fixos); **com pastor e todos os currais cheios, um curral novo (animal_pen do estilo) entra na fila de obras**, depois do armazém e da falta de cama | 10-22 | ✅ feita (0.3.89) | `ShepherdPens.forSpecies`, `ShepherdHerding.separate`, `ShepherdPens.capacityOf`/`allFull`, `PenPlans`, `UrgentPlans` |
| 73 | **Caminho de areia:** a vila do deserto tem caminho próprio — cópia do caminho de terra do Vanilla (mesma altura, mesmo comportamento: coberto por bloco sólido volta a ser o chão, aqui areia; quebrado dá areia) com a cor do arenito. No bioma de deserto todo caminho que a colônia assenta (rua, corredor, caminho de obra, buraco fechado pelo construtor) é ele; fora do deserto ele não é usado, e o caminho segue o do chão (Regra 70 e pedido de 10-20). Conta como rua em qualquer lugar | 10-22 | ✅ feita (0.3.89) | `SandPathBlock`, `ModContent.SAND_PATH`, `VillageRoad.pavingOn(world, at, …)`, `VillageRoad.isPathBlock` |
| 74 | **A cama mais perto do trabalho:** cada aldeão dorme na cama da vila mais perto de onde trabalha de fato (a mediana das últimas 32 ações com lugar do diário; com menos de 8, fica onde está). A vila reavalia uma vez por dia, ao amanhecer, e só muda quem fica ao menos 16 blocos mais perto; quem anda mais escolhe primeiro. Cama livre só vale se se alcança a pé da vila **e tem baú livre ao lado**; cama ocupada troca de dono quando a soma das duas caminhadas cai ao menos 16 blocos, mesmo que o dono passe a andar um pouco mais (o sem ofício sempre cede; autor 10-27, 0.3.97); e a tarefa é oferecida primeiro a quem do ofício mora mais perto do lugar dele — boca da mina, obra aberta, bosque, ou o próprio baú (0.3.97); quem está dormindo não muda nem cede. **A distância é a caminhada pelo mapa a pé** (linha reta só onde o mapa não alcança o trabalho, como a mina). **A mudança é uma transação** (emenda de 10-22, R-043 do estudo v2): antes de mexer, cada mudança tem a cama e o baú que vai com ela — numa troca, cada um fica com o baú do outro, remarcado com o próprio ofício; se falta algo, a mudança cai com a outra metade da troca e ninguém fica sem baú. O `HomeMove` (desistiu 2× abaixo do trabalho) continua como reação imediata | 10-22 | ✅ feita (0.3.92; transação 0.3.93) | `BedPlan`, `BedChoice`, `Walks`, `ChestScanner.freeChestBeside`, `ActionJournal` |
| 53 | A boca da mina só nasce na faixa da borda da caixa da vila (até 8 blocos da linha, dentro ou fora); com a mina aberta, o mineiro sem ramal livre não cava pedra exposta na superfície | 10-11 | ✅ feita (0.3.32) | `MineEdge.nearTheEdge`, `MineDigging.mineOf`, `MineTrouble`, `MineDigging.exposedStoneFallback` |
| 75 | **Movimentação natural:** só o mineiro remodela o terreno para subir (degrau, túnel); ninguém põe bloco debaixo dos pés, e não há escada fixa | 10-15, 10-21 | ✅ no código (registrada 10-22) | `ClimbPolicy` |
| 76 | **O encalhado nunca desiste:** larga o ofício e sobe até a superfície; a escada da fuga é tampada depois que ele sai | 10-01 (E47), 09-24 (N10) | ✅ no código (registrada 10-22) | `ClimbOut`, `StrandedEscape`, `EscapeBackfill` |
| 77 | **Cama como último recurso:** preso 5 vezes no mesmo lugar, ou sem caminho para casa à noite, ele vai para a cama | ADR-036 item 15 | ✅ no código (registrada 10-22) | `BedTeleport`, `NightHome` |
| 78 | **Caminhada longa por etapas:** um trajeto de 50 a 130 blocos vira etapas na superfície | 10-08 (A7) | ✅ no código (registrada 10-22) | `WalkLegs` |
| 79 | **A pedra é emprestada:** o alvo do mineiro vale por um prazo curto, e só o progresso renova | 09-10 (E44) | ✅ no código (registrada 10-22) | `MineLease` |
| 80 | **Só vira alvo o bloco com posição de trabalho válida e alcançável** | 10-08 (E1) | ✅ no código (registrada 10-22) | `MiningTarget` |
| 81 | **Ramal com dono:** cada ramal da mina é reservado a um mineiro, com o estado dito pelo nome | 09-04, 10-08 (E2) | ✅ no código (registrada 10-22) | `MineArm`, `MineBranch` |
| 82 | **O mineiro memoriza a entrada da mina** e chega por ela, por etapas, até o nível de agora | 10-08, 10-14 | ✅ no código (registrada 10-22) | `MineEntrance`, `MineApproachPath` |
| 83 | **Espera a areia assentar** antes de voltar a cavar | 09-19 | ✅ no código (registrada 10-22) | `MineSettling` |
| 84 | **Tapa e escora com o bloco do bioma** (arenito no deserto) | 10-20 | ✅ no código (registrada 10-22) | `MineFill` |
| 85 | **O ferro descansa onde a mina não o tem:** o piso de ferro cru não força busca sem veio | 10-21 | ✅ no código (registrada 10-22) | `IronRest` |
| 86 | **A peça que o construtor não alcança surge no lugar** | 10-20 | ✅ no código (registrada 10-22) | `UnreachablePieces` |
| 87 | **Peça equivalente:** sem a exata e sem tag do jogo, vale a equivalente da tabela | 09-24 (N3) | ✅ no código (registrada 10-22) | `EquivalentPieces` |
| 88 | **Terracota, feno e argila aparecem quando a obra pede** | 10-21 | ✅ no código (registrada 10-22) | `AppearingPieces` |
| 89 | **Degrau na frente da porta** do lote que fica um bloco acima da rua | 09-30 | ✅ no código (registrada 10-22) | `DoorStep` |
| 90 | **Terraplanagem do lote:** corta até 5 e aterra até 2 blocos do chão do bioma, com um corredor de 2 em volta da casa | 10-20, 10-21 | ✅ no código (registrada 10-22) | `LotTerraform` |
| 91 | **Casa fundadora:** primeiro o lote plano exato, depois o aplainável; a vegetação do lote sai | 10-20 (G1) | ✅ no código (registrada 10-22) | `FoundationLotSearch`, `LotGrowth` |
| 92 | **Deserto:** a areia da planta vira arenito (emenda à Regra 27), e o chão da vila de areia não desaba | 09-19, 10-21 | ✅ no código (registrada 10-22) | `DesertSand`, `DesertGround` |
| 93 | **Obra abandonada tem retomada limitada** antes de a vila aceitar obra nova | — | ✅ no código (registrada 10-22) | `BuildingRepairPlanner` |
| 94 | **Faixa de construção:** o lote novo nasce ao lado da rua; muda, flor alta ou arbusto crescendo ali tiram o lote da faixa | 10-11 | ✅ no código (registrada 10-22) | `ConstructionZone` |
| 95 | **Todo ofício trabalha independente do pedido** (produção contínua até os tetos) | 09-26 | ✅ no código (registrada 10-22) | `StandingWork` |
| 96 | **Todo trabalhador de ofício tem um baú nascido para ele** (emenda à Regra 8) | 09-26 | ✅ no código (registrada 10-22) | `ChestSpawner` |
| 97 | **Baú cheio chama os outros ofícios**, e os 10 últimos espaços dos baús da colônia ficam sempre livres | 09-19, ADR-036 item 9 | ✅ no código (registrada 10-22) | `ChestRelief`, `ProfessionChestOverflow` |
| 98 | **O que a vila provou não ter no alcance não é mais buscado**, e o raio varrido vazio espera | 10-20, 10-02 (F-1) | ✅ no código (registrada 10-22) | `KnownAbsent`, `EmptySweeps` |
| 99 | **O alcance da coleta cresce com a vila** | 09-24 (N11) | ✅ no código (registrada 10-22) | `GatheringReach` |
| 100 | **Autossuficiência:** britador do pedreiro (pedra → areia), janela sem vidro, compostor do fazendeiro, pedreiro faz o insumo da fornalha, fundidor sem cru solta a fornalha, areia de beira d'água, argila | 09-19 a 10-21 | ✅ no código (registrada 10-22) | `Crusher`, `WindowEquivalent`, `FarmerCompost`, `FurnaceFeed`, `SmelterGathers`, `SandNearWater`, `ClayPatch` |
| 101 | **Receita de tingir peça pronta é ignorada** | 09-26 | ✅ no código (registrada 10-22) | `RecolorRecipes` |
| 102 | **Toda vila nasce com 2 ovelhas, 2 galinhas, 2 vacas e 2 porcos** | 10-20 | ✅ no código (registrada 10-22) | `VillageFarmAnimals` |
| 103 | **Grama ao pé do poste da ovelha** amarrada, e **o golem sai do curral** pela porteira | 10-20, 10-02 | ✅ no código (registrada 10-22) | `PostGrass`, `PenGolems` |
| 104 | **O que o jogador tirou não volta:** árvore, muda ou flor quebrada pelo jogador não é replantada naquela coluna | 10-11 | ✅ no código (registrada 10-22) | `PlayerClearedPlants` |
| 105 | **Duas colônias da mesma vila viram uma**, e a identidade é a vila gerada, não o centro | ADR-007, 09-30 (E51) | ✅ no código (registrada 10-22) | `ColonyMergeTrigger`, `ColonyIdentity` |
| 106 | **A vila é uma caixa** medida pela vila gerada, e não um raio | 09-30 (ADR-003 Emenda 6) | ✅ no código (registrada 10-22) | `VillageBounds`, `VillageMeasure` |
| 107 | **Marcas no mundo:** contorno de chamas no lote, área da vila, dono de cada baú, nome do aldeão no painel do ofício | 08-12 a 10-08 | ✅ no código (registrada 10-22) | `SiteMarker`, `VillageMarker`, `ChestMarker`, `WorkerNameplateMixin` |
| 108 | **A Tocha das Almas cancela a obra:** uma tocha das almas (também a de parede) posta pelo jogador dentro do volume de uma obra aberta cancela o canteiro e as tarefas de construção; o que já foi posto fica no mundo, e a tocha também; a fundação `BigHouseMOD` não é afetada | — | ✅ no código (registrada 10-22) | `ConstructionCancellation` |
| 109 | **A casa fundadora `BigHouseMOD`:** toda vila adotada a recebe uma única vez — cópia editada da casa grande Vanilla, com sete camas e sete baús para os titulares (mineiro, lenhador, carpinteiro, pedreiro, fundidor, pastor, construtor); o fazendeiro trabalha sem cama reservada nela; ela nunca entra no catálogo de obras | — | ✅ no código (registrada 10-22) | `BigHouseFoundation`, `VillageFoundation` |
| 110 | **Nada que a colônia corta some** (R-059 do estudo v2, autor 10-22): o que a terraplanagem do lote e o corte do corredor tiram do terreno vai para os baús públicos da vila; o que não couber cai no chão ao lado da obra, inteiro | 10-22 | ✅ feita (0.3.93) | `Cuttings`, `LotTerraform.level(…, taken)`, `CorridorCut` |
| 111 | **Suprimento excepcional só pela lista declarada** (R-080 do estudo v2, autor 10-22): o que surge sem uma profissão obter está numa lista única — peça que falta (51), drops automáticos (41), chão moldado (69), peça inalcançável (86), terracota/feno/argila (88), chão do aterro e do apoio (90, 21), baú do trabalhador (96), animais da fundação (102), grama da ovelha (103), peça de lado da obra parada (113), ração do rebanho (117); cada vez que algo surge fica uma linha `VC_SUPPLY` (exceção, regra, colônia, item, quantidade), uma por evento — o aterro de um lote é uma linha só —, e `scripts/supply_report.py` soma por exceção e item | 10-22 | ✅ feita (0.3.93) | `SupplyException`, `Supplies`, `supply_report.py` |
| 112 | **Vocabulário único de bloqueio** (R-052 do estudo v2, autor 10-22): toda soltura, desistência, adiamento, recusa ou encalhe no diário leva também um código — `NO_RESOURCE`, `RESOURCE_INACCESSIBLE`, `NO_PATH`, `SITE_BLOCKED`, `NO_VALID_SITE`, `NO_RECIPE`, `TOOL_MISSING`, `CHEST_FULL`, `TARGET_RESERVED`, `PROTECTED_BLOCK`, `NO_PROGRESS`, `UNSUPPORTED_PIECE`, `WORLD_UNAVAILABLE`, `NO_STORAGE` —, traduzido da frase pelo trecho mais específico; a frase que não casa fica `UNCLASSIFIED` (sinal para acrescentar o trecho); o `action_report.py` agrupa por código | 10-22 | ✅ feita (0.3.93) | `BlockCode`, `ActionJournal`, `action_report.py` |
| 113 | **A obra parada só em peças de lado não prende a vila** (autor 10-22, playtest do deserto: o armazém ficou duas horas em "5 blocks left — no build task" e nenhuma obra nova abriu): a cada ciclo, se a obra em construção não tem mais nada a pôr e não terminou, toda peça de lado volta à fila — seja por material, apoio ou alcance — e a tarefa de obra reabre; depois de 4 voltas a peça surge no lugar (`VC_SUPPLY` `STUCK_PIECE`, Regra 111), sem sobrescrever bloco que já esteja ali (Regra 3), e a obra fecha | 10-22 | ✅ feita (0.3.94) | `PieceRescue`, `StuckPieces`, `UnreachablePieces.surge` |
| 114 | **Ninguém é posto onde o corpo não cabe, e ninguém fica preso para sempre** (autor 10-27, jogo da 0.3.93: 12 aldeões da vila da neve e 2 do deserto morreram sufocados segundos depois de levados para a cama; o mineiro da taiga ficou 3,5 h cercado e o construtor da savana 4 h preso sem cama): todo teleporte de aldeão pousa onde o corpo inteiro cabe — chão firme, nada sólido até a cabeça e sem líquido —, em cima do colchão só com espaço até a cabeça, senão no chão livre mais perto da cama; sem lugar, ele não é levado; o preso sem cama vai ao chão do centro da vila; quem passa 3 rodadas seguidas com todas as saídas do cercado falhando é posto no chão logo atrás da cerca | 10-27 | ✅ feita (0.3.95) | `SafeLanding`, `BedTeleport.toSafety`, `PenLift` |
| 115 | **O armazém do mod sobe do chão e é feito do material do bioma** (autor 10-27: o armazém nascia dentro do solo; e no jogo da 0.3.93 a ardósia funda, a calcita e as folhas de acácia dele surgiam no lugar em todo bioma): o modelo do mod sem porta pisa na camada 0, que vai no nível do chão, e o lote é nivelado como o de uma casa; cada estilo (planície, deserto, savana, taiga, neve) tem o seu armazém, com a madeira e a pedra do bioma — no deserto, arenito no lugar da madeira —, sem folhas, alçapões nem teias, e com baú comum no lugar do baú do fim; o de todos os estilos só vale para o estilo sem o seu | 10-27 | ✅ feita (0.3.95) | `StructureBlueprintReader`, `ColonyModels.tradeHousesFor`, `scripts/storage_variants.py` |
| 116 | **A argila aparece na terceira busca vazia, em todo bioma** (autor 10-27, jogo da 0.3.93: na planície sem argila o fundidor passou a sessão parado na fornalha, milhares de linhas "no clay in reach"): argila e bola de argila surgem no baú de quem busca depois de 3 varreduras vazias seguidas do alcance da vila, e não 4 como os outros materiais (Regra 40); depois, a ausência fica registrada e o próximo lote vai direto ao baú | 10-27 | ✅ feita (0.3.95) | `LocateFallback.attemptsFor` |
| 117 | **O pastor recebe ração quando o fazendeiro não produziu** (autor 10-27, jogo da 0.3.93: "the shepherd has no food" milhares de vezes em todo bioma, rebanho parado abaixo da meta): quando o par está pronto para procriar e nenhum baú tem nenhuma comida que a espécie aceite (com um pouco do fazendeiro guardado, o par espera a colheita), a ração vem sozinha — trigo para ovelha e vaca, cenoura para porco, semente de trigo para galinha — e o par é alimentado, com uma linha `VC_SUPPLY` `ANIMAL_FEED` (Regra 111); o ritmo continua o do pastor (um par por espécie por rodada, até 8 adultos) | 10-27 | ✅ feita (0.3.95) | `ShepherdFlock.ANIMAL_FEED` |
| 118 | **No deserto o bosque do lenhador é o dobro e se espalha pelas bordas** (autor 10-27, jogo da 0.3.93: três lenhadores do deserto 61% ociosos, 39 desistências "made no progress" esperando as mudas, todas juntas perto do baú, crescerem): na vila de estilo deserto o bosque tem o dobro de mudas por lenhador e cada muda nova vai a um ponto sorteado do anel em volta da caixa da vila, com as mesmas folgas de casa, obra e rua | 10-27 | ✅ feita (0.3.95) | `VillageGrove.DESERT_FACTOR`, `VillageGrove.isDesert` |
| 119 | **A mina desce em caracol, abre salão e desce de novo pelos quatro cantos, e o caminho que desce é o de volta** (autor 10-27): cada nível é um caracol de uma volta — 4 lances de 3 degraus, um bloco abaixo por degrau, três pistas de largura e três de altura —, um salão de 9 x 9 x 3 ao pé dele (autor 10-28: era 17 x 17, ~4.875 blocos por nível; 9 x 9 dá ~1.755 e mantém o traçado — 7 cavaria o chão dos degraus), e quatro ramais, um em cada canto do salão: outro caracol, virado para fora, que termina num salão do mesmo tamanho no quadrante diagonal daquele canto; o nível seguinte desce do meio do salão do ramal zero, e o ciclo se repete; nenhuma célula planejada é o chão de um degrau ou de um salão (o caracol fica fora das colunas do salão, e cada ramal fora das do salão e dos outros), e o passo entre caracol, salão e ramal é de no máximo um bloco; a rota do mineiro passa pelo fim de cada lance, pela borda do salão e pelo canto, em trechos curtos; o ramal novo segue indo primeiro para o minério que a vila pede (ADR-039 D3), e o veio que ele segue nunca sobe acima da altura em que começou — de lado e para baixo segue, com o degrau de volta (item 7, 0.3.97); a rede de corredores (ADR-042) foi removida do código (0.3.97; o save antigo ignora as chaves dela) | 10-27 | ✅ feita (0.3.96) | `MineShaft`, `MineShaft.waypoints`, `MineEntrance.route`, `MineShaftLayoutTest` |
| 120 | **A tarefa do lenhador fecha concluída quando o alcance esvazia** (autor 10-27, jogo da 0.3.93: nenhuma tarefa de 256 toras chegava ao fim; todas saíam como "made no progress" e o relatório dava 0% de conclusão): sem árvore madura no alcance depois de ter cortado alguma, a tarefa termina CONCLUÍDA com o que rendeu, e o ciclo seguinte abre outra se a vila ainda pedir madeira; sem ter cortado nada, ele segue esperando o bosque | 10-27 | ✅ feita (0.3.96) | `TreeChoice.closeEmptied` |
| 121 | **A roça conhecida responde primeiro, sempre** (autor 10-27, jogo da 0.3.93: 622 "still sweeping" com roça madura conhecida esperando, e a planície parou em 16 aldeões com 41 camas): o registro de canteiros da colônia é olhado inteiro a cada passagem — o custo é o número de canteiros, não o tamanho da vila —, e o canteiro arado pelo fazendeiro entra nele na hora; lavoura madura do registro vira trabalho mesmo com a varredura do raio no meio da volta; com só canteiro vazio no registro, a varredura anda mais um trecho atrás de lavoura madura e o canteiro fica de reserva; a varredura do raio serve para achar roça nova | 10-27 | ✅ feita (0.3.96) | `CropPatch.survey`, `KnownFarmland.register`, `FarmerTill` |
| 122 | **O curral é do pastor** (autor 10-27, jogo da 0.3.93: 217 entradas em curral na planície e o mineiro da taiga 3,5 h cercado com a cama dentro): o chão de dentro dos currais que o pastor mede é caro no caminho de todo aldeão de ofício que não é pastor (o nó vale como perigo leve, 8 por bloco) — atravessar fica possível quando é o único jeito, mas deixa de ser atalho; o aldeão achado cercado sem portão junto da própria cama larga essa cama, e o jogo escolhe outra; o portão que um aldeão abre fecha sozinho quando ninguém está nele (como já era) | 10-27 | ✅ feita (0.3.96) | `PenFloors`, `LandPathNodeMakerMixin`, `PenLift.leaveBedInPen` |
| 123 | **A rede de caminhos da vila** (autor 10-27, estudo `docs/research/2026-10-27-rede-de-caminhos.md`): cada construção pronta se liga pela porta ao ponto mais barato da rede, e a primeira ao centro da vila; o traçado tem 3 de largura onde cabe, degrau de no máximo um bloco entre passos e **rente ao chão** (emenda do autor de 10-28, jogo da 0.3.98: o aterro de até 4 por coluna levantou paredes de terra e o traçado de 9 portas num tique parou o servidor 12 s): o caminho troca só o bloco de cima, cobre só o buraco de um bloco entre vizinhos no mesmo nível ou acima, corta no máximo um, nunca põe bloco acima do chão natural e por cima só tira planta; cada coluna de lado segue o próprio chão, até um acima ou abaixo do eixo, e onde não dá o caminho estreita; desnível maior se contorna, ou a porta fica sem ligação e é tentada de novo uma hora depois; um traçado por passagem, com o chão de cada coluna lido uma vez; piso de caminho de terra (de areia na areia) e 2 de altura livre; construção, peça da vila, bloco do jogador e água nunca entram no traçado; caminho existente é reaproveitado, e correr colado ao lado de outro é evitado — os caminhos se unem numa rede só; substitui o corredor de 1 bloco da Regra 70 | 10-27 | ✅ feita (0.3.96; atalho ao chafariz quando a rede passa de 2× o direto, D5, 0.3.97; rente ao chão, 0.3.99) | `VillagePathPlanner`, `WorldPathTerrain`, `PathLinks`, `PathPaving`, `NetworkDetour` |
| 124 | **Construção nova nasce a no mínimo 3 blocos de qualquer construção** (autor 10-27, D1): o lote cuja pegada fica a menos de 3 blocos da caixa de outra construção, ou da obra aberta, é recusado (`TOO_CLOSE`); o caminho pode encostar no lote | 10-27 | ✅ feita (0.3.96) | `LotSpacing`, `LotLevel.flatGroundAt` |
| 125 | **Quem assenta o caminho** (autor 10-27, D6): o pedreiro, o carpinteiro e o fundidor sem tarefa do ofício, no expediente e soltos, um por vez por colônia, revezando nessa ordem a cada 8 passos; cada passo é a seção de 3 colunas — aterro, corte do que está em cima do piso, caminho de terra e 2 livres —, e se o mundo mudou e a seção não serve mais, o traçado sai e é refeito | 10-27 | ✅ feita (0.3.96) | `PathPaving` |
| 126 | **Peça colorida sai branca** (autor 10-27, item 5; jogo da 0.3.93: 64 "cama sem lã" e lã preta parada por corante): cama, lã, tapete e estandarte de qualquer cor entram na planta brancos, onde ela é montada — a lã que o rebanho dá é branca e nenhum corante é pedido; as outras peças não mudam | 10-27 | ✅ feita (0.3.96) | `WhitePieces`, `StructureBlueprintReader` |
| 127 | **Ajudas de quem está sem tarefa do ofício, cada uma com um ofício fixo** (autor 10-27, item 8): o pastor colhe e replanta a lavoura madura das roças conhecidas, sem arar terra nova, e guarda a colheita no baú da colônia mais perto (um pastor por colônia por vez); o construtor limpa a faixa de obra (já fazia, `BuilderPaving`); o lenhador sem tarefa vai ao bosque e repõe muda e põe farinha de osso na hora, no máximo a cada meio minuto; o carpinteiro sem tarefa põe a **tocha da colônia** ao lado do caminho onde a luz de bloco é zero, a 8 blocos ou mais de outra — ela surge sem custo, tem luz 15 e um anel de 8 luzes invisíveis a 10 blocos (área clara ~3×), chama e partícula laranja (a luz do jogo não tem cor), e monstro não nasce sozinho a 30 blocos dela (criador de monstro e invasão continuam); quebrada, o anel sai junto; e o pedreiro sem tarefa recolhe com a pá a neve natural da vila quando a obra aberta pede camada ou bloco de neve — uma bola por camada, quatro por bloco, para o baú (item 5) | 10-27 | ✅ feita (0.3.97) | `IdleHelps`, `KnownFarmland`, `ColonyTorchBlock`, `ColonyTorches`, `HostileSpawnMixin`, `SnowShovel` |
| 128 | **Nenhuma profissão espera mais de 5 minutos por nada** (autor 10-27, jogo da 0.3.97: pedreiro 68% e construtor 59% ociosos). 1º degrau: 300 s esperando, bloqueado ou encalhado desde o último golpe de trabalho (caminhar não conta) e ele larga a tarefa, a capacidade descansa e ele entra na cadeia de ajudas. 2º degrau: a mesma tarefa largada de novo 10 min depois faz o produto surgir no baú dele (`WAITED_TOO_LONG`); a peça de obra já surge em 2 ciclos pela obra | 10-27 | ✅ feita | `IdleWatch` |
| 129 | **O nome sobre a cabeça tem a cor do estado** (autor 10-27): laranja no ofício, azul em ajuda, vermelho bloqueado ou encalhado há 30 s, branco à toa no expediente, cinza fora dele | 10-27 | ✅ feita | `WorkTime.Badge`, `ProfessionOverlayRenderer` |
| 130 | **Cinco animais no mesmo poste pedem curral, com prioridade** (autor 10-27): o curral do estilo entra pela fila urgente; pronto, o pastor leva os do poste um a um | 10-27 | ✅ feita | `PenPlans.crowdedPost`, `UrgentPlans` |
| 131 | **O chão em volta da obra nova fica na altura da rua** (autor 10-27): na abertura, um anel de 2 blocos na altura da rua ±1 e uma rampa de 3 que abre a faixa em 1 por passo; só chão natural é cortado e só o vazio é aterrado; nunca construção, peça da vila, bloco do jogador, caminho, água, árvore ou a caixa de outra construção | 10-27 | ✅ feita | `SiteSurroundings` |
| 132 | **As bancadas do mod têm painel e passivas** (autor 10-27): shift+clique mostra titulares, estado, tarefa e espera; só com a bancada do ofício na vila valem as passivas — carpinteiro (ferramenta fabricada sai encantada, conserta ferramenta a 8), fundidor (armadura encantada, restaura armadura a 8, fornalhas 2× a 32), lenhador (mudas crescem a 32), fazendeiro (roça a 24, compostagem), pastor (par cruza a cada 10 min, lã rápida, recolha dos drops, abate em dobro), pedreiro (tapa buraco de rua, +1 pedregulho a cada 4), construtor (1 bloco a mais por minuto, material vem ao baú), mineiro (mina sem monstro a 64, minério em dobro, gema +1, 1% de ferramenta) | 10-27 | ✅ feita | `BenchPanel`, `BenchPassives`, `BenchUpkeep`, `BenchYield`, `CraftedEnchant` |
| 133 | **Estoque cheio vira obra e produto** (autor 10-28, jogo da 0.3.99: lenhador 74%, pedreiro 48% e fundidor 44% parados com 413 toras, 1.202 pedregulhos e 695 pedras guardados): com 80% dos espaços dos baús da vila ocupados (até cair abaixo de 70%), a próxima obra é o armazém e o adulto que sobra vira mais um construtor; e o lenhador, o pedreiro e o fundidor sem tarefa fazem, a cada minuto, um lote de 8 de produto de valor — carvão vegetal da tora, tijolo de pedra e pedra polida, pedra, vidro e terracota — com o cru acima de 64 e o produto abaixo de 128 | 10-28 | ✅ feita (0.3.100) | `ProfessionChestOverflow.measureFullness`, `ProfessionDemand.setSpare`, `ValueGoods` |
| 134 | **O adulto que sobra vai ao ofício que a vila espera** (autor 10-28, jogo da 0.3.99: 6 de 18 aldeões sem profissão): com todas as profissões na meta do crescimento, a vaga a mais vai à profissão que a obra espera agora, na ordem da falta, e ao construtor quando os baús estão cheios; sem espera, vale a meta do crescimento; o máximo de cada profissão continua | 10-28 | ✅ feita (0.3.100) | `ProfessionAssigner.spareHand`, `ProfessionDemand` |
| 135 | **Tarefa só abre quando pode render** (autor 10-28, jogo da 0.3.99): a ordem de estoque de peça de tábua não abre sem tábua no baú quando a reserva de metade em tora não deixa converter (69 tarefas fechadas com 0 peça); a tarefa de lã só é reservada com ovelha lanuda na área do pastor e com menos de 64 da lã pedida nos baús (28 tarefas sem ovelha com lã) | 10-28 | ✅ feita (0.3.100) | `ItemStock.woodHeldInLogs`, `ReservationGate.WOOL_ON_A_SHEEP`, `WoolGate` |
| 136 | **O fazendeiro alcança como o jogador** (autor 10-28, jogo da 0.3.99: roça 2 blocos acima, a 3 de distância, e 9 desistências): colhe e planta a até 4,5 blocos do olho até o meio da planta, com a linha livre, além do alcance de 3 do braço | 10-28 | ✅ feita (0.3.100) | `FarmerWork.seesWithinEyeReach` |
| 137 | **A escada da mina recomeça do topo** (autor 10-28, jogo da 0.3.99: o mineiro passou 30 min na boca, 30 blocos acima do nível, sem descer — a escada entre a superfície e o nível era de antes do caracol): com 2 desistências seguidas em 10 min de um mineiro a 8 blocos ou mais acima da entrada do nível, a mina recomeça na boca de superfície (a lembrada, se está no chão da superfície; senão o chão em cima da entrada, se se alcança a pé) com o cursor no primeiro degrau | 10-28 | ✅ feita (0.3.100) | `MineRestart` |
| 138 | **A rua da colônia é chão gasto e é da vila** (autor 10-28, estudo `docs/research/2026-10-28-ruas-naturais.md`): o meio é o caminho do bioma; a borda sorteia entre o caminho e o desgaste — cascalho ou terra grossa (terra grossa sobre o vazio), arenito na areia —; caminho, cascalho, terra grossa, arenito e barro numa coluna que a colônia assentou nenhuma profissão minera nem recolhe | 10-28 | ✅ feita | `StreetSurface`, `BlockProtection.mayBreak` |
| 139 | **O lote sai do mapa de interesse** (autor 10-28, jogo da 0.3.102: casa a 140 blocos, 24 acima do centro): a busca percorre a rua do centro para fora e, achado o primeiro lote, olha mais 64 colunas e fica com a menor nota — distância ao centro + 3 × a diferença de altura + o caminho até a rua; a nota e a distância média das construções saem no `VC_STATE` | 10-28 | ✅ feita | `LotScore`, `RoadIndex.findAmongRoads`, `ColonyState` |
| 140 | **A rua estica com 3 de largura, pela ponta mais perto do centro** (autor 10-28): o trecho novo tem o meio de caminho e uma borda de cada lado na mistura gasta, no chão a até 1 do meio — a borda que não cabe fica sem calçar —; só o meio da rua é ponta; as pontas vão da mais perto do centro para a mais distante | 10-28 | ✅ feita | `RoadPaving.layEdges`, `RoadPaving.isMiddleOf`, `RoadExtension` |
| 141 | **O centro da vila não muda** (autor 10-28, taiga simulada: o curral sem lote foi a 98 blocos, a caixa cresceu, o centro andou 40 blocos e 4 obras foram atrás): a primeira medida põe o centro no meio da caixa; depois a caixa só cresce e o centro fica; a busca de lote vai do centro ao canto mais longe da caixa + 15; o lote da faixa anexada fica a até 48 blocos do centro, e cada volta inteira pela faixa sem lote estica o alcance em 10 | 10-28 | ✅ feita | `Colony.measure`, `ConstructionPlanner.reachOf`, `StripLot.REACH` |
| 142 | **À noite só o preso vai à cama** (autor 10-28, taiga simulada: 114 teleportes, um por aldeão por noite, 66 de mais de 48 blocos): o caminho de casa é buscado a até 128 blocos com 4× os nós do jogo; sem caminho, o aldeão só vai à cama se ficou a menos de 3 blocos do lugar em 2 conferências seguidas (2 × 600 tiques) | 10-28 | ✅ feita | `NightHome` |
| 143 | **O baú público enche inteiro** (autor 10-28, taiga simulada: 27 vezes o lenhador largou toras no chão com a vila a 38%): o que não cabe no baú do ofício vai aos baús públicos sem o teto de 3 compartimentos por item, do mais perto ao mais longe, um enchendo antes do seguinte | 10-28 | ✅ feita | `PublicChests.store`, `ChestDepositor.deposit` |
| 144 | **Alvo de coleta inalcançável sai da memória** (autor 10-28, taiga simulada: 3 fazendeiros voltaram 79 vezes à mesma terra fora de alcance): o alvo que o coletor não alcança é esquecido pela memória de manchas e a coluna, com 2 em volta, fica fora das buscas por 1 dia de jogo | 10-28 | ✅ feita | `SurfaceHits.unreachable` |
| 145 | **As camas da vila são as da caixa** (autor 10-28, taiga simulada: a sonda de 64 blocos via parte da vila e o número oscilava 15→7→18→10): medida a vila, a cada 30 s contam-se as camas que o jogo registrou dentro da caixa, na faixa de altura da vila, só com todos os chunks dela carregados; a sonda não muda mais esse número | 10-28 | ✅ feita | `BoxBeds`, `Colony.countBeds` |
| 146 | **Fundidor e pedreiro produzem mais** (autor 10-28, taiga simulada: fundidor 80% e pedreiro 64% parados com 1.209 pedras): o teto do produto de valor sobe de 128 para 512; o fundidor funde todo minério cru (ferro, ouro, cobre) e assa toda a carne, e faz pedra, pedra lisa, vidro, tijolo, terracota e arenito liso; o pedreiro faz tijolo de pedra, degrau, laje e muro de pedregulho, tijolo entalhado, arenito cortado e as pedras polidas — cada receita na proporção dela | 10-28 | ✅ feita | `ValueGoods` |
| 147 | **O diário da vila bate com o jogo** (autor 10-28): cada fato vira `VC_EVENT` (obra aberta e fechada com as peças contadas no mundo, caixa que cresce, camas, teleporte, nível da mina, morte); ao virar o dia de jogo, `VC_DAY` com o tempo das profissões no dia inteiro e `VC_AUDIT` conferindo camas, trabalhadores vivos e peças da obra aberta que já estão no lugar; tudo no arquivo do mundo, lido pelo `state_report.py` | 10-28 | ✅ feita | `VillageDiary` |
| 148 | **A obra nunca fica largada meio pronta** (autor 10-28, taiga simulada: casa em "0 blocks left" 28 min sem tarefa e largada): sem peça a pôr, a obra fecha pelo ciclo da colônia; parada no fim (só peças adiadas por material ou apoio, ou nenhuma peça na janela de paciência numa obra começada), tudo o que falta surge no lugar e cada peça vai ao registro de faltas com o motivo END_OF_BUILD; só a obra sem nenhuma peça posta é largada | 10-28 | ✅ feita | `BuildClosing`, `WaitingWork` |
| 149 | **Rua sem falha e ajudantes da vila** (autor 10-28): falha de até 2 blocos de chão natural numa linha reta entre duas colunas de rua vira caminho; o pedreiro sem tarefa tapa até 8 falhas por minuto e, sem falha, nivela em volta de uma casa pronta; o fundidor sem tarefa leva até 64 itens do baú da boca da mina aos baús públicos e põe uma lanterna por minuto ao lado da rua escura | 10-28 | ✅ feita | `StreetGaps`, `VillageHelpers`, `SiteSurroundings.level` |

Das 75 às 109 são decisões do autor que já estavam no código sem número; foram registradas em 2026-10-22
(levantamento em `docs/research/2026-10-22-logicas-candidatas-a-regra.md`; a 108 e a 109 vieram do README).

---

### Regras provisórias

Regras que o autor declarou **temporárias** e vão sair.

| # | Enunciado | Por que existe | O que destrava a saída |
|---|---|---|---|
| 28 | ~~Barreira de teste~~ (**retirada em 10-06**, ADR-036 item 6): ~~só `plains_small_house_1`~~ (caiu em 09-09) e peça dispensada | Tornar as sessões comparáveis entre si | O planejador saber desistir de um objetivo (P1.1) |

**Enquanto a metade que resta valer:**
- A casa sobe com peças da barreira (playtest de 02-10: 16 de 69).
- `TEST BARRIER covered for N of M pieces` é a linha que mede.

---

### Regras emendadas

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
| 43 | Emenda (10-11; lotação trocada em 10-22 pela Regra 72) | **Pastor:** cercado com lugar primeiro (~~≤ 5 animais~~ um a cada 4 blocos de chão, Regra 72; cheio, outro cercado), pelo portão; sem cercado, a cerca mais perto; sem cerca, um poste novo ao lado da casa, com cerca do baú (`ShepherdPens`) |
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

### Regras revogadas

A regra de arquitetura "a colônia não cria recurso" (`Construction-System.md`,
sem número nesta tabela) foi **retirada em 2026-09-30** — ADR-028. A 25 foi
**desfeita em 2026-10-06** — ADR-036 item 5. A 52 (uma roça por fazendeiro, 10-08) foi **substituída pela 67**
em 2026-10-13. Continua dela a posse: cada fazendeiro sem roça recebe a roça
sem dono mais perto do baú dele (`FarmOwners`, `VillageFarms`).

---

### Decididas em 2026-10-02 (estudo de profissões)

Do `docs/research/2026-10-02-melhorias-profissoes-vida-natural.md`, o autor:
recusou a pausa e a chuva (18-e1); trocou a ferramenta fixa pela melhor do baú
(2-e1, 37-e1); fixou que a fome não pesa no trabalho (46) e que ninguém envelhece
(47); limitou o recolher do chão ao que a obra espera (48); deu a ordem das obras
(49); e fez do tempo dos aldeões o critério de toda verificação (50).

---

### Regras que faltam decidir

Nenhuma. A última, P0.7 (pedra como solo de lote), foi aceita pelo autor em 07-10 (ADR-039 F1):
o código já tratava todo bloco sólido como solo desde 15-09 (`LotGround.isLotGround`).

<!-- REGRAS:FIM -->

## Blocos do mod

Todos os blocos aparecem no criativo (bancadas em "Blocos funcionais", ao lado
das estações Vanilla) e têm receita para o jogador fabricar. Quebrados, devolvem
o próprio bloco.

### Bancadas de ofício

Cada bancada é a estação de trabalho de uma profissão: atrai o aldeão para o
ofício, funciona para o jogador como a estação Vanilla equivalente, ilumina
como uma tocha (luz 14) e **dobra a produção** do titular que trabalha perto
dela. Receita de todas: ouro, diamante e ouro em cima; a estação Vanilla do
ofício no meio; ouro nos dois cantos de baixo (4 lingotes de ouro e 1 diamante).

Só por estar na vila, cada bancada liga **passivas** (Regra 132). Shift+clique
em qualquer uma abre no chat o **painel** do ofício: os titulares, o estado de
cada um na cor do nome, a tarefa, há quanto tempo esperam, a obra aberta (no
construtor) e as passivas ligadas.

| Bloco | Estação na receita | Para o jogador funciona como | Passivas na vila (Regra 132) |
|---|---|---|---|
| Bancada do Carpinteiro | mesa de trabalho | mesa de trabalho | ferramenta e arma que o jogador fabrica na vila saem com 1 encantamento sorteado; repara aos poucos a ferramenta de quem está a até 8 blocos |
| Cortador do Pedreiro | cortador de pedra | cortador de pedra | tapa um buraco de rua por minuto a até 32 blocos; a cada 4 pedregulhos minerados, 1 a mais |
| Forja do Ferreiro (fundidor) | alto-forno | fornalha de verdade | fornalhas, alto-fornos e defumadores a até 32 (também os do jogador) com o dobro da velocidade; restaura armadura a até 8; armadura fabricada na vila sai encantada |
| Bancada do Lenhador | mesa de trabalho | mesa de trabalho | mudas a até 32 blocos crescem mais rápido |
| Bancada do Fazendeiro | composteira | composteira | a roça a até 24 blocos ganha um passo de crescimento; a sobra dos baús (sementes acima da reserva, folhas) vira farinha de osso |
| Bancada do Pastor | tear | tear | abaixo da meta, um par cruza sozinho a cada 10 min (a até 36); lã volta mais rápido; o que os animais derrubam vai para os baús; o abate rende o dobro de carne e couro |
| Bancada do Construtor | andaime | mesa de trabalho | a obra a até 48 blocos assenta 1 bloco a mais por minuto; o material da obra guardado em outros baús vem para o baú do construtor |
| Bancada do Mineiro | cortador de pedra | mesa de trabalho | nenhum monstro nasce na mina (até 64 blocos da entrada, abaixo dela); ferro, cobre, carvão, ouro, lápis e redstone rendem o dobro; diamante, esmeralda, ametista e quartzo rendem +1; 1% de chance de soltar uma ferramenta de ferro, ouro ou diamante |

### Outros blocos

| Bloco | Como se obtém | O que faz |
|---|---|---|
| Tocha da Colônia | 4 tochas em volta de 1 lingote de ouro → 4 tochas | Mesmo formato da tocha do jogo, com cabeça de chama laranja de 3 pixels e o centro claro, sobre o cabo marrom; vai no chão ou na parede, como a tocha comum, com luz 15; acende um anel de luz invisível em volta (área clara cerca de 3 vezes maior) e impede que monstro nasça sozinho a até 30 blocos. O carpinteiro sem tarefa a põe sozinho, sem custo, ao lado dos caminhos escuros da vila (Regra 127) |
| Caminho de Areia | pá na areia, como o caminho de terra | Igual ao caminho de terra do jogo, na cor do arenito; é o caminho das vilas do deserto (Regra 73) e volta a ser areia quando algo sólido o cobre |

## Construções e biomas

O catálogo profissional está em
`src/main/java/com/villagecolony/fabric/work/HousePlans.java` e é validado
contra os nomes reais do Minecraft 1.21.1. As famílias permitidas são:

- Planície: casas, oficinas, fazendas, currais, estábulos e lampião.
- Deserto: casas, oficinas, fazendas, currais e lampião.
- Savana: casas, oficinas, fazendas, currais e lampião.
- Taiga: casas, oficinas, fazendas, curral e lampião.
- Tundra nevada: casas, oficinas, fazendas, currais e lampião.

O armazém do mod tem uma versão por estilo de bioma, nasce sempre do solo
para cima e não leva folhas nem alçapões (Regra 115). Toda obra nova é
afastada no mínimo 3 blocos de outra construção (Regra 124) e ganha o chão
nivelado em volta (Regra 131).

O detalhe dos IDs e a regra da estrutura própria estão em
[`src/main/resources/data/villagecolony/structure/houses/README.md`](src/main/resources/data/villagecolony/structure/houses/README.md).

## Instalação

Requisitos: Minecraft Java 1.21.1, Fabric Loader compatível, Fabric API e
Java 21. Coloque o JAR e a Fabric API na pasta `mods`. O servidor precisa do
mod; clientes que entram em um servidor dedicado não precisam instalá-lo. No
cliente, o mod liga as sobreposições de profissão e de obra, e o Mod Menu
(opcional) abre a tela de regras de profissão — mudar regras exige operador
nível 2.
Use um mundo de teste: o mod corta árvores, minera e coloca blocos no mundo.

## Comandos e dados de análise

Todos os comandos são abertos a qualquer jogador e respondem sobre a vila em que você está.

| Comando | O que mostra ou faz |
|---|---|
| `/vc log` | Situação da vila: prioridade das obras, felicidade, **estado agora** (quem está no ofício, ajudando, travado — com os travados nomeados —, estoque e baús cheios, obra aberta, tarefas, mina, ruas, bancadas e animais), atividades recentes, o que a obra espera e a cadeia que a traz, alcance a pé, tempo dos ofícios na última janela de 5 min (Regra 51) e esperas longas |
| `/vc bioma` | Boletim do bioma (estilo, camas, aldeões, lotes e recusas, peças que surgiram, ofícios acima da Regra 51) e o mesmo retrato completo do `/vc log` |
| `/vc bioma nota <texto>` | Anota uma observação sua, com posição e bioma, no log (`VC_BIOME_NOTE`) e no arquivo de análise do mundo |
| `/vc dados` | Grava agora o retrato completo da vila no arquivo de análise e diz onde ficou |

**O retrato da vila (`VC_STATE`).** A cada 5 minutos, por vila com jogador perto, o mod registra num
JSON só:

- dia e hora do jogo, MSPT do servidor, bioma, estilo e tamanho da vila;
- aldeões, ofícios, camas, encalhados, e quantos estão no ofício, ajudando, travados, à toa ou fora do
  expediente (Regra 129), com os travados nomeados;
- tempo de cada ofício na última janela e a parte da Regra 51;
- estoque por grupo, número de baús, quanto estão cheios e se pedem armazém;
- obras prontas, obra aberta (estado, peças que faltam e de lado), lotes aceitos e recusas;
- tarefas abertas por tipo e quantas têm dono;
- mina (altura do nível, níveis abaixo da boca, posições cavadas);
- colunas de rua, bancadas presentes e animais por espécie.

O registro sai no log como `VC_STATE` e fica, junto das notas, em
`<mundo>/villagecolony/analytics/AAAA-MM-DD.jsonl` — uma linha por registro, com o instante real
(`at`) para cruzar com o Spark. O arquivo fica com o mundo entre sessões e versões, e cada registro
traz `schema` e `mod`, para comparar versões.

Relatórios (rodar depois de jogar):

```bash
python scripts/state_report.py
```

```bash
python scripts/time_ledger.py
```

```bash
python scripts/action_report.py
```

O `state_report.py` lê os arquivos de análise de todos os mundos da instalação de teste (ou os arquivos e
logs passados) e mostra, por colônia:

- o período e a versão do mod;
- a evolução de aldeões, ofícios e obras por hora;
- o enchimento dos baús e os travados;
- a média da Regra 51 por ofício;
- o MSPT;
- as notas do jogador.

Os outros relatórios:

| Script | O que mostra |
|---|---|
| `time_ledger.py` | O tempo por ofício (`VC_TIME`) |
| `action_report.py` | O diário de ações por aldeão, com os motivos de recusa |
| `cost_ledger.py` | O custo por fase do ciclo (`VC_COST`) |
| `supply_report.py` | O que surgiu sem um ofício obter (`VC_SUPPLY`) |
| `biome_report.py` | O boletim por bioma (`VC_BIOME`) |

## Desenvolvimento e verificação

### Skills do projeto

Os perfis de trabalho e as skills utilizadas neste repositório estão em
`AGENTS.md`. A matriz operacional é:

- `fabric-development` e `minecraft-modding` para código Fabric e integração
  com Minecraft 1.21.1.
- `minecraft-villager-systems` e `minecraft-code-research` para profissão,
  rotina, caminho, estoque e diagnóstico de aldeões.
- `minecraft-testing` para testes unitários e GameTests.
- `minecraft-ci-release` para CI, versionamento, artefatos e publicação.
- Auditoria (2026-10-10): `modpack-modsmith` (convivência com o pack e a escada
  de testes do `probar.py`), `java-architecture-review`, `java-static-analysis`,
  `minecraft-performance-profiling`, `fabric-persistence-migration` e
  `git-workflow-audit`. Origem e uso em
  [`docs/technical/avaliacao/agent-setup.md`](docs/technical/avaliacao/agent-setup.md).

Os perfis Codex são `village-quick` (consulta), `village-audit` (diagnóstico),
`village-dev` (implementação comum) e `village-max` (mudança entre subsistemas).

```powershell
./gradlew.bat test
./gradlew.bat build
./gradlew.bat runGametest
./gradlew.bat pitest
./gradlew.bat javadoc
python -m unittest discover -s tests
```

O build exige Java 21 (`JAVA_HOME` apontando para um JDK 21).

Cobertura: `test` gera `build/reports/jacoco/test/html` (unitários) e
`runGametest` gera `build/reports/jacoco/gametest/html` (bateria de jogo, que
é quem exercita a camada `fabric`).

Verificação de 2026-10-28 (0.3.102, branch `fix/playtest-0397`, PR #37),
rodada e não copiada:

- 1609 testes unitários: aprovados (`test`, com ArchUnit, Error Prone e o teto
  de 500 linhas por arquivo).
- 126 testes Python: aprovados (o CI instala o `nbtlib` para o teste das
  versões do armazém).
- 900 GameTests: todos aprovados com `--rerun-tasks`; o `runGametest` começa
  cada bateria com o mundo de teste apagado, como o CI.
- `build --rerun-tasks`: aprovado; JAR `village-colony-0.3.102.jar`.
- Mutação (PIT, pacote `core`): não refeita nesta versão; a última medida
  (0.3.93) foi 2140 de 2509 mutações mortas (85,3%, limiar 85%).
- CI do GitHub ("Build and test"): a `main` exige esse check para mesclar.

Para refazer: `scripts/audit/check-tests-green.sh --runs 3` e
`scripts/audit/check-mutation-threshold.sh --run` (ver
[`scripts/audit/README.md`](scripts/audit/README.md)).

## Estado e nota da auditoria

Auditoria técnica de 2026-10-10: nota global **6,9/10** (12 dimensões); pela
régua reprodutível da [`METODOLOGIA`](docs/technical/avaliacao/METODOLOGIA.md),
conceito **B**. Resumo, riscos e ganhos rápidos em
[`EXECUTIVE-SUMMARY.md`](docs/technical/avaliacao/EXECUTIVE-SUMMARY.md); fila
priorizada em [`ACTION-QUEUE.md`](docs/technical/avaliacao/ACTION-QUEUE.md) e no
[`TODO.md`](TODO.md).

Os maiores riscos medidos: a Regra 51 reprovada em jogo (P0), um save com uma
entrada malformada que faz o mundo abrir sem colônias (P1) e o ciclo da colônia
sem versão gravada (P1). A regressão de alternância de obras citada pela
auditoria de 2026-09-21 não se reproduz mais na bateria: o teste dela passa,
mas falta a prova em jogo. O retrato de 09-21 está em
[`docs/archive/technical/Project-Audit-2026-09-21.md`](docs/archive/technical/Project-Audit-2026-09-21.md),
e o de 10-10 em
[`docs/archive/technical/Project-Audit-2026-10-10.md`](docs/archive/technical/Project-Audit-2026-10-10.md).

## Produção futura

O que já está decidido ou estudado e ainda não foi feito — o detalhe e a ordem
ficam no [`TODO.md`](TODO.md), seção "Produção futura":

- **Configuração por mundo e por colônia** (R-153/154 do estudo v2): desenho
  pronto em
  [`docs/research/2026-10-22-desenho-configuracao.md`](docs/research/2026-10-22-desenho-configuracao.md)
  — arquivo JSON na pasta do mundo, comando `/vc config`, 14 ajustes com faixa
  segura. Decisões do autor tomadas; falta implementar.
- **O resto do estudo "Regras v2"**, avaliado ponto a ponto em
  [`docs/research/2026-10-22-avaliacao-regras-v2.md`](docs/research/2026-10-22-avaliacao-regras-v2.md):
  contrato explícito de tarefa (R-047), estados de disponibilidade de recurso
  (R-019) com invalidação por evento (R-021/R-027), bateria de escassez (R-158),
  e os sistemas grandes ainda sem decisão — área protegida pelo jogador (R-035),
  transporte físico de itens (R-140 a R-142) e itens de outros mods (R-026).
- **Pendências de jogo**: o percurso dos mineiros até a mina do lago no deserto
  e as peças de ardósia que o armazém pede no deserto.

## Documentos de entrada

- [`CLAUDE.md`](CLAUDE.md): regras para trabalhar no repositório.
- [`STATE.md`](STATE.md): estado vivo e pendências de playtest.
- [`TODO.md`](TODO.md): backlog canônico, com a produção futura.
- [`docs/RULES.md`](docs/RULES.md): todas as regras do autor (fonte da tabela
  acima).
- [`CHANGELOG.md`](CHANGELOG.md): o que mudou em cada publicação.
- [`docs/decisions/`](docs/decisions/): decisões arquiteturais.
- [`docs/behavioral-tests/`](docs/behavioral-tests/): estratégia e falhas de
  GameTest.
- [`docs/technical/avaliacao/`](docs/technical/avaliacao/): auditoria técnica
  atual e avaliações pela régua.

Licença MIT. Feito para Minecraft 1.21.1 com Fabric.
