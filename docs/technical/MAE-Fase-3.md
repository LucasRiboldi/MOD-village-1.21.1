# MAE — Fase 3: auditoria inicial de estruturas e acesso

Data: 2026-11-03. Base: `main` em `0a7590fc` (PR #49). Esta auditoria não ativa a Fase 3.

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
