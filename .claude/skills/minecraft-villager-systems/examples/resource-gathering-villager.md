# Exemplo — aldeão mineiro

**Pedido:**

> "Quero um aldeão mineiro que cave, traga minério e guarde."

O caso mais pedido, e o que mais se beneficia de desenhar antes de escrever. O
pedido tem **três sistemas** escondidos numa frase.

---

## 1. Decompor

```text
DESCOBERTA    onde há minério
EXTRAÇÃO      cavar
ARMAZENAMENTO onde guardar
```

Misturá-los produz uma task gigante que faz tudo e não é testável. Separá-los
permite trocar cada parte — "o mesmo mineiro cava outro minério" vira uma linha.

## 2. Escopo: profissão

| Peça | Resposta |
|---|---|
| bloco de trabalho | **sim** — a mina, ou uma bancada |
| reivindicação | **sim** — um mineiro por base |
| Schedule | **sim** — cava no horário de trabalho |
| identidade visual | **sim** |
| trades | **não** — produz para a colônia |

> `[DECISÃO]` Profissão, **sem trades**. Um aldeão de trabalho puro não precisa
> de trades, níveis ou XP — o Vanilla não exige, e declarar isso simplifica o
> desenho inteiro.

## 3. A máquina de estados

Explícita, senão os estados implícitos produzem aldeão travado:

```text
IDLE → SEARCHING → TRAVELING → MINING → RETURNING → DEPOSITING → IDLE
```

| Transição | Condição | Falha | Timeout |
|---|---|---|---|
| IDLE→SEARCHING | horário de trabalho | — | — |
| SEARCHING→TRAVELING | achou alvo | nada achado → volta a IDLE | N ciclos |
| TRAVELING→MINING | chegou | inalcançável → esquece o alvo | `MAX_RUN_TIME` |
| MINING→RETURNING | inventário cheio ou veio esgotada | bloco sumiu → volta a SEARCHING | |
| RETURNING→DEPOSITING | chegou ao baú | baú sumiu → procura outro | |
| DEPOSITING→IDLE | esvaziou | baú cheio → procura outro | N tentativas |

**Estados impossíveis inalcançáveis por construção:** viajando sem destino,
depositando sem carga.

## 4. Descoberta — o gargalo

```text
✗  varrer 32³ por mineiro, por ciclo   ← ~32 mil leituras × N mineiros
```

Alternativas, em ordem:

1. **varredura incremental** — um pedaço por ciclo, com **cursor persistido**
2. cache com validade
3. índice por evento

> `[DECISÃO]` Varredura incremental. Dezessete passagens espalhadas não aparecem
> no tick; feitas de uma vez, aparecem.
>
> **O cursor precisa persistir.** Sem isso, cada sessão recomeça do zero e as
> sessões curtas nunca completam uma volta — o sistema parece não funcionar.

E o raio: **cúbico**. Metade do raio é 1/8 do custo.

## 5. Reserva entre mineiros

Dois mineiros cavando o mesmo bloco é bug.

```text
[ ] a reserva vive na COLÔNIA, não no aldeão
[ ] tem dono (UUID) e validade
[ ] liberada ao concluir
[ ] liberada na morte E na conversão
[ ] liberada se o bloco sumir
```

> Sem expiração, uma reserva órfã bloqueia o veio para sempre — e o sintoma é
> "os mineiros pararam", horas depois, sem causa aparente.

`[FATO]` O Vanilla usa esse padrão: `ticketCount` do POI é uma reserva com
contagem.

## 6. Extração — a regra estreita

**Bloco quebrado por engano é dano irrecuperável no save do jogador.**

```text
✗  "minérios num raio de 32"
✓  "só blocos da tabela de minérios, dentro da galeria da colônia,
    nunca acima do nível da vila, e nunca bloco posto pelo jogador"
```

A segunda é auditável e recusável. A primeira derruba a casa de alguém.

```text
[ ] não quebra bloco posto pelo JOGADOR
[ ] não quebra bloco da vila gerada
[ ] as exceções estão escritas, com o motivo
[ ] o que ele repõe (tocha, escada, suporte)?
```

## 7. Leitura segura

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

`world.getBlockState` **força carga de chunk** — numa varredura, isso é gerar
terreno repetidamente.

## 8. Armazenamento

```text
[ ] a posição do baú é REDESCOBERTA, não persistida (existe no mundo)
[ ] o baú é reservado a ele
[ ] baú quebrado → solta a reserva
[ ] baú cheio → procura outro, ou para
[ ] o mod NÃO mexe no que o jogador pôs lá
```

## 9. Falhas — todas com resposta

```text
alvo sumiu           → esquecer, procurar outro
inalcançável         → desistir após N tentativas
inventário cheio     → depositar antes
baú cheio            → procurar outro
baú quebrado         → soltar a reserva
chunk descarregou    → pular (caso NORMAL)
anoiteceu            → a Schedule assume
inimigo perto        → PANIC assume
morreu/convertido    → liberar TODAS as reservas
servidor reiniciou   → o ciclo recomeça, sem estado inconsistente
```

As duas de horário e ameaça são as mais esquecidas — e produzem o mineiro que
cava de madrugada durante um raid.

## 10. Persistência

| Estado | Persiste | Por quê |
|---|---|---|
| profissão de mineiro | **sim** | não existe no Vanilla |
| cursor da varredura | **sim** | senão a sessão recomeça |
| fronteira da galeria | **sim** | senão ele recava o aberto |
| alvo atual | **não** | intenção do momento |
| posição do baú | **não** | existe no mundo |
| reserva de bloco | depende | expira de qualquer forma |

**Dados relacionados por id no mesmo arquivo** — mineiro aponta para colônia, os
dois juntos.

## 11. Performance

```text
[ ] varredura escalonada entre mineiros
[ ] raio mínimo
[ ] pathfinding só quando o destino muda ou falha
[ ] testado com 1, 2, 10, 50
```

O teste de **2** pega a disputa; o de **50**, o custo.

## 12. Testar

```text
[x] ele encontra minério
[x] dois mineiros não cavam o mesmo bloco
[x] ele deposita
[x] não quebra construção do jogador
[x] não quebra a vila gerada
[x] baú cheio: ele não trava
[x] alvo sumido: ele desiste
[x] o ciclo do dia continua: dorme, come, socializa
[x] em raid ele se esconde e o trabalho cede
[x] fechar e reabrir: cursor e galeria preservados; alvo não
[x] 50 mineiros: TPS estável
```

---

## O que este exemplo demonstra

1. **Uma frase escondia três sistemas.** Separá-los é o desenho.
2. **A máquina de estados explícita** evita o aldeão travado.
3. **"Sem trades" é uma decisão válida e simplificadora.**
4. **O cursor persistido** é o detalhe que faz a varredura incremental funcionar
   de verdade.
5. **A regra de quebra estreita** protege o save do jogador — o único erro aqui
   que não é recuperável.
6. **Reserva com expiração**, na colônia, não no aldeão.
