# Workflow — investigar performance

"Está com lag" é sintoma, não diagnóstico. Este workflow existe para não
otimizar por palpite — otimização sem medição costuma acertar o código errado e
deixar o gargalo intacto.

Referência: `references/performance.md`.

---

## 1. Confirmar que é o mod

```text
[ ] reproduzido COM o mod
[ ] reproduzido SEM o mod → o lag some?
[ ] mesma versão, mesmo mundo, mesma quantidade de entidades
```

Sem essa comparação você pode passar o dia otimizando o inocente. Servidor
carregado, chunk sendo gerado e outros mods produzem exatamente o mesmo sintoma.

## 2. Caracterizar

Números, não adjetivos:

```text
QUANDO acontece?          sempre / com N entidades / em certo bioma / após X tempo
ONDE?                     perto da vila / ao carregar chunk / ao abrir tela
QUANTAS entidades?
QUANTOS jogadores?
TPS medido                /debug ou o profiler do servidor
É TPS baixo ou FPS baixo? ← são problemas diferentes
```

**TPS baixo é servidor; FPS baixo é cliente.** Confundi-los manda a investigação
para o lado errado logo no começo. Lógica de mod costuma dar TPS; render costuma
dar FPS.

Travamento periódico com TPS normal costuma ser **GC**, não lógica — ver passo 6.

## 3. Medir

```text
/debug start
… reproduzir por 20-30 segundos …
/debug stop
```

O relatório sai em `debug/` e mostra onde o tick gastou o tempo. Um profiler de
JVM dá mais detalhe quando necessário.

**Procure o caminho quente, não o código feio.** Impressão de lentidão erra mais
do que acerta: o laço que parece caro roda uma vez por sessão, e o método de três
linhas roda vinte vezes por segundo por entidade.

## 4. Os suspeitos, em ordem de frequência

| Suspeito | Como confirmar |
|---|---|
| **Acesso a chunk forçando carga** | `grep -rn "world.getBlockState\|world.getBlockEntity" src/` |
| **Lógica por tick** | `grep -rn "tick\|TICK" src/main/java \| head -30` |
| **Varredura de bloco em volume** | procure laços aninhados sobre x/y/z |
| **Iteração global de entidades** | busca sem caixa, ou com raio enorme |
| **Pathfinding repetido** | recalculando por tick em vez de por mudança |
| **Alocação em caminho quente** | `new BlockPos` em laço, lista por tick |
| **Packets demais** | sincronização por tick por entidade |

### O primeiro é o mais comum e o mais grave

```java
// ✗ carrega o chunk que faltar — do tick, isso é gerar terreno no laço
BlockState state = world.getBlockState(pos);

// ✓ só lê o que já está carregado
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

Chamado de dentro de um evento de carga de chunk, o primeiro **trava a thread**:
ela passa a esperar por um chunk que só ela poderia produzir. O sintoma não é lag,
é o servidor parado.

## 5. Fazer a conta

```text
custo por tick = custo unitário × entidades × frequência
```

Escreva os números:

```text
"busca de raio 32 (≈32k leituras de bloco), por aldeão, a cada tick,
 com 20 aldeões = 13 milhões de leituras por segundo"
```

Com a conta escrita, a correção costuma ficar óbvia. Sem ela, a discussão é sobre
qual código parece pesado.

Teste mentalmente com **1, 10, 50, 100, 500**. O autor quase sempre testou com 2.

## 6. Corrigir — em ordem de retorno

**1. Reduzir a frequência.** O ganho maior e mais barato.

```text
EVERY TICK → PERIODIC (20/100/600 ticks) → EVENT DRIVEN → ON DEMAND
```

**2. Escalonar.** Se N entidades precisam de trabalho periódico, distribua:
`if (entity.getId() % intervalo == tick % intervalo)`. Custo total igual, pico por
tick dividido por N. **Picos são o que o jogador sente.**

**3. Reduzir o escopo.** Raio é cúbico: dobrar multiplica o volume por 8. Metade
do raio é 1/8 do custo.

**4. Indexar.** Mantenha o índice por evento (bloco colocado/quebrado) em vez de
varrer. Para camas e locais de trabalho, o `PointOfInterestStorage` já é um índice
espacial — perguntar a ele é muito mais barato que varrer blocos.

**5. Varrer incrementalmente.** Um pedaço por ciclo, guardando o cursor. Dezessete
passagens espalhadas não aparecem no tick; feitas de uma vez, aparecem.

**6. Cachear.** Com validade explícita — cache sem invalidação vira bug de estado
velho, que é pior que lentidão.

**7. Reduzir alocação.** `BlockPos.Mutable`, evitar lista por tick. **Só em
caminho quente** — otimizar alocação no que roda uma vez por sessão é ruído.

## 7. Medir de novo

```text
[ ] uma mudança por vez
[ ] mesma medição de antes, mesmas condições
[ ] o ganho foi registrado em número
```

**Duas mudanças juntas e você não sabe qual funcionou** — nem se uma delas
piorou.

## 8. Verificar que não quebrou

Otimização muda comportamento com mais frequência do que se admite:

```text
[ ] a feature ainda funciona
[ ] o comportamento é o mesmo, só mais barato
[ ] gametest passa
[ ] chunk não carregado é tratado (pular, não falhar)
[ ] cache invalida quando deve
```

Cuidado específico: trocar leitura forçada por leitura de chunk carregado
introduz o caso `null`. Tratar como "não sei agora, pulo" é quase sempre certo —
mas precisa ser **tratado**.

## 9. Documentar

```text
[ ] o gargalo encontrado
[ ] os números antes e depois
[ ] a mudança
[ ] o que ficou como limite conhecido
```

Isso evita que a próxima sessão "otimize" de volta o que foi feito de propósito.

## Fechamento

`checklists/performance.md`.

Relate com **números medidos**. "Ficou mais rápido" não é resultado; "o tick do
scanner caiu de 12 ms para 0,4 ms com 20 aldeões" é.
