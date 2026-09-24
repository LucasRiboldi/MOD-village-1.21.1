# Testes comportamentais

Esta pasta documenta a rede de segurança do Village Colony: o aldeão deve
continuar trabalhando depois de uma alteração, e a vila deve preservar as
regras de construção, recursos e persistência.

## Evidencia atual de 2026-09-22

| Suíte | Comando | Resultado |
|---|---|---|
| Unitários Java | `./gradlew.bat test --rerun-tasks` | 966 testes, 103 suítes, 0 falhas |
| Testes Python | `python -m unittest discover -s tests` | 76 testes, 0 falhas |
| Fabric GameTests | `./gradlew.bat runGametest --rerun-tasks` | 410 testes, 408 aprovados, 2 falhas |

Falhas obrigatorias atuais:
`FarmPlanGameTest.thenextturnafterahouseisnonresidential` e
`SurfaceGatheringGameTest.farmerGathersDirtOutsideTheSoilProtectedRadius`.
Elas impedem chamar a bateria completa de verde. A primeira deve ser
reproduzida com uma fixture de vila isolada antes de alterar `HousePlans`; a
segunda precisa estabilizar o registro da entidade no `ServerWorld` antes de
alterar coleta ou timeout.

Há também uma falha histórica intermitente documentada em
[`known-failures.md`](known-failures.md): perda de uma entidade criada fora da
arena entre `spawnEntity` e o primeiro tick. Ela não deve ser confundida com a
falha atual de alternância.

## Arquitetura coberta

O projeto separa `core/`, que contém modelo e regras sem Minecraft, de
`fabric/`, que integra Brain, mundo, baús, blocos, eventos e GameTests.
`DependencyRuleTest` protege essa fronteira. Os testes atuais cobrem:

- adoção, abandono, sobreposição e persistência de colônias;
- atribuição, crescimento e compatibilidade das profissões;
- demanda, estoque, receitas e materiais de blueprint;
- seleção de lote, volume vertical, janela livre de 25 blocos e projetos
  pendentes;
- `BigHouseMOD`, camas, baús, fundação e exclusão do catálogo profissional;
- obras concluídas, abandonadas, reparo cíclico e cancelamento por Tocha das
  Almas;
- mina, aproximação por degraus, areia que cai, água, coleta e iluminação;
- lenhador, viveiro de dez árvores, fazendeiro, fundidor, pedreiro e carpinteiro.

## Funções verificadas

As oito funções operacionais são `MINER`, `LUMBERJACK`, `MASON`, `SMELTER`,
`CARPENTER`, `FARMER`, `SHEPHERD` e `BUILDER`. `BREEDER` é aceito somente em
saves antigos e migrado para `SHEPHERD`; `MANUFACTURER` não é válido.

Na fundação da vila, `BigHouseMOD` recebe seis titulares: mineiro, lenhador,
pedreiro, fundidor, pastor e construtor. Agricultor e carpinteiro continuam
profissões completas, com atribuição e tarefas próprias, mas não recebem cama
ou baú fundacional dentro da casa; entram no crescimento normal, conforme a decisão registrada em
[`ADR-020`](../decisions/ADR-020-reparo-ciclico-e-viveiro-da-vila.md).

## Lacunas que continuam abertas

1. Corrigir a alternância `casa -> não residencial -> casa -> outro tipo` no
   `FarmPlanGameTest` sem permitir duas casas consecutivas.
2. Repetir a bateria em CI/Linux para investigar a instabilidade de spawn do
   fundidor antes de considerar a rede verde.
3. Criar testes de endurance para muitos ciclos e muitos aldeões.
4. Criar regressões de deadlock entre fazendeiro, produtor de materiais e
   construtor.
5. Confirmar em save real a fundação, a retomada de obras, a frente arenosa do
   mineiro, o viveiro e a sequência de construções.

## Regras para novas regressões

- Reproduzir a falha antes da correção e manter a reprodução como teste.
- Isolar estado estático, relógio do mundo e entidades entre cenários.
- Registrar motivo de espera, alvo, setor, inventário e número de ticks quando
  um aldeão não progride.
- Não aumentar `tickLimit` ou orçamento apenas para silenciar uma falha.
- Rodar `test`, `build`, `runGametest` e os testes Python proporcionais ao
  escopo da mudança.
- Se a alteração tocar `fabric/`, incluir GameTest e declarar o que ainda exige
  playtest no mundo.

## Relatórios

- [`known-failures.md`](known-failures.md): falhas medidas e ainda abertas.
- [`../../TODO.md`](../../TODO.md): backlog canônico.
- [`../technical/Project-Audit-2026-09-21.md`](../technical/Project-Audit-2026-09-21.md):
  auditoria técnica completa e nota do projeto.
