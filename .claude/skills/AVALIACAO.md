# Avaliação das skills — 2026-08-29 (estendida em 2026-09-01 e 2026-09-02)

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
| 5 | 2026-09-01 | eval-0..3 (originais) | `minecraft-villager-systems`, `fabric-development` | Haiku 4.5 | 1 |
| 6 | 2026-09-02 | eval-0..3 repetido (r2, r3 — completa n=3) | `minecraft-villager-systems`, `fabric-development` | Haiku 4.5 | 3 |
| 7 | 2026-09-02 | `rodada-2-processo` (contador de árvores), primeira vez em Haiku | `fabric-development`, `minecraft-villager-systems` | Haiku 4.5 | 1 |
| 8 | 2026-09-02 | `rodada-2-processo` repetida (r2, r3 — completa n=3) | `fabric-development`, `minecraft-villager-systems` | Haiku 4.5 | 3 |

## Qualidade e usabilidade das três skills (síntese, atualizada após a rodada 6)

Nota de método: "usabilidade" aqui não é opinião sobre organização do
arquivo — é o que os testes acima realmente sustentam. Estrutura boa (todas
as três têm) não é a mesma coisa que comportamento melhor medido (nenhuma das
três provou isso ainda de forma robusta).

| Skill | Estrutura | Evidência empírica | Usabilidade |
|---|---|---|---|
| `minecraft-code-research` | 355 linhas de `SKILL.md` + checklists/exemplos/referências/templates focados em investigação | Opus 5: única a mostrar diferença real — 2 erros factuais achados só no `without_skill` em `eval-5` (nomes de classe/método inventados que o `with_skill` não citou). Haiku 4.5 (n=3, `eval-4`+`eval-5`): não impede fabricação de API (6/6 respostas com pelo menos uma alegação inventada, com ou sem skill) — mas em 2 de 3 rodadas de `eval-5` o `with_skill` foi investigar arquivos reais do projeto (nomes/linhas conferidos certos), o que nenhum `without_skill` fez | 🟡 **Média-alta, parcialmente comprovada** — é a única com sinal real de valor (camada factual em Opus 5, tendência de investigar código real mesmo em Haiku), mas não resolve o problema que mais importa em modelo barato (fabricação de API) |
| `minecraft-villager-systems` | 305 linhas de `SKILL.md`, a mais extensa em checklists/templates/workflows específicos de domínio (Brain, POI, trading, gossip, profissão) | Opus 5 (`eval-0..2`, n=1): empate comportamental nos três. **Haiku 4.5 (`eval-0..2`, n=3 — r1+r2+r3, rodadas 5+6):** `with_skill` claramente melhor em `eval-1`/`eval-2` nas **3 de 3** rodadas — primeira vantagem de n=1 de toda a avaliação que sobrevive a duas repetições sem se desfazer. Mas não é vitória sem erro: `with_skill` cometeu fabricação própria em 2 de 3 rodadas em cada caso (`TradeOffers...put()` com 3 args; `new Identifier(String,String)`, construtor privado) — a vantagem real é diagnosticar a causa certa e o sistema certo (vanilla vs. interno do mod), não ausência de fabricação. `eval-0` segue empatado nas 3 rodadas | 🟢 **Média-alta, confirmada em `eval-1`/`eval-2`** — é a estrutura mais rica das três, e é a única com um achado que sobreviveu a n=3 sem se desfazer. A ressalva que fica: a vantagem é de diagnóstico/domínio, não de imunidade a fabricar API — `with_skill` também erra, só que erra menos grave |
| `fabric-development` | 358 linhas de `SKILL.md`, a maior em workflows/templates (o mais genérico — cobre todo o ciclo de implementação, não um subsistema) | Testada em `eval-3` (item simples: Opus 5 empate; Haiku, n=3 — r1: `with_skill` inventa `ToolMaterials.COPPER`, `without_skill` repete o anti-padrão redundante; r2: `with_skill` afirma ter rodado build sem executar nada, `without_skill` inventa dois métodos de `Item.Settings`; r3: **as duas condições cometem o mesmo bug de compilação** — `new Identifier(String,String)` privado — no entregável central) e em `rodada-2-processo` (empate 9/9, mas **+14% de tokens**, só Opus 5) | 🟠 **Média-baixa, não comprovada + custo documentado** — é a única das três sem nenhum sinal de vantagem em nenhum teste, em nenhum modelo, em nenhuma das 3 rodadas de Haiku — e é a que mais dispara junto com as outras duas (descrição mais ampla) |

