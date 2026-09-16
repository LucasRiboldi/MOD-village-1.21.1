# E45 — A mina presa na boca

> Análise do playtest de **2026-09-16, 03:44–08:23** (log `latest.log`,
> 341.196 linhas). Escrito para ser executado numa sessão seguinte: a
> causa está provada, a correção está proposta, nada foi alterado no
> código ainda.

---

## 1. O que aconteceu

A colônia rodou **4h40 e não concluiu uma única construção**. O mineiro
girou em falso a sessão inteira, do primeiro minuto ao último.

| Medida | Valor |
|---|---|
| Linhas do log | 341.196 |
| `hit stone with nowhere to stand` | 166.559 |
| `no miner branch work` | 166.558 |
| **Soma das duas** | **97,6% do log** |
| Cadência | **10/s, constantes, por 16.657 s sem interrupção** |
| `Miner … took` (pedra quebrada) | **0** |
| `went one level deeper` | **0** |
| Construções concluídas | **0** |

O log traz o par sempre na mesma ordem, em rodízio pelos três mineiros:

```text
[04:27:04] Miner 9e7fd8b4 hit stone with nowhere to stand - the branch ends here
[04:27:04] COLONY: no branch work
[04:27:04] Miner d492ef6b hit stone with nowhere to stand - the branch ends here
[04:27:04] COLONY: no branch work
[04:27:04] Miner fe9a0ec2 hit stone with nowhere to stand - the branch ends here
```

## 2. O custo real — não é o log

O log é o sintoma barulhento. O prejuízo foi a vila parar de construir:

- **Cobblestone: 411 → 10**, e nunca reposto. Quem repõe é o mineiro.
- **Builder parou 46 vezes** por `no minecraft:cobblestone`, 57 por `dirt`.
- A obra em `2442,64,-2972` chegou a **12 blocos restantes** e morreu.

É o padrão de `casa-nao-sobe-e-fome-nao-travamento` por outra porta: **o
mineiro travado mata o builder**.

## 3. O mecanismo, tique a tique

Provado por leitura do código e conferido contra o log. Vale enquanto
`Mine.branchesOpenNow() == 1`, isto é, enquanto o poço não foi cavado.

1. Mineiro A pede braço. `MineClaims.claimArm` dá o braço 0 — `taken[0] = A`,
   e `open.test(0)` é `true` porque `done == false`.
2. A cava, bate no emparedado, `MineArm.blockedAgain(8)` chama `finish()`:
   `done = true`. → **log `hit stone`**.
3. Mineiro B pede. Em `claimArm`, `taken[0]` não é B, e `open.test(0)` agora
   é `false`. Nenhum braço serve → vazio. → **log `no branch work`**.
4. `MineDigging.nextTarget` chama `Mine.deepenIfEveryOpenArmIsDone()`.
   Como `branchesOpenNow() == 1`, **só o braço 0 conta**: `everyOpenArmIsDone()`
   é `true`. O método fecha os quatro braços e chama `deepenIfEveryArmIsDone()`,
   que desce e roda `restartAt` em todos — **`done = false`, `cut = 0`**.
5. O braço 0 está aberto de novo, **com o cursor de volta na boca da mina**,
   no mesmo bloco emparedado. Volta ao passo 1.

### A aritmética que fecha o caso

- `CARVED = HELIX_SIDE(5) × STAIR_HEADROOM(3) × STAIR_LANES(2) × HELIX_FLIGHTS(4) = 120`.
  O poço só é considerado aberto — e `branchesOpenNow()` só passa de 1 para 4 —
  depois de **120** posições vencidas.
- `BLOCKED_BEFORE_TURNING = 8`. O braço fecha na oitava recusa e `restartAt`
  devolve `cut = 0`.
- **O cursor nunca passa de 8 num universo que exige 120.** A mina não tinha
  como abrir o poço; o laço era inevitável, não azar.

### Por que `went one level deeper` não aparece

`MineDigging` só registra a frase quando `deepenIfEveryOpenArmIsDone()`
retorna `true`. Esse retorno é o de `deepenIfEveryArmIsDone()`, que devolve
`false` quando a mina **não pôde descer de fato**. O estado foi reiniciado
(`restartAt` rodou) sem que o retorno dissesse isso. **O reinício é mudo**:
acontece 166 mil vezes e não deixa uma linha.

## 4. Duas hipóteses testadas e descartadas

Ficam registradas para não serem repetidas.

- **Não é o conserto do `ColonyEdits`** (2026-09-16, `MinerWork.java:704`).
  Ele só age depois que a picareta pega. Com **zero** quebras, esse guarda
  nunca chegou a rodar.
- **Não é o lenhador via `PlayerWorldChangeHandler.reopenFrom`.** Ele
  derrubou ~65 árvores/hora, cerca de uma a cada 55 s. **Uma causa de 1/55 s
  não sustenta um efeito de 10/s.**
- **Não é o ramo do fundo da mina** (`!shaft.mayDeepen()` → `rerouted()`).
  Todos os Y do log estão entre **59 e 65** — superfície. `DEEPEST` é `-59`,
  ~120 blocos abaixo. Esse ramo nunca foi tomado, e o teste que o cobre
  (`theDeepestLevelRotatesInsteadOfRepeatingTheSameBlockedPattern`) está
  correto e continua valendo.

## 5. O defeito de fundo: guarda que o caminho de falha zera

Durante **4h40 inteiras** os três contadores de segurança do mineiro
ficaram parados em zero:

```text
stall 0/2400, still 0/300, adrift 0/400
```

Nenhum chegou perto de disparar. O caminho de falha zera os contadores no
mesmo tique em que falha, de modo que nenhum limite é alcançável.

