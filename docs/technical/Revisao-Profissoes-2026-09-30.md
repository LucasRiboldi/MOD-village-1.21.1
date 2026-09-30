# Revisão das profissões — 2026-09-30

Varredura das funções, trabalhos e atividades das oito profissões, seguida das
decisões do autor e da implementação. Código em `ef84143`. As decisões que
mudam regra estão na ADR-028 (regra "a colônia não cria recurso" retirada) e
na ADR-029 (ofício da colônia exclui ofício Vanilla).

**Estado:** 1.196 testes unitários e 528 GameTests passando localmente, 7
deles novos. **Nada foi visto em jogo.**

## 1. O que a varredura achou, e o que o autor decidiu

| # | Achado | Decisão do autor | Feito |
|---|---|---|---|
| A1 | A regra "a colônia não cria recurso" era desmentida em quatro pontos, e a lava entrava na obra sem custo | Retirar a regra; a lava não surge automática | ADR-028; `BlockShaping.isNeverPlaced` |
| A2 | Piso de 64 carvões sem consumo como combustível | — (coberto por A1: a fundição segue sem combustível) | registrado na ADR-028 |
| A3 | A casa-base não alojava carpinteiro nem fazendeiro | Carpinteiro na BigHouseMOD, cama e baú à direita da porta, caminho livre até a escada | `FOUNDATION_ORDER`; `big_house_mod.nbt` |
| A5 | Floresta com dois donos (lenhador replanta, fazendeiro planta viveiro) | O viveiro passa ao lenhador | `LumberjackNursery` |
| A6 | Fazendeiro cavava terra de enxada; ferramenta "de mentira" na argila | Cada aldeão usa a ferramenta de ferro apropriada | `ActionTool` |
| A7 | "Mais necessária" era contagem de cabeças | Primeiro a profissão de que a demanda depende, depois a lista | `ProfessionDemand` |
| B1 | Ninguém fazia pão | Comida é feita pelo fazendeiro | `FarmerBakery` |
| B2 | Suspeita: só quem trabalha comia | Corrigir | **Não era defeito**, ver §3 |
| B3 | Piso de fundido sem cadeia (terracota sem argila): raiz do F2 | Corrigir | `StockRules.rawOf`, `ColonyGoals` |
| B4 | Carpinteiro é o artesão de tudo | Pode continuar, com correção para fabricar mais itens | `DropIngredients`, `ColonySupply` |
| B5 | Mineiro guardava pedra que nada consome | Parar de recolher aos 256 de cada tipo | `MinerHaul.TYPE_CAP` |
| B6 | Pastor não cuidava do rebanho | Fazer procriar e o que for preciso para isso | `ShepherdFlock` |
| C | Corante, linha, pó de osso e drops sem produtor; ofício Vanilla em paralelo | Os drops aparecem automáticos; ofício do mod exclui o Vanilla | `DropIngredients`; ADR-029 |
| D | Comentários e documentos desatualizados | Atualizar | §4 |

## 2. O que mudou, profissão por profissão

- **Construtor:** a posição de lava da planta fica vazia (`Project … leaves …
  empty — lava is never placed by the colony`).
- **Carpinteiro:** titular da BigHouseMOD, o 3º da ordem de fundação, logo
  depois do lenhador. Na planta, o baú fica em (2,0,6) e a cama em (2,0,7) e
  (2,0,8), junto à parede oeste e à direita de quem entra. O caminho
  porta → (3,0,5) → (3,0,7) → (4,0,7) → escada foi conferido livre antes de
  salvar. Quando a receita pede ingrediente de drop, a quantidade que falta
  aparece no baú na hora (`The colony received … — drop ingredients appear on
  their own`).
- **Lenhador:** planta o viveiro. Um lote quando não acha árvore, como antes,
  e uma muda na borda depois de cada árvore derrubada inteira, no ritmo de
  6.000 tiques e até 10 árvores.
- **Fazendeiro:** não planta mais árvore. Depois de cada colheita guardada,
  assa o trigo que passa de 32: três trigos viram um pão (`Farmer at … baked
  N bread`). A terra de obra sai de pá.
- **Pastor:** a cada ciclo da colônia, se o rebanho adulto em 32 blocos tem
  menos de 12 ovelhas e há par pronto, dá um trigo da colônia a cada uma das
  duas e o Vanilla faz o resto. Um par a cada 6.000 tiques (`the shepherd fed
  two sheep to breed`).
