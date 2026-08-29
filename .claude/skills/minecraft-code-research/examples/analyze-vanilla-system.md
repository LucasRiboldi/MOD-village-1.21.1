# Exemplo — analisar um sistema Vanilla

**Pedido do usuário:**

> "Meu aldeão ignora o destino que eu mando. Chamo `startMovingTo` e ele anda dois
> blocos e volta. Como faço ele ir até onde eu quero?"

Este exemplo mostra o método inteiro num caso real, com os comandos que foram
efetivamente rodados e as saídas que eles deram em MC 1.21.1.

---

## Fase 0 — Enquadrar

O pedido parece uma pergunta de API ("qual método uso?"). Não é: é uma pergunta
sobre **quem tem autoridade sobre o movimento do aldeão**. Enquadrar assim já
muda o que vale ler.

```text
SISTEMA         movimento de aldeão
COMPORTAMENTO   fazer o aldeão andar até uma posição escolhida pelo mod
VERSÃO          1.21.1
MAPPINGS        Yarn 1.21.1+build.3
OBJETIVO        modificar comportamento
MODO            FEATURE
```

## Fase 1 — Inventário

```bash
grep -E "minecraft_version|yarn_mappings|loader_version|fabric_version" gradle.properties
```

```text
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.19.3
fabric_version=0.116.15+1.21.1
```

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
MAPPINGS=$(find ~/.gradle/caches/fabric-loom -name "mappings.tiny" | head -1)
```

## Fase 4 — Vanilla: qual sistema decide o movimento?

Primeira pergunta, e a que muda tudo: **o aldeão usa `Goal` ou `Brain`?**

```bash
javap -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -i "brain\|goal"
```

Aparece `initBrain` e `Brain<VillagerEntity>`; não aparece `initGoals`.

> `[FATO]` VillagerEntity usa o sistema `Brain` em 1.21.1.
> Fonte: `javap` sobre `minecraft-merged` 1.21.1 / yarn 1.21.1+build.3.

Isso já explica metade do problema. `getNavigation().startMovingTo(...)` é a API
do sistema **antigo**. Num mob de Brain, o cérebro reavalia o destino a cada tick
seguindo a agenda dele — e sobrescreve o que a navegação recebeu.

> `[INFERÊNCIA]` O aldeão "anda dois blocos e volta" porque a navegação direta é
> reescrita no tick seguinte pelo Brain.

### Quem controla o caminho, então?

O Brain guarda conhecimento em memórias. A memória de destino:

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i "WALK\|LOOK"
```

Confirma `WALK_TARGET` e `LOOK_TARGET`.

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.WalkTarget
```

> `[FATO]` `WalkTarget` recebe um `LookTarget`, uma velocidade (`float`) e um
> alcance de conclusão (`int`).

E as tasks Vanilla de movimento só agem quando a memória está posta — o que
fecha a explicação:

> `[INFERÊNCIA]` Quem manda no caminho de um mob de Brain é a memória
> `WALK_TARGET`. Escrever nela é falar a língua do sistema; chamar a navegação é
> falar por cima dele.

## Fase 5 — Mappings

Antes de escrever qualquer linha, confirmar o método de registro de task:

```bash
grep -P "\tsetTaskList$" "$MAPPINGS" | head
grep -cP "\tsetTaskList$" "$MAPPINGS"
```

```text
m  (Lcom;ILcom/google/common/collect/ImmutableList;)V         a  method_18882  setTaskList
m  (Lcom;ILcom/google/common/collect/ImmutableList;Lccs;)V    a  method_24527  setTaskList
m  (Lcom;Lcom/google/common/collect/ImmutableList;)V          a  method_18881  setTaskList
...
5
```

> `[FATO]` `setTaskList` tem **cinco** sobrecargas em 1.21.1. Um Mixin que mire
> este método precisa de descriptor.

> **Lição de método, aprendida errando:** a primeira versão desta análise disse
> "três", porque o comando usava `grep -m3` e parava no terceiro resultado. O
> número saiu da ferramenta, não do jogo. **Conte antes de afirmar quantidade**
> (`grep -c`), e confira contra o `javap`, que lista todas:
>
> ```bash
> javap -p -cp "$MC_JAR" net.minecraft.entity.ai.brain.Brain | grep -c setTaskList
> ```
>
> Um `[FATO]` com fonte ainda pode estar errado se a fonte foi lida pela metade.

E a classe base de task:

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.task.MultiTickTask
```

