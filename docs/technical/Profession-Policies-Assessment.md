# Avaliação — Políticas de profissões (2026-10-02)

## Resultado

A feature entrega uma política central, persistida por mundo, para as oito
profissões reais: Mineiro, Lenhador, Pedreiro, Fundidor, Carpinteiro,
Fazendeiro, Pastor e Construtor. O Mod Menu é apenas a interface opcional;
o servidor continua sendo a fonte de verdade.

## Contratos verificados

| Item | Resultado | Evidência |
|---|---|---|
| Valores padrão preservam o comportamento anterior | aprovado | ordem histórica é projeção da ordem completa; sem teto e raio automático |
| Profissão desabilitada não recebe novo trabalhador | aprovado | `ProfessionPolicySetTest.disabledProfessionIsNotAssignedToNewWorker` |
| Limite máximo redistribui a vaga | aprovado | `maximumWorkerCapMovesTheNextGrowthSlotForward` |
| Desabilitar produtor redistribui vagas futuras | aprovado | `disabledProducerReleasesItsGrowthSlotsToTheNextEnabledProfession` |
| Limites e aplicabilidade de entrada são validados | aprovado | testes de limites e de raio não aplicável |
| Persistência por mundo | implementada | `ProfessionPolicySavedData` no `PersistentState` do Overworld |
| Sincronização e autorização | implementada | snapshot no ingresso/abertura; atualização exige operador nível 2 |
| Servidor dedicado sem Mod Menu | aprovado | `runGametest` carregou o mod no lado servidor sem Mod Menu |
| Build e regressões automatizadas | aprovado | `gradlew build` (1.040 unitários); GameTests 434/434 |

## Limitações conhecidas

1. Desativar uma profissão impede novas atribuições, mas não remove nem
   interrompe trabalhadores já atribuídos. Realocação automática teria efeito
   de balanceamento mais amplo e ficou fora do escopo desta primeira versão.
2. A interface mostra controles de alteração para jogadores sem permissão; o
   servidor recusa a mudança e reenvia a política autoritativa. Uma futura
   melhoria pode sincronizar a permissão e desabilitar esses botões no cliente.
3. Os controles de busca se aplicam somente aos três fluxos auditados:
   Lenhador, Fazendeiro e Pastor. Os demais não receberam raio artificial.
4. Ainda falta playtest visual manual do Mod Menu e do ciclo de salvar,
   reiniciar o mundo e reconectar. A cobertura automatizada confirma carga do
   servidor e regressões, mas não substitui a interação gráfica real.

## Melhorias recomendadas

- Adicionar GameTests dedicados para a leitura do `PersistentState` e para a
  aplicação de raio em cada fluxo de trabalho.
- Mostrar permissão e estado de salvamento no menu do cliente.
- Avaliar uma política opcional e explícita de realocação após desativação,
  com migração segura de trabalhos em andamento.
- Adicionar teste de interface de cliente quando a infraestrutura de
  `fabric-client-gametest` for adotada pelo projeto.