- **Mineiro:** passado 256 de um tipo no próprio baú, deixa de guardar aquele
  tipo. O excedente não vira item no chão. O item que a tarefa pediu não tem
  teto.
- **Fundidor:** o piso de 16 de cada fundido só entra quando o cru tem meta
  própria ou está no baú. O que a obra pede entra sempre.
- **Todos:** o tempo de quebra (e, na coleta de superfície, o que o bloco
  solta) é calculado com a ferramenta de ferro que quebra aquele bloco mais
  depressa. A da mão fica quando é tão boa ou melhor, como a pá de Toque Suave
  e a picareta de diamante do `ToolUpgrade`.
- **Contratação:** com vaga aberta na colônia, ela vai primeiro à profissão
  com a maior falta de material de obra, até uma cabeça acima da cota; depois
  vale a ordem fixa. A demanda não abre vaga que a população não tem.
- **Ofício Vanilla:** trabalhador com profissão do mod não recebe ofício do
  jogo (mixin em `setVillagerData`). Quem já tinha perde o ofício e solta a
  estação na varredura seguinte (`Villager … leaves the vanilla trade …`).

## 3. O que se corrigiu da própria varredura

**B2 era falso.** A varredura dizia que a refeição só alimentava quem tem
ofício. O `VillagerScanner` registra em `WORKERS` todo aldeão da vila, com ou
sem função, e é dessa lista que a `VillageMeals` sai. Trocar a lista por "todo
aldeão na caixa de 128 blocos" alimentaria aldeões de outras colônias. A
mudança foi desfeita, e um comentário registra a conferência.

**O tear não tem rota em todo bioma.** O primeiro teste afirmava que o tear
tinha rota por causa da linha automática. Ele falhou no cenário: a rota do
tear também depende da tábua do bioma. O que a regra nova garante é a rota da
linha, e é isso que o teste afirma agora.

## 4. Documentação

| Arquivo | Atualizado |
|---|---|
| `docs/decisions/ADR-028-regra-nao-cria-recurso-retirada.md` | novo |
| `docs/decisions/ADR-029-oficio-da-colonia-exclui-oficio-vanilla.md` | novo |
| `docs/decisions/ADR-004-Mixin-Policy.md` | Mixin 3 na superfície permitida |
| `docs/decisions/ADR-016-…` | cláusula retirada anotada |
| `Construction-System.md`, `Storage-System.md` | regra retirada |
| `docs/technical/Profession-Responsibility.md` | matriz, fundação, pendência da profissão Vanilla fechada |
| `docs/RULES.md` | Regra 4 corrigida (a constante não existe), Regras 34–44 |
| `README.md`, `CLAUDE.md` §0.2, `STATE.md`, `TODO.md` | estado de hoje |
| Comentários | `ProfessionRegistry` e `ToolUpgrade` (ferro, e não madeira); `ProfessionType` (construtor titular, ofícios novos) |

## 5. Testes

| Suíte | Resultado |
|---|---|
| Unitários (`gradlew test`) | 1.196, todos passando. Novos: `ProfessionDemandTest` (5 casos), `ColonyGoalsTest.theFurnaceFloorNeedsARawMaterialChain`. Ajustados à nova fundação: 4 em `HiringLogTest`, `ProfessionAssignerTest` e `ProfessionGrowthTest`; 2 em `ColonyGoalsTest` (pedra lisa no lugar do arenito liso) |
| GameTests (`gradlew runGametest`) | 528/528. Novos: `ProfessionReviewGameTest` (6) e `MinerHaulCapGameTest` (1); a execução dos 7 batches foi conferida no log. Ajustados: BigHouseMOD com 7 camas e baús; linha do tear e haste de blaze com a regra nova |
| Em jogo | **não verificado** |

## 6. Pendências

- 🔴 Playtest: as frases da §2 no log e uma BigHouseMOD **nova** com 7 camas.
  A que já existe no save foi erguida com 6; o carpinteiro de lá ganha cama
  fora dela.
- 🟠 Custo no Spark: `ChestWithdrawer.countIn` por drop do mineiro e a
  varredura de ofício Vanilla por ciclo.
- 🟡 O Vanilla volta a reivindicar a estação a cada ciclo e a varredura a
  solta; se custar, filtrar o sensor de POI (ADR-029).
- 🟡 Combustível da fornalha e custo do viveiro continuam fora da economia
  (ADR-028).
