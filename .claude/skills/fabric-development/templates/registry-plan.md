# Plano de registro — <conteúdo>

> Preencha antes de registrar conteúdo novo. A cadeia completa é
> `JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE` — os quatro elos, ou a entrega
> está incompleta.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## O que vai ser registrado

| Objeto | Registro | Identifier |
|---|---|---|
| | `Registries.…` | `meumod:<path>` |

## Tipo de registro

**Estático** (existe no boot, congela) ou **dinâmico** (por mundo, do datapack)?

<Se dinâmico, ele **não está disponível no `onInitialize`**.>

## Onde e quando

| | |
|---|---|
| Classe | `ModItems` / `ModBlocks` / … |
| Chamado de | `onInitialize` |
| A classe é efetivamente carregada? | <registro estático só roda se alguém tocar a classe> |

## As três regras

```text
[ ] INCONDICIONAL      nada de if (config) — ids divergentes derrubam a conexão
[ ] DETERMINÍSTICO     mesma ordem sempre — ordem instável corrompe ids salvos
[ ] ANTES DO FREEZE    no entrypoint
```

## Dependências de ordem

| Registra | Depende de | Motivo |
|---|---|---|
| `BLOCK_ENTITY_TYPE` | o bloco | o builder precisa do bloco |
| `BlockItem` | o bloco | |
| atributos | `ENTITY_TYPE` | sem isso, crash no spawn |
| renderer (cliente) | `ENTITY_TYPE` | sem isso, some ou crasha |

## Sincronização

```text
[ ] precisa existir no cliente?
[ ] sincroniza automaticamente?
[ ] environment no fabric.mod.json está coerente?
```

## Resources — o quarto elo

```text
[ ] lang                     senão aparece o id na tela
[ ] modelo (item e/ou bloco) senão cubo preto e rosa
[ ] blockstate               (bloco)
[ ] textura
[ ] loot table               (bloco) senão não dropa nada
[ ] recipe                   se aplicável
[ ] tags                     ferramenta correta, comportamento condicional
[ ] item group               senão é invisível no criativo
[ ] som                      se aplicável
[ ] renderer no cliente      (entidade / block entity)
```

## Datagen

<Estes resources serão gerados por código? Vale a pena, ou são poucos arquivos
escritos uma vez?>

<Arquivo gerado **não se edita à mão** — o próximo datagen sobrescreve.>

## Riscos

```text
[ ] namespace próprio (nada de minecraft:)
[ ] nenhuma colisão de id conhecida
[ ] nenhum registro condicional
[ ] nenhuma ordem instável
```

## Verificação

```bash
./gradlew build
./gradlew runClient
```

```text
[ ] o conteúdo aparece no criativo
[ ] o nome está traduzido
[ ] a textura aparece
[ ] o bloco dropa algo
[ ] a ferramenta correta funciona
[ ] runServer sobe sem erro
```
