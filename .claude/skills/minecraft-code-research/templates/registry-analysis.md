# Análise de registro — <sistema / conteúdo>

> Use ao investigar como um conteúdo entra no jogo, ou ao planejar registrar
> conteúdo novo. Guarde em `docs/research/systems/`.

**Minecraft:** <versão> · **Mappings:** <…> · **Data:** AAAA-MM-DD

## Pergunta

<Ex.: "dá para acrescentar uma profissão sem Mixin?">

## Cadeia

```text
REGISTRY → IDENTIFIER → OBJECT → BOOTSTRAP → REFERENCE
```

| Elo | Valor |
|---|---|
| Registro | `Registries.…` / `RegistryKeys.…` |
| Identifier | `<namespace>:<path>` |
| Objeto | |
| Onde registra | classe · método |
| Quando | entrypoint / evento / estático |
| Como é referenciado | constante / lookup por id / tag |

## Tipo de registro

**Estático** (existe no boot, congela) — `ITEM`, `BLOCK`, `ENTITY_TYPE`,
`BLOCK_ENTITY_TYPE`, `POINT_OF_INTEREST_TYPE`, `MEMORY_MODULE_TYPE`,
`SENSOR_TYPE`, `VILLAGER_PROFESSION`, `SOUND_EVENT`…

**Dinâmico** (por mundo, do datapack ativo) — biomas, features, estruturas,
encantamentos (1.21+), tipos de dano.

**Este caso é:** estático / dinâmico

> Registro dinâmico **não está disponível no `onInitialize`** — nenhum mundo foi
> carregado ainda.

## Janela de registro

- [ ] registrado no entrypoint
- [ ] **incondicional** (nada de `if (config)` — ids divergentes derrubam a conexão)
- [ ] **determinístico** (mesma ordem sempre — ordem instável corrompe ids salvos)
- [ ] antes do freeze
- [ ] a classe é efetivamente carregada (registro estático só roda se alguém tocar a classe)

## Dependências

<Precisa de outro registro antes? Ex.: block entity precisa do bloco.>

## Sincronização com o cliente

| | |
|---|---|
| Precisa existir no cliente? | |
| Sincroniza automaticamente? | |
| `environment` no `fabric.mod.json` | `*` / `client` / `server` |

## Recursos associados

> `JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE`. Os quatro elos, ou o conteúdo
> não está pronto — o bloco existe e aparece como cubo preto e rosa.

- [ ] `lang` (nome traduzido)
- [ ] modelo / blockstate / textura
- [ ] loot table
- [ ] recipe
- [ ] tags
- [ ] som

## Datagen

<Estes recursos são gerados por código? Onde? Arquivo em `src/main/generated/`
não se edita à mão.>

## Extension point

**Este registro aceita entrada de mod?** sim / não / parcial

<Se sim, este é o degrau 2 da escada, e provavelmente dispensa Mixin.>

## Risco

| Risco | Severidade |
|---|---|
| colisão de namespace | |
| registro condicional | |
| ordem instável | |
| recurso ausente | |

## Evidência

| Afirmação | Etiqueta | Fonte |
|---|---|---|

## Conclusão
