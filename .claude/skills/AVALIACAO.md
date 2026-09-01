# Avaliação das skills — 2026-08-29 (estendida em 2026-09-01)

Registro honesto do que foi testado, o que passou e o que **não** se sustentou.
Está aqui para que uma sessão futura não refaça o trabalho nem repita a alegação
errada.

## Índice das rodadas

| Rodada | Data | O quê | Skill(s) | Modelo | n por condição |
|---|---|---|---|---|---|
| 1 | 2026-08-29 | 4 casos de conhecimento (eval-0..3) | `minecraft-villager-systems`, `fabric-development` | Opus 5 | 1 |
| 1 | 2026-08-29 | `rodada-2-processo` (contador de árvores) | `fabric-development`, `minecraft-villager-systems` | Opus 5 | 1 |
| 2 | 2026-09-01 | eval-4/eval-5 (novos) | `minecraft-code-research` | Opus 5 | 1 |
| 3 | 2026-09-01 | eval-4 repetido | `minecraft-code-research` | Haiku 4.5 | 3 |
| 4 | 2026-09-01 | eval-5 repetido | `minecraft-code-research` | Haiku 4.5 | 3 |

## O que falta — dificuldade e importância

| Item | Dificuldade | Importância | Por quê |
|---|---|---|---|
| Repetir eval-4/eval-5 em Opus 5 (hoje n=1) | Baixa — mesma mecânica já rodada, só repetir | Média | Confirma se o empate 12/12-estilo se sustenta com variância, como já foi feito pro Haiku |
| Testar os 4 casos originais (eval-0..3) em Haiku | Baixa/Média — mecânica simples, mas 4 casos × repetição é volume grande (até ~24 rodadas pra n=3 completo) | **Alta** — é a base do resultado mais citado ("empate 12/12") e nunca foi testada em modelo menor nem repetida | — |
| Repetir `rodada-2-processo` (Opus 5 ou Haiku) | **Alta** — precisa clone git isolado (cuidado com paths longos no Windows, já bateu nisso uma vez) + rodar `gradlew build` de verdade, muito mais lento que a rodada de conhecimento | **Alta** — é a única rodada que mede disciplina de processo real (build, regressão, honestidade), não só conhecimento de API | — |
| Confirmar a hipótese "with_skill investiga mais o código real do projeto" (achada em eval-5, 2 de 3) | Média — precisa repetir em `eval-4` e nos casos originais pra ver se é geral ou específico do prompt | Média-alta — se confirmar, é o achado mais forte e citável de toda a avaliação | — |
| Gatilho de migração de versão do Minecraft | N/A — depende de evento externo (mudar `minecraft_version` no `gradle.properties`) | Baixa agora, alta quando acontecer | Já documentado o que fazer (`evals/README.md`); não há nada a rodar hoje |
| Mecanismo de captura de `total_tokens`/`duration_ms` pra subagentes nomeados | Desconhecida — pode ser limitação da plataforma, não do projeto | Média — afeta só a comparação de custo, não invalida os achados de qualidade/factuais já registrados | Sem isso, toda rodada futura mede duração só por relógio de parede |
| Robustez de `grade-processo.py` para novos casos | Baixa, se/quando existirem novos casos de processo | Baixa hoje — o script já tem os ajustes de 2026-08-29 e a filosofia do projeto é sempre ler à mão mesmo assim | — |

## Veredito (rodada 2026-08-29)

| Camada | Resultado |
|---|---|
| Estrutura | ✅ aprovada |
| Precisão factual | ✅ aprovada **após 3 correções que o teste encontrou** |
| Comportamento (conhecimento) | ⚖️ empate — 12/12 × 12/12 |
| Comportamento (processo) | ⚖️ empate — 9/9 × 9/9 |

**Nenhuma vantagem de qualidade foi medida.** As skills custaram ~14% mais
tokens (221k × 194k) na tarefa de implementação.

## O que a camada factual encontrou

Esta foi a única camada que se pagou. Conferir cada `[FATO]` contra o jar, em vez
de contra a memória:

