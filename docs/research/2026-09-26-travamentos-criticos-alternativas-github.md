# Travamentos criticos: auditoria e duas alternativas

**Data:** 2026-09-26
**Modo:** FORENSIC
**Escopo:** crescimento da vila, construcao, navegacao de trabalhadores,
blueprints e custo por tick.
**Linha de base:** `gradlew build` verde em 2026-09-26. Isso prova compilacao e
testes unitarios existentes; nao substitui `runGametest` nem a sessao no save.

## Pergunta

Qual arquitetura deve orientar a correcao dos travamentos criticos: evoluir os
subsistemas atuais em etapas pequenas ou substitui-los por um motor central de
ordens, navegacao e construcao?

## Limite da conclusao

Nao foi encontrado crash da JVM, deadlock de thread ou corrupcao de save
confirmado. Os travamentos criticos observados sao **travamentos logicos**: o
servidor segue em 20 TPS, mas uma obra, trabalhador ou vila deixa de progredir.

As etiquetas abaixo separam o que o codigo e o save demonstram:

- **FATO:** observado no codigo, teste ou sessao registrada.
- **INFERENCIA:** explicacao mais consistente com os fatos, ainda sem teste
  vermelho especifico.
- **VALIDACAO:** precisa de GameTest novo, perfil ou playtest no save.

## Erros e travamentos criticos

| Prioridade | Mecanismo | Evidencia atual | Consequencia |
|---|---|---|---|
| P0 | Crescimento repete lotes inviaveis quando as pontas de rua nao avancam | **FATO:** 714.589 colunas recusadas, 68,5% por pegada cruzando rua reservada; 78 ciclos sem ponta pavimentavel | vila varre o mesmo espaco e pode passar horas sem nova obra |
| P0 | Obra com todas as pecas restantes adiadas nao termina | **FATO:** `WaitingWork.giveUpIfStalled` apaga os dois relogios quando `nextBlock()` esta vazio e retorna `false` | obra ocupa a fila indefinidamente se a assinatura de apoio nunca mudar |
| P0 | Obra abandonada com zero blocos conserva o lote | **FATO:** comportamento protegido por tres GameTests; politica ainda aguarda o autor | evita sobreposicao, mas pode bloquear para sempre um lote que nunca recebeu bloco |
| P1 | Trabalhador ocioso ou sem rota pode ficar fora da escala | **FATO:** a recuperacao depende do fluxo de tarefa; E44/E45 volta a selecionar a mesma pedra inalcançavel em novas sessoes | trabalhador permanece abaixo do solo ou repete alvo impossivel |
| P1 | Scanner, guarda e crescimento nao compartilham uma unica decisao de viabilidade | **INFERENCIA:** a sessao selecionou lote que a guarda de alcance recusou, com zero bloco colocado | planejamento aceita trabalho que a execucao rejeita |
| P1 | Blueprint perde propriedades direcionais | **FATO:** `BlueprintBlock` guarda somente id/furniture e o leitor descarta estado da paleta; ADR-008 ja definiu `Optional<Side>` | escadas, camas e troncos podem ser colocados com estado incorreto |
| P2 | Varredura e pathfinding repetem consultas caras | **FATO:** sessao anterior registrou 65 ciclos acima do tick, planejador medio de 69 ms e pico de 214 ms | risco de picos mesmo sem queda sustentada de TPS |
| P2 | Estado de servidor continua distribuido em campos `static` mutaveis | **FATO:** `ServerMemory.resetAll` centralizou a limpeza, mas nao a propriedade dos dados | aumenta risco de vazamento entre mundos e torna recuperacao dificil de observar |

## Referencias externas conferidas

