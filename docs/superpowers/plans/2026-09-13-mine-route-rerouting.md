# Plano — Rotas alternativas da mina no limite

**Status: testes automatizados verdes; playtest pendente.**

## Resultado esperado

Quando todos os braços terminarem no nível mais fundo, a mina conserva a
boca, muda a orientação da hélice e reabre os braços. Saves v4 retomam com
cursores zerados e preservam a entrada e o arco já construído.

## Sequência

1. Escrever teste vermelho para rotação da hélice e migração v4.
2. Manter `turned()` para galerias; adicionar operação separada de rota e
   usá-la apenas quando o nível mais fundo não puder descer.
3. Incrementar `SHAPE_VERSION` e preservar os demais dados válidos do save.
4. Registrar a decisão na ADR-013 e atualizar estado, pendências, padrões e
   histórico do projeto.
5. Rodar testes focados, `build` e `runGametest`; revisar o diff. Os dois
   testes focados passaram; `build` aprovou 808 testes unitários e
   `runGametest` aprovou 314/314.
6. Após aprovação do lote, commitar, enviar `main` e atualizar os JARs em
   `downloads/` e no diretório `mods` do launcher, conferindo hashes.

## Limites

Não deslocar a boca, não forçar carregamento de chunks, não mudar a rota nos
níveis que ainda podem aprofundar e não afirmar validação visual sem jogo.