1. **`setTaskList` tem cinco sobrecargas, não três.** O erro nasceu do comando —
   `grep -m3` parava no terceiro resultado. O número veio da ferramenta, não do
   jogo.
2. **Faltava metade do fato.** `setTaskList` acrescenta a lista de tasks
   (`computeIfAbsent` + `Set.add`), mas `requiredActivityMemories` usa `Map.put`
   — as memórias exigidas da Activity são substituídas.
3. **Exemplo redundante.** `.maxCount(1).maxDamage(180)`: `maxDamage` já seta
   `MAX_STACK_SIZE = 1`. **Quem apontou foi o agente de baseline, sem skill.**

Bônus: `getWorldChunk` também devolve `null` fora da thread do servidor.

## Como os testes foram feitos

**Rodada 1 — conhecimento.** 4 perguntas de uma tacada (WALK_TARGET, profissão
vs trades, POI/block states, item simples), com e sem skill, 12 critérios.

**Rodada 2 — processo.** Tarefa de implementação real (contador de árvores por
colônia, persistido, logado no stop), em clones isolados do repositório. 9
critérios lidos do código produzido e do relatório de entrega — nenhum media
conhecimento de API.

A rodada 2 tinha uma armadilha deliberada: `static Map<UUID, Integer>` resolve o
pedido, compila e perde tudo no restart. **Nenhum dos dois caiu nela.**

## O baseline fez, sem skill nenhuma

- rodou `./gradlew build` de verdade (BUILD SUCCESSFUL em 4m26s)
- rodou testes de unidade e `runGametest`
- integrou ao `ColonySavedData` existente, no mesmo arquivo
- tratou save antigo (chave ausente → 0) e save corrompido (total negativo)
- atualizou gametest, testes de unidade e o `Save-Data-System.md`
- escreveu seções "o que rodou de verdade", "o que **não** foi verificado" e
  "limite conhecido (não corrigido de propósito)"

## Aviso metodológico

O corretor por palavra-chave produziu **quatro falsos resultados**, todos
pegos por leitura manual, e **três deles favoreciam a skill**. Correção
automática por regex não é confiável aqui: o baseline expressa a mesma ideia
com outro vocabulário ("Não executado" em vez de "não verificado").

**Só o par script + leitura vale.**

## Limites deste resultado

- n=1 por condição na rodada 2. Uma tarefa não prova ausência de diferença.
- Ambas as rodadas usaram Opus 5. Modelos menores podem se beneficiar mais.
- Não foi medida consistência entre sessões, que é onde a skill mais plausivelmente
  ajuda — e que exigiria repetição, não uma execução.

## O que isso significa na prática

As skills **não** devem ser vendidas como "o Claude não sabe isso" — ele sabe.

O que elas continuam sendo, com valor real:

- **referência verificada** contra o jar desta versão, com os comandos de checagem
- **documentação do projeto**, versionada junto do código
- **convenções explícitas** para quem não é o autor
- possivelmente úteis para modelos menores ou sessões longas

O que **não** se pode afirmar sem novo teste: que melhoram a qualidade da saída
do Opus 5.

---

# Rodada 2026-09-01 — minecraft-code-research (casos eval-4 e eval-5)

Cobre o buraco que a rodada de 2026-08-29 deixou: `minecraft-code-research`
não tinha nenhum caso próprio. Dois casos novos, mesmo formato da rodada 1
(uma pergunta, com skill vs. sem skill), desenhados para testar exatamente o
que essa skill promete — seguir a escada de extensão antes de Mixin
(`eval-4-mixin-vs-escada`) e exigir evidência de perfil antes de otimizar
(`eval-5-perf-antes-de-otimizar`). Prompts completos e critério de cada um em
`evals/prompts.json`.

## Veredito

| Caso | Comportamento (escada/evidência) | Erros factuais |
|---|---|---|
| eval-4 (Mixin vs. escada) | empate — os dois evitaram Mixin e recomendaram `setPersistent()` + `ServerEntityEvents.ENTITY_LOAD` | nenhum nos dois lados |
| eval-5 (perfil antes de otimizar) | empate — os dois exigiram spark profiler + teste A/B antes de mexer na frequência | **2 erros, só no `without_skill`** |

