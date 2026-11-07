> **Histórico.** Descreve a escrita própria do MAE, que saiu do código em 2026-11-06 (ADR-047).

# MAE — Fase 1: região e escrita segura

Data: 2026-11-02. Versão candidata: 0.4.9. Branch: `codex/mae-region-safety`.
Base: `main` após merge do PR #47, commit `8a8ae09844761566f9b7e2e9fc06c67bac1db29d`.
Escopo e limites: [ADR-044](../decisions/ADR-044-mae-region-safety.md).

## Aprovações e escopo

O autor aprovou região maior supervisionada com aprovação individual de cada obra.
Para elegibilidade, escolheu a opção A: registrar evidência na geração e revalidar
antes de cada escrita; sem prova, bloquear. O marcador `VillageOrigin` continua
descrevendo somente a origem da vila, sem conferir permissão de expansão.

Esta entrega prepara a API de segurança. Não gera ruas, suaviza terreno, implanta
estruturas ou chama obras MAE pelo ciclo de aldeões. Não modifica saves do jogador,
CI, mixins, dependências ou o grafo existente. Após a validação, o autor solicitou
commit, push e atualização do JAR, antes da Fase 2. Integração na `main` e
publicação de release continuam fora desta entrega.

## Entrega local — 02-11-2026

`remapJar` aprovado, sem repetir a bateria de testes. O manifesto interno confirma
`villagecolony` 0.4.9, Minecraft 1.21.1 e Java 21. Build, `downloads/` e instalação
do TLauncher têm SHA-256 `7184B163EB44AE3CBE26F0748DE5632E77A9FBE2D26C03D80A0462C953C1688C`.
O JAR 0.4.8 foi preservado como `.disabled`; nenhum JAR entra no Git.
Esta instalação continua candidata, sem playtest do autor.

## Integração com sistemas existentes

| Responsabilidade | Integração |
| --- | --- |
| Identidade e lote | `Building`; volume inclusivo puro `WorkArea` |
| Prova local | `MaeGenerationEvidence`, attachment Fabric por chunk |
| Origem da vila | `VillageOrigin` preservado; teste demonstra que não basta |
| Proteção | `MaeProtection` reutiliza `BlockProtection`, `BUILDINGS`, `RoadIndex` e células planejadas de `MINES` |
| Escrita MAE | `MaeWorldWriter`; nenhum escritor legado redirecionado nesta fase |
| Persistência | `MaeWorkStore`, seção opcional do `ColonySavedData`, leitura via `ColonySaveReader` |
| Ciclo de vida | `VillageColonyMod` registra evento de geração; orçamento limpa pelo `ServerMemory` |
| Caminhos e terreno | `RoadGraph`/`RoadIndex` e preparação legada permanecem em seus fluxos atuais |
| Testes locais | Suites `unit`/`gametest` já usadas pelo Sentinela, sem execução automática |

O registro persistente continua único. Não há banco paralelo nem cópia de todos
os blocos do mundo. Recibos guardam hashes por seção; plano e histórico guardam
apenas mudanças aprovadas, com limites conservadores.

## Fluxo da API

1. Durante `CHUNK_GENERATE`, registrar a referência dos estados de cada seção
   no Overworld. `CHUNK_LOAD` nunca cria ou atualiza prova.
2. `MaeWorldWriter.of(world).reserve(...)` valida região e prova atual e registra
   reserva supervisionada. Não coloca blocos. Chunks incompletos ou ausentes
   bloqueiam; consultas não solicitam sua carga.
3. `approve(...)` recebe um `Building` e mudanças com estado anterior/posterior.
   Revalida lote e margem; fixa plano, hashes esperados e cursor. Reservar região
   ou selecionar blueprint não substitui essa aprovação.
4. `runSlice(...)` verifica orçamento global do mundo e revalida antes de cada
   escrita. Registra intenção, altera um bloco, verifica impressão posterior,
   confirma histórico e avança cursor. Não usa atualização de vizinhos nesta fase.
