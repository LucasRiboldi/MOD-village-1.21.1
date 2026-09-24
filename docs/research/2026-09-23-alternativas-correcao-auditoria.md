# Pesquisa comparativa: alternativas para a fila de correções

**Data:** 2026-09-23
**Status:** insumo de decisão, sem alteração de comportamento
**Escopo:** pendências abertas e confirmações em jogo que ainda faltam no `TODO.md`.

A pauta para registrar as escolhas do autor, sem autorizar implementação, está
em [`../technical/Decisoes-de-Correcao-2026-09-23.md`](../technical/Decisoes-de-Correcao-2026-09-23.md).

## Leitura e limites

Esta é uma comparação arquitetural, não uma alegação de que outro mod esteja livre
de falhas. Código e documentação demonstram padrões implementados; issues públicos
demonstram que uma classe de problema foi vista em operação, não que ela exista
neste mod.

As notas usam escalas de 1 a 5:

| Medida | 1 | 5 |
|---|---:|---:|
| Resolução esperada | pouco cobre o sintoma | cobre a causa descrita por contrato |
| Risco sistêmico | alteração local e reversível | altera estado, mundo ou vários subsistemas |
| Testabilidade | depende quase só de save | prova unitário e GameTest |
| Compatibilidade | conflita com regras atuais | preserva regras e saves atuais |

"Certeza" só existe após reprodução vermelha, implementação e testes verdes.
Nesta fase, nenhuma alternativa recebe certeza de correção em jogo. "Recomendada"
significa menor risco para testar primeiro, não aprovação para codificar.

## Fontes externas examinadas

