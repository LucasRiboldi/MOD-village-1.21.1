# ADR-033 — Armazenamento de emergência no salão da mina

**Estado:** Aceita pelo autor em 2026-10-04
**Escopo:** excedente protegido de baús profissionais depois de esgotar os
baús comunitários da vila.

## Contexto

Os dez últimos slots dos baús profissionais precisam ficar livres para que a
profissão continue guardando o que produz. A ADR-032 já proíbe usar o baú de
outra profissão, mas uma vila pode não ter baú comunitário com espaço. O autor
aprovou criar armazenamento físico adicional na mina, em vez de descartar item
ou transferi-lo a um trabalhador diferente.

## Decisão

1. O alívio tenta primeiro baús comunitários totalmente vazios, depois os
   demais baús comunitários que comportem a pilha. Baú profissional nunca é
   destino.
2. Somente o que não couber após essa ordem pode solicitar armazenamento de
   emergência.
3. A solicitação só cria um baú no salão comum do nível atual quando o salão
   inteiro já foi escavado, o chunk já está carregado e a posição determinística
   tem ar com piso sólido.
4. Cada salão concluído admite um baú de emergência. Se ele lotar, o próximo
   só poderá surgir no salão de outro nível que a mina abrir naturalmente; a
   regra não força chunk, escavação nem abertura de mina.
5. O bloco de baú é a fonte da verdade. Ao ser encontrado, entra nos baús da
   vila mesmo abaixo da altura das camas, sem estado adicional persistido.

## Consequências

- A profissão recupera espaço sem misturar inventários privados e sem perda.
- Um depósito não bloqueia a escada, a passagem 3x3 ou o salão ainda em uso.
- Uma mina ainda incompleta não recebe construção surpresa; até completar o
  salão, o excedente permanece fisicamente no baú profissional para nova
  tentativa posterior.
- O caminho é limitado às posições já carregadas e conhecidas da mina, portanto
  não introduz leitura ampla nem geração de chunk.

## Verificação

`ProfessionChestOverflowGameTest` cobre prioridade de baú vazio, baús duplos,
isolamento entre vilas e ausência de destino. `MineOverflowStorageGameTest`
cobre a criação no salão concluído e na posição determinística. Falta playtest
no save com uma vila sem baú comunitário livre e uma mina que complete dois
níveis.