**De novo, nenhuma vantagem de comportamento foi medida** — nos dois casos as
duas condições chegaram à mesma recomendação e pela mesma lógica. Isso
confirma, num terreno desenhado especificamente para favorecer a skill (a
"escada de extensão" é o argumento central dela), o padrão já visto em
2026-08-29.

## O que a camada factual encontrou (checado por `javap` contra o jar real do
projeto — `1.21.1+build.3` merged, o mesmo do `gradle.properties`)

Toda a base factual usada nas quatro respostas foi conferida no bytecode, não
por memória — inclusive a alegação central de `eval-4`
(`VillagerEntity.canImmediatelyDespawn(double)` → `iconst_0; ireturn`,
confirmado). Os dois erros abaixo só apareceram na resposta `without_skill` de
`eval-5`:

1. **Nome de classe errado.** A resposta citou `NearestLivingEntitySensor`; o
   nome real (confirmado na lista de `.class` do jar) é
   `NearestLivingEntitiesSensor` (plural).
2. **Método inventado.** A resposta recomendou checar se o Sensor
   "sobrescreveu `getSenseInterval()` incorretamente". Esse método não existe
   — `javap -p` na classe `Sensor` mostra `senseInterval` como campo
   `private final`, setado só via construtor (`Sensor(int)`), sem getter nem
   método sobrescrevível.

A resposta `with_skill` do mesmo caso não fez nenhuma das duas afirmações —
não citou nomes de sensores vanilla específicos, e descreveu `senseInterval`
corretamente como campo checado por `Sensor.tick` (método `final`, confirmado).

## Como os testes foram feitos

Sem clone isolado (essa rodada não escreve código, só a resposta) — agentes
com acesso de leitura ao repositório real, instruídos a não criar/editar/apagar
nada nele. Quatro subagentes em paralelo (2 casos × with/without), respostas
lidas por inteiro à mão, depois checagem factual manual das alegações
concretas (`javap` na classe correspondente do jar mapeado).

## Aviso metodológico

O mecanismo de subagente usado aqui não devolveu `total_tokens`/`duration_ms`
por notificação — só deu pra estimar por relógio de parede, monitorando status
periodicamente:

- eval-5 `without_skill`: < 2 min · eval-5 `with_skill`: ~4 min
- eval-4 `without_skill`: ~6–7 min · eval-4 `with_skill`: ~9–10 min

Direcionalmente consistente com o achado de 2026-08-29 (skill custa mais sem
ganho medido de qualidade), mas **sem contagem de tokens real desta vez** —
não dá pra repetir a comparação de custo (~14%) com este dado. Ver nota em
`evals/README.md`.

## Limites deste resultado

- Mesmos limites da rodada de 2026-08-29: n=1 por condição, só Opus 5, e
  `minecraft-code-research` também pode ter disparado nos casos de
  2026-08-29 sem isso ter sido registrado (o campo `skill` nunca foi uma
  trava — ver `aviso-metodologico` em `prompts.json`).
- Os erros factuais achados aqui são pequenos e não invalidam a resposta
  `without_skill` — a recomendação prática (medir antes de otimizar) continua
  correta nos dois casos. É o mesmo tipo de achado que 2026-08-29 já registrou:
  a única camada onde apareceu diferença foi a factual, não a de processo.

---

# Rodada 2026-09-01 (2) — os mesmos eval-4/eval-5, em Haiku 4.5

A hipótese em aberto desde 2026-08-29 era "possivelmente úteis para modelos
menores". Reaproveitei os mesmos dois casos, mesmos prompts, trocando só o
modelo (Opus 5 → Haiku 4.5, via parâmetro `model` do subagente), com e sem
skill — 4 rodadas novas.

## Veredito — resultado oposto ao esperado

