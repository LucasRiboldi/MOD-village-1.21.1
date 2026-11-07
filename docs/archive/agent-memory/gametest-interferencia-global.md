---
name: gametest-interferencia-global
description: Na bateria de gametest do MOD-village, seis canais fazem um teste mexer no outro — ciclo de 600 ticks, raio de busca de 64, limpeza depois do assert, relógio do mundo, aldeão sem IA e escrita fora da arena
metadata:
  type: project
---

A bateria `runGametest` roda testes concorrentes num único mundo. Três
canais de interferência já causaram intermitência real (diagnosticados em
2026-08-19):

1. **Ciclo global de 600 ticks.** `VillageDetectionHandler.onServerTick`
   roda `runColonyCycles` sobre toda colônia em `VillageColonyMod.COLONIES`.
   Ele reserva de novo qualquer tarefa AVAILABLE. Um teste que afirme
   "a tarefa voltou para a fila" num tique combinado lá adiante disputa
   com esse ciclo. Ou se afirma o estado no instante em que ele acontece,
   ou não se registra a colônia — `LumberjackWork.run` recebe a colônia
   na mão e `tick` só olha os trabalhos abertos.
2. **`LumberjackWork.SEARCH_RADIUS = 64`.** A busca por árvore alcança as
   estruturas dos outros testes. Um lenhador solto reivindica árvore
   alheia e derruba o teste do vizinho.
3. **Limpeza depois do assert.** `context.assertTrue` lança. Se
   `owned.cleanUp()` e a restauração de estáticos vierem depois dela, uma
   falha deixa trabalhador vivo e estático alterado para o resto da
   bateria — e o sintoma aparece em outro teste. Restaurar **antes** de
   afirmar.

**Por quê:** o sintoma nunca aponta para a causa. A mensagem de falha
acusava o guarda de travamento, que estava correto; o defeito era a
janela da afirmação.

**Como aplicar:** ao caçar teste de jogo intermitente, instrumentar antes
de teorizar — um log de estado a cada N ticks dentro do teste custa uma
execução e responde em uma leitura. Ver [[village-colony-build-env]].

## Aplicado em 2026-08-26 — o E20 fechou por aqui

O canal 1 era a causa do E20. Conserto: `LumberjackGameTest.
theStallGuardReturnsTheTaskAndForgetsTheTree` **deixou de registrar a
colônia em COLONIES**. 3 vermelhas em 15 rodadas viraram 0 em 20.
Nenhuma linha de produção mudou — o guarda sempre esteve certo.

Detalhe que faltava nesta nota: a tarefa não voltava para a árvore
marcada (a marca vale 6.000 ticks). O ciclo achava **outra** árvore, pelo
canal 2. Os dois canais agem juntos, e por isso forçar só o ciclo — sem
árvore vizinha ao alcance naquele instante — **não** reproduz: foi o que
aconteceu numa sonda no tique 200, três rodadas verdes.

**O canal 3 fechou em 2026-08-26, na bateria inteira.** Eram 44 dos 46
pontos de limpeza depois de uma afirmação; agora sao `try/finally`.
O `RoadExtensionGameTest` ja estava certo. Junto, o `ColonyFixture`
passou a esquecer as **seis** profissoes do `VillagerLifecycleHandler`
— faltavam mineiro, fundidor e pastor, chamados a mao nos testes.

Provado quebrando uma afirmacao de proposito: o `finally` logou
`worker ainda registrado? false`. **Nao provado:** a reducao de cascata
em numero — o teste usado deixou de contaminar por causa da correcao do
E20, e deu 1 falha nos dois arranjos. O ganho contra cascata e argumento
estrutural.


## Mais três canais, achados em 2026-09-04

4. **O relógio do mundo é um só, e a bateria o faz andar.** 22 chamadas
   de `setTimeOfDay` em 6 classes. `WorkClock.isWorkTime` é
   `floorMod(t, 24000) < 11000`, e `WorkHoursGameTest.AFTERNOON = 10_000`
   deixa mil tiques de pista antes do `DUSK`. Onde cada teste cai no dia
   depende de quantos tiques a bateria gastou antes dele — **acrescentar
   um teste em qualquer lugar move todos os outros**. Teste que dependa
   de expediente fixa o próprio relógio.
5. **`setAiDisabled(true)` vaza.** O aldeão sem IA sobrevive à limpeza da
   arena e derrubou um teste vizinho em 3 de 3 rodadas. Não usar para
   congelar aldeão.
6. **Escrever fora da arena vaza.** `EMPTY_STRUCTURE` vai até y=7:
   `PERCH.up()` com `PERCH = (3,7,5)` é y=8, **fora**, e escreve no mundo
   compartilhado. Mesmo estrago do canal 5.

**A cura que funcionou** para guarda contado por passagem: o teste chama
`MinerWork.tick(world)` N vezes no mesmo tique em vez de esperar N tiques
de servidor. Nada muda de bloco entre as chamadas. Levou 1 falha em 3
para 1 em 8.

**O que sobra é do mod, não do teste:** `startNextStone` zera
`job.stalled` e `job.stall` a cada alvo novo, e isso está em seis
profissões — quem troca de alvo com frequência é imune aos dois guardas.
Ver [[contagem-honesta-de-testes]].

## Aplicado em 2026-10-11 — FreeChestCountGameTest

A varredura `VillagerScanner.scan` usa a caixa de `VillageDetector.SEARCH_RADIUS` (64) e contava 4 e 5
aldeões em vez de 2 (2 falhas em 8 rodadas, logo depois de entrar um teste novo com 2 aldeões). Conserto:
afirmar sobre os UUIDs e o baú da própria arena, não sobre o tamanho do conjunto. Regra: teste que conta
entidades ou baús num raio grande afirma sobre os seus, nunca sobre o total.
