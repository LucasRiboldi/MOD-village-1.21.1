# TODO

**Atualizado:** 2026-08-19

Lista curta e priorizada. O enunciado de cada regra está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md); a
lista longa, com a razão de cada item, em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md). Onde os dois
discordarem, vale o Backlog.

**Uma distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas abaixo.

---

## 🔴 Crítico

- **Rodar em jogo as sete regras do ciclo de 08-19.** Nenhuma delas foi
  vista funcionando: 10 (fabricação), 14 (alcance), 17 (porta na rua),
  18 (expediente), 19 (nível da rua), 20 (bioma) e 21 (mobília). Todas
  têm teste; teste não é sessão. Vale mais que qualquer regra nova.
  O que olhar: a casa terminando **sem buracos** — era o defeito do lote
  fora de nível —, a porta dando na rua, e a linha
  `houses still missing [white_bed, lantern]`.
- **Verificar em jogo as correções antigas que só têm teste:** o baú
  criado ao lado da cama, o despertar de `WAITING_RESOURCES`, a árvore
  fora de alcance e o descarte da obra antiga presa no save.

## 🟠 Importante

- **Teste instável do guarda de travamento.**
  `theStallGuardReturnsTheTaskAndForgetsTheTree` falha cerca de 1 a cada
  4 execuções, e é anterior ao ciclo de 08-19. Enquanto ele oscilar, a
  bateria não prova que nada quebrou. A pista é estado estático
  compartilhado entre testes concorrentes.
- **Regra 10, metade do fabricante.** A do construtor está feita. Falta
  o fabricante produzir porta, janela, cama e baú **por estoque**, sem
  depender de haver obra. Depende do `ItemRequest`: a tarefa carrega um
  `ResourceType`, e porta não está nessa lista — nem deve estar, porque
  a lista sai da planta.
- **Regra 15 — a estrada cresce com a vila.** O construtor estende a rua
  a partir da ponta mais distante do centro, um trecho por casa. Sem
  isso a vila para quando acaba a beira de rua que o mundo deu. Ficou
  mais urgente com a Regra 19: lote no nível da rua é mais raro.
- **Regra 16 — espaço em volta da casa.** Distância mínima (corredor de
  obra) e máxima (não soltar da malha) entre construções, e o lote
  conferido como volume — largura, profundidade e altura.
- **Regra 11 — garantir uma de cada profissão por vila.** O mecanismo
  existe (`ProfessionAssigner.vacancy`), mas nada garante o piso, nenhum
  teste o afirma, e a dispensa pode tirar o último de uma profissão.
- **Decidir o movimento de centro da colônia.** Ela troca de âncora e
  volta, a cada 30 segundos, entre 49 camas e 7 — visto nos logs de
  08-18 e 08-19. É comportamento da ADR-003 e **espera decisão do
  autor**.
- **Decidir se a casa do jogo vira a obra padrão.** O schema já está em
  `data/villagecolony/structure/houses/small_house.nbt` e carrega. Ela
  pede 65 blocos que a colônia não produz — o jogador teria de guardar
  pedregulho, vidro e tronco descascado no baú. É troca de modo de jogo,
  e **espera decisão do autor**.

## 🟡 Melhoria

- **Quebrar os arquivos acima de 500 linhas.** `LumberjackWork` (1149),
  `VillageDetectionHandler` (901), `TreeHarvester` (704), `BuilderWork`
  (697). O ciclo de 08-19 já tirou a fabricação de `BuilderWork` para
  `ColonySupply` — o mesmo caminho serve para o resto.
- **Envelhecimento de tarefa** (`Task.age`), para que a tarefa mais
  antiga não seja esquecida para sempre.
- **Cobrir a Regra 8 por inteiro.** O baú nasce quando um *trabalhador*
  precisa dele; cama de aldeão que não trabalha continua sem baú.
- **Proteção estrutural** — perguntar ao jogo quais blocos são de vila
  gerada, em vez de inferir.
- **A vila fora da planície de ponta a ponta.** A tabela de biomas tem
  teste, mas a aceitação de uma vila de taiga ou savana nunca rodou: a
  arena da bateria tem bioma fixo.

## 🟢 Futuro

- **Cadeias de produção que não existem:** minerar, fundir, tosquiar,
  descascar. São elas que devolveriam a casa do jogo como obra possível,
  e que fariam a colônia produzir a cama e o lampião da Regra 21 em vez
  de esperar pelo jogador.
- **O trabalhador pedir o que lhe falta** (`ItemRequest`), em vez de
  travar. Toca `Task`, que é o centro.
- **Fundir colônias sobrepostas.** Hoje o mod só avisa. O critério foi
  decidido em 2026-08-12.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
- **Deserto:** a colônia nasce e não constrói, por não haver árvore. O
  relatório precisa dizer isso em vez de calar.
