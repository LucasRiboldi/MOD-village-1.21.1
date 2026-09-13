# Architecture Decision Record 011

# Village Colony — crescimento das profissões produtoras

**Status:** Accepted
**Date:** 2026-09-13
**Decision Type:** Architecture / Gameplay / Data

---

## 1. Contexto

O sistema atual limita cada uma das oito profissões a dois trabalhadores,
incluindo o Construtor. A regra autoral agora define sete profissões
produtoras do mod e uma progressão pela população adulta. O Construtor é uma
função interna de construção, não uma profissão produtora. Os ofícios do mod
continuam paralelos aos empregos Vanilla.

## 2. Decisão

### 2.1 Ordem das profissões produtoras

1. `MINER` — Mineiro
2. `LUMBERJACK` — Lenhador
3. `MASON` — Pedreiro
4. `SMELTER` — Fundidor
5. `CARPENTER` — Carpinteiro
6. `FARMER` — Agricultor
7. `BREEDER` — Criador

As profissões Vanilla do aldeão não são trocadas nem removidas.

### 2.2 Vagas por população

Os sete primeiros adultos aptos cobrem uma vaga de cada profissão. A partir
de 15 adultos, cada crescimento de população habilita uma vaga seguindo a
ordem acima até completar o lote de sete; o lote seguinte começa no próximo
múltiplo de 15. Logo, aos 15 adultos há um de cada, aos 16 o Mineiro recebe
a segunda vaga, aos 30 há dois de cada, e o 31º habilita o terceiro Mineiro.
As vagas não criadas permanecem Vanilla.

Todos os aldeões adultos da vila contam para a faixa, inclusive Nitwits;
somente adultos aptos podem receber profissão. Bebês contam quando se tornam
adultos. O scanner da colônia é a fonte do número observado, dentro do mesmo
volume já usado para localizar aldeões.

### 2.3 Retenção e construção

Mudança de população não demite nem rebaixa trabalhadores existentes. Morte
remove o trabalhador pelo ciclo de vida existente; a vaga resultante pode ser
preenchida por um adulto apto segundo a quota atual.

Construção é uma capacidade temporária do trabalhador durante a tarefa
`BUILD`. Qualquer trabalhador com profissão produtora pode assumir essa
tarefa quando estiver ocioso e houver obra; sua profissão produtora não muda.
`BUILDER` permanece legível em saves antigos, mas não é contratado em novas
vagas.

`SHEPHERD` também permanece legível em saves antigos, mas novas contratações
usam `BREEDER`. Para as cotas, um `SHEPHERD` legado conta como `BREEDER`,
sem alterar o tipo persistido; isso evita criar uma vaga redundante para o
mesmo trabalho em colônias existentes.

## 3. Consequências

- A população adulta define somente o número e a sequência de novas vagas;
  não altera profissões persistidas.
- A construção não consome uma oitava vaga permanente.
- A lista e sua ordem são contrato de gameplay e devem ser cobertas por
  testes.
- As cadeias materiais do Criador e dos demais produtores são desenvolvidas
  separadamente, sem introduzir recursos sem origem física no mundo.
