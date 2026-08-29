# Protocolo de pesquisa

O roteiro completo das fases. O `SKILL.md` traz o resumo e os modos; aqui está o
que fazer dentro de cada fase, e por quê.

Não execute as 15 fases em toda pesquisa. O modo escolhido diz quais valem:

| Modo | Fases |
|---|---|
| QUICK | 0, 4 (parcial), 15 |
| FEATURE | 0, 1, 2, 4, 5, 6, 7, 11, 15 |
| DEEP | todas |
| FORENSIC | 0, 1, 3, 13, 14, 15 + `experiment-methodology.md` |

---

## Fase 0 — Enquadrar

Sem isto, a pesquisa não tem critério de parada.

Responda e **escreva** (vai para o `research-status.md`):

```text
SISTEMA · COMPORTAMENTO · VERSÃO · LOADER/MAPPINGS · OBJETIVO FINAL
```

Regra prática: se você não consegue escrever o comportamento alvo em uma frase,
você ainda não sabe o que está pesquisando. Pergunte ao usuário antes de ler
código — cinco minutos de conversa economizam uma hora de leitura errada.

---

## Fase 1 — Inventário do projeto

Antes de ler qualquer classe a fundo, saiba onde você está.

```bash
cat gradle.properties
cat settings.gradle build.gradle
cat src/main/resources/fabric.mod.json
find . -name "*.mixins.json" -not -path "*/build/*"
find src -maxdepth 3 -type d
```

Registre:

```text
Minecraft · Java · Loader · Fabric API · Mappings · Gradle · Loom
Mod ID · Entrypoints (main/client/server) · Mixins · Dependencies
Datagen? · Resources? · Data files? · source sets extras?
```

**Não assuma layout fixo.** Existem projetos com `src/client/java` separado
(split sourceset), outros com tudo em `src/main`. Existem mods multi-módulo,
mods com `common/` compartilhado entre loaders, mods com sourceset de gametest
próprio. O layout conta a história da arquitetura — leia-o antes de opinar.

Sinais que valem anotar já:

- `environment` em `fabric.mod.json`: `*`, `client` ou `server`.
- Muitos mixins em classes Vanilla profundas → risco de compatibilidade alto.
- Ausência de mixin nenhum → o mod acha extensão por API; bom sinal.
- `accessWidener` declarado → o mod alarga acesso a membros Vanilla.

---

## Fase 2 — Mapear a arquitetura

Classifique os pacotes por responsabilidade. Categorias comuns:

```text
CORE · REGISTRY · ITEM · BLOCK · ENTITY · BLOCK_ENTITY · SCREEN · MENU
NETWORK · EVENT · MIXIN · CONFIG · DATA · DATAGEN · CLIENT · SERVER
COMPAT · UTILITY
```

E as específicas de domínio, quando existirem:

```text
VILLAGER · AI · GOAL · BRAIN · MEMORY · SENSOR · POI · TRADE
INVENTORY · PATHFINDING · AUTOMATION
```

**Não force categorias.** Se o projeto organiza por domínio (`mining/`,
`construction/`) em vez de por tipo técnico, essa é a informação — registre-a
assim. Crie categorias novas quando o projeto pedir.

Pergunta que revela a arquitetura de verdade: **onde mora o estado?** Um projeto
com toda a lógica em `fabric/` acoplada a `ServerWorld` tem arquitetura diferente
de um com um núcleo de domínio puro e uma camada fina de adaptação, mesmo que os
dois tenham os mesmos pacotes.

---

## Fase 3 — Ciclo de vida

Separe três linhas do tempo que costumam ser confundidas:

**Ciclo do jogo**

```text
GAME START → FABRIC LOADER → MOD INIT → REGISTRATION →
WORLD LOAD → TICK → EVENTS → NETWORK → SAVE → UNLOAD
```

**Ciclo da entidade**

```text
spawn → initialize → tick → ai tick → interaction → damage →
death → save → load → despawn
```

**Ciclo da BlockEntity**

```text
place → create → load → tick → markDirty → save → chunk unload → remove
```

Confundir os três é a origem de bugs de persistência. Um campo que existe pelo
tempo da entidade não sobrevive ao restart do servidor; um estático não sobrevive
à troca de save. Ver `persistence-analysis.md`.

Localize **em qual ponto de qual linha** o sistema que você estuda atua.

---

## Fase 4 — Pesquisa Vanilla

A fase mais importante. Detalhes operacionais em `vanilla-analysis.md`.

Monte a cadeia:

```text
VANILLA SYSTEM
├── Entry          (o que dispara)
├── Core Class     (onde a decisão mora)
├── Supporting     (o que ela usa)
├── Data           (o que ela lê)
├── State          (o que ela guarda, e onde)
├── AI             (se houver)
├── Events         (o que ela emite)
├── Persistence    (o que sobrevive)
└── Integration    (quem mais toca nisso)
```

