# TODO

**Atualizado:** 2026-08-20

Lista curta e priorizada. O enunciado de cada regra está em
[`docs/technical/Project-State.md`](docs/technical/Project-State.md); a
lista longa, com a razão de cada item, em
[`docs/technical/Backlog.md`](docs/technical/Backlog.md). Onde os dois
discordarem, vale o Backlog.

**Uma distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas abaixo.

---

## 🔒 Regra imutável

- **Regra 27, de 2026-08-20.** O construtor de cada bioma só levanta
  estruturas da pasta `minecraft-assets_structure` — o mod não cria
  casa. E ele **aguarda o bloco específico** de que precisa, sem
  substituir e sem pular. Ela desfaz partes das Regras 13, 21 e 25, e
  isso está registrado no `Project-State.md`.

## 🔴 Crítico

- **Rodar em jogo a cadeia de produção.** Mineiro, pastor e fundidor
  entraram em 2026-08-20 e **nenhum foi visto trabalhando numa vila de
  verdade**. Junto vêm a paleta por bioma, as casas do catálogo do jogo
  em lugar da cabana do mod, e a planta que se adapta ao lote.
  - **O que olhar:** `Miner ... took N from ...`, `Shepherd ... sheared`,
    `Smelter ... made minecraft:glass out of minecraft:sand`, e uma linha
    `planned` seguida de casa subindo.
  - **Numa vila de deserto:** ela deve construir pela primeira vez, e a
    casa deve ser uma das 28 que o jogo tem para deserto.
  - **O que não deve aparecer:** `no building work: nothing to work on`
    em laço, e nenhuma linha de mobília repetida.

- **Verificar em jogo as correções de 08-20 que só têm teste:** o alvo da
  obra perguntado à colônia, o cursor de busca por colônia, o miolo oco
  da cabana recusado como lote, e a obra parada saindo da frente depois
  de vinte ciclos.

## 🟠 Importante

- **Tocha e ferro travam as casas de vila.** A tocha pede carvão e o
  lampião pede ferro; nenhum dos dois é minerado. Como o construtor agora
  **espera** pelo bloco exato (Regra 27), toda casa com tocha para nela
  até o jogador trazer — ou fica pela metade quando a paciência acabar.
- **A areia não é colhida por ninguém.** O fundidor funde a areia que
  houver nos baús, e nada a põe lá. Falta a meta de areia — e ela depende
  de decompor a receita da vidraça, que é o `ItemRequest` do backlog.
- **Descascar tronco.** O último material da casa de planície que a
  colônia não produz. Não é receita de bancada: é machado no tronco, e
  pede caminho próprio no lenhador ou no fabricante.
- **Teste instável do guarda de travamento.**
  `theStallGuardReturnsTheTaskAndForgetsTheTree` falha cerca de 1 a cada
  4 execuções, e é anterior a 08-19. A pista é estado estático
  compartilhado entre testes concorrentes.
- **Regra 15 — a estrada cresce com a vila.** A vila só constrói em beira
  de rua que já existe. Quando ela acabar, a colônia para de crescer — e
  a Regra 25 só adiou isso, não resolveu.
- **Regra 16 — distância entre construções.** A metade da altura foi
  feita com a Regra 22; falta a distância mínima (corredor de obra) e
  máxima (não soltar da malha da vila).
- **O critério de escolha entre as casas do catálogo é grosso.** Hoje é
  "a maior que couber", entre quatro tamanhos por bioma. Falta escolher
  por *função* — casa de moradia antes de celeiro, oficina quando houver
  profissão para ela — e a ordem em que a vila cresce.
- **Regra 10, metade do fabricante.** Porta, janela, cama e baú por
  estoque, sem depender de haver obra. Depende do `ItemRequest`.
- **Regra 11 — garantir uma de cada profissão por vila.** Ficou maior com
  a cadeia de produção: são **sete** profissões agora, e catorze vagas
  por colônia pela Regra 4. Nada garante o piso, e a dispensa pode tirar
  o último de uma profissão.
- **Decidir o movimento de centro da colônia.** Ela troca de âncora e
  volta, a cada 30 segundos, entre 49 camas e 7 — visto nos logs de
  08-18, 08-19 e 08-20. É comportamento da ADR-003 e **espera decisão do
  autor**.

## 🟡 Melhoria

- **O buraco que o mineiro deixa.** O lenhador replanta o que corta
  (Regra 7); o mineiro não tem equivalente, porque pedra não cresce. A
  vila vai ficando com covas rasas em volta. Decidir se preenche, se cava
  em galeria, ou se aceita.
- **O fundidor lê os baús na ordem de registro**, e não por distância nem
  somando entre eles. `ColonyChests` já faz as duas coisas certo, e o
  fabricante tem a mesma dívida.
- **Quebrar os arquivos ainda acima de 500 linhas:**
  `VillageDetectionHandler` (914), `BuilderWork` (721), `BuildSiteScanner`
  (580), `ColonySavedData` (528), `ManufacturerWork` (510).
- **Migrar `TreeScanner` e `BuildSiteScanner` para `RingSweep`.** A
  espiral orçada agora existe escrita uma vez, e as duas continuam com a
  cópia delas.
- **Envelhecimento de tarefa** (`Task.age`), para que a tarefa mais
  antiga não seja esquecida para sempre.
- **Cobrir a Regra 8 por inteiro.** O baú nasce quando um *trabalhador*
  precisa dele; cama de aldeão que não trabalha continua sem baú.
- **A vila fora da planície de ponta a ponta.** A tabela de biomas tem
  teste, mas a aceitação de uma vila de taiga, savana ou deserto nunca
  rodou: a arena da bateria tem bioma fixo.
- **A escada ainda sai no estado padrão da planta.** A porta já não — ela
  é girada para a rua. Escada e placa continuam sem orientação.

## 🟢 Futuro

- **Ferro.** O lampião pede ferro, e ferro pede minerar fundo e fundir. O
  fundidor já sabe fundir; falta o mineiro descer.
- **O fazendeiro.** Tem nome, enxada e baú desde a Fase 4, e nenhum
  trabalho. É a última profissão do modelo sem código.
- **O trabalhador pedir o que lhe falta** (`ItemRequest`), em vez de
  travar. Toca `Task`, que é o centro — e é ele que destrava a areia, a
  metade do fabricante e a cadeia de receitas.
- **Fundir colônias sobrepostas.** Hoje o mod só avisa. O critério foi
  decidido em 2026-08-12.
- **Lado do cliente:** nome sobre a cabeça, rachadura e braço na tela.
- **Defesa.** Nada no modelo ainda.
