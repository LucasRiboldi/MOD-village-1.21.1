# Análise de Mixin — <alvo>

> Preencha **antes** de escrever o Mixin, e mantenha junto do código. Um Mixin
> sem este documento é um Mixin que ninguém vai saber remover depois.

**Minecraft:** <versão> · **Mappings:** <Yarn + build> · **Data:** AAAA-MM-DD

## Alvo

| | |
|---|---|
| Classe | `net.minecraft.…` |
| Método | `<nome>` |
| Descriptor | `(L…;)V` |
| Verificado com | `javap -s -cp "$MC_JAR" <fqn>` |

> Havendo sobrecarga, o descriptor é **obrigatório** no `method =`.

## Injeção

| | |
|---|---|
| Tipo | `@Accessor` / `@Invoker` / `@Inject` / `@ModifyVariable` / `@Redirect` / `@WrapOperation` / `@Overwrite` |
| Ponto | `HEAD` / `TAIL` / `RETURN` / `INVOKE` / `FIELD` / `CONSTANT` |
| Prioridade | <padrão 1000> |
| Cancelável | sim / não |
| Usa locais | sim / não |

> `TAIL` roda uma vez; `RETURN` roda por return. Método com três saídas executa
> seu código três vezes — se o efeito não for idempotente, é bug.

## Comportamento Vanilla

<O que o método faz hoje, antes de você tocar. Se você não consegue descrever,
não está pronto para modificá-lo.>

## Modificação

<O que muda, exatamente.>

## Por que este Mixin existe

<A justificativa. Esta seção é o motivo do documento existir.>

## Alternativas verificadas

| Degrau | Alternativa | Existe? | Por que não serve |
|---|---|---|---|
| 1 | Sistema Vanilla | | |
| 2 | Registro Vanilla | | |
| 3 | Data-driven (tag/JSON) | | |
| 4 | Fabric API | | |
| 5 | Fabric Events | | |
| 6 | Composição | | |
| 7 | Interface existente | | |
| 8 | Herança | | |
| — | Access widener | | |

> "Não procurei" não preenche nenhuma linha. Ausência afirmada de memória é
> hipótese, e costuma estar errada.

## Side effects

<O que mais é afetado: estado, outros callers, ordem de execução.>

## Modo de falha

**Se o injector não aplicar:** <o que acontece>
**Se o código lançar:** <o que acontece>

> Um Mixin que falha deve degradar para **comportamento Vanilla**, não para
> estado inconsistente. Nunca deixe exceção sua escapar de dentro de um método
> Vanilla — capture e logue.

`injectors.defaultRequire`: <1 = falha alto, recomendado>

## Risco de compatibilidade

**Classificação:** LOW / MEDIUM / HIGH

| Pergunta | Resposta |
|---|---|
| O tipo é exclusivo (`@Redirect`/`@Overwrite`)? | |
| Quais mods conhecidos miram esta classe? | |
| Assume índice de lista ou estado prévio? | |
| Remove ou cancela comportamento Vanilla? | |

## Risco de versão

<Quão provável é este alvo mudar? `INVOKE`/`FIELD` são precisos e frágeis: um
refactor interno que não muda assinatura já quebra o ponto.>

## Estratégia de teste

- [ ] compila
- [ ] o injector aplica (sem aviso no log de boot)
- [ ] o comportamento novo acontece
- [ ] o comportamento Vanilla preservado continua
- [ ] gametest cobrindo o caso
- [ ] testado em `runServer`, não só `runClient`

## Evidência

| Afirmação | Etiqueta | Fonte |
|---|---|---|
| | `[FATO]` / `[HIPÓTESE]` | `javap` / sources / experimento |

## Recomendação

**Fazer / não fazer:** <…>
**Se não, o que fazer no lugar:** <…>
