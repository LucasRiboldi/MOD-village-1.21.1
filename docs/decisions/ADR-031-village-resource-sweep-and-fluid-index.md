# ADR-031 — Varredura de recursos pela caixa da vila

**Estado:** Aceita pelo autor em 2026-10-03
**Escopo:** coleta de recursos de superfície e índice transitório de fluidos.

## Contexto

A busca de terra, grama e areia partia de um círculo deslocado do centro. Ela
gastava leituras em colunas já descartadas pelo setor protegido e não usava a
caixa real da vila como ponto de partida. Água e lava conhecidas voltavam a ser
lidas em cada nova busca, embora não fossem alvos válidos para essa coleta.

O autor decidiu que a busca deve começar na borda atual, avançar em espiral
até o centro e, se nada encontrar, continuar por fora sem reler colunas já
examinadas. Ao medir a vila, água e lava devem ser lembradas para buscas
seguintes; uma expansão exige nova medição completa.

## Decisão

1. A mudança vale somente para `SurfaceGatheringWork`; agricultura e outros
   scanners conservam seus próprios contratos.
2. `VillageSpiralSweep` percorre perímetros retangulares: borda da caixa para
   dentro e, depois, anéis externos crescentes. O cursor por trabalhador guarda
   a próxima coluna exata e respeita o teto de 1.024 leituras de mundo.
3. Filtros puramente geométricos, inclusive o setor externo protegido, são
   aplicados antes de consumir esse orçamento. As regras de proteção e alcance
   permanecem inalteradas.
4. `VillageFluidIndex` existe somente na memória do servidor. Ele classifica,
   em parcelas de até 1.024 colunas, água ou lava entre `minY` e `maxY` da
   caixa e consulta somente chunks já carregados.
5. Uma coluna indexada como fluido é ignorada nas buscas seguintes. Ausência
   no índice nunca prova que há recurso: a leitura final do mundo continua
   sendo a fonte da verdade.
6. A caixa exata faz parte da identidade do índice. Crescimento confirmado por
   `VillageColonyMod.reportGrowth` invalida a medida; a próxima busca começa
   uma varredura completa da nova caixa.
7. Cursores e índice não são persistidos. `ServerMemory` os limpa ao parar o
   servidor, evitando estado derivado no save.

## Consequências

- A busca cobre primeiro a área conhecida e pode achar recursos fora dela sem
  revisitar colunas dentro da mesma passagem.
- Vilas grandes distribuem a medição e as leituras de recurso entre ciclos.
- Chunk ausente não é carregado para alimentar o índice e não vira falso solo.
- Uma coluna lembrada como fluido só é reclassificada após invalidação por
  expansão ou reinício; isso cumpre o pedido de não relê-la a cada busca.
- O índice apenas evita consultas; ele nunca autoriza quebra, colocação ou
  criação de itens.

## Verificação

`VillageSpiralSweepTest` cobre ordem, retomada sem duplicação, expansão para
fora e reinício com nova caixa. `VillageFluidIndexTest` cobre orçamento,
chunks carregados, água/lava e invalidação. `SurfaceGatheringGameTest` integra
a busca externa e prova que uma coluna de fluido já medida é ignorada. A
confirmação do comportamento e do custo no save real continua pendente.
