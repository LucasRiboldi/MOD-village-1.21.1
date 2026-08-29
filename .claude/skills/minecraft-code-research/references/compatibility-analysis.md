# Análise de compatibilidade

Seu mod não roda sozinho. O jogador tem outros trinta, e a maioria dos relatórios
que você vai receber começa com "não funciona junto com...".

Compatibilidade não é o que se conserta depois. É consequência direta de qual
degrau da escada de extensão você escolheu — e portanto se decide durante a
pesquisa.

## As sete fontes de conflito

### 1. Colisão de Mixin

A mais comum. Dois mods no mesmo alvo:

| Tipo | Convivem? |
|---|---|
| `@Accessor`, `@Invoker` | sim |
| `@Inject` | geralmente sim, ordem indefinida |
| `@ModifyVariable`, `@ModifyArg`, `@ModifyConstant` | frágil — encadeiam de forma imprevisível |
| `@Redirect` | **não** — exclusivo por construção |
| `@Overwrite` | **não** — o último vence, os outros somem |
| `@WrapOperation` (MixinExtras) | sim, encadeável |

Dois `@Redirect` no mesmo alvo: um dos mods simplesmente não roda. E não há aviso
que o jogador entenda.

Auditar:

```bash
grep -rn "@Redirect\|@Overwrite\|@ModifyVariable\|@ModifyConstant" src/main/java
```

Cada ocorrência é um `[RISCO]` a declarar, com a classe alvo nomeada.

### 2. Colisão de registro

Mesmo `Identifier` de dois mods. Namespace próprio resolve — usar `minecraft:`
para conteúdo próprio é colisão garantida.

Mais sutil: **registro condicional**. Um mod que registra só se uma config estiver
ativa produz ids diferentes entre cliente e servidor, e a conexão cai no
handshake. Ver `registry-analysis.md`.

### 3. Ordem de evento

A ordem entre listeners de mods diferentes **não é garantida**. Lógica que depende
de rodar antes ou depois de outro mod é frágil por definição.

```text
O meu listener assume algum estado que outro pode ter mudado?
E se outro rodar depois e desfizer o que eu fiz?
O resultado é o mesmo em qualquer ordem?
```

Idempotência e independência de ordem valem mais aqui do que qualquer prioridade.

### 4. Substituição de dados

Duas coisas se comportam de formas opostas:

- **Tags** são **aditivas** com `"replace": false`. Baixo risco.
- **Loot tables, recipes, e a maioria dos JSON** são **substituídos** por id. Dois
  mods editando a mesma loot table Vanilla: um perde, silenciosamente.

Ver `data-driven-analysis.md`.

### 5. Estado estático

Estado global do seu mod pode ser lido/alterado por outro caminho que você não
previu, e vaza entre saves. Ver `persistence-analysis.md`.

### 6. Premissas de client/server

Assumir que o cliente tem o mod, ou que o servidor tem, quebra em setups mistos.
`"environment"` no `fabric.mod.json` declara a intenção — e precisa bater com o
código.

### 7. Versão

Mudança de versão do Minecraft, da Fabric API ou dos mappings quebra Mixins,
assinaturas e formato de dados. Ver `mappings.md`.

## Como o degrau escolhido determina o risco

| Degrau | Risco típico |
|---|---|
| Sistema Vanilla, registro, data-driven | **baixo** — feito para receber extensão |
| Fabric API / eventos | **baixo a médio** — ordem entre mods é a única dúvida |
| Composição, interface, herança | **baixo** |
| Accessor / Invoker | **baixo** |
| `@Inject` raso, sem cancelar | **médio** |
| `@ModifyVariable` / `@ModifyArg` / `@Redirect` | **alto** |
| `@Overwrite` | **muito alto** |

Isto é o argumento prático para a escada de extensão: cada degrau que você sobe
compra poder e vende convivência.

## Classificar o risco

Toda recomendação técnica sai com uma classificação:

**LOW** — extensão por mecanismo previsto; nada exclusivo; sem premissa de ordem;
degrada para Vanilla se falhar.

**MEDIUM** — `@Inject` em classe popular, ou dependência de ordem de evento, ou
substituição de dado Vanilla. Funciona com quase tudo, mas há cenários conhecidos.

**HIGH** — `@Redirect`/`@Overwrite`, alteração de comportamento central, premissa
sobre estado que outro mod pode mudar. **Precisa de justificativa escrita e de um
plano** de degradação.

Um `[RISCO]` sem classificação é decorativo. Com classificação, é decisão.

## Mods de referência que merecem atenção especial

Ao analisar compatibilidade, alguns vizinhos aparecem sempre:

- **Mods de aldeão** (VillagerConfig, Easy Villagers, Guard Villagers) — disputam
  exatamente os mesmos alvos: `VillagerEntity`, Brain, POI, profissões, trades.
- **Mods de performance** (Lithium, Sodium e afins) — reescrevem caminhos quentes
  do Vanilla, inclusive IA e POI. Mixin em método interno de IA tem chance real de
  colidir.
- **Mods grandes** (Create) — muitos touchpoints; vale ver como resolvem, e é o
  vizinho mais provável.

Isso não é lista de inimigos: é onde olhar primeiro quando o alvo é aldeão.

## O que investigar

```text
MIXIN COLLISION         quais alvos, que tipo, quem mais mira ali
REGISTRY COLLISION      namespace próprio? registro condicional?
EVENT ORDER             a lógica depende de ordem?
STATIC STATE            estado global vaza?
NETWORK ASSUMPTIONS     assume mod dos dois lados?
CLIENT/SERVER           funciona em servidor dedicado?
DATA FORMAT             substitui JSON Vanilla?
MOD INTERACTION         quem mais mexe neste sistema?
```

## Reduzir risco sem perder a feature

- Escolha o **degrau mais baixo** que resolve.
- Prefira `@WrapOperation` a `@Redirect` quando precisar envolver uma chamada.
- **Acrescente, não substitua**: `setTaskList` adiciona sem remover task Vanilla.
- **Não assuma índice de lista** — outro mod pode ter inserido antes.
- **Degrade em silêncio**: capture a falha e siga com comportamento Vanilla, em
  vez de propagar exceção de dentro de método Vanilla.
- **Declare a dependência** no `fabric.mod.json` quando houver de verdade.
- **Documente o conflito conhecido** em vez de fingir que não existe. Um conflito
  documentado é suporte; um não documentado é um relatório de bug confuso.
