# Plano de teste — <feature / sistema>

> Escolha os níveis **proporcionalmente ao risco**. Nem toda mudança precisa de
> todos; toda mudança precisa de alguns.

**Data:** AAAA-MM-DD · **Minecraft:** <versão>

## O que precisa ser verdade

<Em linguagem de comportamento, não de código. "O aldeão deposita o que colheu no
baú reservado dele" — não "o método `deposit` retorna true".>

1.
2.
3.

## Níveis

| # | Nível | Aplica? | Como | Resultado |
|---|---|---|---|---|
| 1 | compile | | `./gradlew build` | |
| 2 | game start | | `runClient` · `runServer` | |
| 3 | feature | | | |
| 4 | save / load | | fechar e reabrir o mundo | |
| 5 | multiplayer | | cliente → servidor dedicado | |
| 6 | regressão | | | |
| 7 | performance | | com carga realista | |

Guia rápido:

```text
texto/config       1, 2
item novo          1, 2, 3
bloco com estado   1–4
Mixin              1–3, 6 + gametest
persistência       1–4, 6
networking         1–3, 5
IA                 1–3, 6, 7
migração           todos
```

## Testes de unidade

<Lógica que não depende do Minecraft. — não se aplica: <por quê>>

| Teste | Cobre |
|---|---|

## Gametest

| Teste | Cobre | `batchId` |
|---|---|---|

```bash
./gradlew runGametest
```

### O que o gametest NÃO cobre

> Declare. O mundo de teste é **vazio**: sem vila gerada, sem estruturas, sem
> bioma real. Esconder isso faz um teste verde significar menos do que parece.

<…>

## Edge cases

```text
[ ] alvo sumiu entre a decisão e a ação
[ ] chunk descarregou no meio
[ ] jogador quebrou o bloco envolvido
[ ] entidade morreu / foi convertida
[ ] recurso indisponível
[ ] dois atores disputando o mesmo alvo
[ ] servidor reiniciou no meio
[ ] valor no limite (0, máximo, negativo)
```

## Verificação manual

<O que precisa de sessão de jogo. Passos reproduzíveis.>

1.
2.

**Quantas vezes observado:** <reproduzido uma vez não é reprodutível>

## Regressão

<O que funcionava e precisa continuar funcionando.>

```text
[ ] outras features do mesmo sistema
[ ] os comportamentos Vanilla que o mod preserva
[ ] save/load
[ ] multiplayer
```

## Performance

| Cenário | Métrica | Esperado | Medido |
|---|---|---|---|
| 1 entidade | | | |
| 10 | | | |
| 50 | | | |
| 100 | | | |

## Resultado

> **Separe explicitamente.** "Tem teste" e "foi verificado rodando" são coisas
> diferentes.

**Verificado rodando:**

**Tem teste escrito (não executado agora):**

**Não verificado, e por quê:**

---

**Nunca declare que passou sem ter executado.** Se o build falhou, mostre a saída.