**Achado que atravessa as três:** nenhuma provou, em Opus 5, gerar uma
resposta melhor do que o modelo já daria sozinho — o valor medido ali é
outro: referência verificada, documentação do projeto, convenção explícita.
Em Haiku a leitura mudou: `minecraft-code-research` mostrou uma tendência
(2/3) de investigar mais o código real do projeto sem reduzir fabricação de
API; `minecraft-villager-systems` é agora a **única skill com um achado de
n=1 confirmado por repetição** — `with_skill` ganha em `eval-1`/`eval-2` nas
3 de 3 rodadas testadas, o oposto do que aconteceu com `eval-4` (onde n=1
não sobreviveu). `fabric-development` segue sem nenhum sinal, em nenhum
modelo, em nenhuma rodada.

**Padrão novo, cross-caso, achado na rodada 6:** o construtor
`new Identifier(String,String)` — privado em 1.21.1, o correto é
`Identifier.of(...)` — apareceu **4 vezes em 16 respostas** (r2+r3),
em `with_skill` de `eval-2` (r2 e r3) e nos dois lados de `eval-3` (r3). É a
fabricação mais repetida da rodada 6 e não é específica de uma condição;
`grade-conhecimento.py` não tem critério para ela ainda.

**Por que isso é difícil de separar limpo:** as três têm gatilho por
descrição, não por invocação manual, e `fabric-development` e
`minecraft-code-research` dizem explicitamente no próprio `SKILL.md` que
disparam junto em pedido de implementação. Nenhum teste até agora controlou
qual skill efetivamente disparou em cada resposta (ver `aviso-metodologico`
em `evals/prompts.json`) — o placar por skill acima é o melhor que dá pra
afirmar com o que foi medido, não uma separação limpa e garantida.

## O que falta — dificuldade e importância

| Item | Dificuldade | Importância | Por quê |
|---|---|---|---|
| ~~Repetir eval-0..3 em Haiku a n=3~~ | — | — | ✅ **Feito em 2026-09-02** (rodada 6, r2+r3). Confirmado: `eval-1`/`eval-2` sustentam vantagem de `with_skill` em 3 de 3; `eval-0`/`eval-3` seguem sem vantagem clara. Ver seção "Rodada 2026-09-02" |
| ~~Repetir `rodada-2-processo` em Haiku~~ | — | — | ✅ **Feito em 2026-09-02 (2)**, n=1. Achado mais forte de toda a avaliação até agora: `without_skill` tem um bug funcional real (conta árvore por tora, não por árvore inteira) que `with_skill` não tem. Ver seção "Rodada 2026-09-02 (2)" — **precisa repetir a n=3** antes de virar conclusão definitiva |
| ~~Repetir `rodada-2-processo` em Haiku a n=3~~ | — | — | ✅ **Feito em 2026-09-02 (3)**. A vantagem de r1 **não se sustentou como regra**: `with_skill` acerta a semântica em 2/3, `without_skill` em 1/3 — vantagem parcial, não unânime como `eval-1`/`eval-2`. Ver seção "Rodada 2026-09-02 (3)" |
| Repetir eval-4/eval-5 em Opus 5 (hoje n=1) | Baixa — mesma mecânica já rodada, só repetir | Média, agora a de maior prioridade que resta | Confirma se o empate 12/12-estilo se sustenta com variância, como já foi feito pro Haiku |
| Confirmar a hipótese "with_skill investiga mais o código real do projeto" (achada em eval-5, 2 de 3) | Média — precisa repetir em `eval-4` e nos casos originais pra ver se é geral ou específico do prompt | Média-alta — se confirmar, é o achado mais forte e citável de toda a avaliação | — |
| Adicionar sentinela em `grade-conhecimento.py` para `new Identifier(String,String)` | Baixa — é um regex simples, mesmo molde dos sentinelas já existentes de eval-4/eval-5 | Média — apareceu 4x em 16 respostas na rodada 6, cross-condição, e nenhum critério automático pega hoje | Achado só na leitura manual; sem sentinela, a próxima rodada perde o sinal se não repetir a leitura à mão |
| Ampliar `grade-processo.py`: check da armadilha não cobre arquivo novo (untracked) | Baixa — mudar `untracked()` pra também prefixar linhas com `+` antes de concatenar, ou aplicar os checks de CODIGO separadamente sobre conteúdo bruto | Média — achado na rodada 7: o detector de `static Map` só olha linhas `+` de diffs *rastreados*; um arquivo inteiramente novo (caso comum de feature nova) nunca é coberto, mesmo se a armadilha inteira morar lá | Achado por acidente nesta rodada (o campo morto `SESSION_COUNTS` está num arquivo novo) — a próxima vez pode não ter alguém lendo à mão |
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

---

# Rodada 2026-09-01 (5) — os 4 casos originais (eval-0..3), em Haiku 4.5 (n=1)

