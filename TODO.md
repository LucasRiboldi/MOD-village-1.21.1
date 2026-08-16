# TODO

**Atualizado:** 2026-08-15

Lista curta e priorizada. A lista completa, com a razão de cada item e a
seção de origem, está em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md) — **34 itens
abertos em 9 grupos**. Onde os dois discordarem, vale o Backlog.

---

## 🔴 Crítico

Bloqueia o sexto e último passo do MVP.

- **Ver a cabana inteira subir em jogo.** Dois blocos foram postos em
  2026-08-15; nenhuma casa terminou. Todas as travas conhecidas foram
  corrigidas, e nenhuma das correções rodou em jogo ainda.
- **Verificar em jogo as cinco correções que só têm teste:** o baú
  criado ao lado da cama, o despertar de `WAITING_RESOURCES`, a árvore
  fora de alcance, a cabana como obra do MVP, e o descarte da obra
  antiga presa no save.

## 🟠 Importante

- **Regra 10 — o construtor fabricando o que a obra pede.** Decidida em
  2026-08-15 e não começada. Acesso a todos os baús do mais próximo para
  o mais longe, acumulando até juntar a quantidade; e o construtor faz o
  craft dos blocos da obra. É a maior das regras pendentes.
- **Regra 11 — garantir uma de cada profissão por vila.** O mecanismo já
  existe (`ProfessionAssigner.vacancy` devolve a mais escassa), mas nada
  garante o piso, nenhum teste o afirma, e a dispensa pode tirar o último
  de uma profissão.
- **Decidir o movimento de centro da colônia.** Ela pode trocar o centro
  recusando encolher — em 2026-08-15 saiu de uma âncora de 6 camas para
  uma de 3 e deixou a obra 65 blocos atrás. É comportamento da ADR-003 e
  espera decisão do autor.
- **Estender a estrada.** A colônia só constrói na beira de rua que já
  existe; quando ela acabar, a vila para de crescer.

## 🟡 Melhoria

- **Quebrar os arquivos acima de 500 linhas.** `LumberjackWork` (1149),
  `VillageDetectionHandler` (901), `TreeHarvester` (651), `BuilderWork`
  (599), `ManufacturerWork` (510). Os três primeiros já estavam grandes;
  o ciclo de 08-15 piorou os dois de trabalho ao acrescentar
  instrumentação.
- **Envelhecimento de tarefa** (`Task.age`), para que a tarefa mais
  antiga não seja esquecida para sempre.
- **Cobrir a Regra 8 por inteiro.** Hoje o baú nasce quando um
  *trabalhador* precisa dele. Cama de aldeão que não trabalha continua
  sem baú — cobrir isso exige propagar as posições das camas por
  colônia, que a varredura hoje descarta.
- **Orientação de blocos.** Escada e porta saem no padrão; falta girar
  conforme a planta.
- **Proteção estrutural** — perguntar ao jogo quais blocos são de vila
  gerada, em vez de inferir.

## 🟢 Futuro

- **Cadeias de produção que não existem:** minerar, fundir, tosquiar,
  descascar. São elas que devolveriam a casa de vila do jogo como obra
  possível — hoje ela pede 66 blocos que a colônia não sabe fazer.
- **O trabalhador pedir o que lhe falta** (`ItemRequest`), em vez de
  travar. Toca `Task`, que é o centro; só depois do MVP fechar.
- **Fundir colônias sobrepostas.** Hoje o mod só avisa. O critério já
  foi decidido em 2026-08-12 e depende de a construção existir.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
