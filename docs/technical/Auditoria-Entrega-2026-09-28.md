# Auditoria de entrega e estabilidade - 2026-09-28

## Escopo e evidencia

Esta auditoria separa defeito reproduzido, risco de desempenho e playtest
pendente. Ela nao considera uma assinatura de log como defeito por si so.

| Verificacao | Resultado | Evidencia |
|---|---:|---|
| Unitarios Java | 1.163/1.163 | 139 suites, 0 falhas, 0 erros, 0 ignorados |
| GameTests Fabric | 498/498 | `build/gametest/logs/latest.log`, 58,38 s |
| Suite Python | 86/86 | `python -m unittest discover -s tests -v` |
| Log do save | 2.118 linhas | `scripts/analyze_village_log.py` |
| Analise estrutural | atualizada | Graphify: 8.148 nos, 28.731 relacoes, 9 hiperarestas |

O build compilou com 25 avisos Error Prone. Nenhum dos avisos se tornou falha
nos testes desta rodada; `Enum.ordinal()` em prioridade/substituicao e conversao
de `long` para `double` merecem uma correcao de baixo risco depois dos gargalos
de jogo.

## Estatisticas do codigo

| Area | Arquivos Java | Linhas |
|---|---:|---:|
| `src/main/java` | 311 | 48.620 |
| `src/test/java` | 139 | 19.109 |
| `src/gametest/java` | 73 | 24.973 |
| Total | 523 | 92.702 |

Nao ha arquivo de producao acima de 500 linhas na medicao atual. Os maiores
GameTests sao `MinerGameTest` (4.764 linhas), `BuildSiteGameTest` (2.043),
`LumberjackGameTest` (1.930) e `BuilderGameTest` (1.487). Sao pontos de
manutencao de fixture, nao um gargalo de runtime, mas devem ser divididos por
cenario quando novos casos tornarem a leitura dificil.

## Gargalo confirmado no save

O ciclo de colonia excedeu o orcamento de 50 ms repetidamente: 91, 98, 95,
259, 129, 149, 145, 139, 156, 129 e 123 ms. Nas amostras com detalhamento, a
fase `chests` custou 76-122 ms; planejamento custou 9-30 ms e deteccao 4-206
ms. Portanto, o gargalo dominante e o levantamento sincrono dos baus de todas
as colonias, nao o trabalho de uma profissao.

O codigo ja tem um limite de planejamento por colonia e cursor para varredura
de lotes. A fase de estoque precisa ganhar a mesma disciplina sem criar um
inventario virtual: o mundo continua sendo a fonte da verdade.

### P0 - Levantamento de baus por ciclo

**Risco:** picos de 76-122 ms somente em estoque travam todas as decisoes da
vila, mesmo quando a profissao e o planejador estao baratos.

**Alternativa A - indice incremental invalidado.** `WarehouseIndex` conserva
uma fotografia por colonia, invalida somente o bau alterado por jogador,
deposito, retirada ou quebra, e faz uma verificacao completa em intervalo
longo. Exige teste que compare a fotografia com uma leitura completa apos cada
mutacao relevante.

**Alternativa B - levantamento fatiado.** Um cursor por colonia le uma cota de
baus por ciclo e publica a fotografia apenas quando a rodada completa fecha;
enquanto isso, quem exige certeza recebe `PARTIAL` e espera. E mais simples de
introduzir, mas aumenta a latencia de reagir a recurso novo. Exige GameTest de
obra que recebe material no ultimo bau do cursor.

**Escolha aplicada - B.** `ColonyChestSurvey` le no maximo oito baus por
colonia e marca a rodada como `pending` ate a ultima fatia. Enquanto somente
ha pendencia, `ColonyCycleRunner` decide estritamente pelo estoque observado;
se qualquer bau conhecido estiver inalcançavel, bloqueia a decisao. O conteudo
nao e cacheado entre rodadas e continua vindo de baus fisicos. `StorageGameTest
.aSlicedSurveyPublishesTheLastChestOnlyAfterItsRoundCloses` cobre o nono bau.