Fechando a maior lacuna que sobrava: `minecraft-villager-systems` e
`fabric-development` nunca tinham sido testadas fora do Opus 5. Rodei
`eval-0` a `eval-3` em Haiku, mesmos prompts de 2026-08-29, n=1 por
condição (ainda não repetido — ver Limites).

## Veredito — ao contrário de eval-4/eval-5, aqui a skill fez diferença visível

| Caso | Skill | Resultado |
|---|---|---|
| `eval-0` (WALK_TARGET) | `minecraft-villager-systems` | Empate raso no diagnóstico central; os dois erram uma API (`with_skill` inventa `Brain.setActivity()`, `without_skill` inventa `Brain.getTaskList()` — nenhum dos dois existe, confirmado por `javap`). Script: 3/4 × 4/4. |
| `eval-1` (profissão/trades) | `minecraft-villager-systems` | **`with_skill` claramente melhor.** Construtor de `VillagerProfession` com os 6 argumentos certos (confirmado: `TradeOffers.PROFESSION_TO_LEVELED_TRADE` existe de verdade), dois predicados corretos, checagem de ciclo de lucro do trade, cobertura completa de recursos. `without_skill` usou um construtor de 3 argumentos (não existe essa sobrecarga), `new Identifier(...)` (forma antiga, não é mais construtor público em 1.21.1), import errado de `PointOfInterestTypes`, e **caiu exatamente na armadilha de só um `BlockState`** que o `eval-2` existe pra testar. Script: 5/5 × 2/5. |
| `eval-2` (POI block states) | `minecraft-villager-systems` | **`with_skill` claramente melhor.** Deu o fix certo e concreto (`getStateManager().getStates()`). `without_skill` ficou vago, chegou a inventar um mecanismo de registro de POI via datapack JSON que não existe em Vanilla (POI é registrado em código, não datapack). Script: 3/3 × 2/3. |
| `eval-3` (item simples) | `fabric-development` | Os dois erram, de formas diferentes. `with_skill` inventa `ToolMaterials.COPPER` (confirmado por `javap`: só existem `WOOD/STONE/IRON/DIAMOND/GOLD/NETHERITE`) — um erro que não compila. `without_skill` reproduz o **mesmo anti-padrão que o teste de 2026-08-29 já tinha flagrado** (`.maxCount(1)` redundante com `.maxDamage(180)`) e também usa `new Identifier(...)` antigo. Script marcou `with_skill` como regressão em "registro correto", mas isso é um artefato do critério: `with_skill` usou `getMaxCount()` sobrescrito em vez de `.maxCount(1)` no Settings — estilo diferente, não necessariamente errado; o erro real dele é outro (`ToolMaterials.COPPER`). |

## O que isso muda na leitura de `minecraft-villager-systems`

Essa é a primeira vez, em toda a avaliação, que `with_skill` ganha de forma
clara e não-ambígua — e ganha duas vezes seguidas, nos dois casos que
testam justamente o domínio que essa skill mais documenta (profissão, POI,
trades). Isso muda a nota de usabilidade dela — mas com a ressalva de
sempre: **n=1**. O padrão de `eval-4` (n=1 sugerindo algo que não sobreviveu
a n=3) é o aviso vivo de que isso precisa ser repetido antes de virar
conclusão.

## Padrão que se repete de novo

Em **6 de 8** respostas (todas menos as duas de `eval-2`, que não citaram
API específica o bastante pra checar) há pelo menos uma alegação de
API/construtor inventada, confirmada por `javap` — o mesmo padrão de
`eval-4`/`eval-5`. A diferença aqui não é "menos fabricação com skill" — é
"a fabricação, quando acontece, não impede a resposta de acertar a causa e
a arquitetura certa" em `eval-1`/`eval-2` no `with_skill`.

## Como foi medido

Mesmo método das rodadas anteriores: 8 agentes em paralelo (4 casos ×
2 condições), leitura manual completa, verificação por `javap` contra o
mesmo jar merged 1.21.1+build.3 dos claims mais centrais e checáveis de
cada resposta (não every single linha — orçamento de tempo já era alto
nesta sessão).

## Limites

- **n=1.** Depois do que `eval-4` ensinou, esse é o aviso mais importante
  desta seção: a vantagem clara de `with_skill` em `eval-1`/`eval-2` pode
  não sobreviver a repetição, exatamente como "skill dá falsa sensação de
  rigor" não sobreviveu. Repetir a n=3 é o próximo passo natural antes de
  atualizar a nota de usabilidade de `minecraft-villager-systems` de forma
  definitiva.
- `eval-3` (`fabric-development`) continua sem nenhuma vantagem clara de
  skill visível, em nenhum modelo testado até agora — Opus 5 (empate) ou
  Haiku (os dois erram, de formas diferentes).
- `rodada-2-processo` continua nunca testada em Haiku, e é o único teste
  que mede disciplina de build real — não conhecimento de API, que é tudo
  que as rodadas de conhecimento (0 a 5) medem.