| Fonte | Padrão confirmado | Uso |
|---|---|---|
| [MineColonies: estados de pedido](https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/api/colony/requestsystem/request/RequestState.java) | pedido persistente de `CREATED` a `FAILED` | avaliar fila global |
| [MineColonies: retry](https://github.com/ldtteam/minecolonies/blob/version/main/src/main/java/com/minecolonies/api/colony/requestsystem/resolver/retrying/IRetryingRequestResolver.java) | tentativas e atraso máximos | backoff controlado |
| [Documentação MineColonies](https://minecolonies.com/wiki/systems/request/) | logística central por armazém e entregador | comparar com baús locais |
| [Issue MineColonies 11422](https://github.com/ldtteam/minecolonies/issues/11422) | mineiro `NEEDS_ITEM` sem requisito visível | risco de estado global opaco |
| [Issue MineColonies 10802](https://github.com/ldtteam/minecolonies/issues/10802) | construtor pode não pedir material durante limpeza | risco de fluxo central incompleto |
| [Structurize issue 838](https://github.com/ldtteam/Structurize/issues/838) | tag não fabricável bloqueia obra | validar projeto e rota antes de abrir |
| [Millenaire: fases](https://github.com/Leviaria/Millenaire/blob/main/src/main/java/org/millenaire/building/PlacementPhase.java) | `DELETION`, `STRUCTURE`, `DEPENDENT` e `SPECIAL` | ordem de dependência |
| [Millenaire: tarefa](https://github.com/Leviaria/Millenaire/blob/main/src/main/java/org/millenaire/building/ConstructionTask.java) | cursor, reserva e três falhas antes de bloquear | limite local e estado terminal |
| [Millenaire: scheduler](https://github.com/Leviaria/Millenaire/blob/main/src/main/java/org/millenaire/goal/GoalScheduler.java) | cooldown, backoff ocioso e watchdog | alternativa de espera |
| [MineFortress: iterador](https://github.com/remmintan/minefortress/blob/master/src/core/java/net/remmintan/mods/minefortress/core/automation/iterators/ResetableIterator.java) | cursor reiniciável | trabalho incremental |

## Evidência local e simulações

Foram executados, sem editar código:

```text
./gradlew.bat --no-daemon test --tests WorkAssignmentTest --tests WorkerTest \
  --tests MineTest --tests MineShaftTest --tests MinerWorkLifecycleTest \
  --tests ActivityLogTest --tests HousePlansTest
BUILD SUCCESSFUL, 60 testes aprovados, 12 s
```

A bateria confirma contratos atuais, entre eles: escada compartilhada única,
quatro ramais após a área comum, término finito, alternância de casas,
telemetria estruturada e a segunda passagem que ignora o descanso. Ela não
prova alternativas que ainda não existem.

Três modelos discretos também foram executados. Eles não são GameTests e não
simulam pathfinding, chunks ou save.

| Modelo | Alternativa A | Alternativa B | Resultado |
|---|---|---|---|
| Descanso de 4 ciclos, horizonte de 12 | filtro: 8 atribuições após a janela, 0 reatribuições precoces | retry: 3 registros | A elimina reatribuição; B cria estado |
| Planta de 5 passos, 1 dependência | fases: 5/5, 0 aberta | pular: 4/5, 1 aberta | pular não pode concluir |
| 36 colônias | round-robin de 4, cobertura em até 9 ticks | varredura total, pico 36 | orçamento reduz o pico por fator 9 |

A confirmação no mundo continua necessária para P1.3, P1.4, P2.1 e os itens
marcados como playtest no `TODO.md`.

## Mapa de decisão

| Grupo | Itens | Recomendação |
|---|---|---|
| 1. Agendamento | E42, E43 | A1: elegibilidade única |
| 2. Construção e material | P0.10, P0.11, P1.2, bloco sem apoio, espera e reparo | A2: dependência e validação |
| 3. Mina | P1.3, E44/E45 residuais, carvão | A3: ciclo finito físico |
| 4. Estoque | P1.1, E38, E3, E21 | A4: consumidor explícito |
| 5. Lote e estrada | P0.9, C3, S6/S4, scanner | A5: políticas separadas |
| 6. Desempenho | P2.1, E41 | A6: orçamento justo |
| 7. Diagnóstico | P1.4, logs | A7: transições amostradas |
| 8. Save e estruturas | P0.8, P1.4, migrações | A8: migração única |
| 9. Cobertura | pastor, fundidor, E4, E21 | A9: cenários críticos |
| 10. Release | `STATE.md`, JAR, histórico e `ConstructionService.forget` | A10: manifesto e auditoria de restos |
| 11. Crescimento | Village Growth Planner | A11: inventário antes de score |

## 1. Agendamento: E42 e E43

**Contrato atual.** `WorkAssignment.takeOneTask` respeita `rest` na primeira
passagem e reserva precisamente a capacidade descansando na segunda. E42 ainda
não tem reprodução: falta roça impossível, casa disponível e duas passagens.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A1. Predicado único:** descanso impede reserva; tarefa aguarda expiração ou outro profissional | 5 | 2 | 5 | 5 | **Recomendada** |
| **B1. Retry/backoff:** 1/2/4 ciclos e terminal `BLOCKED`/`FAILED` | 4 | 4 | 3 | 3 | somente se A1 causar starvation |

**A1:** preserva o sentido de `rest` e não cria estado persistente. O custo é
quatro ciclos sem produtividade se só há um profissional. **B1:** segue o
scheduler de Millenaire e reduz consultas, mas cria identidade, expiração,
limpeza e possível persistência de cada retry. Os issues MineColonies tornam
esse custo grande para o problema atual.

**Provas:** GameTest E42; unitário de quatro ciclos sem reatribuição; cenário
com segundo mineiro disponível.

## 2. Construção, dependências e material

**Contrato atual.** `BuilderWork` trata `WAITING_RESOURCES` como estado previsto.
A decisão pendente é `ladder`/`wall_torch` quando falta apoio físico.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A2. Fases e validação prévia:** estrutura, dependente e especial; rejeita projeto sem rota/suporte | 5 | 2 | 5 | 5 | **Recomendada** |
| **B2. Pular bloco impossível:** registrar lacuna e concluir parcialmente | 2 | 3 | 3 | 2 | Não recomendada |

**A2:** adota a separação de Millenaire e transforma o caso Structurize em
erro explícito, não espera infinita. Exige uma tabela de dependências e um
terminal `INVALID_BLUEPRINT`. **B2:** é simples, mas uma casa incompleta parece
concluída e o reparo pode tentar recriar o bloco omitido.

**Provas:** testes vermelhos para escada sem apoio e tocha sem face sólida;
GameTest de projeto inválido sem ocupar lote ou abrir pedido impossível.

## 3. Mina, portal e minério

**Contrato atual.** `Mine` já tem boca, espiral compartilhada, quatro ramais
após o trecho comum e `EXHAUSTED`. `archRaised` separa portal nunca criado de
portal quebrado. Falta playtest com o JAR atual.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A3. Estados finitos persistidos:** `OPEN → DIGGING → LEVEL_DONE → DEEPENED/EXHAUSTED → ABANDONED`; avanço só por bloco removido | 5 | 2 | 5 | 5 | **Recomendada, em grande parte presente** |
| **B3. Replanejar/reconstruir boca e ramais ao falhar** | 2 | 5 | 2 | 1 | Rejeitada |

**A3:** preserva a geometria e torna o fim terminal. A preferência por veios,
incluindo carvão, entra como seleção de alvo em `DIGGING` sem mudar propriedade
da mina. Exige migração rigorosa e regra explícita para nova mina no lado
oposto. **B3:** parece recuperar mundo alterado, mas é a família do portal
infinito e pode escavar em duplicidade.

**Provas:** portal removido; espiral única de dez; quatro ramais de dez; coleta
limitada; `EXHAUSTED` sem recriar boca; nova mina no lado oposto. No save, usar
o SHA-256 `62FCECB70ACF7864DA852F707A1ADBA2197FEC8613A952A2E691DE7BE13BF1EF`.

## 4. Resíduos, baús e rotas: E38, E3, E21

**Contrato atual.** Baús são fontes físicas. O fallback só cria recurso quando
não há rota. E38 ainda não decidiu o destino de varas, maçãs e mudas.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A4. Consumidor por item e reserva de capacidade:** replantar, usar, guardar e parar coleta quando cheio | 5 | 2 | 4 | 5 | **Recomendada** |
| **B4. Armazém global e árvore genérica de pedidos** | 4 | 5 | 2 | 2 | Postergar |

**A4:** cada item tem origem, destino e limite físico observáveis, preservando a
regra do baú do construtor. Exige catálogo crescente. **B4:** centraliza
transferências como MineColonies, mas importa um subsistema inteiro de
atribuição e recuperação, com os riscos operacionais vistos nas issues.

**Provas:** matriz `origem → consumidor → limite → overflow`; GameTest de baú
cheio sem destruição/drops repetidos; reproduzir E21 antes de atribuí-lo ao
estoque.

## 5. Lote, estrada, centro e scanner

**Contrato atual.** O scanner tem cursor e orçamento. C3 é adoção incompleta de
camas; S6/S4 é diferença entre regra de estrada e lote. Não são causa
confirmada da obra condenada.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A5. Políticas separadas e métricas por motivo** | 4 | 2 | 5 | 5 | **Recomendada** |
| **B5. Régua única e terraplanagem/estrada automática** | 3 | 5 | 2 | 2 | Não adotar sem ADR |

**A5:** mede `view not provably complete`, separa estrada de lote e permite
dividir `BuildSiteScanner` por responsabilidade. Não produz solução visual
imediata e pode concluir que as políticas devem continuar diferentes. **B5:**
é visualmente uniforme, mas altera mundo do jogador, amplia o raio de impacto e
pode reabrir E2/E46. Não há comportamento Vanilla de terreno carregado para
copiar com segurança.

**Provas:** GameTest de grupo parcial de camas e de terreno divergente; contagem
de motivos antes/depois; ADR para qualquer alteração fora da obra.

## 6. Desempenho e endurance: P2.1 e E41

**Contrato atual.** A meta de 50 ms só pode ser medida no save. E41 é lacuna de
cobertura, não regressão demonstrada.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A6. Orçamento por colônia e round-robin com métricas por fase** | 5 | 2 | 4 | 5 | **Recomendada** |
| **B6. Cache global agressivo ou acesso assíncrono ao mundo** | 3 | 5 | 1 | 2 | Rejeitada para 1.21.1 |

**A6:** no modelo, pico relativo cai de 36 para 4 e cada colônia roda em até
nove ticks. É compatível com cursor explícito. A latência aumenta de forma
previsível e precisa aparecer como progresso, não travamento. **B6:** pode
melhorar média em mundos grandes, mas cache invalida com jogador e acesso
assíncrono ao mundo Fabric não é solução segura.

**Provas:** 36 colônias com semente fixa, assert de justiça e trabalho por fase;
no save, mediana/p95/máximo de `Colony cycle took`.

## 7. Telemetria: P1.4

**Contrato atual.** `VC_ACTIVITY version=1` já usa profissão, atividade,
resultado e motivo controlado, sem UUID, posição ou texto livre. O relatório
encontra candidatos, não defeitos confirmados.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A7. Transições com sessão, contador e amostragem de progresso** | 5 | 2 | 5 | 5 | **Recomendada** |
| **B7. Traço por tick e persistência de log no save** | 2 | 4 | 2 | 2 | Não recomendada |

**A7:** mantém baixo volume e privacidade, distinguindo sem tarefa de sem
avanço. Requer transições simétricas em cada nova atividade. **B7:** ajuda em
falha rara, mas traz I/O, posições e a falsa ideia de que log é estado.

**Provas:** parser para `WAITING → RECOVERED` e falhas; versão desconhecida;
ausência de identificadores pessoais; playtest P1.4 com JAR atual.

## 8. Save, BigHouse e estruturas

**Contrato atual.** BigHouse, baús seguros e exclusão da BigHouse estão
automatizados; P0.8/P1.2/P1.4 ainda pedem confirmação no save. Portal é o
precedente: uma estrutura destruída não pode voltar por recuperação contínua.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A8. Migração única, idempotente e auditável; terminal explícito** | 5 | 2 | 5 | 5 | **Recomendada** |
| **B8. Auto-reparo em todo ciclo/carregamento** | 2 | 5 | 2 | 1 | Rejeitada |

**A8:** permite testar save anterior, atual e já migrado, sem tocar no mundo do
jogador após a migração. Aumenta matriz de versões. **B8:** parece recuperar
esquecimentos, mas repete portal infinito e pode duplicar baús.

**Provas:** três fixtures NBT por regra; carregar duas vezes sem novo bloco;
playtest no save real sem cancelamento manual.

## 9. Cobertura: pastor, fundidor, E4, E21 e E41

**Contrato atual.** Pastor tem dois GameTests e mineiro tem dezenas. E4 e E21
são suspeitas, não causas conhecidas.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A9. Cenários críticos, sementes fixas e playtest controlado** | 5 | 2 | 5 | 5 | **Recomendada** |
| **B9. Endurance/randômicos genéricos primeiro** | 3 | 3 | 2 | 4 | Complementar |

**A9:** cobre profissão sem material, inventário cheio, alvo removido, água,
cancelamento e retomada. É depurável. **B9:** é útil para vazamento e custo
crescente depois das invariantes, mas pode mascarar causa e gerar instabilidade.

**Provas:** matriz pastor/fundidor antes de elevar autonomia; endurance mede
tarefas abertas, jobs vivos, itens perdidos e p95 a cada marco.

## 10. Documentação e release

**Contrato atual.** `STATE.md` excede seu teto e contém histórico que conflita
com estado vivo. Commit, hashes, testes e três destinos do JAR precisam ser
verificáveis juntos.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A10. Manifesto gerado: commit, hashes, testes e destinos** | 5 | 2 | 5 | 5 | **Recomendada** |
| **B10. Reconciliação manual periódica** | 2 | 3 | 1 | 3 | Temporária |

**A10:** reduz divergência entre `build/libs`, `downloads` e `.minecraft/mods`;
mantém o estado curto e referencia relatórios datados. Exige tooling e definição
de quais testes bloqueiam publicação. **B10:** não requer automação agora, mas
é a origem provável da deriva documental atual.

**Provas:** script falha com hash divergente; dry-run falha sem GameTests finais
ou cópia do JAR.

## 11. Village Growth Planner

**Contrato atual.** Está especificado, não implementado. O TODO pede confirmar
E45 e E46 antes de construir por cima. A alternância de casas deve sobreviver
até troca deliberada.

| Alternativa | Res. | Risco | Teste | Comp. | Resultado |
|---|---:|---:|---:|---:|---|
| **A11. `VillageInventory`, depois `NEED_SCORE` puro e testável** | 5 | 3 | 5 | 4 | **Recomendada após playtests** |
| **B11. Trocar imediatamente todo planejador por score global** | 3 | 5 | 1 | 1 | Não iniciar |

**A11:** separa observação de decisão e permite comparar plano antigo/novo,
conservando casa → não residencial na transição. **B11:** oferece salto maior,
mas mistura diagnóstico, crescimento, lote, recursos e reparo numa regressão
difícil de isolar.

**Provas:** ADR do estado; testes de empate, inventário incompleto, primeira
casa e alternância; GameTest de vila existente.

## Ordem sugerida para decisão

1. Decidir A1 ou B1 para E43 junto do GameTest E42. É a única escolha de
   produto que bloqueia uma correção P0 aberta.
2. Aprovar A2 para blocos dependentes, evitando espera infinita ou obra parcial.
3. Rodar playtests P1.3/P1.4/P2.1 com o JAR atual antes de tocar mina, migração,
   baús ou desempenho.
4. Decidir A4, A5, A6, A7 e A9 somente após reproduzir os respectivos casos.
5. Tratar A10 e A11 depois: melhoram governança e evolução, não corrigem o
   travamento em jogo.

## Limites de decisão

- Não transformar o mod em MineColonies nem copiar a árvore de pedidos.
- Não recriar portal, baú ou estrutura destruída para "recuperar" falha.
- Não unificar terreno de rua e lote sem medição e ADR.
- Não chamar candidatos de log de defeitos confirmados.
- Não afirmar playtest verde sem usar o JAR correto no save.