| Projeto inspecionado | Padrao aproveitavel | Limite para este mod |
|---|---|---|
| [Baritone, branch 1.21.4](https://github.com/cabaletta/baritone/blob/1.21.4/FEATURES.md), commit `3d3da10` | A* segmentado, timeout, corte na borda de chunks carregados, custo incremental e metas proximas ao alvo | versao diferente; LGPL-3.0; arquitetura de bot, nao Brain de aldeao |
| [MineColonies `PathingStuckHandler`](https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/core/entity/pathfinding/navigation/PathingStuckHandler.java), commit `6b3916a` | mede mudanca de distancia/no do caminho e escala recuperacao: recalcular, afastar, contornar, alterar terreno ou desistir | checkout inspecionado declara Minecraft 1.20.1 e usa Forge; GPL-3.0 |
| [Structurize `Blueprint`](https://github.com/ldtteam/Structurize/blob/version/main/src/main/java/com/ldtteam/structurize/blueprints/v1/Blueprint.java), commit `e786907` | conserva transformacao `RotationMirror` e aplica orientacao ao estado real do bloco | checkout inspecionado declara Minecraft 1.20.1; GPL-3.0; modelo maior que o necessario |
| [Lithium: mixins de IA](https://github.com/CaffeineMC/lithium/blob/develop/lithium-fabric-mixin-config.md), commit `7766602` | cache de tipos de nos e acesso mais barato a chunks, POI e sensores | otimiza caminho existente; nao resolve alvo impossivel nem politica de obra; LGPL-3.0 |

Esses repositorios sao referencias de mecanismo. A implementacao deve ser
original e compatível com Fabric 1.21.1, Java 21, Vanilla Brain e as ADRs do
projeto; nenhum codigo GPL/LGPL deve ser transplantado.

## Alternativa A — recuperacao incremental e limitada (recomendada)

Preserva os subsistemas atuais e introduz contratos pequenos, observaveis e
testaveis.

1. **Viabilidade unica de lote:** scanner, planejador e guarda consultam a
   mesma classificacao. Rejeicoes ficam em cache por assinatura do terreno e
   sao invalidadas quando estrada ou mundo relevante muda. A ponta de rua
   informa a causa terminal, em vez de apenas repetir a varredura.
2. **Estado terminal para obra adiada:** uma assinatura de suporte sem mudanca
   tem retry limitado. Ao esgotar, a obra sai da fila ativa como suspensa; o
   lote permanece reservado ate uma politica explicita decidir liberacao. Isso
   remove o loop sem abrir sobreposicao silenciosa.
3. **Recuperacao comum de trabalhador:** medir progresso real (distancia,
   posicao e destino), escalar `WALK_TARGET` de novo, alvo adjacente, desvio
   seguro e, por ultimo, recusar/liberar a tarefa. Sem teleporte e sem cancelar
   a IA Vanilla.
4. **Lease de alvo do mineiro:** coordenada, motivo e validade impedem que a
   mesma pedra inalcançavel seja reescolhida imediatamente ou apos recarregar.
5. **Orientacao da planta:** implementar a ADR-008 ja aceita, com `Side` neutro
   no Core e conversao para `BlockState` apenas no Fabric.
6. **Custo por tick:** cache limitado e apenas chunks carregados, inspirado no
   recorte do Baritone e nas consultas especializadas do Lithium; medir antes
   e depois.
7. **Estado global:** migracao gradual para um contexto por servidor, sem
   reescrever todos os oficios de uma vez.

**Vantagens:** menor risco para saves, diffs pequenos, causa e aceite isolados,
mantem Vanilla Brain e permite parar depois de cada ganho comprovado.

**Custos:** exige uma sequencia de correcoes e deixa adaptadores antigos durante
a migracao. Ainda requer playtest longo para provar que a vila progride.

## Alternativa B — motor central de ordens e navegacao

Substitui os fluxos atuais por uma maquina persistente de `WorkOrder`, com
estados, tentativas, causa terminal, navegacao e colocacao de blocos comuns a
todos os oficios. O blueprint passa a conservar um mapa completo de
propriedades e transformacoes. Um contexto por servidor concentra filas,
indices, caches e telemetria.

**Vantagens:** modelo uniforme, observabilidade melhor e menos guardas locais;
faz sentido se o objetivo for aproximar o projeto de uma plataforma de colonia
com ordens persistentes.

**Custos:** reescrita transversal, migracao de save, ADR nova, muitos GameTests
e maior chance de regressao em construcao, profissao e Brain. MineColonies e
Structurize nao sao referencias portaveis: os checkouts inspecionados sao
Forge/1.20.1 e GPL-3.0. O prazo e o risco sao varias vezes maiores que na
alternativa A.

## Comparacao para decisao

| Criterio | Alternativa A | Alternativa B |
|---|---|---|
| Primeiro resultado | curto, por defeito | longo, depois da fundacao |
| Risco para saves | baixo a medio | alto |
| Compatibilidade Vanilla Brain | preservada | precisa ser redesenhada e provada |
| Cobertura necessaria | testes focados + GameTests por etapa | bateria transversal + migracao + endurance |
| Debito remanescente | moderado, reduzido gradualmente | menor se a reescrita terminar |
| Recomendacao | **sim** | somente com aceite explicito de reescrita |

## Sequencia de prova da alternativa A

1. Reproduzir T1 com todas as pecas adiadas e provar a saida terminal.
2. Reproduzir lote aceito pelo planejador e recusado pela execucao; unificar a
   decisao sem afrouxar protecao de rua.
3. Reproduzir trabalhador sem progresso e mesma pedra apos reload; provar a
   escalada e a recusa persistida.
4. Implementar ADR-008 com testes de escada, cama e eixo de tronco.
5. Rodar `test`, `build`, `runGametest --rerun-tasks`, perfil por tick e sessao
   longa no save. Automatizacao verde nao encerra a validacao em jogo.

## Decisao e aplicacao

O autor escolheu a **alternativa A** em 2026-09-26. A primeira aplicacao
confirmou que persistencia de recusas da mina, desvios, orcamento do planejador
e limpeza central ja existiam. Foram implementadas e provadas as duas lacunas
diretas: T1 sai da fila depois da paciencia preservando a obra parcial e o
lote; a ADR-008 conserva e gira o `facing` horizontal do blueprint.

A politica de lote a 1-2 blocos da rua, a liberacao de lote sem nenhum bloco e
a migracao total do estado `static` continuam fora desta entrega: as duas
primeiras exigem decisao arquitetural e a ultima e migracao gradual. A prova
automatizada fechou em 473/473 GameTests; o comportamento no save ainda precisa
de playtest.