Para cada classe importante, use `templates/vanilla-class-analysis.md`.

---

## Fase 5 — Mappings

Todo nome que você vai escrever num Mixin ou numa chamada precisa ser verificado
contra o mapping **desta** versão. Ver `mappings.md`.

Regra curta: **nome de tutorial não é nome válido**. Verifique com `javap` antes
de afirmar.

---

## Fase 6 — Pesquisa Fabric

Antes de qualquer solução própria, veja se a Fabric já entrega. Ver
`fabric-analysis.md`.

Registre:

```text
PROBLEMA · API DISPONÍVEL? · EVENTO DISPONÍVEL? · LIMITAÇÕES · VERSÃO · ALTERNATIVA
```

Um "não existe" só vale escrito depois de você ter procurado nos sources jars da
Fabric API — não de memória.

---

## Fase 7 — Pesquisa de outros mods

Não tente entender o mod inteiro. Ver `mod-analysis.md`.

```text
INVENTÁRIO → OBJETIVO → LOCALIZAR FEATURE → ENTRY POINT → SEGUIR FLUXO →
TOUCHPOINT VANILLA → TOUCHPOINT FABRIC → MIXINS → DADOS → ESTADO → DOCUMENTAR
```

---

## Fase 8 — Mixins

Mixin não é mágica: é bytecode em cima de método concreto de versão concreta.
Ver `mixin-analysis.md` para os tipos, a escada de risco e o que investigar.

---

## Fase 9 — Entidades

Ver `entity-analysis.md`. Separe sempre **lógica de servidor** de
**representação de cliente** — é onde a maioria dos bugs de multiplayer nasce.

---

## Fase 10 — IA

Ver `ai-brain-analysis.md`. **Nunca assuma que o Minecraft usa só `Goal`.**
Mobs modernos (Villager, Piglin, Axolotl, Warden...) usam `Brain`. Descubra qual
modelo o mob alvo usa antes de desenhar qualquer coisa.

---

## Fase 11 — Dados

Ver `data-driven-analysis.md`. Para cada dado:

```text
ONDE É DEFINIDO? · COMO É CARREGADO? · QUEM CONSOME? · PODE SER MODIFICADO?
É DATA-DRIVEN? · É PERSISTENTE? · É SINCRONIZADO? · CLIENT OU SERVER?
```

Muita coisa que parece exigir Java é, na verdade, um JSON de datapack. Isso é o
degrau 3 da escada de extensão e resolve mais casos do que parece.

---

## Fase 12 — Registries

Ver `registry-analysis.md`.

```text
REGISTRY → IDENTIFIER → OBJECT → BOOTSTRAP → REFERENCE
```

Registrar fora da janela correta do ciclo de vida é uma das causas mais comuns de
crash no boot.

---

## Fase 13 — Client / Server

Ver `client-server-analysis.md`. As perguntas que decidem:

```text
Quem possui o estado verdadeiro? · O cliente pode modificar? · O servidor valida?
Existe packet? · Quando sincroniza? · O que acontece ao reconectar?
```

Funcionar em singleplayer não é evidência de correção: o singleplayer roda um
servidor integrado no mesmo processo, e o mesmo objeto acaba visível dos dois
lados. O bug só aparece em multiplayer.

---

## Fase 14 — Persistência

Ver `persistence-analysis.md`.

```text
RUNTIME STATE → SAVE → SERIALIZATION → WORLD STORAGE → LOAD → RECONSTRUCTION
```

**Nunca assuma persistência só porque existe um campo.** Procure quem escreve e
quem lê. Campo sem escrita no save é campo que some no restart.

---

## Fase 15 — Fluxo de execução

Para a feature específica, monte a cadeia e **siga as chamadas** — não liste
métodos soltos. Use `templates/execution-flow.md`.

```text
TRIGGER → EVENT/TICK/INTERACTION → ENTRY METHOD → VALIDATION →
DECISION → ACTION → STATE CHANGE → SYNC → PERSISTENCE
```

Se algum elo estiver em `[HIPÓTESE]`, é ali que o experimento da
`experiment-methodology.md` deve mirar.

---

## Fase 16 — Comparação (quando houver vários mods)

Ver `comparison-analysis.md` e `templates/comparison.md`. Não é para eleger o
"melhor mod" — é para ver quais abordagens existem e qual serve ao seu problema.

---

## Fecho

Toda pesquisa acima de QUICK termina com os seis itens do "O que uma pesquisa
entrega" do `SKILL.md`. Se você não consegue produzi-los, a pesquisa não acabou —
ou acabou e você deveria dizer que não encontrou o suficiente, que também é um
resultado.