**A hipótese não se sustentou. Nos dois casos, nas duas condições (com e sem
skill), o Haiku inventou nomes de método/classe que não existem no jar real**
— e a skill não impediu isso, apesar do checklist dela pedir exatamente essa
verificação.

| Caso | with_skill (Haiku) | without_skill (Haiku) |
|---|---|---|
| eval-4 (despawn) | inventou `canDespawn()` + comportamento fictício por reputação; marcou `[FATO]`/checklist como ✅ sem checar o jar; recomendou Mixin | inventou `shouldDespawn()` (nome diferente, também inexistente); recomendou Mixin |
| eval-5 (perf) | não citou `spark`; timer manual + JFR; nenhuma verificação de bytecode | citou `spark` corretamente; mas inventou `World.findClosestPoiPosition()`/`getPoiManager().findClosestPoiTypes()` — não existem; o real é `PointOfInterestStorage.getNearestPosition(...)` |

Nenhuma das quatro respostas do Haiku achou o mecanismo real
(`VillagerEntity.canImmediatelyDespawn(double)` → sempre `false`,
`setPersistent()`) que **as duas** respostas de Opus 5 (com e sem skill)
tinham achado no mesmo caso. As quatro respostas do Haiku convergiram pra
Mixin ou pra métodos fictícios — checado por `javap` contra o mesmo jar
merged 1.21.1+build.3:

- `LivingEntity`/`MobEntity`: não existe `canDespawn()` nem `shouldDespawn()`.
  O que existe é `checkDespawn()`, `canImmediatelyDespawn(double)`,
  `cannotDespawn()`, `isPersistent()`, `setPersistent()` — os mesmos que
  Opus 5 citou certo nas duas condições.
- `PointOfInterestStorage`: não existe `findClosestPoiPosition` nem
  `findClosestPoiTypes`. Existe `getNearestPosition(...)`,
  `getNearestTypeAndPosition(...)`, `getTypesAndPositions(...)`.

## O achado mais importante desta rodada: a skill deu uma falsa sensação de rigor

A resposta `with_skill` de `eval-4` produziu tags `[FATO]` e marcou itens do
checklist (`before-modifying-vanilla.md`) como `✅ Confirmado`, no formato
exato que a skill pede — **sem ter rodado nenhum comando de verificação**. Ou
seja: no Opus 5, o formato da skill parece ter vindo acompanhado da disciplina
de checar (achado de 2026-08-29: a skill corrigiu 3 erros reais via
`[FATO]` conferido). No Haiku, o mesmo formato apareceu **sem** a verificação
por trás — o texto ficou com a aparência de rigor, não o rigor. Isso é pior do
que não ter skill nenhuma, porque a resposta sem skill pelo menos não finge
ter checado.

## Como isso foi medido

Mesmo método da rodada anterior (agentes de leitura, sem clone, respostas
lidas por inteiro), com verificação adicional por `javap` das classes
`LivingEntity` e `PointOfInterestStorage` (além de `MobEntity`/`VillagerEntity`,
já verificadas na rodada Opus).

## Limites deste resultado

- **n=1 por condição, de novo** — e aqui pesa mais: uma única resposta ruim
  pode ser sorte azarada do sampling, não um padrão do modelo. Antes de tirar
  qualquer conclusão operacional (ex.: "não usar Haiku pra isso"), precisa
  repetir 3-5x.
- Testado só com os 2 casos de `minecraft-code-research`; não dá pra saber se
  o mesmo acontece nos 4 casos de `minecraft-villager-systems`/
  `fabric-development` da rodada original.
- Isto muda a leitura da política de modelos no `CLAUDE.md` deste ambiente
  (subagentes mecânicos `eco-*` rodam em Haiku): se o padrão se confirmar com
  mais amostras, tarefas que exigem verificar fato contra o jar/decompilado
  (o que `minecraft-code-research` faz o tempo todo) podem não ser um bom
  encaixe para Haiku, skill ou não — o problema aqui não foi falta de
  instrução, foi o modelo declarar "confirmado" sem confirmar.

---

# Rodada 2026-09-01 (3) — eval-4 em Haiku, repetido (n=3 por condição)

