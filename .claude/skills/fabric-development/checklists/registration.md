# Checklist — registro

> `JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE` — os quatro elos, ou a entrega
> está incompleta.

## Onde e quando

```text
[ ] no entrypoint (onInitialize)
[ ] antes do freeze
[ ] a classe é EFETIVAMENTE carregada
    (registro estático só roda se alguém tocar a classe — daí a chamada explícita)
```

## As três regras

```text
[ ] INCONDICIONAL — nada de if (config) register(...)
[ ] DETERMINÍSTICO — mesma ordem em toda execução
[ ] namespace próprio, nunca minecraft:
```

> Registro condicional → ids diferentes entre cliente e servidor → **conexão cai
> no handshake**. Ordem instável → ids numéricos mudam → saves antigos apontam
> para a coisa errada.

## Identifier

```text
[ ] usa a constante MOD_ID, não string literal repetida
[ ] path em snake_case
[ ] nenhuma colisão com conteúdo existente
[ ] a factory confere com a versão (Identifier.of é 1.21+)
```

## Tipo de registro

```text
[ ] sei se é estático ou dinâmico
[ ] se dinâmico: NÃO estou assumindo disponibilidade no onInitialize
```

## Dependências de ordem

```text
[ ] BLOCK antes de BLOCK_ENTITY_TYPE
[ ] BLOCK antes de BlockItem
[ ] ENTITY_TYPE antes dos atributos
[ ] ENTITY_TYPE antes do renderer
```

## Registros obrigatórios por tipo

**Bloco**

```text
[ ] Registries.BLOCK
[ ] BlockItem — senão não pode ser obtido nem colocado
```

**Entidade**

```text
[ ] Registries.ENTITY_TYPE
[ ] atributos            ← sem isto, CRASH no spawn
[ ] renderer NO CLIENTE  ← sem isto, some ou crasha ao aparecer
```

**Block entity**

```text
[ ] Registries.BLOCK_ENTITY_TYPE, com o bloco já registrado
[ ] renderer no cliente, se tiver render próprio
```

## Recursos — o quarto elo

```text
[ ] lang            senão "block.meumod.foo" na tela
[ ] modelo de item
[ ] modelo de bloco + blockstate
[ ] textura         senão cubo preto e rosa
[ ] loot table      senão o bloco não dropa nada
[ ] recipe          se aplicável
[ ] tags de mineração
[ ] item group      senão é invisível no criativo
[ ] som             se aplicável
```

## Sincronização

```text
[ ] o conteúdo precisa existir no cliente?
[ ] environment no fabric.mod.json está coerente
[ ] a dependência está declarada, se o cliente precisa do mod
```

## Verificação

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

```text
[ ] o conteúdo aparece no criativo
[ ] o nome está traduzido
[ ] a textura aparece
[ ] o bloco dropa algo
[ ] a ferramenta correta funciona
[ ] o servidor dedicado sobe sem erro
[ ] cliente conecta ao servidor dedicado
```

> O último pega o erro de registro condicional, que só aparece no handshake.

## Sinais de problema

```text
[ ] registro dentro de evento de mundo
[ ] id montado por concatenação condicional
[ ] namespace minecraft:
[ ] registro em bloco estático sem chamada garantida
[ ] registros espalhados por vinte arquivos
[ ] conteúdo sem lang nem modelo
```
