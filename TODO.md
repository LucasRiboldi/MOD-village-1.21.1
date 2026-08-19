# TODO

**Atualizado:** 2026-08-19

Lista curta e priorizada. A lista completa, com a razão de cada item e a
seção de origem, está em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md). O enunciado de
cada regra está em `docs/technical/Project-State.md`. Onde os dois
discordarem, vale o Backlog.

---

## 🔴 Crítico

Bloqueia o sexto e último passo do MVP: ver uma casa inteira subir.

- **Ver a cabana inteira subir em jogo.** Seis regras novas têm teste e
  **nenhuma rodou em jogo**: 14 (alcance), 18 (expediente), 10
  (fabricação), 17 (porta na rua), 19 (nível da rua), 20 (bioma) e 21
  (mobília). É a próxima sessão, e ela vale mais que qualquer regra
  nova.
- **Verificar em jogo as cinco correções que só têm teste:** o baú
  criado ao lado da cama, o despertar de `WAITING_RESOURCES`, a árvore
  fora de alcance, a cabana como obra do MVP, e o descarte da obra
  antiga presa no save.

## 🟠 Importante

- **Teste instável do guarda de travamento.**
  `theStallGuardReturnsTheTaskAndForgetsTheTree` falha cerca de 1 a cada
  4 execuções da bateria, e é anterior ao ciclo de 08-19. Enquanto ele
  oscilar, a bateria não prova que nada quebrou. A pista é estado
  estático compartilhado entre testes concorrentes.
- **Regra 10, metade do fabricante.** A do construtor foi feita em
  2026-08-19 — ele fabrica o que falta, juntando material dos baús do
  mais próximo ao mais longe e acumulando entre eles. Falta o fabricante
  produzir porta, janela, cama e baú **por estoque**, sem depender de
  haver obra. Depende do `ItemRequest`: a tarefa carrega um
  `ResourceType`, e porta não está nessa lista — nem deve estar, porque
  a lista sai da planta.
- **A mobília em jogo.** A Regra 21 põe cama, baú e lampião, e só o baú
  a colônia fabrica. Cama e lampião dependem de você guardar lã e ferro
  num baú — falta ver isso acontecendo numa sessão.
- **Regra 15 — a estrada cresce com a vila.** O construtor estende a
  rua a partir da ponta mais distante do centro, um trecho por casa.
  Sem isso a vila para quando acaba a beira de rua que o mundo deu.
- **Regra 16 — espaço em volta da casa.** Distância mínima (corredor de
  obra) e máxima (não soltar da malha) entre construções, e lote
  conferido como volume — largura, profundidade e altura — e não como
  retângulo de chão.
- **Regra 11 — garantir uma de cada profissão por vila.** O mecanismo já
  existe (`ProfessionAssigner.vacancy` devolve a mais escassa), mas nada
  garante o piso, nenhum teste o afirma, e a dispensa pode tirar o último
  de uma profissão.
- **Decidir o movimento de centro da colônia.** Ela pode trocar o centro
  recusando encolher — em 2026-08-15 saiu de uma âncora de 6 camas para
  uma de 3 e deixou a obra 65 blocos atrás. É comportamento da ADR-003 e
  espera decisão do autor.

## 🟡 Melhoria

- **Quebrar os arquivos acima de 500 linhas.** `LumberjackWork` (1149),
  `VillageDetectionHandler` (901), `TreeHarvester` (651), `BuilderWork`
  (599), `ManufacturerWork` (510). As Regras 14 a 17 caem justamente
  sobre `BuilderWork`, `ConstructionPlanner` e `BuildSiteScanner` —
  quebrar antes de crescer sai mais barato que depois.
- **Envelhecimento de tarefa** (`Task.age`), para que a tarefa mais
  antiga não seja esquecida para sempre.
- **Cobrir a Regra 8 por inteiro.** Hoje o baú nasce quando um
  *trabalhador* precisa dele. Cama de aldeão que não trabalha continua
  sem baú — cobrir isso exige propagar as posições das camas por
  colônia, que a varredura hoje descarta.
- **Proteção estrutural** — perguntar ao jogo quais blocos são de vila
  gerada, em vez de inferir.

## 🟢 Futuro

- **Cadeias de produção que não existem:** minerar, fundir, tosquiar,
  descascar. São elas que devolveriam a casa de vila do jogo como obra
  possível — hoje ela pede 66 blocos que a colônia não sabe fazer. A
  janela da Regra 10 depende de fundir: até lá o jogador guarda o vidro.
- **O trabalhador pedir o que lhe falta** (`ItemRequest`), em vez de
  travar. Toca `Task`, que é o centro; só depois do MVP fechar.
- **Fundir colônias sobrepostas.** Hoje o mod só avisa. O critério já
  foi decidido em 2026-08-12 e depende de a construção existir.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