Item pendente da rodada anterior: n=1 não prova padrão, "antes de concluir
qualquer coisa operacional, repetir 3-5x". Repeti `eval-4` mais 2 vezes por
condição (r2, r3 — a r1 já registrada acima), mesmo prompt, mesmo modelo.

## Veredito — a leitura muda com mais amostra

| Rodada | with_skill achou `setPersistent()`? | without_skill achou `setPersistent()`? | Nome de método inventado em algum lugar |
|---|---|---|---|
| r1 | Não — inventou `canDespawn()`, recomendou Mixin | Não — inventou `shouldDespawn()`, recomendou Mixin | Nas duas |
| r2 | **Sim** — recomendou como opção 1, zero Mixin | **Sim** — recomendou como opção A, zero Mixin | Nas duas (eventos diferentes: `ALLOW_DEATH`/`AFTER_DEATH` real; `AFTER_LOAD`/`TICK` inventados) |
| r3 | Não — hedged ("`canDespawn()` **se existir**"), recomendou Mixin, mas citou `Vanilla-Integration.md` e `ADR-004 §3 Regra 3` **corretos** (conferi, os dois existem de verdade) | Não — usou `checkDespawn` (nome **real**, desta vez certo) mas propôs `@ModifyVariable`/`@Inject`, não achou `setPersistent` | Só `without_skill` (eventos hipotéticos, mas apresentados como hipótese, não `[FATO]`) |

**Duas correções ao que a rodada anterior (n=1) sugeria:**

1. **"A skill dá falsa sensação de rigor" não se repetiu.** Só a r1
   `with_skill` marcou checklist como `✅` sem verificar. Em r2 e r3 o
   `with_skill` foi mais cauteloso (`se existir`, cita documento real do
   projeto). Não dá pra generalizar esse achado específico a partir de uma
   amostra — era exatamente o risco que o aviso de n=1 já continha.
2. **O padrão que sobrevive à repetição:** em **6 de 6** respostas (as 3
   rodadas × 2 condições), pelo menos um nome de método/classe inventado
   aparece em algum lugar do texto — com skill ou sem. Isso não mudou com
   mais amostra; se algo, ficou mais sólido. E a taxa de acerto do
   recomendação central (achar `setPersistent()` em vez de ficar preso em
   Mixin) ficou em 2/3 pras duas condições — sem vantagem visível da skill
   nessa amostra (`with_skill`: r1 não, r2 sim, r3 não · `without_skill`: r1
   não, r2 sim, r3 não — **empatados até no padrão de acerto por rodada**).

## O que isso ensina sobre o próprio método de avaliação

Isto é a prova prática do aviso que já estava em `evals/README.md`: **n=1
pode te dar exatamente a leitura errada**, e não só "impreciso" — na r1
sozinha, a conclusão teria sido "skill dá falsa sensação de segurança em
modelo menor", uma afirmação específica e citável. Com mais 2 amostras essa
afirmação específica não sobrevive. O que sobrevive (fabricação de API
acontece com ou sem skill, no Haiku, neste tipo de tarefa) é mais chato de
manchete, mas é o que os dados aguentam.

## Correção ao corretor, feita durante esta rodada

O critério "acha a solução sem Mixin" em `grade-conhecimento.py` exigia
`setpersistent` **e** `entity_load|serverentityevents` juntos — calibrado só
na resposta padrão do Opus 5 (que usa `ServerEntityEvents.ENTITY_LOAD`). Isso
deu `FALHA` errado pra r2 `with_skill`, que resolve com `setPersistent()`
chamado direto (sem evento) ou via `ServerLivingEntityEvents` — solução
igualmente válida, só que o regex não previa. Relaxado para exigir só
`setpersistent`. Achado durante a leitura manual desta rodada, não antes —
mais um caso do que `evals/README.md` já avisa: leia à mão antes de confiar
no placar do script.

## Limites que continuam de pé

