# Workflow — lag com aldeões

Sintoma: "o servidor trava perto da vila", "o TPS cai com muitos aldeões".

Aldeões são a entidade mais cara do jogo por unidade — cada um roda um Brain
completo. **Uma vila grande já pesa antes do seu mod.**

---

## 1. É o mod?

```text
[ ] reproduzi COM o mod
[ ] reproduzi SEM o mod → o lag some?
[ ] mesma vila, mesmo número de aldeões, mesma versão
```

Sem essa comparação você pode passar o dia otimizando o inocente.

## 2. Caracterizar — com números

```text
QUANTOS aldeões?
QUANTAS vilas carregadas?
QUANDO acontece?         sempre · com N aldeões · ao carregar chunk · após X tempo
É TPS baixo ou FPS baixo?   ← são problemas diferentes
```

**TPS é servidor; FPS é cliente.** Lógica de aldeão dá TPS. Travamento periódico
com TPS normal costuma ser GC (alocação), não lógica.

## 3. Medir

```text
/debug start
… reproduzir 20-30 s perto da vila …
/debug stop
```

**Procure o caminho quente, não o código feio.** O laço que parece caro roda uma
vez por sessão; o método de três linhas roda 20×/s por aldeão.

## 4. Os suspeitos, em ordem

| Suspeito | Como confirmar |
|---|---|
| **acesso a chunk forçando carga** | `grep -rn "world.getBlockState\|getBlockEntity" src/` |
| **busca por tick** | task ou sensor sem controle de frequência |
| **raio grande demais** | raio é cúbico |
| **pathfinding repetido** | recalculado por tick |
| **sensor caro** | área × frequência × população |
| **varredura de bloco** | laços aninhados sobre x/y/z |
| **iteração global de entidades** | busca sem caixa |
| **alocação** | `new BlockPos` em laço |
| **reprodução sem limite** | população crescendo sozinha |

### O primeiro é o mais grave

```java
// ✗ carrega o chunk que faltar — do tick, é gerar terreno no laço
BlockState state = world.getBlockState(pos);

// ✓
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

De dentro de um evento de carga de chunk, o primeiro **trava a thread**. O
sintoma não é lag: é o servidor parado.

## 5. A conta

```text
custo = custo unitário × aldeões × frequência
```

Escreva os números:

```text
"busca de raio 32 (≈32k leituras), por aldeão, a cada tick, com 20 aldeões
 = 13 milhões de leituras por segundo"
```

Com a conta escrita, a correção fica óbvia. Sem ela, a discussão é sobre qual
código parece pesado.

## 6. Corrigir — em ordem de retorno

**1. Reduzir a frequência.**

```text
EVERY TICK → PERIODIC (20/100/600) → EVENT DRIVEN → ON DEMAND
```

**2. Escalonar entre aldeões** — o ganho mais subestimado:

```java
if ((villager.getId() + world.getTime()) % INTERVALO != 0) return;
```

Custo total igual, **pico dividido por N**. Picos são o que o jogador sente.

**3. Reduzir o raio.** Cúbico: metade do raio é 1/8 do custo.

**4. Consultar o `PointOfInterestStorage`** em vez de varrer — se o alvo pode ser
POI, o jogo já tem o índice.

**5. Índice próprio por evento** — bloco colocado/quebrado atualiza.

**6. Varredura incremental** — um pedaço por ciclo, com **cursor persistido**.
Sem persistir, cada sessão recomeça e as curtas nunca completam uma volta.

**7. Cache com validade explícita.** Sem invalidação vira bug de estado velho.

**8. Reduzir alocação** — só em caminho quente.

## 7. Pathfinding

```text
[ ] recalcula só quando o destino muda ou o caminho falha
[ ] há limite de tentativas
[ ] alvo inalcançável é abandonado
```

`[FATO]` `CANT_REACH_WALK_TARGET_SINCE` existe no Vanilla justamente para
desistir. Retry infinito gasta o cálculo mais caro do jogo, para sempre.

## 8. População

```text
[ ] há limite superior de aldeões?
[ ] a reprodução do meu mod tem teto?
[ ] o que acontece com 200 camas?
```

Reprodução é a única mecânica que **cria entidades sozinha** — um gerador de lag
com atraso.

## 9. Medir de novo

```text
[ ] uma mudança por vez
[ ] mesmas condições
[ ] ganho registrado em NÚMERO
```

Duas mudanças juntas e você não sabe qual funcionou — nem se uma piorou.

## 10. Verificar que não quebrou

```text
[ ] a feature ainda funciona
[ ] o caso null (chunk não carregado) é tratado
[ ] o cursor de varredura persiste
[ ] o cache invalida quando deve
[ ] gametest passa
[ ] testado com 1, 10, 50, 100
```

Trocar leitura forçada por leitura de chunk carregado **introduz o caso `null`** —
tratar como "não sei agora, pulo" é quase sempre certo, mas precisa ser tratado.

## 11. Documentar

```text
[ ] o gargalo encontrado
[ ] números antes e depois
[ ] o que ficou como limite conhecido
```

Registrar os números evita que a próxima sessão "otimize" de volta o que foi
feito de propósito.

## Fechamento

`checklists/performance.md`.

Relate com **números medidos**. "Ficou mais rápido" não é resultado; "o scanner
caiu de 12 ms para 0,4 ms com 20 aldeões" é.
