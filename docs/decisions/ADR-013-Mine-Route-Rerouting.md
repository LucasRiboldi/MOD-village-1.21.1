# ADR-013 — Ciclo finito da mina e troca da boca no limite

**Status:** Accepted  
**Date:** 2026-09-23
**Decision Type:** Architecture / Gameplay / Save Data

## Contexto

O ciclo anterior reutilizava a entrada e variava somente a rota no limite de
profundidade. Isso permitia que a mina voltasse a cavar uma boca conhecida e
que a manutencao visual restaurasse o arco mesmo depois de o jogador quebra-lo.
Tambem abria ramais antes de existir uma escada comum concluida.

Em 2026-09-23 o autor definiu a geometria completa: um caracol de dez degraus,
50 blocos de area comum e quatro ramais, cada um com dez degraus e 50 blocos
de area. O ciclo se repete apenas enquanto couber acima da rocha-mae; no
limite, a proxima mina deve nascer no lado oposto da vila.

## Decisão

1. Um nivel tem 10 degraus de caracol compartilhados, 50 blocos de area comum
   e quatro ramais. Cada ramal tem 10 degraus e 50 blocos de area. A ordem e
   deterministica para que o cursor salvo retome a mesma posicao.
2. A escada e a area comuns pertencem ao primeiro mineiro. Os quatro ramais
   so ficam disponiveis depois de os 110 blocos compartilhados terminarem.
3. A prioridade de `MineArm.followVein` continua antes da ordem geometrica,
   para que o mineiro encerre um veio encontrado antes de retomar a area.
4. Depois dos quatro ramais, um novo nivel comeca dez blocos abaixo. Quando o
   proximo nivel cruzaria a faixa mineravel, a mina fica esgotada.
5. Uma mina esgotada so e substituida quando `MineSite.mouthOnSide` encontra
   uma boca valida no lado oposto da vila. Sem essa boca, ela fica aguardando;
   nao ha fallback para outra direcao nem reabertura da mesma boca.
6. `MineMouth.furnish` e chamado somente ao abrir uma mina nova. Uma boca
   conhecida recebe iluminacao, mas arco e lanterna quebrados pelo jogador nao
   sao reconstruidos.
7. `MineSave.SHAPE_VERSION` passa de 5 para 6. Saves da forma 5 preservam a
   boca e o estado visual, mas reiniciam cursores para a nova geometria.

## Consequências

- A mina tem trabalho limitado por nivel e nao pode gerar ramais infinitos.
- O mundo permanece a fonte da verdade para tuneis e para blocos quebrados
  pelo jogador; a persistencia guarda apenas cursores e metadados do ciclo.
- Na migracao, uma mina pode reexaminar blocos ja abertos, com custo limitado
  pela leitura incremental, mas nao interpreta indices da geometria anterior.
- A confirmacao visual de uma descida completa e da troca de boca continua
  exigindo playtest em save.

## Verificação

`MineShaftTest` e `MineTest` verificam a contagem compartilhada, os quatro
ramais, o fundo e os cursores. `MineSaveTest` verifica a migracao da forma 5
para 6. `MinerGameTest` verifica que um portal quebrado nao volta e que a boca
de reposicao fica no lado oposto; a descida real usa o quarto degrau derivado
da geometria. Em 2026-09-23, `test` passou com 928 testes e
`runGametest --rerun-tasks` com 417/417. O playtest visual permanece pendente.

## Emenda — 2026-10-03: portal 5x4 e vão 3x3

A moldura da boca passa a medir cinco blocos de largura por quatro de altura,
com um lampião no topo de cada pilar lateral. O vão útil central permanece
livre em três blocos de largura por três de altura. A escada e as galerias já
usavam três blocos de largura na geometria da forma 6; por isso esta emenda não
altera cursores persistidos nem incrementa `MineSave.SHAPE_VERSION`.

`MinerGameTest` verifica os dois lampiões, a moldura 5x4 e o vão central 3x3.
Como antes, quebrar a moldura depois da abertura não autoriza reconstrução.

## Emenda — 2026-10-03: dois lances, salões 10x10x3 e dez rotas

Cada ciclo subterrâneo mantém dez degraus de descida, agora divididos em dois
lances de cinco. A área comum e a área de cada ramal passam a ser salões de
10 x 10 x 3 blocos. A passagem útil continua 3 x 3 e `MineFloor` conserva o
piso com tijolos de pedra quando a escavação abre um vão não planejado.

Uma frente sem progresso tenta dez rotas antes de considerar a boca perdida.
A seleção continua determinística e retomável: o mundo decide se a rota está
livre e o cursor apenas ordena as células. Como todos os índices posteriores
à escada mudam, `MineSave.SHAPE_VERSION` sobe de 7 para 8; saves anteriores
mantêm boca, orientação e arco, mas reiniciam os cursores.
