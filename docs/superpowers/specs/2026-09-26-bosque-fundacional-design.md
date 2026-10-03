# Bosque fundacional da vila

## Objetivo

Dar ao lenhador madeira utilizável desde o nascimento da vila e renovar essa
reserva de forma finita à medida que a população viva aumenta.

## Decisão aprovada

1. A primeira adoção de uma colônia cria duas árvores Vanilla adultas, de
   espécies diferentes e aproveitáveis pelo catálogo de madeira da vila.
2. As duas espécies saem de uma tabela por bioma: a primeira é a madeira da
   construção; a segunda é uma espécie companheira explicitamente suportada.
   Deserto continua usando a reserva de carvalho já escolhida para a opção A.
3. Cada árvore nasce em solo natural, entre 48 e 56 blocos do centro, em chunk
   já carregado. O local precisa ter uma caixa livre para a copa, distância
   mínima das estruturas reconhecidas da vila/colônia e nenhuma folha
   persistente. O gerador nunca substitui solo, baú, construção ou bloco
   existente.
4. A cada novo grupo de dez aldeões vivos adultos, a colônia tenta gerar uma
   árvore adulta adicional. Ela só grava o marco depois de gerar a árvore; se
   não houver espaço ou chunk carregado, tenta novamente em ciclo posterior.
5. A reserva fundacional acontece somente no caminho de criação da colônia.
   Reabrir save, sondar o centro ou construir uma casa não a repete.
6. O viveiro continua sendo o mecanismo de mudas e o lenhador continua
   responsável pela colheita. Cortar uma árvore do bosque não dispara reposição
   imediata.

## Limites e invariantes

- Fabric 1.21.1 e Java 21; nenhuma dependência nova e nenhum chunk forçado.
- O estado do marco pertence a `Colony`, não ao mundo Vanilla, e precisa de
  serialização retrocompatível.
- `core/` não importa Minecraft, Fabric, `data/` ou a camada de integração.
- Não usar mixin, `@Overwrite` ou geração global de mundo.
- Toda modificação de mundo exige GameTest; a confirmação visual ainda exige
  playtest em save do autor.

## Critérios de aceite

- Uma nova colônia recebe duas árvores adultas de espécies distintas dentro do
  anel acessível ao lenhador.
- Um bloco deliberadamente colocado na caixa de uma árvore impede o plantio e
  permanece intacto.
- A mesma colônia não ganha outra dupla depois de ser lida de um save.
- Com vinte aldeões adultos e marco salvo em uma dezena, uma árvore adicional
  é gerada e o marco passa para duas dezenas; falha de espaço não avança esse
  valor.
- Save sem o campo novo abre com marco zero.
