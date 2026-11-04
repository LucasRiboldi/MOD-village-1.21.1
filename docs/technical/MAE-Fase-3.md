# MAE — Fase 3: auditoria inicial de estruturas e acesso

Data: 2026-11-03. Base da auditoria inicial: `main` em `0a7590fc` (PR #49).
A retomada e a matriz abaixo registram a conferência posterior ao PR #50.
Esta auditoria não ativa a construção automática da Fase 3.

## Sistemas existentes

- `Blueprint` guarda blocos, posição relativa, dimensões e propriedades. `doorSide()` escolhe a parede mais próxima da primeira porta; não representa um ponto completo de acesso.
- `StructureBlueprintReader` lê o template e omite a metade superior dos blocos duplos; a colocação da segunda metade é responsabilidade de `BlockShaping`. A preservação de dados de block entities precisa ser confirmada por cenário específico.
- `BlueprintPlacement` orienta a planta em direção à rua. `Corridor.doorOf` deriva uma saída da porta e `WorkPath` a usa para planejar o corredor de uma obra existente.
- `ConstructionProject` mantém os blocos restantes e o estado; `ConstructionState.COMPLETED` já representa a conclusão. É necessário verificar a persistência e o momento de consumo desse estado antes de conectá-lo a um novo caminho.
- `ColonyModels` resolve modelos próprios e substituições. `VillageStructures` mantém o catálogo por estilo; uma biblioteca MVP deve reutilizá-los, sem duplicar a fonte dos modelos.
- `MaeTerrainPreparation` e `MaeWorldWriter` preparam e aprovam alterações em regiões elegíveis. A integração ao construtor de edifícios ainda não existe; o writer atual não deve ser presumido capaz de colocar todas as peças da estrutura.

## Pontos a resolver

1. `Corridor.doorOf` usa a altura da origem da obra, não o deslocamento vertical da porta. `WorkPath` também passa a altura da origem para a busca de chão. Isso é risco para plantas com porta acima da origem; falta reprodução que comprove o efeito em um modelo real. Alterar o corredor legado globalmente pode mudar vilas existentes.
2. Confirmar a orientação real da porta, a metade inferior, o chão de apoio, o espaço livre e a saída acessível nas quatro rotações. A proximidade da parede, sozinha, não comprova acessibilidade.
3. Selecionar e verificar modelos pequenos dos biomas suportados, incluindo estruturas próprias e dados de block entities, antes de compor uma lista MVP.
4. Projetar a sequência de reserva, preparação de terreno, construção, conclusão e disponibilização de acesso, com revalidação e retomada após recarga. Nenhuma escrita pode ultrapassar a região comprovadamente elegível ou ignorar proteção e mudanças posteriores do jogador.
5. A Fase 2 limita seu levantamento de desempenho a uma fixture sintética. `TODO.md` exige medição em carga real e playtest antes da orquestração automática. Manter essa ativação bloqueada até haver evidência.
6. A ligação ao grafo pertence à Fase 4 e permanece sujeita ao gate histórico de navegação na água do plano mestre.

## Validação realizada

Resultados históricos da auditoria inicial; não houve nova execução na etapa documental da matriz.

`./gradlew.bat build --console=plain`: passou em 51 s. O relatório PMD registrou 131 violações; o build não falhou por elas. Esta execução não comprova a Fase 3, GameTests, carga real nem playtest.

`./gradlew.bat runGametest -PgametestOnly=MaeTerrainGameTest --console=plain`:
14 GameTests passaram. Cobrem a derivação do piso de entrada nas quatro rotações
de uma planta sintética e da casa pequena vanilla de planície, e a recusa de porta
inconsistente ou entrada ausente da superfície. Antes da correção, o caso de
porta inconsistente falhou com a recusa incorreta `missing_structure_entry_surface`.
Os testes não demonstram que a saída é desobstruída nem acessível no mundo.

`Proposal.forProject` prepara somente metadados: usa a parede mais próxima da
porta, exige uma porta inferior e uma superfície de entrada e não escreve no mundo.
Não há chamador de produção. O critério de parede não substitui a validação da
orientação real e da circulação após colocação dos blocos.

## Próxima implementação

Definir e validar o contrato de acesso com a planta e o ciclo de construção existentes, cobrindo rotação, apoio, bloqueios, bioma e block entities. Antes de ativar a construção automática, medir a Fase 2 em carga e obter playtest. Não modificar saves nem certificar áreas antigas retroativamente.

## Retomada priorizada pelo autor — 03-11

Esta conferência é documental: não implementa acesso, não executa testes e não altera saves.
O autor priorizou o MAE sobre a continuidade do diagnóstico de navegação e profissões.

### Branches verificadas

- `main` e `origin/main` apontam para `adb7630c` (PR #50, metadados da Fase 3).
- `codex/mae-structures` não apresenta diferença de conteúdo contra `main`; não há implementação pendente a reaproveitar nessa branch.
- A linha atual, `codex/mine-home-search`, termina em `2b8d8f20` e contém quatro commits locais posteriores à `main`, das candidatas 0.4.12–0.4.15. Eles não foram integrados nem publicados nesta conferência.
- Os quatro commits não modificam `MaeTerrainPreparation`, `StructureBlueprintReader` nem `core/construction`. Isso não comprova ausência de interação no ciclo completo da vila.
- Proposta para a implementação seguinte: nova branch `codex/mae-structure-access` a partir da `main` atualizada, preservando a linha da mina. Nenhuma branch foi criada nesta etapa.

### Lacunas do contrato de acesso

| Informação | Fonte existente | Lacuna a resolver |
| --- | --- | --- |
| Identidade, dimensões, peças e orientação | `Blueprint`, `BlueprintBlock` e rotação existente | Reutilizar; não criar catálogo ou persistência paralelos. |
| Porta inferior e cota do piso | `Proposal.forProject` | Há metadados, mas não há prova de circulação física. |
| Direção de saída | `Blueprint.doorSide()` escolhe a parede mais próxima | Validar orientação e passagem dos dois lados; a parede mais próxima não comprova uma saída. |
| Apoio, altura livre e ligação local | Plano de terreno e mundo | Verificar a geometria final, incluindo portas elevadas, escadas e obstáculos; não aceitar somente a presença da célula em `surfaces`. |
| Estruturas sem porta ou com várias entradas | Catálogo atual; armazém sem porta reconhecido pelo leitor | A proposta atual exige exatamente uma porta inferior; ainda não atende ao catálogo inteiro. |
| Dados de block entities | Template NBT original | `BlueprintBlock` não transporta esse payload. `StructureBlueprintReader` lê `final_state` do jigsaw, mas não conserva NBT genérico de cada peça. Colocar o bloco não comprova conservar seus dados. |
| Conclusão da construção | `BuildClosing.complete`, `ConstructionState.COMPLETED` e salvamento existente | O estado é gravado por `ColonySavedData` e lido por `ColonySaveReader`. Reutilizá-lo, mas validar acesso separadamente: a regra atual permite concluir sem mobília. |
| Escrita e retomada | Reserva, diário e writer MAE das Fases 1–2 | Não presumir que o writer de terreno já coloca edifícios; a integração completa continua pendente. |

### Recorte escolhido pelo autor: matriz completa antes da implementação

O autor escolheu a alternativa B: detalhar casas, oficinas, currais, depósitos
sem porta e conexões antes de implementar. A proposta anterior de iniciar pela
casa pequena foi substituída por esta etapa documental.

- [Matriz e contrato A aprovado](MAE-Fase-3-Matriz.md): cobre os cinco estilos,
  os grupos de modelos próprios, portas, portões, vãos abertos, conteúdo NBT
  e requisitos de comprovação futura.
- [Inventário completo](MAE-Fase-3-Inventario.csv): 143 templates vanilla
  autorizados e 18 arquivos próprios, inspecionados sem escrever no mundo.
- O curral grande já tem água, 12 cultivos e quatro tochas no NBT; isso não
  comprova sua construção ou utilização. Depósitos grandes não têm porta;
  portões altos não comprovam uma entrada térrea.
- O autor aprovou o contrato combinado de acesso e a política restrita de
  conteúdo. Pediu quatro portões centrais no curral grande, um por lado, e
  delegou as demais escolhas compatíveis com o jogo. As escolhas do agente
  estão identificadas na matriz. Nenhum modelo foi alterado ou certificado
  fisicamente nesta etapa.
- Em 2026-11-04, o contrato foi formalizado na
  [ADR-046](../decisions/ADR-046-mae-structure-access.md) e detalhado no
  [plano de implementação](../superpowers/plans/2026-11-04-mae-structure-access.md).
  A execução ainda depende dos gates de branch, modelo e testes. O plano
  inclui a lacuna de escrita supervisionada de edifícios, sem presumir que
  o writer de terreno já a resolve. A orquestração automática continua
  dependendo da carga real e do playtest da Fase 2.

A conexão ao grafo continua na Fase 4, com seu gate de navegação preservado.
Esta proposta não aprova migração de saves, suporte genérico a NBT ou criação de um segundo estado de conclusão.

## Implementação parcial autorizada — 04-11

PR #51 integrado após CI; branch `codex/mae-structure-access` criada da main
`9dc8b4f2`. O autor liberou implementação e testes focados com 6.1 Sol/high.
Os registros anteriores deste documento descrevem a auditoria, não o estado atual.

Implementados `BlueprintAccess`, extração de candidatos e vãos declarados do
mesmo template carregado, dimensões declaradas, rotação e prova Fabric local de
circulação. `readForMae` confere propriedades completas, partes secundárias,
quatro rotações e payloads permitidos. A leitura legada conserva seus filtros.
`PaletteStates.planned` interpreta estados estritos sem dependência do pacote
`integration` para `work`; a colocação legada não recebe essa política restrita.

Opção B escolhida pelo autor: quatro portões apenas na variante supervisionada
`villagecolony:mae/large_animal_pen`. Id de projeto, catálogo e capacidade não
mudam. O NBT automático de um portão permanece idêntico. O pastor reconhece
acessos do mesmo cercado e escolhe uma alternativa válida, sem multiplicar a
capacidade 20. Coleta legada ainda teleporta animais: os testes não demonstram
que eles caminham fisicamente pelos portões.

Regressões reproduziram o orçamento contornado por overflow, a exposição dos
quatro portões ao leitor automático e a perda de dimensões no NBT do pórtico.
Corrigiram-se também limites/rotação de templates e a dependência circular
apontada pelo teste de arquitetura. O pórtico mantém as mesmas 164 peças e
geometria: apenas `size` e `pos` passam a listas de inteiros compatíveis com o
leitor Minecraft. Provas e recusas por modelo estão na matriz, seção 7.

### Execuções finais focadas

```powershell
.\gradlew.bat test --tests '*BlueprintTest' --tests '*BlueprintRotationTest' --tests '*ShapeStatesTest' --tests '*ArchitectureRulesTest' --console=plain
.\gradlew.bat runGametest '-PgametestOnly=BlueprintReader,MaeStructureAccess,MaeStructureModel,MaeTerrain,PenModel,PenAccess,ShepherdPens,ShepherdHerding,PlanStates' --console=plain
```

Ambas passaram; contagens atuais em `STATE.md`. Logs locais em
`.gradle/mae-phase3-units-architecture-green.log` e
`.gradle/mae-phase3-focused-final.log`. As novas classes estão registradas em
`fabric.mod.json`; as suites `unit` e `gametest` do Sentinela já as descobrem.
Não foi criado outro runner nem executada a bateria completa do Sentinela.

### Writer e fluxo supervisionado — 04-11 (0.4.16)

O contrato `structure_v3` está na ADR-046. `MaeStructureExecution` aprova,
escreve, conclui e retoma; `MaeStructureAccess.beginFor` deriva circulação e
destino; `MaeStructureSite` conduz terreno → acesso → obra. Resultado por
modelo na matriz, seção 7.

```powershell
.\gradlew.bat runGametest "-PgametestOnly=Mae" --console=plain
.\gradlew.bat test --tests "com.villagecolony.architecture.*" --tests "com.villagecolony.data.save.MaeWorkStoreTest" --console=plain
```

Passaram 105 GameTests MAE e 23 unitários. Mutações na retirada do baú, na
falha de acesso, na classificação da retomada, na checagem de colônia, no
filtro da circulação, na prova de terreno plano e na rotação foram mortas.
Bateria completa não executada.

### O que ainda impede concluir a fase

- Conteúdo funcional e circulação individual dos demais modelos antes de habilitá-los.
- Lote e origem reais e um disparo supervisionado em jogo.
- Travessia controlada do pastor e dos animais; coleta legada por teleporte não é essa prova.
- Carga real e playtest da Fase 2 antes de qualquer orquestração automática.

`PersistentState.markDirty` agenda salvamento; não garante transação atômica
entre inventário, blocos e diário. A próxima integração precisa registrar consumo
antes das peças dependentes e bloquear ambiguidade, sem presumir durabilidade de
um simples round-trip NBT. O detalhamento de persistência deve anteceder código.
Nenhum save do jogador foi alterado. A candidata 0.4.16 é a primeira versão com JAR desta fase.
