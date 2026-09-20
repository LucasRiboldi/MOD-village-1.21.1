# ADR-019 — catálogo Vanilla explícito e coleta segura fora da vila

## Status

Aceita em 2026-09-20.

## Contexto

O planejador do mod consultava todos os arquivos Vanilla encontrados em
`village/<bioma>/houses/`. Isso permitia que uma obra fora da lista aprovada
fosse escolhida e também confundia nomes conceituais com os ids reais do
Minecraft 1.21.1. O fundidor precisava atender areia e `grass_block`, mas não
deveria raspar o terreno ocupado pela vila. A boca da mina também podia ser
aceita na margem de água ou em uma posição que aparecia antes de outra mais
distante e acessível.

## Decisão

1. `VillageStructures` mantém uma whitelist explícita por estilo para casas,
   oficinas, agricultura, pecuária e acessórios autorizados pelo mod. A lista
   usa os caminhos presentes no catálogo Vanilla verificado. Nomes como
   `desert_cartographer_house_1`, `savanna_fisher_cottage_1`,
   `taiga_armorer_house_1` e `snowy_farm_1` são usados quando diferem do nome
   abreviado inicialmente proposto. Postes de luz permanecem elegíveis mesmo
   estando na raiz do estilo, fora de `houses/`.
2. A Vanilla continua disponível e intacta. A whitelist restringe somente as
   escolhas de construção do mod; ruas e estruturas Vanilla não são removidas.
3. Areia, terra e relva são coletadas pelo `SMELTER` somente quando uma tarefa
   aberta de construção solicita o recurso. A busca começa no setor mais
   distante da vila e rejeita qualquer coluna fora desse setor protegido.
4. Uma boca de mina exige chão sólido, dois blocos livres de entrada, ausência
   de fluido na posição e em um raio horizontal de quatro blocos, além das
   proteções de estruturas existentes. Entre as candidatas elegíveis, a busca
   escolhe a mais distante do centro; em empate, prefere a mais alta.

## Consequências

- Novos arquivos Vanilla não entram automaticamente nas construções do mod;
  precisam ser adicionados à whitelist e ao teste de contrato correspondente.
- A coleta de superfície deixa o terreno da vila intacto, mas pode esperar
  mais quando o setor externo carregado não contém o recurso.
- A boca pode ficar mais longe que a primeira posição encontrada, favorecendo
  uma entrada seca, acessível e orientada para terreno elevado.

## Validação

`VillageStructuresGameTest` compara os ids autorizados de todos os cinco
estilos. `SurfaceGatheringGameTest` prova que areia próxima permanece intacta
e que a areia externa é entregue ao baú do fundidor. `MinerGameTest` prova a
rejeição da margem de água e a preferência pelo terreno elevado. A suíte
completa de GameTests deve permanecer verde.