---

# Rodada 2026-09-02 — eval-0..3 em Haiku, repetido (r2 e r3, completando n=3)

Item de maior prioridade da lista de pendências: repetir os 4 casos
originais (`eval-0..3`) mais duas vezes em Haiku 4.5 (r2, r3 — a r1 é a
rodada "(5)" de 2026-09-01), fechando **n=3 por condição**. Mesmo método das
repetições de `eval-4`/`eval-5`: 16 agentes de leitura em paralelo (2
rodadas × 4 casos × 2 condições), sem clone, leitura manual completa +
verificação por `javap` contra o mesmo jar merged `1.21.1+build.3`.

## Veredito por caso, agora com n=3

| Caso | r1 (09-01) | r2 | r3 | Leitura com n=3 |
|---|---|---|---|---|
| `eval-0` (WALK_TARGET) | empate raso, os dois inventam um método de `Brain` que não existe | `with_skill` importa `net.minecraft.entity.ai.goal.WalkTarget` — pacote errado, não compila (o real é `entity.ai.brain.WalkTarget`); `without_skill` limpo | empate limpo, nenhum erro factual nos dois | **Sem vantagem confirmada.** `with_skill` errou em 2 de 3 rodadas, `without_skill` em 1 de 3 — leve vantagem para `without_skill` em não fabricar, mas o diagnóstico central (manter `WALK_TARGET` a cada tick) acerta nas 6 respostas |
| `eval-1` (profissão/trades) | `with_skill` claramente melhor — construtor de 6 args certo; `without_skill` usa construtor de 3 args inexistente e cai na armadilha do `eval-2` | `with_skill` certo no diagnóstico (6 args, POI, separação profissão/trades) mas erra `TradeOffers.PROFESSION_TO_LEVELED_TRADE.put(...)` com 3 args — `Map.put` só aceita 2; `without_skill` erra o domínio inteiro, responde com o sistema **interno do mod** em vez da API vanilla pedida | `with_skill` de novo limpo e certo; `without_skill` de novo erra o domínio inteiro, reaproveita `VillagerProfession.WEAPONSMITH` em vez de criar a profissão pedida | **Confirmado nas 3.** Primeira vez em toda a avaliação que uma vantagem de n=1 sobrevive a duas repetições sem se desfazer. `with_skill` sempre acerta a API vanilla certa; `without_skill` falha de formas diferentes nas 3 rodadas, mas falha nas 3 |
| `eval-2` (POI block states) | `with_skill` claramente melhor — fix concreto certo; `without_skill` inventa registro de POI via datapack JSON | `with_skill` certo na causa e na solução, mas usa `new Identifier(String,String)` — construtor privado em 1.21.1, não compila; `without_skill` inventa `PointOfInterestType.register()` e uma assinatura de construtor errada | `with_skill` certo de novo, mesmo erro de `Identifier` no exemplo bônus; `without_skill` pior ainda — inventa uma classe inteira (`PoiTypes`) atribuída à Fabric API que **não existe em nenhum lugar do jar** (confirmado por busca no jar inteiro), mais `VillagerEntity#detachHome()` e um comando `/poi` inexistentes | **Confirmado nas 3, com ressalva.** `with_skill` sempre acerta a causa e o mecanismo; mas comete o mesmo erro de `Identifier` privado em r2 **e** r3 — regressão nova, não vista em r1. `without_skill` nunca acerta a causa e fabrica mais a cada rodada |
| `eval-3` (item simples) | os dois erram diferente — `with_skill` inventa `ToolMaterials.COPPER`; `without_skill` repete o anti-padrão já flagrado em 2026-08-29 | `with_skill` registra certo mas afirma ter rodado `gradlew build`/`runClient`/`runServer` sem ter executado nada (mesmo padrão "confirma sem confirmar" de 09-01); `without_skill` inventa `.repairable(...)` e `.useBlockPrefab()` em `Item.Settings` | **os dois cometem o mesmo bug** — `new Identifier(MOD_ID, id)`, construtor privado, não compila; os dois repetem o anti-padrão redundante `.maxDamage(180).maxCount(1)`; `without_skill` soma mais dois erros (`ToolMaterial.DIAMOND` em vez de `ToolMaterials.DIAMOND` — interface vs. enum; `createAttributeModifiers()` inexistente em `ToolItem`) | **Sem vantagem confirmada — como já era.** `fabric-development` segue sem nenhum sinal de vantagem em nenhuma das 3 rodadas; em r3 as duas condições têm o mesmo bug central de compilação |

## O que muda na leitura das três skills

