# Análise de mods externos

Objetivo: extrair **como alguém resolveu um problema parecido com o seu**. Não é
entender o mod inteiro — isso é caro e quase sempre desnecessário.

## Onde procurar

O projeto pode ter uma pasta de pesquisa com mods de referência. Procure antes de
supor que não existe:

```bash
ls -d PesquisaFabricMOD */Pesquisa* ../Pesquisa* 2>/dev/null
find . -maxdepth 3 -name "fabric.mod.json" -not -path "*/build/*"
```

Fontes típicas: tutoriais de Fabric, mods de aldeão (VillagerConfig, Easy
Villagers, SimpleVillagers, GuardVillagers), mods grandes (Create). Se a pasta
não existir, a análise vale igual para qualquer repositório clonado.

**Nenhum mod de referência é "a implementação correta".** Cada um resolveu o
problema *dele*, na versão *dele*, com as restrições *dele*. Use como evidência
de que uma abordagem é viável — nunca como autoridade.

## Roteiro

```text
INVENTÁRIO → OBJETIVO → LOCALIZAR FEATURE → ENTRY POINT → SEGUIR FLUXO →
TOUCHPOINT VANILLA → TOUCHPOINT FABRIC → MIXINS → DADOS → ESTADO → DOCUMENTAR
```

**Não tente entender tudo de uma vez.** Um mod grande tem centenas de classes e
você precisa de cinco.

### 1. Inventário (5 minutos)

```bash
cat gradle.properties 2>/dev/null | grep -E "minecraft|yarn|loader|fabric|mod_version"
cat src/main/resources/fabric.mod.json
find . -name "*.mixins.json" -not -path "*/build/*" -exec cat {} \;
find src -maxdepth 4 -type d | head -40
```

Anote antes de qualquer coisa: **versão do Minecraft e mappings**. Um mod de
1.19 responde "é possível?" mas não responde "como escrevo isso hoje".

### 2. Qual é a pergunta?

Escreva a pergunta que você quer que este mod responda. Uma frase.

> "Como o Easy Villagers faz o aldeão trabalhar dentro de um bloco sem quebrar a
> IA Vanilla?"

Sem isso, você lê tudo e não conclui nada.

### 3. Localizar a feature

Vá pelo que o jogador vê — nome, item, bloco, mensagem — e caia no código:

```bash
grep -rn "trader\|profession\|job_site" src/main/resources/assets/*/lang/en_us.json | head
grep -rln "VillagerEntity\|VillagerProfession" src/main/java | head -20
```

### 4. Entry point

Como aquele código começa a existir? Um dos três, quase sempre:

- registrado no `ModInitializer`
- disparado por um evento da Fabric
- injetado por Mixin

Isso já classifica a abordagem do mod na escada de extensão.

### 5. Seguir o fluxo

Do entry point até o efeito no mundo. Use `templates/execution-flow.md`.

```text
TRIGGER → ENTRY → VALIDATION → DECISION → ACTION → STATE → SYNC → PERSISTENCE
```

Pare quando a sua pergunta estiver respondida. Não continue "para completar".

### 6. Touchpoints

Onde o mod **encosta** no Vanilla e na Fabric:

```bash
grep -rn "@Inject\|@Redirect\|@Overwrite\|@ModifyVariable\|@Accessor" src/main/java | head -30
grep -rn "net.fabricmc.fabric.api" src/main/java | sed 's/.*import //' | sort -u | head -30
```

O segundo comando é ouro: a lista de imports da Fabric API é o resumo honesto de
quais APIs o mod usa — e portanto quais existiam e serviram.

## O que registrar

Use `templates/mod-analysis.md`. Os campos que mais rendem:

- **Extension Strategy** — em qual degrau da escada ele parou, e aparentemente
  por quê
- **Mixins** — quantos, de que tipo, em que classes. É o indicador mais direto de
  risco de compatibilidade
- **Conceitos reutilizáveis** — a ideia, não o código
- **Conceitos a evitar** — igualmente valioso; um mod que resolveu com
  `@Overwrite` ensina o que não fazer
- **Lições** — o que isso muda na sua decisão

## Licença

Antes de reusar **código** (não ideia), confira a licença:

```bash
ls LICENSE* COPYING* 2>/dev/null && head -5 LICENSE*
grep -i "license" src/main/resources/fabric.mod.json
```

Ideia, arquitetura e abordagem são livres para aprender. Trecho de código
carrega a licença do projeto de origem — MIT e Apache-2.0 exigem atribuição,
GPL/LGPL têm exigências de licenciamento sobre o resultado. Se for reusar código
de verdade, registre a origem e a licença no seu projeto.

Na dúvida: **leia, entenda, escreva o seu.** Além de resolver a licença, é o que
garante que você entendeu.

## Erros comuns nesta fase

- **Copiar antes de datar.** O mod é de outra versão em mais da metade dos casos.
- **Confundir "funciona lá" com "está certo".** O mod pode ter um bug que o autor
  não viu, ou uma restrição que você não tem.
- **Ler o mod inteiro.** Se você não consegue dizer qual pergunta está
  respondendo, pare.
- **Ignorar os mixins.** É onde as decisões mais consequentes de um mod ficam.
- **Um mod só.** Uma amostra não mostra o leque. Ver `comparison-analysis.md`.
