# ADR-032 — Slots reservados nos baús profissionais

**Estado:** Substituída em parte pela ADR-033 em 2026-10-04
**Escopo:** transferência de excedente dos baús associados a trabalhadores.

## Contexto

Um trabalhador perde o destino de sua produção quando todos os slots de seu
baú estão ocupados. A vila pode ainda ter espaço comunitário, mas os depósitos
de produção priorizam corretamente o baú da profissão e não devem passar a
misturar diretamente os baús privados de trabalhadores diferentes.

O autor decidiu que, ao lotar um baú profissional, seus dez slots finais devem
ser esvaziados para qualquer baú da vila que tenha espaço e não pertença a uma
profissão.

## Decisão

1. A regra roda no ciclo da colônia, depois de descobrir os baús da vila e
   antes de produzir o retrato de estoque daquele ciclo.
2. Somente um baú profissional sem slot vazio ativa o alívio.
3. São examinados exatamente os dez últimos slots físicos da origem.
4. Destinos são somente baús comunitários presentes na lista da mesma vila;
   baús profissionais e posições de outras vilas são excluídos.
5. A transferência completa primeiro pilhas com item e componentes idênticos,
   depois usa slots vazios. Nome, encantamentos, desgaste e demais componentes
   são preservados.
6. A origem perde somente a quantidade que coube fisicamente no destino. Se
   nenhum destino tiver espaço, os itens permanecem onde estavam.
7. Nenhum item é descartado e nenhum chunk é carregado à força para cumprir a
   regra. A exceção controlada de armazenamento de emergência está na ADR-033.

## Consequências

- O trabalhador recupera até dez slots sem transformar outro baú profissional
  em depósito genérico.
- Um baú comunitário cheio pode liberar apenas parte da reserva; o restante
  continua seguro na origem e será tentado em outro ciclo.
- O retrato de estoque observa o estado posterior à transferência, sem contar
  o mesmo item duas vezes.
- O custo ocorre uma vez por ciclo e percorre apenas os baús já descobertos da
  vila; quando a origem não está cheia, não há movimentação.

## Verificação

`ProfessionChestOverflowGameTest` cobre transferência dos dez slots, exclusão
de outro baú profissional, ausência de espaço e isolamento entre vilas. A
suíte completa confirmou 572/572 GameTests. Ainda falta observar no save real
a interação com baús duplos e vários destinos parcialmente ocupados.