**`minecraft-villager-systems` passa de "sinal não confirmado" para
confirmado, com nuance.** `eval-1` e `eval-2` — os dois casos desenhados
especificamente para o domínio que essa skill documenta (profissão, POI,
trades) — mostraram `with_skill` melhor nas **3 de 3** rodadas, a primeira
vez em toda a avaliação que um achado de n=1 sobrevive a n=3 sem se desfazer
(o oposto do que aconteceu com `eval-4`). Mas a vitória não é "zero erro":
`with_skill` cometeu um erro de compilação próprio em 2 de 3 rodadas em cada
caso (o `Map.put` de 3 args em `eval-1`/r2; o `Identifier` privado em
`eval-2`/r2 e r3) — a vantagem real é **diagnóstico da causa e escolha do
sistema certo (vanilla vs. interno do mod)**, não ausência de fabricação.

**O erro de `Identifier(String,String)` virou um padrão cross-caso.**
Apareceu em `with_skill` de `eval-2` (r2, r3) e nos dois lados de `eval-3`
(r3) — quatro ocorrências em 16 respostas, nenhuma delas coberta por nenhum
critério do `grade-conhecimento.py`. É a fabricação mais repetida desta
rodada, e não é específica de uma condição.

**`fabric-development` continua a única sem nenhum sinal de vantagem**, em
nenhum modelo, em nenhuma rodada até agora — e em r3 chegou ao caso mais
direto disso: as duas condições cometeram exatamente o mesmo bug de
compilação no entregável central.

## Fabricação de API — contagem

Contando resposta como "limpa" só se nenhuma alegação factual checável nela
estiver errada (não só "existe fabricação sim/não"):

| Rodada | `with_skill` limpo | `without_skill` limpo |
|---|---|---|
| r2 (4 casos) | 1 de 4 (só `eval-0` errou por engano de pacote — as outras três também erraram) | 1 de 4 (só `eval-0` limpo) |
| r3 (4 casos) | 2 de 4 (`eval-0`, `eval-1` limpos) | 1 de 4 (só `eval-0` limpo) |

Confirma o padrão já documentado: fabricação de API acontece com ou sem
skill, na maioria das respostas, em Haiku 4.5 — a diferença que sobrevive é
a de **diagnóstico/domínio** em `eval-1`/`eval-2`, não a de fabricação.

## Falso positivo confirmado no corretor automático

O check "SEM over-engineering" de `eval-3` marcou `with_skill` (r2) como
regressão por conter a palavra "ItemFactory" — mas a resposta cita isso como
o padrão a **evitar** ("✗ ItemFactory genérica"), não como o que foi feito.
Mais um caso do aviso de sempre em `evals/README.md`: leia à mão antes de
confiar no placar do script.

## Como foi medido

Mesmo método de `eval-4`/`eval-5`: agentes de leitura em paralelo (Haiku
4.5, sem clone, restritos a não criar/editar/apagar nada no repositório), 16
respostas no total (r2 + r3), lidas por inteiro e verificadas por `javap`
contra o jar merged `1.21.1+build.3` nas alegações centrais e checáveis de
cada uma. Dois agentes individuais escreveram num caminho com erro de
digitação (`1.21.1` em vez de `1-21-1`) — corrigido movendo os dois arquivos
para o lugar certo antes da checagem; conteúdo intacto, nada regerado.

## Limites que ficam

- **n=3 fecha o item de maior prioridade da lista de pendências**, mas não
  é uma amostra grande — a confirmação de `eval-1`/`eval-2` é sólida por ser
  3-de-3 unânime, não por volume.
- `rodada-2-processo` continua nunca testada em Haiku — é o único teste que
  mede disciplina de build real, e é o próximo item de maior prioridade que
  resta.
- O padrão do `Identifier` privado (4 ocorrências) é novo o bastante para
  não ter critério no `grade-conhecimento.py` ainda — vale considerar
  adicionar um sentinela na próxima passada pelo script.

---

# Rodada 2026-09-02 (2) — `rodada-2-processo` em Haiku, pela primeira vez

Item de maior prioridade que restava: a tarefa de implementação real (o
contador de árvores por colônia, mesmo pedido de 2026-08-29) nunca tinha
rodado em Haiku 4.5 — só em Opus 5 (empate 9/9). Dois clones locais
isolados (`git clone --local`), workspace em `C:\temp\evalproc-r1` (caminho
curto — o `README.md` já avisa que caminhos aninhados demais quebram o
clone no Windows). Nenhum agente foi avisado sobre a armadilha
(`static Map` perde tudo no restart) — ela precisa ser evitada por
disciplina, não por dica.

## O que rodou de verdade (conferido no disco, não só no relato)

