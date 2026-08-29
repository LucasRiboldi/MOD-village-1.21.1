# Plano de Mixin — <alvo>

> Preencha **antes** de escrever, e guarde junto do código. Mixin sem este
> documento é Mixin que ninguém vai saber remover quando a API cobrir o caso.

**Minecraft:** <versão> · **Mappings:** <…> · **Data:** AAAA-MM-DD

## A escada de extensão

> Verificada nesta ordem, com evidência de cada "não". **"Não procurei" não
> preenche nenhuma linha.**

| # | Alternativa | Existe? | Por que não serve |
|---|---|---|---|
| 1 | sistema Vanilla | | |
| 2 | registro Vanilla | | |
| 3 | data-driven (tag/JSON) | | |
| 4 | Fabric API | | |
| 5 | Fabric Events | | |
| 6 | composição | | |
| 7 | interface existente | | |
| 8 | herança / método protected | | |
| — | access widener | | |

**Justificativa do Mixin:** <por que os degraus acima não bastam>

## Escopo

> A pergunta não é "Mixin sim ou não" — é **"Mixin para quê"**. Reduza até: faz
> uma coisa, em três linhas, e delega.

<O que exatamente este Mixin faz. E o que ele NÃO faz.>

## Alvo

| | |
|---|---|
| Classe | `net.minecraft.…` |
| Método | |
| Descriptor | |
| Verificado com | `javap -s -cp "$MC_JAR" <fqn>` em AAAA-MM-DD |

```text
[ ] a classe existe nesta versão
[ ] o método existe
[ ] a assinatura veio do javap, não da memória
[ ] se há sobrecarga, o descriptor entra no method =
[ ] o alvo não é lambda, classe anônima nem método sintético
```

## Injeção

| | |
|---|---|
| Tipo | `@Accessor` / `@Invoker` / `@Inject` / `@WrapOperation` / `@Modify*` / `@Redirect` / `@Overwrite` |
| Ponto | `HEAD` / `TAIL` / `RETURN` / `INVOKE` / `FIELD` |
| Cancelável | sim / não |
| Prioridade | |

> `TAIL` roda uma vez; `RETURN` roda por return. Se usar `RETURN`, o efeito
> precisa ser **idempotente**.
>
> Precisa envolver uma chamada? **`@WrapOperation` antes de `@Redirect`** — ele
> encadeia; o outro é exclusivo.

## Expected state

<O que já é verdade quando o seu código roda. Se você assume algo que outro mod
pode ter mudado, isso é risco.>

## Modificação

<O que muda no comportamento Vanilla. E o que é **preservado**.>

```text
[ ] não remove task/comportamento Vanilla
[ ] não assume índice de lista
[ ] não cancela (ou está justificado)
```

## Failure mode

**Se o injector não aplicar:** <o que acontece>
**Se o código lançar:** <o que acontece>

```text
[ ] injectors.defaultRequire: 1   ← falha alto em vez de silenciar
[ ] exceção capturada e logada, nunca propagada para o Vanilla
[ ] sem o Mixin, o resultado é COMPORTAMENTO VANILLA — não estado quebrado
```

## Version risk

<Quão provável é este alvo mudar? `INVOKE`/`FIELD` quebram com refactor interno
que não muda assinatura nenhuma.>

## Compatibility risk

**Classificação:** LOW / MEDIUM / HIGH

| Pergunta | Resposta |
|---|---|
| O tipo é exclusivo (`@Redirect`/`@Overwrite`)? | |
| Quais mods conhecidos miram esta classe? | |
| A lógica depende de ordem entre mods? | |

<Se HIGH: justificativa e plano de degradação, obrigatórios.>

## Test

```text
[ ] compila
[ ] o log de BOOT não tem aviso de mixin
[ ] o comportamento novo acontece
[ ] o comportamento Vanilla preservado CONTINUA acontecendo
[ ] gametest cobrindo o caso
[ ] runServer, não só runClient
```

## Decisão

**Fazer / não fazer:** <…>
**Se não, o que no lugar:** <…>

---

> **Sinal de alarme:** mais de dois ou três Mixins para uma única feature indica
> que a arquitetura está lutando contra o Vanilla em vez de se encaixar nele. A
> resposta está na escada, não em mais um injector.
