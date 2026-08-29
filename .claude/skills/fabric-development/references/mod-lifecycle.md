# Ciclo de vida do mod

Cada linha de código tem **um momento certo** para rodar. Colocá-la no momento
errado produz erros que parecem misteriosos e têm sempre a mesma raiz: o que ela
precisava ainda não existia, ou já tinha deixado de existir.

## A linha do tempo

```text
FABRIC LOADER          descobre e ordena os mods
      ↓
MOD INITIALIZATION     onInitialize() — entrypoint main
      ↓
REGISTRATION           registrar conteúdo acontece AQUI
      ↓
CLIENT INITIALIZATION  onInitializeClient() — só no cliente
      ↓
freeze dos registros
      ↓
WORLD LOAD             SERVER_STARTED — agora existe MinecraftServer
      ↓
GAMEPLAY / TICK
      ↓
SAVE
      ↓
SERVER_STOPPING → UNLOAD
```

## O que vai em cada ponto

| Momento | Pode | Não pode |
|---|---|---|
| `onInitialize` | registrar conteúdo, assinar eventos | tocar em `World`, `MinecraftServer`, jogador |
| `onInitializeClient` | render, keybind, tela, model | lógica autoritativa |
| `SERVER_STARTED` | ler save, montar estado do mundo | registrar conteúdo (tarde demais) |
| tick | lógica de jogo | trabalho pesado sem controle de frequência |
| `SERVER_STOPPING` | salvar, limpar estado global | assumir que o processo vai morrer |

**No `onInitialize` nenhum mundo existe.** Qualquer coisa que precise de
`MinecraftServer` ou `ServerWorld` vai num evento de ciclo de vida.

## Registro

```java
@Override
public void onInitialize() {
    ModItems.register();     // incondicional
    ModBlocks.register();    // determinístico
    ServerLifecycleHandler.register();
}
```

Três regras, todas com a mesma raiz — **os ids precisam ser iguais em toda
execução e nos dois lados**:

1. **Incondicional.** `if (config.ativado) register(...)` produz ids diferentes
   entre cliente e servidor e derruba a conexão no handshake.
2. **Determinístico.** Ordem instável (iterar `HashSet`) muda ids numéricos entre
   execuções, e saves antigos passam a apontar para a coisa errada.
3. **Antes do freeze.** Registrar depois lança.

Armadilha de Java: registro por inicialização estática só roda quando a classe é
**carregada**. Se ninguém a toca, nada registra — por isso a chamada explícita.

Ver `registration.md`.

## Entrada e saída do mundo

Os dois pontos onde o estado do mod entra e sai da memória:

```java
public static void register() {
    ServerLifecycleEvents.SERVER_STARTED.register(MeuMod::onStarted);
    ServerLifecycleEvents.SERVER_STOPPING.register(MeuMod::onStopping);
}

private static void onStarted(MinecraftServer server) {
    ESTADO.clear();                       // ← limpar ANTES de carregar
    MeuSavedData data = MeuSavedData.get(server);
    data.entradas().forEach(ESTADO::register);
}

private static void onStopping(MinecraftServer server) {
    MeuSavedData.get(server).sync(ESTADO.all());
    ESTADO.clear();                       // ← e limpar DEPOIS de salvar
}
```

**Limpar nos dois lados, não em um só.** O processo pode abrir outro save sem
reiniciar; sem a limpeza, o estado do mundo anterior vaza para o novo. Esse bug é
invisível em desenvolvimento — o dev reinicia o jogo entre testes — e certo em
produção.

## Ordem de carregamento entre mods

O Loader ordena por dependências declaradas. Fora disso, **a ordem não é
garantida** — e a ordem entre listeners de mods diferentes também não.

Se a sua lógica depende de rodar antes ou depois de outro mod, isso é um risco de
compatibilidade a declarar, não um detalhe. Idempotência vale mais que
prioridade. Ver `compatibility.md`.

## Ciclos que não são o do jogo

Três linhas do tempo distintas, e confundi-las é a origem de quase todo bug de
persistência:

**Entidade**

```text
spawn/load → initialize → tick → interaction → damage → death/conversion → save/despawn
```

**BlockEntity**

```text
place → create → load → tick → markDirty → save → chunk unload → remove
```

**Chunk** — entre o tick e o save existe a descarga: a BlockEntity sai da memória
sem o mundo ser salvo. Estado em memória associado a uma posição some aí.

Um campo que existe pelo tempo da entidade não sobrevive ao restart do servidor.
Um `static` não sobrevive à troca de save. Ver `persistence.md`.

## Eventos úteis por necessidade

| Preciso de | Evento |
|---|---|
| carregar estado do save | `ServerLifecycleEvents.SERVER_STARTED` |
| salvar estado | `ServerLifecycleEvents.SERVER_STOPPING` |
| trabalho periódico global | `ServerTickEvents` — com controle de frequência |
| reagir a chunk | `ServerChunkEvents` |
| entidade entrando no mundo | `ServerEntityEvents` |
| morte | `ServerLivingEntityEvents.AFTER_DEATH` |
| **conversão** (zumbificação) | `ServerLivingEntityEvents.MOB_CONVERSION` |

Os dois últimos são **eventos diferentes para saídas diferentes**. Aldeão mordido
por zumbi é convertido, não morto: quem só escuta `AFTER_DEATH` perde o caso mais
comum. Ver `fabric-events.md`.

## Regra final

Antes de escrever qualquer inicialização, pergunte:

```text
O que este código precisa que exista?
Isso já existe neste ponto do ciclo de vida?
```

Se a resposta for "não sei", é aí que o bug vai nascer.
