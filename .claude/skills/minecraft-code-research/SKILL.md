---
name: minecraft-code-research
description: >-
  Pesquisa técnica profunda de Minecraft Java Edition e modding Fabric — mapeia o
  sistema Vanilla, o ciclo de vida, os extension points e o que outros mods já
  fizeram ANTES de escrever qualquer código. Use sempre que o trabalho tocar
  Minecraft, Fabric, Yarn/mappings, Mixin, Villager, Brain AI, Goal, Sensor,
  Memory Module, POI, pathfinding, Entity, BlockEntity, Registry, Data Components,
  NBT, PersistentState, datapack, tag, recipe, loot table, networking, client/server
  ou compatibilidade entre mods. Dispare também quando o pedido for de
  implementação ("adiciona", "cria", "muda o comportamento de", "por que o mod está
  com lag", "como funciona X no Minecraft", "analisa esse mod") — pesquisar antes
  é justamente o ponto. Vale para perguntas em português ou inglês.
---

# Minecraft Code Research

Esta skill existe para impedir um ciclo específico e caro:

```text
pedido → acha uma classe → escreve um Mixin → "funciona" →
quebra com outro mod → mais Mixin → mais patch → arquitetura frágil
```

O que ela instala no lugar:

```text
PROBLEMA → SISTEMA VANILLA → ARQUITETURA → CICLO DE VIDA →
EXTENSION POINTS → FABRIC API → OUTROS MODS → HIPÓTESE →
VALIDAÇÃO → DECISÃO → IMPLEMENTAÇÃO
```

## A regra que manda em todas as outras

**Nunca modifique um sistema do Minecraft antes de compreender o sistema Vanilla
que está sendo modificado.**

O motivo não é pureza de processo. Minecraft distribui um único comportamento
entre código Vanilla, Fabric Loader, Fabric API, Mixins de terceiros, JSON de
datapack, tags, registries e a fronteira client/server. Quem lê uma classe e
conclui que entendeu o sistema quase sempre encontrou um dos pedaços, não o
mecanismo. O patch nasce torto porque a premissa nasceu torta.

**Nunca assuma que toda a lógica está numa única classe.**

## Antes de tudo: qual é o alvo?

Não comece a ler código sem responder isto. Registre a resposta — ela vai para o
`research-status.md` (ver `references/research-continuity.md`).

```text
QUAL SISTEMA?          (ex.: Villager AI, chunk loading, loot table)
QUAL COMPORTAMENTO?    (o que precisa mudar ou ser entendido, em uma frase)
QUAL VERSÃO?           (ex.: 1.21.1)
QUAL LOADER/MAPPING?   (ex.: Fabric loader 0.19.3, Yarn 1.21.1+build.3)
QUAL OBJETIVO FINAL?
```

Objetivo final, escolha um:

`compreender Vanilla` · `adicionar feature` · `modificar comportamento` ·
`criar entidade` · `criar profissão` · `modificar IA` · `automação` ·
`investigar bug` · `estudar outro mod` · `migrar versão` ·
`resolver compatibilidade` · `substituir sistema` · `integrar sistemas`

Objetivo diferente muda o que vale a pena ler. "Investigar bug" quer call graph e
estado; "criar profissão" quer registries e ciclo de vida. Pular esta etapa é o
que produz leitura infinita sem conclusão.

## Início de sessão

Esta skill precisa funcionar sem o contexto da conversa anterior. Ao começar:

1. Procure documentação de pesquisa existente (`docs/research/`, `research-status.md`).
2. Leia o `research-status.md` se existir. Ele diz o objetivo, o que já é fato e o que está pendente.
3. Confirme versão e mappings **no `gradle.properties` de hoje**, não no que o documento diz.
4. Se o código mudou desde a última análise, marque as conclusões afetadas como a revalidar.
5. Continue pela prioridade mais alta pendente. **Não repita pesquisa concluída.**

Se não houver nada disso, você está na primeira sessão: crie o `research-status.md`
a partir de `templates/research-status.md` antes de fechar o trabalho.

## Os quatro modos

Escolha o modo pelo tamanho real da dúvida. Usar DEEP para uma pergunta de QUICK
queima contexto sem melhorar a resposta; usar QUICK para uma decisão de
arquitetura produz um Mixin que você vai remover depois.

| Modo | Quando | Roteiro |
|---|---|---|
| **QUICK** | Pergunta pontual, sem decisão de arquitetura pendurada nela | objetivo → localizar símbolo → conferir contexto → responder |
| **FEATURE** | Entender/estender uma funcionalidade | feature → sistema Vanilla → fluxo de execução → dados → extensão Fabric → mods existentes → conclusão |
| **DEEP** | Sistema central do projeto, decisão que vai durar | inventário → arquitetura → ciclo de vida → Vanilla → Fabric → mods → execução → dados → estado → persistência → networking → compatibilidade → experimentos |
| **FORENSIC** | Bug complexo, lag, comportamento intermitente | reproduzir → tracear → log → estado → call graph → experimento → causa raiz → opções de correção |

Anuncie o modo escolhido. Mudar de modo no meio é normal — QUICK que revela uma
decisão de arquitetura vira FEATURE. Diga que mudou e por quê.

O roteiro detalhado de cada fase está em `references/research-protocol.md`.

## Hierarquia de pesquisa

Sempre nesta ordem. Cada degrau responde "isto já está resolvido?" antes de o
seguinte existir.

```text
1. VANILLA                o jogo já faz isso?
2. FABRIC API             existe evento/API pronta?
3. FABRIC IMPLEMENTATION  como a própria Fabric resolveu?
4. OUTROS MODS            alguém já enfrentou isso?
5. MIXINS                 é mesmo necessário?
6. IMPLEMENTAÇÃO PRÓPRIA
```

Inverter a ordem é permitido, mas exige justificativa escrita. "Foi mais rápido"
não é justificativa — o custo de um Mixin desnecessário não aparece hoje, aparece
no primeiro relatório de incompatibilidade.

## Escada de extensão

Antes de modificar qualquer comportamento, procure a alternativa **menos invasiva
que resolve de verdade**:

```text
 1. Sistema Vanilla existente
 2. Registro Vanilla
 3. Configuração data-driven (JSON, tag, datapack)
 4. Fabric API
 5. Fabric Events
 6. Composição
 7. Interface existente
 8. Herança
 9. Accessor Mixin
10. Inject Mixin
11. Overwrite Mixin
```

A ordem não é lei física — às vezes o degrau 4 é pior que o 9 para o caso
concreto. Mas **todo salto para uma solução mais invasiva precisa de uma linha
dizendo por quê**, e essa linha vai para a documentação da decisão. Sem ela,
ninguém três meses depois consegue saber se o Mixin era necessário ou preguiça.

## Profundidade progressiva e regra de parada

Não leia o projeto inteiro. Desça um nível de cada vez:

```text
LAYER 1  mapa geral do projeto
LAYER 2  o sistema relevante
LAYER 3  as classes críticas
LAYER 4  os métodos
LAYER 5  internals do Vanilla
```

Antes de descer mais um nível, pergunte:

```text
Esta leitura resolve uma dúvida real?
Muda alguma decisão?
Valida uma hipótese?
Reduz risco?
É necessária para a implementação?
```

Se todas forem "não", registre como `FUTURE RESEARCH` no research-status e
**pare**. Pesquisa sem pergunta é leitura decorativa: consome contexto, produz
resumo e não muda nada.

### Prioridade

`P0` bloqueia entendimento ou implementação · `P1` necessário para arquitetura ·
`P2` importante para a feature · `P3` contexto · `P4` pesquisa futura.

## Disciplina de evidência

Toda afirmação técnica carrega uma etiqueta. Misturar as categorias é como a
pesquisa vira ficção convincente.

| Etiqueta | Significa |
|---|---|
| `[FATO]` | Observado diretamente no código/log/execução |
| `[INFERÊNCIA]` | Conclusão a partir de evidência, não vista diretamente |
| `[HIPÓTESE]` | Ainda precisa de validação |
| `[VALIDAÇÃO NECESSÁRIA]` | O próximo passo técnico concreto |
| `[DECISÃO]` | Escolha feita para o projeto |
| `[RISCO]` | Problema possível |
| `[VERSÃO]` | Para qual versão a conclusão vale |

Todo `[FATO]` registra a fonte: **tipo** (Vanilla, Fabric API, doc Fabric, código
de mod, experimento, teste em runtime), **arquivo, classe, método e versão**.

**Nunca invente número de linha.** Se não tiver a linha, `arquivo + classe +
método` basta e é honesto. Linha inventada é pior que linha ausente: ela parece
verificável e não é.

Detalhes em `references/evidence-and-claims.md`.

## Onde está a verdade de campo

Esta é a diferença entre pesquisar e adivinhar. Num projeto Loom, os nomes reais
da sua versão estão no disco — não é preciso confiar em tutorial nem em memória.

**Descubra o ambiente primeiro:**

```bash
grep -E "minecraft_version|yarn_mappings|loader_version|fabric_version" gradle.properties
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
MAPPINGS=$(find ~/.gradle/caches/fabric-loom -name "mappings.tiny" | head -1)
```

**As três verificações que substituem o chute:**

```bash
# 1. Essa classe existe com esse nome nesta versão?
unzip -l "$MC_JAR" | grep "VillagerEntity.class"

# 2. Quais são as assinaturas REAIS? (não as que você lembra)
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.task.MultiTickTask

# 3. Esse nome de método existe no mapping? (obf <-> intermediary <-> yarn)
grep -P "\tsetTaskList$" "$MAPPINGS"
```

**Para ler o corpo de um método Vanilla**, gere os fontes uma vez:
`./gradlew genSources` — depois o `*-sources.jar` fica ao lado do `$MC_JAR`.

**Os fontes da Fabric API já vêm com o projeto**, sem gerar nada:

```bash
find . -path "*loom-cache/remapped_mods*" -name "*-sources.jar"
unzip -p <sources.jar> net/fabricmc/fabric/api/.../Alguma.java
```

Use isso **antes** de afirmar que uma API existe. `javap` leva segundos e é a
diferença entre `[FATO]` e `[HIPÓTESE]`.

Detalhes e casos difíceis em `references/mappings.md` e `references/vanilla-analysis.md`.

## Roteamento — o que ler quando

Não carregue tudo. Vá ao arquivo que corresponde ao que você está fazendo agora.

### Método

| Situação | Leia |
|---|---|
| Começando; quer o protocolo de fases completo | `references/research-protocol.md` |
| Retomar ou registrar pesquisa entre sessões | `references/research-continuity.md` |
| Vai afirmar algo técnico e quer marcar corretamente | `references/evidence-and-claims.md` |
| A análise não decide — precisa testar | `references/experiment-methodology.md` |
| Quer saber o que **não** fazer | `references/anti-patterns.md` |

### Domínio

| Situação | Leia |
|---|---|
| Ler código Vanilla, achar a classe certa, mapear callers | `references/vanilla-analysis.md` |
| Nome de classe/método, versão, migração | `references/mappings.md` |
| Verificar se a Fabric já resolve; entrypoints, eventos | `references/fabric-analysis.md` |
| Analisar um mod externo | `references/mod-analysis.md` |
| Avaliar, escrever ou revisar um Mixin | `references/mixin-analysis.md` |
| Entidade: spawn, tick, dados, interação, morte | `references/entity-analysis.md` |
| IA: Brain, Goal, Activity, Sensor, Memory, POI, pathfinding | `references/ai-brain-analysis.md` |
| JSON, NBT, Data Components, tags, recipes, loot tables | `references/data-driven-analysis.md` |
| Registries, Identifier, bootstrap, registries dinâmicos | `references/registry-analysis.md` |
| Sides, packets, sincronização, vazamento de classe client | `references/client-server-analysis.md` |
| Salvar/carregar estado | `references/persistence-analysis.md` |
| Tick, scan, chunk, pathfinding, lag | `references/performance-analysis.md` |
| Conflito entre mods, ordem de evento, risco | `references/compatibility-analysis.md` |
| Comparar como vários mods resolveram o mesmo problema | `references/comparison-analysis.md` |

### Templates — o formato de saída

| Vou produzir | Use |
|---|---|
| O índice vivo da pesquisa | `templates/research-status.md` |
| Análise de um sistema (FEATURE/DEEP) | `templates/system-analysis.md` |
| Análise de uma classe Vanilla | `templates/vanilla-class-analysis.md` |
| Análise de um mod externo | `templates/mod-analysis.md` |
| Avaliação de um Mixin | `templates/mixin-analysis.md` |
| Traçado de um comportamento | `templates/execution-flow.md` |
| Análise de entidade | `templates/entity-analysis.md` |
| Análise de IA (Brain/Goal) | `templates/ai-system-analysis.md` |
| Análise de registro | `templates/registry-analysis.md` |
| Decisão de arquitetura (ADR) | `templates/architecture-decision.md` |
| Experimento | `templates/experiment.md` |
| Matriz comparativa | `templates/comparison.md` |

### Checklists — verificações objetivas

| Antes de | Rode |
|---|---|
| **Escrever código que toca Vanilla** | **`checklists/before-modifying-vanilla.md`** ← a mais importante |
| Fechar a análise de um sistema Vanilla | `checklists/vanilla-system.md` |
| Concluir "a Fabric não resolve isso" | `checklists/fabric-system.md` |
| Fechar a análise de um mod externo | `checklists/mod-analysis.md` |
| Escrever ou aprovar um Mixin | `checklists/mixin.md` |
| Estender comportamento de entidade ou IA | `checklists/entity-and-ai.md` |
| Fechar qualquer recomendação técnica | `checklists/compatibility.md` |
| Dizer que a pesquisa acabou | `checklists/research-completion.md` |

### Exemplos — o método em movimento

Pesquisas completas, com os comandos que foram rodados e a evidência real de
MC 1.21.1. Leia o que combinar com a sua situação antes de começar do zero.

| Situação | Exemplo |
|---|---|
| "Como funciona X no Vanilla?" | `examples/analyze-vanilla-system.md` |
| "Por que essa entidade se comporta assim?" (bug, FORENSIC) | `examples/trace-entity-behavior.md` |
| "Preciso de um Mixin para isso?" | `examples/analyze-mixin.md` |
| "Analisa esse mod / essa pasta de pesquisa" | `examples/analyze-fabric-mod.md` |
| "Qual abordagem devo escolher?" | `examples/compare-mod-implementations.md` |

## O que uma pesquisa entrega

Pesquisa que não deixa artefato foi conversa. O fecho de qualquer modo acima de
QUICK produz:

1. **Resposta direta à pergunta original** — não um resumo do que você leu.
2. **Um documento** em `docs/research/` a partir do template certo.
3. **Etiquetas de evidência** separando fato, inferência e hipótese.
4. **A decisão recomendada** com o degrau da escada de extensão e a justificativa.
5. **Riscos** de versão, compatibilidade e performance, quando houver.
6. **`research-status.md` atualizado** — objetivo, pendências, próximo passo.

Se a pesquisa não mudou nenhuma decisão e não reduziu nenhum risco, diga isso
também. É um resultado válido e evita que a próxima sessão refaça o caminho.

## Handoff para implementação

Esta skill termina na decisão, não no código. Quando a pesquisa concluir e o
próximo passo for escrever mod:

```text
minecraft-code-research  →  fabric-development
   COMPREENDER                ARQUITETAR
   VALIDAR                    IMPLEMENTAR
   DOCUMENTAR                 TESTAR
```

O handoff carrega: sistema Vanilla identificado, ciclo de vida, degrau da escada
de extensão escolhido, riscos conhecidos e o caminho dos documentos gerados. Se a
`fabric-development` encontrar uma incerteza que a pesquisa não cobriu, ela
devolve o trabalho para cá em vez de adivinhar.

## Prioridade final

```text
COMPREENDER → VALIDAR → DOCUMENTAR → DECIDIR → PROTOTIPAR → IMPLEMENTAR
```

Inverter é permitido quando o usuário pede explicitamente ("só me dá o código") —
mas diga qual etapa está pulando e qual risco isso assume. O usuário pode aceitar
o risco; ele só não pode aceitar um risco que ninguém mencionou.