**Guarda que o caminho de falha zera não é guarda.** Esta é a terceira volta
do mesmo laço — 19.193 linhas em 02:58 de 09-16, nove desistências em 09-11,
agora 166.559. Cada conserto anterior fechou **uma porta específica**
(`arm.digging()` cedo demais; a picareta vista como edição do jogador). O
contador que deveria pegar *qualquer* porta nunca disparou em nenhuma das
três.

---

## 6. Correção proposta

Quatro itens. Os três primeiros são independentes e podem entrar juntos; o
quarto é decisão do autor.

### C1 — O reinício do braço não pode ser mudo 🔴

**Problema.** `restartAt` roda 166 mil vezes sem deixar rastro, e por isso o
laço só foi visível pelo efeito colateral.

**Correção.** Registrar em `Mine.deepenIfEveryArmIsDone` quando os braços são
reiniciados **sem que a mina desça** — o caso `!shaft.mayDeepen()` e o caso em
que `deepened()` não muda o nível. Uma linha por reinício, com o `y` de
origem e destino.

**Prova.** Teste que chama o reinício com a mina impedida de descer e afirma
que o evento foi comunicado (contador ou callback observável no core, sem
depender do logger do Fabric).

### C2 — Repetição sem progresso tem de custar 🔴

**Problema.** O braço fecha e reabre no mesmo `cut`, indefinidamente, e
nenhum contador sobrevive ao `restartAt` (`blocked = 0`).

**Correção.** Dar à `Mine` um contador de **reinícios sem picareta**, que
`restartAt` **não zera** e que só `MineDigging.pickaxeTook` zera — mesmo dono
do zeramento adotado em 2026-09-11, pelo mesmo motivo: quem zera é o bloco
saindo do mundo, não o servir da posição. Ao passar de um limite (sugestão: 3),
a mina para de servir aquele desenho e toma a saída de C4.

**Prova.** Teste que fecha o braço N+1 vezes sem nunca chamar `pickaxeTook` e
afirma que a mina deixou de devolver a mesma posição.

### C3 — `everyOpenArmIsDone` não pode aceitar 1 de 4 como "todos" 🟠

**Problema.** Com `branchesOpenNow() == 1`, o braço 0 sozinho satisfaz
`everyOpenArmIsDone()`, e o método então chama `finish()` nos outros três
— que **nunca foram trabalhados**. Fechar braço não trabalhado para declarar
o nível terminado é o que aciona o reinício global a cada oitava recusa.

**Correção.** Exigir que o braço 0 tenha **avançado** (por exemplo,
`cut() > BLOCKED_BEFORE_TURNING`, ou pelo menos uma quebra registrada) antes
que o fechamento dos outros três seja permitido. Sem avanço, o caso não é
"nível terminado", é "boca intransponível" — e pertence a C4.

**Prova.** Teste que fecha o braço 0 com `cut()` abaixo do limiar e afirma que
os outros três **continuam abertos** e que a mina **não** reinicia.

⚠️ **Cuidado:** o comentário em `Mine.deepenIfEveryOpenArmIsDone` documenta um
limbo real de 2026-09-04 ("não posso entregar / não posso descer") que essa
lógica existe para evitar. A correção precisa preservar aquele caso — por isso
o critério é *avanço*, não simplesmente exigir os quatro braços.

### C4 — Decisão do autor: o que fazer quando a boca é intransponível 🟠

**O caso.** O poço exige 120 posições; a primeira é emparedada e não há
onde o aldeão fique de pé. Hoje a resposta é repetir para sempre.

Três saídas possíveis, e **a escolha é sua**:

| Saída | O que faz | Preço |
|---|---|---|
| **(a) Mudar a boca** | `MineSite.mouthOf` procura outra coluna e a mina renasce ali | Abandona o poço iniciado; boca pode migrar |
| **(b) Girar a hélice no mesmo nível** | Reusa `MineShaft.rerouted()`, que já existe para o fundo | Mantém a boca; pode girar em vão se o impedimento for a boca |
| **(c) Desistir por um prazo** | A mina dorme N tiques e o mineiro cai no `exposedStone` | A colônia raspa pedra exposta e sobrevive, mas não cava |

**Recomendação:** **(b) primeiro, (a) como escalada** — girar é barato e
reusa código já testado; se as quatro hélices falharem, a boca é que está
ruim, e aí (a) se justifica. (c) fica como rede de segurança de C2, para a
colônia nunca ficar sem pedra nenhuma enquanto isso se resolve.

---

## 7. O que falta — estado para a próxima sessão

**Nada foi alterado no código.** O repositório está como estava ao fim do
ciclo de 2026-09-15; esta análise é o único artefato novo.

**Ordem sugerida:** C1 (torna o laço visível) → C3 (impede o reinício
indevido) → C2 (dá custo à repetição) → C4 (decisão + implementação).

**Antes de implementar C4, o autor precisa escolher** entre (a), (b) e (c).

**Não verificado nesta sessão:** nenhum build, teste ou gametest foi
executado — a sessão foi de leitura de log e código. Os números de
`STATE.md` (884 unitários, 335/335 GameTests) são do ciclo anterior e
**não** foram reconfirmados aqui.

**Ponto de partida concreto:**

- `Mine.deepenIfEveryOpenArmIsDone` — `src/main/java/com/villagecolony/core/construction/model/Mine.java:298`
- `Mine.deepenIfEveryArmIsDone` — `Mine.java:255`
- `MineArm.restartAt` — `MineArm.java:252`
- `MineClaims.claimArm` — `MineClaims.java:126`
- `MineDigging.nextTarget` — `MineDigging.java:134`
