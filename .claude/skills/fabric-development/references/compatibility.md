# Compatibilidade

Seu mod não roda sozinho. O jogador tem outros trinta, e boa parte dos relatórios
que você vai receber começa com "não funciona junto com...".

**Compatibilidade não é o que se conserta depois.** É consequência direta do
degrau da escada de extensão que você escolheu — decidida na arquitetura, não no
suporte.

## O degrau determina o risco

| Degrau | Risco |
|---|---|
| Sistema Vanilla, registro, data-driven | **baixo** — feito para receber extensão |
| Fabric API / eventos | **baixo a médio** — ordem entre mods é a dúvida |
| Composição, interface, herança | **baixo** |
| `@Accessor` / `@Invoker` | **baixo** |
| `@Inject` raso, sem cancelar | **médio** |
| `@ModifyVariable` / `@ModifyArg` / `@Redirect` | **alto** |
| `@Overwrite` | **muito alto** |

Cada degrau que você sobe compra poder e vende convivência.

## As sete fontes de conflito

### 1. Colisão de Mixin

| Tipo | Convivem |
|---|---|
| `@Accessor`, `@Invoker` | sim |
| `@Inject` | geralmente, ordem indefinida |
| `@Modify*` | frágil |
| `@Redirect` | **não — exclusivo** |
| `@Overwrite` | **não** |
| `@WrapOperation` | sim, encadeia |

Dois `@Redirect` no mesmo alvo: um dos mods não roda. Sem aviso que o jogador
entenda.

```bash
grep -rn "@Redirect\|@Overwrite\|@ModifyVariable\|@ModifyConstant" src/main/java
```

Cada ocorrência é um risco a declarar, com a classe alvo nomeada.

### 2. Colisão de registro

Namespace próprio resolve o óbvio. O sutil é **registro condicional**: config
diferente entre cliente e servidor → ids diferentes → conexão cai no handshake.
Ver `registration.md`.

### 3. Ordem de evento

**Não é garantida** entre mods. Lógica que depende de rodar antes ou depois de
outro mod é frágil por construção.

```text
O resultado é o mesmo em qualquer ordem?
Eu assumo estado que outro pode ter mudado?
E se outro rodar depois e desfizer o que eu fiz?
```

Idempotência vale mais que prioridade.

### 4. Substituição de dados

- **Tags** são **aditivas** com `"replace": false`. Baixo risco.
- **Loot tables e recipes** são **substituídas por id**. Dois mods editando a
  mesma loot table Vanilla: um perde, silenciosamente.

### 5. Estado estático

Estado global vaza entre saves e pode ser alterado por caminho que você não
previu. Ver `persistence.md`.

### 6. Premissas de client/server

Assumir que o cliente tem o mod, ou que o servidor tem, quebra em setups mistos.
`environment` no `fabric.mod.json` declara a intenção — e precisa bater com o
código.

### 7. Versão

Mudança de MC, Fabric API ou mappings quebra Mixins, assinaturas e formato de
dados. Ver `migration.md`.

## Vizinhos prováveis

Alguns mods aparecem sempre. Não é lista de inimigos — é onde olhar primeiro:

- **Mods de aldeão** (VillagerConfig, Easy Villagers, Guard Villagers) — disputam
  exatamente os mesmos alvos: `VillagerEntity`, Brain, POI, profissões, trades.
- **Mods de performance** (Lithium, Sodium e afins) — reescrevem caminhos quentes
  do Vanilla, inclusive IA e POI. Mixin em método interno de IA tem chance real
  de colidir.
- **Mods grandes** (Create) — muitos touchpoints, e o vizinho mais provável.

## Reduzir risco sem perder a feature

```text
[ ] escolha o degrau mais baixo que resolve
[ ] @WrapOperation em vez de @Redirect
[ ] ACRESCENTE, não substitua      setTaskList adiciona sem remover Vanilla
[ ] não assuma índice de lista     outro mod pode ter inserido antes
[ ] degrade em silêncio            capture, logue, siga com comportamento Vanilla
[ ] nenhuma exceção sua escapa de método Vanilla
[ ] declare dependências reais no fabric.mod.json
[ ] documente o conflito conhecido
```

O penúltimo é o padrão que mais rende: **se a sua integração falhar, o resultado
deve ser o Vanilla** — que é exatamente o estado de antes da sua mudança.

## Classificar

Toda entrega sai com classificação:

**LOW** — extensão por mecanismo previsto; nada exclusivo; sem premissa de ordem;
degrada para Vanilla.

**MEDIUM** — `@Inject` em classe popular, dependência de ordem, ou substituição de
dado Vanilla. Funciona com quase tudo, com cenários conhecidos.

**HIGH** — `@Redirect`/`@Overwrite`, alteração de comportamento central, premissa
sobre estado que outro mod pode mudar. Exige **justificativa escrita e plano de
degradação**.

Risco sem classificação é decorativo. Com classificação, é decisão.

## Documentar o conflito

Conflito conhecido e documentado é **suporte**: o jogador lê e entende. Conflito
não documentado é um relatório de bug confuso que consome mais tempo do que teria
custado escrever a linha.

```text
Incompatível com <mod X>: ambos injetam em <classe>. Sintoma: <o que acontece>.
Contorno: <se houver>.
```

## Checklist

Detalhamento em `checklists/compatibility.md`.

```text
[ ] Mixins listados e classificados
[ ] nenhum @Redirect/@Overwrite sem justificativa
[ ] namespace próprio, registro incondicional e determinístico
[ ] a lógica não depende de ordem entre mods
[ ] tags com "replace": false
[ ] nenhum JSON Vanilla sobrescrito sem declarar
[ ] nenhum estado de mundo em static
[ ] environment coerente
[ ] testado em runServer
[ ] testado com pelo menos um mod do mesmo domínio, se houver
[ ] classificação final LOW/MEDIUM/HIGH registrada
```
