# Eventos e entrypoints da Fabric

Eventos são o degrau 5 da escada de extensão: integração sem tocar em bytecode
Vanilla. Antes de escrever Mixin, esgote esta página.

## Entrypoints

```json
"entrypoints": {
  "main":   ["com.exemplo.MeuMod"],
  "client": ["com.exemplo.MeuModClient"],
  "server": ["com.exemplo.MeuModServer"],
  "fabric-gametest": ["com.exemplo.gametest.MeuGameTest"]
}
```

| Entrypoint | Interface | Roda em |
|---|---|---|
| `main` | `ModInitializer` | ambos |
| `client` | `ClientModInitializer` | só cliente |
| `server` | `DedicatedServerModInitializer` | só servidor dedicado |
| `fabric-gametest` | — | servidor de teste |

**Código que toca classe de render vai no `client`, nunca no `main`.** No
servidor dedicado, o `main` é carregado e a referência a classe de cliente lança
`NoClassDefFoundError` no boot.

**O entrypoint de gametest não pode apontar para classes fora do jar publicado.**
Um servidor dedicado carrega `fabric-gametest` no boot; apontar para o que não
existe derruba o servidor antes de o mod iniciar. Por isso gametest merece
sourceset e `fabric.mod.json` próprios.

## Achar o evento certo

**Não afirme que um evento não existe sem procurar.** Os sources vêm com o
projeto:

```bash
find . -path "*loom-cache/remapped_mods*" -name "*-sources.jar" | head -40

J=$(find . -name "fabric-lifecycle-events-v1-*-sources.jar" | head -1)
unzip -l "$J" | grep "\.java$"
unzip -p "$J" net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.java
```

Trinta segundos, e a resposta é `[FATO]` em vez de memória.

## Mapa por necessidade

| Preciso de | Módulo |
|---|---|
| servidor iniciando/parando, tick de servidor/mundo | `fabric-lifecycle-events-v1` |
| chunk carregando/descarregando, entidade entrando no mundo | `fabric-lifecycle-events-v1` |
| morte, conversão, dano | `fabric-entity-events-v1` |
| uso/quebra/interação de bloco pelo jogador | `fabric-events-interaction-v0` |
| packets tipados | `fabric-networking-api-v1` |
| tipo de entidade, block entity, POI, atributos | `fabric-object-builder-api-v1` |
| sincronizar registro do mod | `fabric-registry-sync-v0` |
| datagen | `fabric-data-generation-api-v1` |
| itens/fluidos entre blocos | `fabric-transfer-api-v1` |
| comandos | `fabric-command-api-v2` |
| testes de jogo | `fabric-gametest-api-v1` |

`v1`/`v2`/`v0` no pacote é **versão de API**, não de módulo. Use a que o resto do
projeto usa; misturar `v1` e `v2` do mesmo assunto confunde quem dispara quando.

## Eventos verificados em 1.21.1

Confirmados em código que compila com Fabric API `0.116.15+1.21.1`:

```java
// net.fabricmc.fabric.api.event.lifecycle.v1
ServerLifecycleEvents.SERVER_STARTED.register(server -> { ... });
ServerLifecycleEvents.SERVER_STOPPING.register(server -> { ... });

// net.fabricmc.fabric.api.entity.event.v1
ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> { ... });
ServerLivingEntityEvents.MOB_CONVERSION.register(...);
```

### A armadilha que só a leitura revela

**Zumbificação é conversão, não morte.** Um aldeão mordido por zumbi passa por
`MOB_CONVERSION` e **nunca** dispara `AFTER_DEATH`.

Consequência prática: um mod que só escuta `AFTER_DEATH` corrige o caso raro
(morte por dano) e mantém o bug no caso mais comum de jogo real. Pior, passa em
todos os testes.

Sempre que reagir a "a entidade se foi", pergunte: **por quantos caminhos ela
pode ir embora?** Morte, despawn, descarga de chunk e conversão são quatro.

## Como assinar

```java
public final class ServerLifecycleHandler {

    private ServerLifecycleHandler() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleHandler::onStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleHandler::onStopping);
    }

    private static void onStarted(MinecraftServer server) { ... }
    private static void onStopping(MinecraftServer server) { ... }
}
```

O registro do evento vive na classe do evento; o `onInitialize` só chama
`register()`. Assim, acrescentar um evento não significa alterar o entrypoint.

## Antes de usar um evento

```text
QUANDO dispara exatamente?   antes ou depois do efeito?
Em qual SIDE?                a maioria dos lifecycle é server-side
É CANCELÁVEL?                qual o tipo de retorno?
Qual a ORDEM entre mods?     ← geralmente indefinida
O que se meu listener lançar?
```

## Ordem entre mods

**Não é garantida.** Lógica que depende de rodar antes ou depois de outro mod é
frágil por construção.

```text
✓  idempotente — o resultado é o mesmo em qualquer ordem
✓  independente — não assume estado que outro pode ter mudado
✗  "eu rodo primeiro, então posso assumir X"
```

Se a dependência de ordem for inevitável, é `[RISCO]` a declarar em
`compatibility.md` — não um detalhe.

## No seu listener

```java
private static void onDeath(LivingEntity entity, DamageSource cause) {
    if (!(entity instanceof VillagerEntity villager)) return;      // filtre cedo
    if (!(villager.getWorld() instanceof ServerWorld world)) return;

    try {
        fazerOTrabalho(world, villager);
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] falha ao processar morte de aldeão", falha);
    }
}
```

```text
[ ] filtre cedo, saia cedo — o evento dispara para tudo
[ ] confirme o side com instanceof ServerWorld (ganha o tipo certo de brinde)
[ ] não lance: sua exceção pode derrubar o tick de quem disparou
[ ] não faça trabalho pesado — o evento roda na thread do servidor
```

## Quando não há evento

Conclusão legítima, se escrita assim:

```text
Não há evento em <módulos consultados>, versão <X>. Listei os .java dos dois.
O caminho restante é <composição / herança / Mixin>.
```

O que não vale é "a Fabric não tem" sem ter aberto os jars.