```text
protected boolean shouldRun(ServerWorld, E);
protected void    run(ServerWorld, E, long);
protected boolean shouldKeepRunning(ServerWorld, E, long);
public final void tick(ServerWorld, E, long);
public MultiTickTask(Map<MemoryModuleType<?>, MemoryModuleState>, int, int);
```

> `[FATO]` `tick`, `tryStarting` e `stop` são `final` — o ciclo é dirigido pelo
> Brain. O que se sobrescreve é `shouldRun` / `run` / `shouldKeepRunning`.

## Fase 6 — A Fabric resolve?

```bash
find . -path "*loom-cache/remapped_mods*" -name "*-sources.jar" | grep -i "entity\|lifecycle"
J=$(find . -name "fabric-entity-events-v1-*-sources.jar" | head -1)
unzip -l "$J" | grep "\.java$"
```

Há eventos de morte, conversão e sleep — **nada sobre destino de IA**.

> `[FATO]` Não há API Fabric para controlar destino de mob de Brain. Procurei em
> `fabric-entity-events-v1` e `fabric-lifecycle-events-v1`, versão 0.116.15+1.21.1.

## Fase 7 — Escada de extensão

| Degrau | Avaliação |
|---|---|
| 1. Sistema Vanilla | **é aqui** — a memória `WALK_TARGET` existe para isso |
| 2. Registro | não se aplica |
| 3. Data-driven | não se aplica |
| 4–5. Fabric API/eventos | não cobre |
| 6–8. Composição/interface/herança | task própria estendendo `MultiTickTask` |
| 9–11. Mixin | necessário **só** para instalar a task no Brain |

A descoberta principal: o problema não pedia Mixin para *mover* o aldeão. Pedia
escrever numa memória. O Mixin entra apenas para **registrar a task**, uma vez,
no `initBrain`.

## Fase 15 — Fluxo resultante

```text
o mod decide um destino
        ↓
guarda em um registro próprio (por UUID do aldeão)
        ↓
task no Brain lê o registro          ← shouldRun
        ↓
escreve WALK_TARGET                  ← run
        ↓
tasks Vanilla de movimento agem
        ↓
task mantém a memória enquanto valer ← shouldKeepRunning
        ↓
destino cumprido → registro limpo → memória para de ser reposta
```

## Conclusão

**Resposta ao usuário:** o aldeão não ignora o destino — o Brain o sobrescreve.
Em 1.21.1, escreva `MemoryModuleType.WALK_TARGET` e **mantenha** a memória
enquanto o destino valer. Não use `getNavigation().startMovingTo`.

**Recomendação:** degrau 1 + 6 (mecanismo Vanilla + task própria), com um Mixin
mínimo em `initBrain` só para instalar.

**Riscos:**

- `[RISCO]` MEDIUM — outros mods de aldeão também mexem no Brain. Mitigação:
  `setTaskList` **acrescenta**, não substitui; não remover task Vanilla; não
  assumir índice de lista.
- `[RISCO]` LOW — se a instalação falhar, capturar e logar: um aldeão sem a task
  é um aldeão Vanilla, que é o estado de antes.

**Ainda em aberto:**

- `[HIPÓTESE]` A task em `Activity.CORE` roda também durante `PANIC`.
  `[VALIDAÇÃO NECESSÁRIA]` gametest com um zumbi próximo.

---

## O que este exemplo demonstra

1. **A pergunta do usuário não era a pergunta certa.** "Qual método uso" virou
   "quem tem autoridade sobre o movimento" — e só a segunda tem resposta.
2. **Uma verificação de 30 segundos (`javap`) matou a hipótese errada** e
   redirecionou a pesquisa inteira.
3. **A escada evitou o Mixin grande.** A primeira intuição seria injetar no
   movimento; o mecanismo Vanilla já existia.
4. **O "não existe API Fabric" foi verificado**, não afirmado de memória — e por
   isso pôde ser escrito como `[FATO]`.