## Riscos reproduziveis no log

| Prioridade | Assinatura | Ocorrencias atuais | Leitura |
|---|---|---:|---|
| P1 | `site_sweep_budget_exhausted` | 19 | Busca de lote/recursos termina o orcamento antes de responder. |
| P1 | `cycle_over_tick` | 17 | Ciclo acima de 50 ms; o estoque e a primeira causa medida. |
| P1 | `surface_worker_unreachable` | 7 | Coleta encontrou alvo sem rota praticavel. |
| P1 | `builder_pathing_stalled` | 5 | Construtor manteve alvo sem progresso fisico. |
| P2 | `log_error_line` | 1 | Erro de refmap na carga; precisa isolar mod/origem antes de atribuir ao Village Colony. |

### P1 - Varredura de lotes e recursos

**Alternativa A - cota adaptativa por colonia.** Usar a medicao por fase para
dar mais colunas somente a colonia observada e reduzir a cota apos um ciclo
lento. Preserva cursor e evita que uma colonia grande monopolize a rodada.

**Alternativa B - busca especializada.** Separar a procura de lote, cultivo e
coleta de superficie em cursores independentes, com filtros aritmeticos antes
de qualquer leitura de bloco. Reduz trabalho irrelevante, mas pede mais
invariantes de limpeza de cursor.

**Escolha aplicada - B.** `RingSweep` agora separa cursores geral, de cultivo
e de superficie para o mesmo dono. O scanner de lote ja tinha cursor proprio;
os filtros de coluna continuam antes das leituras de mundo. `RingSweepResumeTest
.separateScanKindsKeepIndependentCursorsForTheSameOwner` prova o isolamento.

### P1 - Caminhos sem progresso

**Alternativa A - validar ponto de apoio antes da reserva.** O planejador
escolhe um ponto de pe navegavel dentro do alcance do bloco e so entao reserva
a tarefa. Evita alocar trabalhador a alvo que nao pode executar.

**Alternativa B - devolucao antecipada com memoria curta.** O watchdog devolve
a tarefa apos poucos ciclos sem avanco e marca somente aquele alvo como
temporariamente ruim; outro alvo pode ser escolhido sem alterar terreno ou
teleportar aldeao.

As duas alternativas devem nascer de GameTests que reproduzam um lote valido
sem ponto de pe e uma coleta de superficie inacessivel. Nao se deve aumentar
timeout para esconder a repeticao.

**Escolha aplicada - A, para obra.** Antes de reservar `BUILD`, a integracao
confirma que o proximo bloco da obra possui ponto de apoio fisico no alcance;
o Core recebe apenas esse predicado e continua sem depender de Minecraft.
`WorkAssignmentTest.aPhysicalPreconditionKeepsAnOtherwiseEligibleBuildAvailable`
e `BuilderApproachGameTest.aBuildIsNotReservedWithoutAnyStandingSpotInReach`
cobrem a regra. `surface_worker_unreachable` permanece aberto: nao foi mudado
sem a reproducao equivalente.

## Inconsistencias documentais encontradas

1. `CLAUDE.md` ainda declarava falha obrigatoria de GameTest em 2026-09-21;
   foi atualizado e a rodada atual passou em 498/498.
2. O topo de `TODO.md` dizia 491 GameTests; foi atualizado e recebeu
   a fila desta auditoria.
3. `STATE.md` declara limite de 150 linhas, mas contem 273. Esta pendencia P2
   ficou aberta para migrar historico sem apagar playtests pendentes.

## Limites desta entrega

Os testes automatizados estao verdes, mas nao substituem o playtest do save.
Ainda precisam de confirmacao manual: entrega antecipada de escadas/troncos
sem consumir materia-prima bruta, recuperacao de trabalhador encalhado,
retomada de obra em relevo e mina seca/aquatica. O perfil Spark informado em
sessao anterior nao foi reaberto nesta rodada porque o endereco nao ficou
acessivel pelo leitor automatizado; as conclusoes de desempenho acima usam as
medicoes textuais do `latest.log` atual.
