# ADR-026 — bosque fundacional da vila

## Status

Aceita em 2026-09-26.

## Contexto

O viveiro compartilhado resolve a reposição de mudas, mas uma vila recém
adotada em campo aberto pode deixar o lenhador sem uma primeira árvore madura.
O plantio lento não supre a primeira tarefa e não comunica uma reserva de
madeira ao redor da vila.

O autor decidiu que cada nova vila deve receber duas árvores maduras, bem
espaçadas e ao alcance do lenhador, e uma árvore adulta adicional por cada dez
aldeões vivos. A regra não pode reescrever construções, carregar áreas distantes
nem reaplicar a fundação quando uma vila é relida.

## Decisão

1. `VillageForest` será a única porta Fabric para gerar o bosque. Ela usa o
   gerador Vanilla de muda para produzir árvores reais, e só o chama depois de
   validar solo natural, chunks carregados e uma caixa de copa vazia.
2. A tabela de `VillageBiomes` passa a declarar o par de espécies do bosque.
   A primeira coincide com a madeira de construção; a segunda é uma espécie
   Vanilla compatível com coleta, tábuas e fabricação da colônia.
3. A criação inicial é chamada exclusivamente após a fundação da primeira
   adoção em `VillageAdoption`. São buscadas duas posições distintas no anel
   de 48 a 56 blocos. Falha em uma posição é logada e não muda blocos.
4. `Colony.forestPopulationMilestone` guarda a maior dezena cujo plantio foi
   concluído. O ciclo da colônia conta aldeões adultos vivos pelo scanner,
   tenta no máximo uma árvore por passagem e aumenta o marco somente depois do
   sucesso. Assim falha de terreno fica pendente sem duplicar árvores após
   save/reload.
5. O viveiro de `FarmerNursery` não muda de teto nem de ritmo. Ele serve às
   mudas; o bosque atende a reserva inicial e o crescimento populacional.

## Consequências

- Uma vila de deserto recebe a reserva de carvalho escolhida na alternativa A;
  isso é uma reserva produtiva, não uma afirmação de vegetação nativa.
- A segunda espécie pode não ser a árvore mais comum daquele bioma, mas sempre
  é uma espécie que o catálogo da vila sabe colher e transformar.
- População que salta várias dezenas é atendida uma árvore por ciclo, evitando
  uma rajada de geração de mundo.
- Áreas fechadas ou descarregadas atrasam o crescimento sem gerar dano. O log
  torna essa espera observável.

## Validação

`ColonySavedDataTest` cobre a persistência e a leitura de save antigo.
`VillageForestGameTest` cobre a dupla inicial, a proteção da caixa ocupada e o
avanço condicionado do marco. `runGametest` é obrigatório; a aparência e a
rota do lenhador ainda precisam de playtest em um save real.
