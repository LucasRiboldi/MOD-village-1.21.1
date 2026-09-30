# ADR-028 - Retirada da regra "a colônia não cria recurso"

**Status:** Accepted
**Date:** 2026-09-30
**Decision Type:** Gameplay / Economy
**Supersedes:** a primeira regra de arquitetura do `Construction-System.md`
("Construção nunca cria recursos") e o trecho correspondente de
`Storage-System.md` e da ADR-016 §31.

## Contexto

A varredura das profissões de 2026-09-30 mostrou que a regra já não
descrevia o código. Quatro caminhos punham material no mundo ou no baú
sem que uma profissão o obtivesse:

| Caminho | Onde |
|---|---|
| Bloco sem item (água, caldeirão, canteiro, caminho, cultivo) montado no local | `BlockShaping.isShapedFromTheGround` |
| Peça sem rota, e depois o ingrediente sem rota, aparecendo no baú após três tentativas | `BiomeConstructionSupply` (F5, F6) |
| Terra enraizada e muda do viveiro nascendo sem sair de baú | `TreeNursery.plant` |
| Fundição sem combustível | `SmelterWork` ("sem forno, e é decisão") |

Cada exceção tinha sido decidida em separado, e o documento continuava
dizendo o contrário. A **lava** entrava pelo primeiro caminho, montada sem
custo dentro de casa de madeira.

## Decisão

O autor retirou a regra:

> A regra: A colônia não cria recurso, deve ser retirada. Lava não pode surgir
> automático.

1. **A regra deixa de existir.** Os quatro caminhos acima são comportamento
   previsto, não exceção.
2. **A lava nunca é assentada** (`BlockShaping.isNeverPlaced`). O construtor
   pula a posição, que fica vazia, e a obra segue. A obra não espera por ela.
3. **Ingredientes de drop aparecem sozinhos** (`DropIngredients`): corantes,
   linha, pó de osso, osso, couro, pena, pólvora, haste de blaze e os demais
   itens que caem de inimigos e animais. Quando a receita do artesão pede um
   deles e o baú não tem, a quantidade que falta aparece no baú na hora
   (`ColonySupply.makeWhatIsMissing`), sem as três tentativas da peça sem
   rota. Para a pergunta "tem rota no bioma?", eles sempre têm.
4. **O que tem cadeia dentro da colônia continua usando a cadeia.** O vaso com
   cacto sai de vaso e cacto do baú; o tijolo, da argila assada. Retirar a regra
   não autoriza pular uma cadeia que existe.
5. **O natural continua vindo das profissões.** A regra de 2026-09-26
   (`BiomeConstructionSupply.isNatural`) segue valendo: tora, pedra, terra,
   areia, lã e trigo nunca aparecem no baú.

## Consequências

- O tear, a cama colorida e a vidraça colorida (F8) deixam de esperar por um
  ingrediente que nenhum aldeão traria.
- O combustível da fornalha e o custo do viveiro continuam fora da economia.
  Se o autor quiser que custem, é uma decisão nova.
- `ProfessionReviewGameTest` guarda a lava e a linha do tear;
  `BuilderGameTest.aBrewingStandIsMadeWithAnAutomaticBlazeRod` guarda a haste
  de blaze.
