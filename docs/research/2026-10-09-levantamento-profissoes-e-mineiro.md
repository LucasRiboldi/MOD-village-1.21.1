# Levantamento das profissões e flexibilidade do mineiro - 2026-10-09

## Escopo

Leitura do save de teste pelo log local mais recente em
`%APPDATA%\.minecraft\logs\latest.log`, com foco em:

- ações atuais de cada profissão;
- campos travados, `WAITING_RESOURCES`, erros e ciclos acima de um tique;
- causas que deixam o mineiro pouco maleável;
- fila recomendada de correção antes de publicar o próximo JAR.

Este documento é diagnóstico. Ele não afirma correção em jogo.

## Comandos executados

```powershell
python scripts\analyze_village_log.py (Join-Path $env:APPDATA '.minecraft\logs\latest.log')
python scripts\time_ledger.py (Join-Path $env:APPDATA '.minecraft\logs\latest.log')
python scripts\cost_ledger.py (Join-Path $env:APPDATA '.minecraft\logs\latest.log')
```

Também foram feitas buscas direcionadas por `VC_ACTIVITY`,
`VC_SUPPLY_ERROR`, `WAITING_RESOURCES`, `barn_majest`, mensagens do mineiro e
avisos/erros do servidor.

## Resumo executivo

Não há evidência de congelamento global contínuo no log. Há, sim, fluxo ruim em
várias profissões e picos de ciclo acima de 50 ms.

O achado mais forte é que `villagecolony:colony/barn_majest` continua sendo
selecionado no jogo e gera obra praticamente tóxica para a colônia: reabre
repetidas vezes, exige itens sem rota confiável e produz `WAITING_RESOURCES`.
Isso confirma o pedido de remover esse modelo da lista de construções antes de
publicar o próximo JAR.

O mineiro não está totalmente parado, mas está rígido demais em quatro pontos:

1. espera ramal enquanto a mina ainda é uma única frente;
2. insiste por até 400 tiques úteis em alvos cuja aproximação já mostra
   distância ou altura inviável;
3. repete marcação de bloco inalcançável em rajadas;
4. ocasionalmente reporta trabalho em `Ar`, sinal de alvo obsoleto ou mudança
   de bloco não revalidada a tempo no relatório/trabalho.

## Sinais quantitativos do log

`analyze_village_log.py`:

```text
Candidatos a loop: 5
  miner_no_branch_work: 8
  construction_waiting_resources: 22
  builder_pathing_stalled: 55
  site_sweep_budget_exhausted: 24
  cycle_over_tick: 7
Ciclos acima de um tique: 7 (pior 74 ms, mediana dos lentos 53 ms)
```

`cost_ledger.py`:

```text
2-3 colônias - 6 amostras (ms: média / p95 / máximo)
  planner         8.65    48.64    48.64
  detect          5.13    11.00    11.00
  assign          1.71     5.45     5.45
  workers         0.39     1.10     1.10
  total          17.21    66.01    66.01
```

Leitura: o custo maior está em `planner` e `assign`, não nos workers por si só.
O mineiro incomoda por fluxo e espera, não por ser o principal consumidor de ms.

## Tempo por profissão

`time_ledger.py`, seis janelas de cinco minutos:

| Profissão | Work | Walk | Wait | Blocked | Idle | Diagnóstico |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| BUILDER | 2% | 4% | 0% | 2% | 93% | Fluxo ruim: 94% sem trabalhar |
| CARPENTER | 20% | 26% | 50% | 1% | 3% | Fluxo ruim: 54% sem trabalhar |
| FARMER | 2% | 47% | 33% | 1% | 18% | Fluxo ruim: 51% sem trabalhar |
| LUMBERJACK | 11% | 66% | 0% | 14% | 9% | Travamento real: 14% bloqueado |
| MASON | 33% | 21% | 38% | 1% | 7% | Fluxo ruim: 46% sem trabalhar |
| MINER | 21% | 39% | 19% | 2% | 20% | Fluxo ruim: 41% sem trabalhar |
| SHEPHERD | 3% | 73% | 9% | 1% | 14% | Aceitável no critério atual |
| SMELTER | 17% | 0% | 1% | 0% | 80% | Fluxo ruim: 82% sem trabalhar |
| NONE | 0% | 0% | 0% | 0% | 100% | Desemprego/sem atividade |

## Atividades e achados por profissão

### Builder

Responsabilidade: consumir materiais e colocar peças da obra.

