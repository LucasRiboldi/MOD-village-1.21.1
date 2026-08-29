# Workflow — entender um comportamento de aldeão

Antes de mudar qualquer coisa. É o workflow que impede o Mixin no lugar errado.

---

## 1. Descrever o comportamento observado

Em linguagem de jogo, não de código:

```text
"o aldeão fazendeiro vai até a plantação de manhã, colhe, e guarda no baú"
```

E o que você quer mudar, também em linguagem de jogo. Se você não consegue
descrever os dois, ainda não sabe o que está investigando.

## 2. Localizar a camada

```text
MUNDO → SENSOR → MEMORY → ACTIVITY → TASK → AÇÃO
```

Qual peça produz o que você observa?

| Observação | Camada provável |
|---|---|
| ele **nota** algo | sensor |
| ele **lembra** de algo | memória |
| ele **muda de modo** | Activity / Schedule |
| ele **faz** algo | task |
| ele **anda** para algum lugar | memória `WALK_TARGET` |
| ele **reivindica** um bloco | POI |
| ele **é** algo | profissão |

Errar a camada aqui é o que produz o patch no lugar errado.

## 3. Verificar o ambiente

```bash
grep -E "minecraft_version|yarn_mappings" gradle.properties
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
```

## 4. Mapear as peças

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i "<conceito>"
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.Activity
javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession | head -20
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestType
```

Para ler o **corpo** das tasks Vanilla, gere os fontes uma vez:

```bash
./gradlew genSources
mkdir -p /tmp/mcsrc && unzip -q -o "$MC_SOURCES" -d /tmp/mcsrc
grep -rn "MemoryModuleType.JOB_SITE" /tmp/mcsrc/net/minecraft | head -20
```

**Quem usa uma memória conta mais sobre o sistema do que a declaração dela.**

## 5. Montar a cadeia

```text
GATILHO      o que faz isso começar
   ↓
SENSOR       quem percebe, com que frequência
   ↓
MEMÓRIA      onde fica, qual tipo, quanto dura
   ↓
ACTIVITY     qual modo permite
   ↓
TASK         quem age, com qual gate de memórias
   ↓
AÇÃO         o que muda no mundo
   ↓
LIMPEZA      o que é esquecido depois
```

Marque cada elo com `[FATO]` ou `[HIPÓTESE]`. Elo em hipótese é onde investigar a
seguir — `javap` se for assinatura, gametest se for ordem ou estado.

## 6. Se faltar fato sobre o Vanilla — pare

```text
minecraft-villager-systems → detecta lacuna → minecraft-code-research
                          → investiga → documenta → volta
```

**Não invente API.** Os gatilhos de parada:

```text
[ ] não sei qual memória guarda isto
[ ] não sei qual task executa
[ ] não sei quem escreve a memória
[ ] não sei em qual Activity isso roda
[ ] não sei a assinatura da classe envolvida
```

## 7. Achar o ponto de extensão

Com a cadeia montada, a pergunta fica fácil:

```text
Quero que ele NOTE outra coisa       → sensor + memória      (degrau 2)
Quero que ele LEMBRE de outra coisa  → memória               (degrau 2)
Quero que ele FAÇA outra coisa       → task acrescentada     (degrau 2+6)
Quero que ele faça em outro MOMENTO  → consultar a Schedule
Quero que ele trabalhe em outro BLOCO → POI + profissão      (degrau 2)
Quero IMPEDIR algo Vanilla           → Mixin, justificado    (degrau 9+)
```

As cinco primeiras não tocam o Vanilla.

## 8. Registrar

Um documento em `docs/research/`, com:

```text
[ ] o comportamento descrito em linguagem de jogo
[ ] a cadeia completa, elo a elo
[ ] cada elo etiquetado, com fonte e versão
[ ] o ponto de extensão escolhido, e o degrau
[ ] o que ficou em hipótese, e como validar
```

## Fechamento

Você deve conseguir responder, sem abrir código de novo:

```text
[ ] o que o aldeão sabe, e como
[ ] onde isso é armazenado
[ ] quem decide e quem executa
[ ] em qual camada eu vou mexer
[ ] por que não preciso mexer nas outras
```

A última é a que evita o dano colateral.
