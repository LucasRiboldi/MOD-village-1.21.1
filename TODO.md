# TODO

**Atualizado:** 2026-08-19 (v0.2.0-alpha)

Lista curta e priorizada. O enunciado de cada regra está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md); a
lista longa, com a razão de cada item, em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md). Onde os dois
discordarem, vale o Backlog.

**Uma distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas abaixo.

---

## 🔴 Crítico

- **Rodar em jogo o que a v0.2.0 traz.** Onze regras entraram desde a
  última sessão e **nenhuma foi vista funcionando**. A maior delas é a
  troca da casa de planície pela casa do próprio jogo.
  - **Antes de entrar:** guarde **pedregulho, tronco descascado e
    vidraça** num baú da vila. Sem isso a obra para — e isso é a regra,
    não defeito.
  - **O que olhar:** a casa terminando sem buracos; a porta dando na
    rua; o log dizendo `houses still missing [...]` quando faltar cama
    ou lampião; e nenhuma casa nascendo em volta de um tronco.
- **Verificar em jogo as correções antigas que só têm teste:** o baú
  criado ao lado da cama, o despertar de `WAITING_RESOURCES` e o
  descarte da obra antiga presa no save.

## 🟠 Importante

- **Teste instável do guarda de travamento.**
  `theStallGuardReturnsTheTaskAndForgetsTheTree` falha cerca de 1 a cada
  4 execuções, e é anterior ao ciclo de 08-19. Enquanto ele oscilar, a
  bateria não prova que nada quebrou. A pista é estado estático
  compartilhado entre testes concorrentes.
- **Regra 15 — a estrada cresce com a vila.** O construtor estende a rua
  a partir da ponta mais distante do centro, um trecho por casa. Ficou
  mais urgente na v0.2.0: com o lote exigido no nível da rua **e** livre
  no volume, lote bom é bem mais raro — e a casa do jogo é 7×7×7, contra
  os 5×5×4 da cabana.
- **Regra 16 — distância entre construções.** A metade da altura foi
  feita com a Regra 22; falta a distância mínima (corredor de obra) e
  máxima (não soltar da malha da vila).
- **Escolher entre as 1.180 estruturas do catálogo.** A lista está em
  `data/villagecolony/catalog/vanilla_structures.json`. Falta o critério
  — que construção, para qual vila, em que ordem — e a conta de
  materiais de cada uma, que hoje só existe sob demanda
  (`Blueprint.materials`).
- **Regra 10, metade do fabricante.** O fabricante produzir porta,
  janela, cama e baú **por estoque**, sem depender de haver obra.
  Depende do `ItemRequest`: a tarefa carrega um `ResourceType`, e porta
  não está nessa lista — nem deve estar, porque a lista sai da planta.
- **Regra 11 — garantir uma de cada profissão por vila.** O mecanismo
  existe (`ProfessionAssigner.vacancy`), mas nada garante o piso, nenhum
  teste o afirma, e a dispensa pode tirar o último de uma profissão.
- **Decidir o movimento de centro da colônia.** Ela troca de âncora e
  volta, a cada 30 segundos, entre 49 camas e 7 — visto nos logs de
  08-18 e 08-19. É comportamento da ADR-003 e **espera decisão do
  autor**.

## 🟡 Melhoria

- **Quebrar os arquivos acima de 500 linhas.** `LumberjackWork` (1232),
  `VillageDetectionHandler` (914), `BuilderWork` (721), `TreeHarvester`
  (711), `ConstructionPlanner` (552). O ciclo de 08-19 já tirou a
  fabricação de `BuilderWork` para `ColonySupply` e a limpeza de
  canteiro para `SitePreparation` — o mesmo caminho serve para o resto.
- **O fabricante lendo os baús como o construtor lê.** Ele já usa todos
  os baús da colônia, mas na ordem de registro e sem somar entre eles.
  `ColonySupply` já faz as duas coisas certo.
- **Envelhecimento de tarefa** (`Task.age`), para que a tarefa mais
  antiga não seja esquecida para sempre.
- **Cobrir a Regra 8 por inteiro.** O baú nasce quando um *trabalhador*
  precisa dele; cama de aldeão que não trabalha continua sem baú.
- **Proteção estrutural** — perguntar ao jogo quais blocos são de vila
  gerada, em vez de inferir.
- **A vila fora da planície de ponta a ponta.** A tabela de biomas tem
  teste, mas a aceitação de uma vila de taiga ou savana nunca rodou: a
  arena da bateria tem bioma fixo.
- **A escada ainda sai no estado padrão da planta.** A porta já não —
  ela é girada para a rua. Escada e placa continuam sem orientação.

## 🟢 Futuro

- **Cadeias de produção que não existem:** minerar, fundir, tosquiar,
  descascar. São elas que fariam a casa de planície subir sozinha, e que
  fariam a colônia produzir a cama e o lampião em vez de esperar pelo
  jogador.
- **O trabalhador pedir o que lhe falta** (`ItemRequest`), em vez de
  travar. Toca `Task`, que é o centro.
- **Fundir colônias sobrepostas.** Hoje o mod só avisa. O critério foi
  decidido em 2026-08-12.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
- **Deserto:** a colônia nasce e não constrói, por não haver árvore. O
  relatório precisa dizer isso em vez de calar.