Achados:

- 94% do tempo sem trabalho efetivo;
- 22 ocorrências de `WAITING_RESOURCES`;
- 55 candidatos `builder_pathing_stalled`;
- várias obras `barn_majest` abertas, canceladas por Soul Torch e reabertas;
- uma obra `barn_majest` ficou em 15 peças por 1200 tiques úteis sem colocar
  nada, depois foi abandonada.

Conclusão: o construtor está sendo intoxicado por uma estrutura que exige itens
fora das rotas normais e por varreduras de lote/obra caras. O primeiro corte é
remover `barn_majest` do catálogo.

### Carpenter

Responsabilidade: transformar madeira em peças de construção.

Achados:

- 20% trabalho, 50% espera, 26% caminhada;
- não aparece como erro principal no log;
- provável gargalo indireto: quando a obra pede item sem rota ou item de outro
  domínio, o carpinteiro fica esperando demanda executável.

Conclusão: revisar após retirar `barn_majest`, para não otimizar uma profissão
em cima de demanda inválida.

### Farmer

Responsabilidade: comida e solo.

Achados:

- 51% sem trabalhar;
- muitos registros `FARMING` com `NO_TARGET` e `SWEEP_INCOMPLETE`;
- 4 registros `MAINTAIN_FOOD` com `WORK_STALLED`;
- há falha automatizada recente fora deste log em
  `SurfaceGatheringGameTest.farmerGathersDirtOutsideTheSoilProtectedRadius`,
  que bloqueia publicação segura.

Conclusão: o fazendeiro precisa de investigação própria de busca de alvo/solo,
mas não é o primeiro culpado da obra travada.

### Lumberjack

Responsabilidade: madeira e viveiro de árvores.

Achados:

- único acima do limite de travamento do ledger: 14% bloqueado;
- 66% caminhando;
- registros pontuais de `WORK_STALLED`.

Conclusão: há travamento real de mobilidade/alcance. Deve entrar logo após
`barn_majest` e mineiro, porque é bloqueio medido, não apenas ociosidade.

### Mason

Responsabilidade: peças de pedra e cerâmica simples.

Achados:

- 33% trabalho, 38% espera;
- aparece em `VC_SUPPLY_ERROR` por itens como `glowstone_dust` e
  `polished_tuff`, principalmente vindos de `barn_majest`.

Conclusão: a espera do pedreiro é contaminada pela demanda inválida do modelo.
Reavaliar depois de remover a estrutura da rotação.

### Miner

Responsabilidade: pedregulho, pedra, carvão, ferro bruto e material de mina.

Achados:

- 21% trabalho, 39% caminhada, 19% espera, 20% ocioso;
- 8 `miner_no_branch_work`;
- 8 `MINER/COLLECT_STONE/ERROR/WORK_STALLED`;
- 8 `MINER/COLLECT_STONE/ABANDONED/WORK_STALLED`;
- longos trechos com mineiros esperando ramal enquanto a mina ainda é uma
  frente única;
- alvos inalcançáveis geram desistência depois de 400 tiques úteis;
- em um ponto, o mesmo bloco `-647,65,-803` é marcado como inalcançável dezenas
  de vezes em segundos;
- há linhas de relatório com o mineiro "digging Ar", sugerindo alvo obsoleto ou
  relatório feito antes de soltar alvo já virado ar.

Conclusão: o mineiro precisa ser mais maleável, mas a correção deve ser por
testes pequenos e separados: alvo inalcançável, reserva de ramal, alvo que vira
ar, e fallback de superfície.

### Shepherd

Responsabilidade: lã e criação de ovelhas.

Achados:

- ledger classifica como `ok`;
- muita caminhada, mas pouco bloqueio;
- registros `NO_TASK` baixos.

Conclusão: não é prioridade neste log.

### Smelter

Responsabilidade: fundição e recursos de superfície associados.

Achados:

- 82% sem trabalhar;
- muitos `SMELTING/NO_TASK`;
- quase sem caminhada e sem bloqueio.

Conclusão: parece mais falta de demanda executável que travamento. Reavaliar
após corrigir demanda de construção.

### NONE

Achados:

- 6157 segundos 100% ociosos.

Conclusão: pode ser população sem profissão ou slots ainda não atribuídos. Não
é erro por si só, mas deve entrar em auditoria de distribuição de mão de obra se
o save deveria estar com todos empregados.

## Diagnóstico específico do mineiro

### 1. Gargalo inicial de ramal único