| | `with_skill` | `without_skill` |
|---|---|---|
| Build (`./gradlew build`) | ✅ real — `build/test-results` com **565 testes unitários, 0 falhas** | ✅ real — mesmos **565 testes unitários, 0 falhas** |
| Gametest (`./gradlew runGametest`) | ✅ real — `build/gametest-report.xml` existe, e o log tem a linha exata `All 218 required tests passed :)` | ❌ nunca rodou — não existe `build/gametest*` no clone. O relatório afirma que a máquina não permite ("O build em máquina fraca... não permite executar servidor Minecraft") — **falso**, confirmado pelo `with_skill` rodando no mesmo ciclo, na mesma máquina |
| Teste novo para a feature | Nenhum dos dois criou | Nenhum dos dois criou |

O corretor automático (`grade-processo.py`) media dois `git clone`, então
"rodou de verdade" aqui não é auto-relato — é o conteúdo de
`build/test-results/`, `build/gametest-report.xml` e `build/gametest/logs/latest.log` lido diretamente do disco.

## O achado central: um bug funcional real, não só de API

**`without_skill` incrementa o contador por bloco de tora, não por árvore.**
Em `TreeFelling.chop()` (chamado uma vez por bloco quebrado — o próprio
javadoc do método diz *"escreve um bloco por lenhador"*):

```java
if (logsCollected > 0) {
    VillageColonyMod.COLONIES.find(job.task.colonyId())
            .ifPresent(colony -> colony.addTreesHarvested(1));
}
```

Uma árvore com 6 blocos de tora conta como **6 árvores**. O pedido do
usuário foi explicitamente "quantas árvores... já derrubaram" — o número
reportado no log ficaria sistematicamente errado, e nenhum teste (dos dois
lados) cobre isso, então o build verde não pega o defeito.

