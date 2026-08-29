# Análise da Fabric

O degrau 2 da hierarquia de pesquisa. A pergunta é sempre a mesma: **isto já está
resolvido pela Fabric?** Responder "não" sem ter procurado é como um Mixin
desnecessário nasce.

## Loader ≠ API

Duas coisas distintas, e confundi-las leva a procurar no lugar errado.

| | Fabric **Loader** | Fabric **API** |
|---|---|---|
| O que é | carrega mods, aplica Mixins, resolve dependências | biblioteca de hooks e utilidades, ela própria um mod |
| Dá a você | entrypoints, `ModContainer`, ciclo de carga, Mixin | eventos, registries auxiliares, networking, datagen, transfer, gametest |
| Versão | `loader_version` | `fabric_version` |

Se você procura "um evento para quando o servidor inicia", é API. Se procura
"como meu mod começa a existir", é Loader.

## Ler os fontes da Fabric API

Não dependa de memória nem de doc online: os sources vêm com o projeto.

```bash
find . -path "*loom-cache/remapped_mods*" -name "*-sources.jar" | head -40
```

Os módulos vêm separados, e o nome já diz o assunto — `fabric-lifecycle-events-v1`,
`fabric-networking-api-v1`, `fabric-object-builder-api-v1`,
`fabric-registry-sync-v0`, `fabric-transfer-api-v1`, `fabric-gametest-api-v1`...

Listar o que existe num módulo:

```bash
J=$(find . -name "fabric-lifecycle-events-v1-*-sources.jar" | head -1)
unzip -l "$J" | grep "\.java$"
```

Ler uma classe inteira:

```bash
unzip -p "$J" net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.java
```

Isso transforma "acho que existe um evento" em `[FATO]` com fonte.

## O sufixo de versão importa

`...api.v1`, `...v2`, `...v0` no pacote é **versão de API**, não de módulo. `v1` e
`v2` podem coexistir, com semânticas diferentes; a `v0` costuma ser experimental
ou legada.

Use a versão que o resto do projeto já usa, salvo motivo escrito. Misturar `v1` e
`v2` do mesmo assunto no mesmo mod gera confusão real sobre qual dispara quando.

## Mapa de necessidade → onde procurar

Ponto de partida para a busca. **Confirme no sources jar da sua versão** antes de
afirmar que existe.

| Preciso de | Procure em |
|---|---|
| Servidor iniciando/parando, tick de servidor/mundo | `fabric-lifecycle-events-v1` |
| Chunk carregando/descarregando, entidade entrando no mundo | `fabric-lifecycle-events-v1` |
| Morte, conversão, dano de entidade | `fabric-entity-events-v1` |
| Uso/quebra/interação de bloco pelo jogador | `fabric-events-interaction-v0` |
| Packets, payload tipado, sincronização | `fabric-networking-api-v1` |
| Registrar tipo de entidade, block entity, POI, atributos | `fabric-object-builder-api-v1` |
| Sincronizar registro do mod com o cliente | `fabric-registry-sync-v0` |
| Datagen (recipes, tags, loot, models, lang) | `fabric-data-generation-api-v1` |
| Itens/fluidos/energia entre blocos | `fabric-transfer-api-v1` |
| Comandos | `fabric-command-api-v2` |
| Tags além do Vanilla | `fabric-tag-api-v1`, `fabric-client-tags-api-v1` |
| Biomas, worldgen | `fabric-biome-api-v1` |
| Testes automatizados de jogo | `fabric-gametest-api-v1` |
| Renderização, HUD, keybind, tela | módulos `*-client-*` / `*-rendering-*` |

## Eventos verificados em 1.21.1

Estes foram confirmados em código que compila com Fabric API `0.116.15+1.21.1`:

```java
// net.fabricmc.fabric.api.event.lifecycle.v1
ServerLifecycleEvents.SERVER_STARTED.register(server -> { ... });
ServerLifecycleEvents.SERVER_STOPPING.register(server -> { ... });

// net.fabricmc.fabric.api.entity.event.v1
ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> { ... });
ServerLivingEntityEvents.MOB_CONVERSION.register(...);   // zumbificação NÃO passa por AFTER_DEATH
```

O segundo é um bom exemplo do valor de ler a API em vez de supor: aldeão mordido
por zumbi é **convertido**, não morto, e um mod que só escuta `AFTER_DEATH` perde
o caso mais comum de perder um aldeão. Isso não está óbvio no nome do evento —
está no código.

## Como um evento se comporta

Ao escolher um evento, responda antes de usar:

```text
QUANDO dispara exatamente? (antes ou depois do efeito?)
Em qual SIDE? (a maioria dos lifecycle é server-side)
É CANCELÁVEL? Qual o tipo de retorno?
Qual a ORDEM entre listeners de mods diferentes?
O que acontece se meu listener lançar exceção?
```

A ordem entre mods raramente é garantida. Se a sua lógica depende de rodar antes
ou depois de outro mod, isso é `[RISCO]` de compatibilidade — registre em
`compatibility-analysis.md`.

## Entrypoints

Declarados em `fabric.mod.json`:

```json
"entrypoints": {
  "main":   ["com.exemplo.MeuMod"],
  "client": ["com.exemplo.MeuModClient"],
  "server": ["com.exemplo.MeuModServer"],
  "fabric-gametest": ["com.exemplo.gametest.MeuGameTest"]
}
```

- `main` → `ModInitializer.onInitialize()`, roda nos dois lados.
- `client` → `ClientModInitializer`, **só** no cliente. Código que toca classe de
  render vive aqui, nunca no `main`.
- `server` → `DedicatedServerModInitializer`, só em servidor dedicado.
- `fabric-gametest` → carregado no boot de servidor de teste. Apontar para classe
  que não está no jar publicado **derruba o servidor** — por isso gametest merece
  sourceset e `fabric.mod.json` próprios.

O entrypoint roda **antes** de qualquer mundo existir. Registro de conteúdo vai
aqui; qualquer coisa que precise de `MinecraftServer` ou `World` **não** — vai
num evento de ciclo de vida.

## Quando a Fabric não cobre

Conclusão legítima, desde que escrita assim:

```text
[FATO] Não há evento em fabric-lifecycle-events-v1 nem em fabric-entity-events-v1
para <X>. Procurei listando os .java dos dois módulos na versão 0.116.15+1.21.1.
[INFERÊNCIA] O caminho restante é <composição / herança / Mixin>.
```

O que não vale é "a Fabric não tem" sem ter aberto os jars. Ausência afirmada de
memória é hipótese, e costuma estar errada — a API é grande.