Exemplo observado: dois mineiros ficam esperando porque "the shaft is one branch
until the gallery starts" enquanto um terceiro abre a descida. Isto é coerente
com `Mine.branchesOpenNow()`, que só libera os quatro ramais depois da galeria.

Melhoria possível: quando a mina ainda é uma frente única, mineiros extras não
devem competir por ramo. Eles devem priorizar alternativa útil e barata:

- carregar/depositar material pendente;
- varrer pedra exposta somente se a mina realmente não puder avançar;
- abrir rota auxiliar se houver decisão arquitetural para isso;
- ou registrar espera limpa sem gastar orçamento de busca.

### 2. Alvo inalcançável demora demais para ser abandonado

Há casos em que o aldeão anda sem se aproximar por 400 tiques úteis antes de
desistir. O relatório já sabe que a distância não melhora e que o destino de
andar não chega ao bloco.

Melhoria possível: antes de esperar 400 tiques, reduzir a tolerância quando:

- `MinerApproach.approachTo` devolve posição muito acima do aldeão;
- a navegação diz `does not reach`;
- o alvo está a dezenas de blocos e o "closest" não melhora por uma janela
  curta;
- a posição de pé exige subir mais que `MinerWork.CLIMB`.

### 3. Marcações repetidas do mesmo bloco

O bloco `-647,65,-803` foi marcado como inalcançável em rajada, subindo de uma
recusa até dezenas de recusas quase instantaneamente.

Melhoria possível: deduplicar marcação por alvo/tique ou fazer a tentativa que
descobre `TOO_HIGH` consumir o alvo uma só vez antes de procurar outro. Isto
evita inflar o backoff para 48000 tiques por repetição local.

### 4. Trabalho reportado em `Ar`

O log mostra `digging Ar` com `0/0 ticks`. `MinerSteps.step` solta alvo quando
`state.isAir()`, então o sintoma pode estar no relatório ou em uma janela entre
escolha, remoção por outro sistema e leitura de status.

Melhoria possível: revalidar estado no relatório e no alvo salvo, tratando ar
como `TARGET_CHANGED`, sem exibir como trabalho real.

### 5. Fallback de superfície com mensagem enganosa

Depois de `every open branch is taken or finished`, o fallback registra `no mine
mouth, and no exposed stone within 48 blocks either`, mesmo quando a colônia tem
mina. A chamada passa células planejadas da mina para evitar invadir o poço, mas
a mensagem descreve o caso de "sem boca de mina".

Melhoria possível: separar dois assuntos de idle:

- sem boca de mina: busca de emergência por pedra exposta;
- mina ocupada/acabada: trabalho auxiliar enquanto espera ramal.

## Fila recomendada

1. P0 - Remover `barn_majest` do catálogo de construções e impedir reabertura
   no save atual. Evidência: múltiplas aberturas, cancelamentos e
   `VC_SUPPLY_ERROR` para itens sem rota.
2. P0 - Resolver a falha automatizada recente
   `SurfaceGatheringGameTest.farmerGathersDirtOutsideTheSoilProtectedRadius`
   antes de commit/push/JAR.
3. P1 - Mineiro maleável, bateria de testes:
   - alvo inalcançável com navegação que não chega deve retargetar antes de 400
     tiques úteis;
   - bloco `TOO_HIGH` não deve gerar rajada de recusas no mesmo alvo;
   - alvo que vira ar não deve aparecer como trabalho ativo;
   - mineiro sem ramal disponível não deve gastar busca de mina como se pudesse
     cavar.
4. P1 - Lenhador: investigar 14% bloqueado e trajetos longos.
5. P1 - Builder/planner: reduzir picos de `planner`/`assign` acima de 50 ms,
   principalmente quando a varredura de lote encontra obra já aberta ou modelo
   inválido.
6. P2 - Reavaliar carpenter, mason e smelter depois da remoção do
   `barn_majest`, porque parte da espera vem de demanda inválida.
7. P2 - Auditar `NONE` se a intenção do save é manter todos empregados.

## Próximo passo seguro

Antes de publicar:

1. manter a remoção de `barn_majest`;
2. corrigir a falha GameTest do fazendeiro ou isolar se ela for pré-existente;
3. adicionar testes pequenos para o mineiro;
4. rodar `./gradlew.bat build`;
5. rodar `./gradlew.bat runGametest --rerun-tasks --no-daemon`;
6. só então copiar/publicar o JAR.