5. `pause(...)` interrompe; `resume(...)` exige chamada explícita e nova validação.
   Estado desconhecido, divergência ou intenção não confirmada bloqueiam.
6. Uma obra posterior requer outra aprovação. Só hashes de escritas confirmadas
   na própria região substituem a referência original.

Não existe comando de jogador ou interface de aprovação nesta fase. A chamada
explícita da API representa o ato supervisionado para futura integração.

## Segurança e compatibilidade

- A inspeção protege o volume inteiro e sua margem de dois blocos. Protege
  também bounding boxes de estruturas Vanilla, incluindo espaços entre peças.
- Fluidos, cultivos, árvores, BlockEntities e materiais fora da lista passiva
  bloqueiam. Passagens de mina e ruas registradas continuam reservadas.
- Região de outra colônia ou outra reserva não pode ser sobreposta. Identidade
  da colônia, altura e dimensão precisam ser válidas.
- Ausência ou versão desconhecida de prova nunca vira autorização. Payload
  futuro de `maeWork` é preservado opaco; metadados inválidos não executam obra.
- Impressão de seção completa é conservadora: mudança vizinha ou natural pode
  bloquear o lote. Não recapturar prova para contornar a recusa. Coincidência de
  hashes não constitui histórico de autoria de cada bloco.
- Intenção e blocos não possuem transação atômica de disco. Divergência após
  interrupção bloqueia; histórico não executa rollback automático.
- Quatro tentativas e dois milissegundos acumulados por tick são limites entre
  operações indivisíveis. Uma operação pode exceder esse tempo. Reserva e
  aprovação são síncronas, com limites de volume/seções; carga real ainda deve
  ser medida antes de integrar o orquestrador.

## Verificação

O teste inicial `onlyGenerationSuppliesEvidence` falhou com a implementação
ausente, pela causa esperada: `geração não registrou evidência persistente`.
O arquivo local `build/mae-phase1/red.log` registra essa execução.

Os cenários seguintes verificam ausência de autorização retroativa, mudanças
no terreno, chunks desconhecidos, estruturas e referências remotas, proteção
adicionada após aprovação, plano fora do lote, orçamento entre instâncias,
pausa/retomada, progresso recarregado e intenção não confirmada. As fixtures
ficam acima da cobertura de barreiras do GameTest, sem enfraquecer proteções.

Comandos focados:

```powershell
.\gradlew.bat test --tests com.villagecolony.core.construction.model.WorkAreaTest --tests com.villagecolony.data.save.MaeWorkStoreTest --tests com.villagecolony.data.save.ColonySavedDataTest --tests com.villagecolony.architecture.ArchitectureRulesTest
.\gradlew.bat runGametest -PgametestOnly=MaeSafetyGameTest
py -3 scripts/rules_to_readme.py --check
git diff --check
```

Passaram 35 unitários focados (geometria, store, save e ArchUnit), seis testes
de dependências/memória e 25 GameTests (dez MAE e 15 de origem da vila/migração
do grafo). O total inclui o caso adicional de preservação de campos do envelope,
validado novamente com as correções da revisão. Logs locais em `build/mae-phase1/`:
`review-fixes.log` (35 unitários), `gametest-final.log` (dez MAE),
`compatibility.log` (seis unitários e 15 GameTests). Relatórios Gradle
podem ser sobrescritos pela próxima chamada. Não foi executado PIT ou a bateria
completa para repetir a validação do PR #47. Revisão da branch concluída.

## Próximo gate

A revisão identificou dois custos eliminados na própria fundação: a geometria
da mina agora é calculada uma vez por inspeção, e a consulta de região copia
somente a entrada selecionada. `runSlice` verifica esgotamento antes de acessar
o store e contabiliza o tempo da consulta. A revisão das correções não encontrou
outra falha acionável; latência real não foi medida por essa revisão.

Apresentar Fase 2 e obter aprovação antes de integrar preparação de terreno.
A fundação não demonstra qualidade visual de smoothing, conectividade real de
ruas, recuperação de aldeões ou desempenho em um playtest. Esses problemas
permanecem nos planos próprios; não são declarados corrigidos por esta fase.
