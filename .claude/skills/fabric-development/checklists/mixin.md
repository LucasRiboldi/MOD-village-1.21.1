# Checklist — Mixin

> Complementa `workflows/mixin-workflow.md`. Nenhum item se marca por impressão.

## Justificativa

```text
[ ] passei pela escada de extensão e REGISTREI o motivo de cada alternativa não servir
[ ] "não procurei" não preencheu nenhuma linha
[ ] o motivo está num documento (templates/mixin-plan.md), não só na conversa
[ ] escolhi o tipo MENOS invasivo que resolve
[ ] o escopo é mínimo: o Mixin faz UMA coisa e DELEGA
```

## Alvo verificado

```bash
unzip -l "$MC_JAR" | grep "<Classe>.class"
javap -s -cp "$MC_JAR" <fqn> | grep -A1 "<metodo>"
```

```text
[ ] a classe existe NESTA versão
[ ] o método existe, com assinatura copiada do javap
[ ] havendo sobrecarga, o descriptor está no method =
[ ] o alvo não é lambda, classe anônima nem método sintético
[ ] se o alvo é default de interface, estou mirando a interface
```

## Injeção

```text
[ ] ponto escolhido conscientemente
[ ] se RETURN: o efeito é IDEMPOTENTE (roda por return, não uma vez)
[ ] considerei @WrapOperation antes de @Redirect
[ ] não assumo índice de lista
[ ] não removo comportamento Vanilla (ou está justificado)
[ ] não cancelo (ou está justificado)
```

## Configuração

```text
[ ] registrado no *.mixins.json
[ ] package correto
[ ] compatibilityLevel bate com o Java do projeto
[ ] mixin de cliente na lista "client", não na comum
[ ] injectors.defaultRequire: 1
[ ] o *.mixins.json está listado no fabric.mod.json
```

> `require = 0` só se a ausência do alvo for esperada **e tratada**. Mixin que
> silenciosamente não aplica vira bug fantasma meses depois.

## Conteúdo

```text
[ ] nome do método com prefixo modid$
[ ] a LÓGICA NÃO mora no Mixin — ele delega
[ ] nenhuma regra de negócio no corpo
[ ] nenhuma exceção escapa (capturada e logada do outro lado)
```

## Degradação

```text
[ ] sei o que acontece se o injector não aplicar
[ ] sei o que acontece se o código lançar
[ ] SEM o Mixin, o resultado é COMPORTAMENTO VANILLA — não estado quebrado
```

## Compatibilidade

```text
[ ] classificado LOW / MEDIUM / HIGH
[ ] se @Redirect ou @Overwrite: sei que são EXCLUSIVOS e justifiquei
[ ] sei quais mods conhecidos miram esta classe
[ ] conflito conhecido está DOCUMENTADO, não escondido
```

## Verificação

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGametest
```

```text
[ ] compila
[ ] o log de BOOT não tem aviso de mixin
[ ] o comportamento novo acontece
[ ] o comportamento Vanilla preservado CONTINUA acontecendo   ← o que separa
[ ] gametest cobrindo o caso
[ ] testado em runServer, não só runClient
```

## Documentação

```text
[ ] mixin-plan.md preenchido e guardado
[ ] comentário no código dizendo por que o extension point não bastou
[ ] risco de versão registrado
```

## Sinais de que algo está errado

```text
[ ] @Overwrite em método grande
[ ] @Redirect em método chamado por muita gente
[ ] regra de negócio dentro do Mixin
[ ] Mixin em classe de outro mod sem dependência declarada
[ ] copiado de outro mod sem verificar o alvo nesta versão
[ ] MAIS DE DOIS OU TRÊS Mixins para uma única feature
```

> O último é o mais importante: indica arquitetura lutando contra o Vanilla. A
> resposta está na escada de extensão, não em mais um injector.