**`with_skill` incrementa em `TreeChoice.startNextTree()`**, no bloco que só
executa quando `job.plan != null` — ou seja, exatamente quando a árvore
*inteira* anterior terminou (o log de contexto já dizia "finished the tree
at ... — N logs and M leaves"). Semântica certa: um incremento por árvore.

## O que também apareceu

- **`with_skill` tem um campo morto.** `TreesHarvestedLog.java` (arquivo
  novo) declara `private static final Map<UUID, Integer> SESSION_COUNTS`
  — nunca lido, só limpo em `clearAll()`. Não afeta o comportamento (o
  relatório real lê `colony.treesHarvested()`, não esse mapa) — é código
  morto, possivelmente um vestígio de cogitar a forma errada (a própria
  armadilha) e trocar de ideia sem apagar a declaração.
- **Nenhum dos dois criou teste novo** para a feature — os 565 testes que
  passam são todos preexistentes. Contra a cultura do próprio projeto
  (`TODO.md` é rígido sobre fase vermelha conferida), é uma lacuna
  compartilhada pelas duas condições.
- **Os dois relatórios separam claramente o que foi verificado do que não**
  (ex.: `with_skill` lista explicitamente "NÃO foi executado: teste de jogo
  completo, persistência real"; `without_skill` tem uma seção dedicada "O
  Que NÃO Foi Testado"). Nenhum dos dois afirma ter verificado algo que não
  rodou — a única alegação factualmente errada é a de `without_skill` sobre
  a *capacidade* do ambiente, não sobre o que ele próprio rodou.

## Dois bugs achados e corrigidos no `grade-processo.py` durante esta rodada

1. **`has()` não tinha `re.MULTILINE`.** Todo check com `^\+` (o detector da
   armadilha `static Map`, e o check de NBT-com-default) usa `^` esperando
   ancorar em cada linha do diff — sem `re.M`, `^` só ancora no início do
   texto inteiro (que começa com `diff --git...`), nunca com uma linha
   `+`. Consequência: **o detector da armadilha nunca podia falhar,
   mesmo que a armadilha fosse pisada** — sempre PASS, desde que o script
   existe. E o check de NBT-com-default nunca podia passar, mesmo com
   código correto — sempre FALHA. Os dois vieses passaram despercebidos
   porque, por coincidência, sempre favoreciam a leitura "os dois empatam".
2. **Adicionar só `re.M` não bastava — `re.S` (DOTALL) já estava lá, e os
   dois juntos são piores que nenhum.** Com `.` cruzando linha (`re.S`) e
   `^` ancorando em cada linha (`re.M`), o padrão gencioso `^\+.*Map<`
   passou a "casar" o primeiro `+` do diff inteiro com um `Map<`
   **centenas de linhas depois, em outro arquivo** — foi assim que o
   campo morto `SESSION_COUNTS` (não relacionado ao diff rastreado)
   quase disparou um falso positivo da armadilha antes de eu isolar a
   causa. Corrigido removendo `re.S` de `has()` — nenhum dos 9 critérios
   deste script precisa de `.` cruzando linha.
3. Placar antes da correção: 6/9 × 6/9 (com os dois vieses se cancelando
   por acaso). Placar depois: **7/9 × 7/9** — os dois FALHA restantes
   ("declara o que NÃO verificou", "separa verificado de não-verificado")
   são falsos negativos confirmados por leitura manual: os dois relatórios
   fazem exatamente isso, só que com um vocabulário que os regexes não
   cobrem (mesmo aviso de sempre do `evals/README.md`). Lidos à mão, o
   placar real de processo é **9/9 × 9/9** — o mesmo empate de 2026-08-29,
   Opus 5.

## Limitação nova descoberta no design do checker (não é bug de regex)

O detector de `static Map` só enxerga linhas `+` do **diff de arquivos
rastreados** (`git diff HEAD`). Um arquivo inteiramente **novo** (caso
comum ao adicionar uma feature) entra pelo caminho `untracked()`, que
concatena o conteúdo bruto sem marcador `+` — então a armadilha, se
morasse inteira num arquivo novo, **nunca seria pega**, regex correto ou
não. É exatamente o arquivo onde o campo morto `SESSION_COUNTS` apareceu
nesta rodada. Não corrigido ainda — ver "O que falta".

## Veredito

**A primeira vantagem *funcional* de toda a avaliação**, distinta de todas
as anteriores (que eram sobre citar a API certa ou diagnosticar a causa
certa): `without_skill` entrega um contador que super-conta sistematicamente
(por tora, não por árvore) e afirma uma limitação de ambiente que é falsa;
`with_skill` entrega a semântica certa e é honesto sobre o que não testou,
com o custo de um campo morto inofensivo. É n=1 — o próximo passo natural,
como sempre, é repetir.

## Como foi medido

Clones isolados (`git clone --local`) em `C:\temp\evalproc-r1\run-with` e
`run-without`, dois agentes Haiku 4.5 em paralelo, sem dica sobre a
armadilha. Verificação por leitura direta do disco (diffs via `git diff
HEAD`, artefatos de build/teste em `build/`), não por auto-relato dos
agentes — inclusive a alegação de "218 testes passaram" e a de "máquina não
permite gametest" foram checadas contra evidência real antes de aceitar ou
rejeitar.

## Limites deste resultado

- **n=1.** O mesmo aviso de sempre — a vantagem funcional encontrada aqui
  precisa sobreviver a repetição antes de virar conclusão, exatamente como
  o padrão de `eval-4` já ensinou.
- Testado só o caso do lenhador (`skills-esperadas: fabric-development,
  minecraft-villager-systems`); não se sabe se o mesmo tipo de bug apareceria
  numa tarefa de outro subsistema.
- `minecraft-code-research` não é o alvo deste caso (a tarefa mexe num
  sistema que o mod já tem) — continua sem nenhum caso de processo dedicado.

---

# Rodada 2026-09-02 (3) — `rodada-2-processo` repetida a n=3 (r2, r3)

Item de maior prioridade que restava: repetir a tarefa do contador de
árvores mais duas vezes em Haiku 4.5 (r2, r3 — a r1 é a seção "Rodada
2026-09-02 (2)" acima), fechando **n=3**. Mesma mecânica: 4 clones locais
isolados (`git clone --local`), 4 agentes em paralelo, nenhuma dica sobre a
armadilha (`static Map`).

## Veredito por rodada, agora com n=3

| Rodada | `with_skill`: semântica do contador | `without_skill`: semântica do contador | Build/processo |
|---|---|---|---|
| r1 | ✅ correta — um incremento por árvore, em `TreeChoice.startNextTree()` | ❌ errada — incrementa por bloco em `TreeFelling.chop()`; e afirma falsamente que a máquina não roda `runGametest` | `with_skill` rodou build + 218 gametests reais; `without_skill` só build |
| r2 | ❌ errada — incrementa por bloco em `TreeFelling.chop()`, mas **rotula como "trees"** — inconsistente com o próprio relatório, que descreve corretamente o mecanismo por-bloco na frase anterior | ❌ errada — mesmo mecanismo por bloco, mas **rotula honestamente como "logs"** | as duas rodaram build real (565/565 testes); nenhuma mentiu sobre o que verificou |
| r3 | ✅ correta — um incremento por árvore em `LumberjackWork.closePlan()`, com o gate `job.index >= job.plan.logs()`, que também protege contra árvore abandonada a meio caminho | ✅ correta — mesmo gate, mesma proteção | `without_skill` compilou de verdade (565/565); `with_skill` **não conseguiu compilar** por contenção real de lock do Gradle — `FileSystemException` genuína, não desculpa |

## O que isso muda na leitura

**A vantagem de r1 não se sustenta como regra.** Em 3 rodadas: `with_skill`
acerta a semântica em **2 de 3** (r1, r3); `without_skill` acerta em **1 de
3** (r3). Não é o padrão unânime de 3-de-3 que `eval-1`/`eval-2` mostraram —
é uma vantagem parcial, com uma rodada (r2) em que as duas condições cometem
exatamente o mesmo bug. O que muda de rodada pra rodada não é a disciplina
em si, é **onde** cada implementador independente escolhe colocar o
incremento: `TreeChoice.startNextTree()` (r1-with), `TreeFelling.chop()`
(r1-without, r2-ambos), `LumberjackWork.closePlan()` (r3-ambos) — três
localizações diferentes em seis respostas, e só duas delas têm a semântica
certa por construção (as que só disparam quando a árvore inteira termina).

**Achado novo e mais sutil em r2: mentir sobre o rótulo é pior que errar a
métrica.** As duas condições de r2 cometem o mesmo bug de contagem, mas só
`with_skill` chama o resultado de "trees" no log e no relatório enquanto
descreve, na frase anterior, o mecanismo por-bloco que o contradiz — uma
inconsistência interna que `without_skill` não tem: ele rotula honestamente
como "logs", sem prometer o que não mede.

**A alegação de "cache do Gradle corrompido" em r3 (`with_skill`) era
verdadeira, não desculpa.** Verificado: `FileSystemException` real, tentativa
de acessar um jar do Loom já travado por outro processo — resultado esperado
de rodar 4 builds Gradle simultâneos competindo pelo mesmo cache global
(`~/.gradle/caches/fabric-loom`) no Windows. Diferente da alegação falsa de
r1 ("máquina fraca não permite rodar servidor") — aqui a limitação era real,
e o relatório não inventou sucesso. **Nota de método para a próxima
rodada:** rodar `rodada-2-processo` em paralelo tem esse custo real — a
próxima repetição deveria isolar `GRADLE_USER_HOME` por clone, ou rodar
sequencial, para não perder uma amostra de build por contenção de lock.

## Veredito consolidado (n=3)

| | `with_skill` | `without_skill` |
|---|---|---|
| Semântica correta (por árvore, não por bloco) | 2/3 | 1/3 |
| Alegações de build/teste batem com evidência real em disco | 3/3 | 3/3 |
| Caiu na armadilha (`static Map`) | 0/3 | 0/3 |
| Fez uma alegação falsa sobre o que verificou/o ambiente | 0/3 (r2 rotula errado, mas não mente sobre ter *rodado*) | 1/3 (r1, sobre capacidade da máquina) |

**Leitura final:** a vantagem funcional de r1 (o achado mais forte da
avaliação até aquele ponto) **não se replica como regra geral** — é uma
vantagem parcial de 2 contra 1 em acertar a semântica do contador, não uma
garantia. O que se sustenta nas 3 rodadas é outra coisa, mais frágil:
`without_skill` teve a única alegação genuinamente falsa sobre capacidade do
ambiente (r1); `with_skill`, quando algo deu errado de verdade (r3), relatou
a causa real em vez de inventar sucesso. Essa leitura também é só n=3 e
merece a mesma desconfiança de sempre antes de virar conclusão operacional.

## Como foi medido

Mesmo aparato da r1: 4 clones isolados (`git clone --local`), 4 agentes
Haiku 4.5 em paralelo (2 rodadas × 2 condições), sem dica sobre a armadilha.
Verificação por dois forks de leitura em paralelo (um por rodada), cada um
checando: onde o incremento acontece e se a condição de disparo é
por-árvore ou por-bloco (lendo o método inteiro, não só a linha do
incremento — inclusive seguindo o gate `job.index >= job.plan.logs()` até
confirmar que só é verdadeiro quando todos os blocos da árvore já foram
processados); evidência real em disco (`build/test-results`,
`build/gametest-report.xml`, ou mensagem de erro real) contra cada alegação
do relatório, em vez de aceitar o auto-relato; e a armadilha (`static Map`).

## Limites

- n=3 fecha o item de maior prioridade, mas o resultado é mais fraco (e mais
  interessante) do que uma confirmação limpa — é o tipo de leitura mista que
  só aparece com repetição, exatamente o que este método existe para
  capturar. Generalizar "with_skill evita o bug de contagem" a partir disso
  seria repetir o erro que `eval-4` já ensinou a não cometer.
- Rodar 4 builds Gradle em paralelo no mesmo cache global é uma fonte de
  ruído real — perdeu uma amostra de build em r3. Não invalida o achado de
  semântica (que não depende do build), mas é um ponto de método a corrigir.
- Três localizações de implementação diferentes em seis respostas — não dá
  pra saber se existe uma quarta localização "errada" comum que ainda não
  apareceu, nem se a escolha de localização é influenciada pela skill de
  alguma forma sistemática (os dados atuais não sustentam essa afirmação).