- n=3 ainda é pouco pra estatística de verdade, mas já é o suficiente pra
  invalidar a leitura de n=1. Pra afirmar uma taxa de acerto com confiança,
  precisaria de mais.
- Só `eval-4` foi repetido; `eval-5` continua em n=1 por condição.
- Continua valendo: não testado se o mesmo padrão de fabricação aparece nos
  4 casos originais de `minecraft-villager-systems`/`fabric-development`.

---

# Rodada 2026-09-01 (4) — eval-5 em Haiku, repetido (n=3 por condição)

Mesma lógica da rodada (3), agora pro outro caso. Repeti `eval-5` mais 2 vezes
por condição (r2, r3 — a r1 já registrada na rodada (2)).

## Veredito — padrão parecido, com uma nuance nova

| Rodada | `with_skill` | `without_skill` |
|---|---|---|
| r1 | Sem `spark`; timer manual + JFR; nenhuma investigação do projeto real | Cita `spark` certo; inventa `NearestLivingEntitySensor` (real: plural) e `getSenseInterval()` (não existe) |
| r2 | Foi ler o código real do projeto (`VillageDetectionHandler.java`, `VillageScanner.java`, `VillageDetector.java`) — **todo método e toda linha citados existem de verdade**, conferido por `grep -n`; cita `spark` certo | Genérico, não investiga o projeto; inventa comando `/profiler` (não existe — o certo é `/debug start`/`stop`) e erra a atribuição do link do Spark (`sk1er/spark` como texto, mas o link aponta pro repo certo, `lucko/spark`) |
| r3 | Cita `Performance-Rules.md`, `Debugging-Strategy.md` e `GoToWorkTargetTask.java` — **os três existem de verdade**, `/debug start`/`stop` correto (comando vanilla real) — mas não cita `spark` nenhuma vez | Cita `spark` e `getNearestPosition(...)` corretos (ambos verificados), mas inventa `ServerEntityEvents.AFTER_LOAD` (o campo real, verificado por `javap`, é `ENTITY_LOAD`) |

**Confirma o padrão de `eval-4`:** nas 6 respostas, todas têm pelo menos uma
alegação fabricada em algum lugar (comando, evento ou método que não existe)
— incluindo a `AFTER_LOAD` de r3, que nenhum critério automático pegou (o
`grade-conhecimento.py` só sabe procurar as fabricações já catalogadas de
2026-08-29/09-01; essa é nova, achada só na leitura manual — o script não
serve pra detectar alucinação nova, só pra checar as já conhecidas).

**Nuance que `eval-4` não tinha:** em 2 das 3 rodadas (r2 e r3), o
`with_skill` foi investigar arquivos reais do projeto — com nomes de classe,
método e número de linha que bateram exatamente ao conferir — enquanto
**nenhuma** das 3 respostas `without_skill` fez esse tipo de investigação
específica do projeto; ficaram no nível genérico ("seu Sensor", sem abrir
nenhum arquivo do repo). Isso não é sobre citar API do Minecraft certo (onde
as duas condições continuam empatando em fabricar algo) — é sobre **ir olhar
o código do usuário antes de responder**, que é outra parte do que a skill
pede e que, nesta amostra pequena, ela pareceu puxar mais.

## Correção ao corretor, de novo

`eval-4` já tinha ensinado a não confiar demais em critério estreito. Aqui
não editei o script — o achado da `AFTER_LOAD` só apareceu na leitura manual
mesmo, e criar um critério author para cada fabricação nova vira lista
infinita, não sentinela útil. Registro aqui em vez de no script.

## Limites

- n=3, mesmo aviso da rodada (3): suficiente pra desconfiar de conclusão de
  n=1, insuficiente pra taxa de acerto confiável.
- A observação "with_skill investiga mais o projeto real" é de 2 em 3 — não
  vira regra a partir disso. Registrado como hipótese pra próxima rodada
  checar, não como achado fechado.
- Com isso, `eval-4` e `eval-5` estão os dois em n=3. Os 4 casos originais
  (2026-08-29) e a `rodada-2-processo` continuam n=1 e nunca rodaram em
  Haiku.
