# A escolha da área de construção — varredura, inconsistências e alternativas

> **2026-09-17.** Varredura do subsistema que decide **onde a casa sobe**,
> pedida pelo autor. Método: skill `minecraft-code-research`, hierarquia
> Vanilla → Fabric → mod. Ambiente conferido: MC **1.21.1**, Yarn
> **1.21.1+build.3**.
>
> ⚠️ **Sobre "como outros mods tratam isso":** MineColonies, Millénaire e
> Ancient Warfare **não estão neste disco** e a busca web não estava
> disponível. Não descrevo o que não li. O que há aqui de externo é
> **Vanilla**, conferido por `javap`, e está etiquetado.

---

## 1. O subsistema, medido

| Arquivo | Linhas | Papel |
|---|---|---|
| `BuildSiteScanner` | **1.509** | Acha o lote. Varredura, índice de ruas, predicados de chão |
| `RoadExtension` | 604 | Estende a rua quando não há lote |
| `SweepLog` | 278 | Conta o custo da varredura |
| `LotRefusals` | 218 | Conta por que cada coluna caiu |
| `ColonyRoads` | 109 | O índice gravado |

⚠️ **`BuildSiteScanner` tem 1.509 linhas contra o teto de 500 do
`CLAUDE.md`**, com **36 métodos** e **5 mapas estáticos de estado**
(`SWEEPS`, `ROADS`, `BUILDING`, `ROAD_CURSOR`, e o `Sweep` embutido).

---

## 2. Inconsistências — com evidência

### 2.1 🔴 O custo da volta é maior que a sessão inteira

**A aritmética, com os números do playtest de 2026-09-17:**

```text
raio 64            → 16.641 colunas
MAX_COLUMNS        → 1.024 por passagem
                   → 16,3 passagens por volta

PlannerTurns.PER_CYCLE = 8, com 36 colônias
                   → a vila tem vez a cada 4,5 ciclos
                   → 73 ciclos × 30 s = 37 MINUTOS por volta
```

`[FATO]` As sessões do autor duraram **28, 31 e 42 minutos**, e as três
fecharam com `0 complete rounds`. **A volta não cabe na sessão.**

`[FATO]` Medido no log: `32 planner runs, 27 passes over 22116 columns,
0 complete rounds`.

⚠️ **Mas os 37 minutos são o pior caso, e não o caso do autor.** Conferido
em `VillageDetectionHandler.coloniesNearPlayers`: quando há jogador online,
as colônias que ele está vendo **entram na cota na frente** das outras. O
autor joga perto da vila, então ela fura a fila — e o log confirma: **32
`planner runs` em 32 minutos** é aproximadamente uma vez por ciclo, não uma
a cada 4,5.

`[INFERÊNCIA]` Com vez em quase todo ciclo, a volta custaria ~8 minutos, e
mesmo assim deu `0 complete rounds` em 42. **A aritmética da cota não
explica sozinha** — o que explica é o §2.2.

### 2.2 🔴 A vila já pagou a volta — e perdeu no reinício

`[FATO]` Mesma linha: **27 passagens feitas**, quando **16,3** bastariam
para fechar o raio. E ao lado: `1 restarts (1 by drift, farthest 40 blocks)`.

`[FATO]` O centro se moveu **40 blocos**, de `2495,65,-3003` para
`2491,64,-3043`, e `CENTER_DRIFT` é **20** — a varredura inteira foi
descartada.

`[FATO]` O log mostra **dois anchors** para a mesma colônia:
`2448,64,-2942` (50×) e `2491,64,-3043` (30×). A colônia oscila entre dois
aglomerados de camas.

⚠️ **É o padrão `obra-orfa-por-deriva-do-centro`, agora atingindo a
varredura em vez da obra.** O trabalho não foi insuficiente: foi jogado
fora.

### 2.3 🟠 Duas réguas para "este chão serve?"

`[FATO]` Dois predicados coexistem no mesmo arquivo:

| Predicado | Conta | Quem usa |
|---|---|---|
| `isNaturalGround(state)` | lista fechada: grama, terra, terra grossa, podzol, tag `SAND` | **`RoadExtension`** (a estrada) |
| `isLotGround(world,pos)` | `isSolidBlock` — qualquer sólido | **`flatGroundAt`** (o lote) |

**A estrada é mais exigente que a casa.** Uma coluna de pedra ou cascalho
serve de lote e **não** serve de estrada.

`[INFERÊNCIA]` Isso pode estreitar o crescimento: a regra do projeto é
"estrada primeiro, casa ligada a ela", então a régua mais apertada governa
a expansão. **Não medi o impacto** — seria preciso contar recusas de ponta
de rua por motivo, e esse contador não existe.

### 2.4 🟠 `LotRefusals` fora do ciclo de vida

`[FATO]` `ServerLifecycleHandler` limpa `RoadExtension`, `BuildSiteScanner`
e `SweepLog`. **`LotRefusals.clearAll` não é chamado em produção** — só nos
gametests.

Há teto de `MAX_COLONIES = 256` e o `report` remove por colônia, então não
é vazamento grave. Mas é **assimetria**: quatro vizinhos são limpos e um
não, e o mapa `ACCEPTED` entrou hoje sem essa cobertura.

### 2.5 🟡 O que eu afirmei antes e estava errado

⚠️ Na sessão anterior escrevi que *"a vila também não consegue estender
estrada"*. **Errado.** Medido agora: `extended/grew the road` = **10**
contra 3 recusas. **A estrada cresce.** A frase `none of the road ends may
be paved` aparece quando as pontas estão em castigo temporário, não em
falha permanente.

---

## 3. Alternativas — o que o Vanilla faz

`[FATO]` O Vanilla **não varre colunas** para colocar estrutura. Ele usa
`StructurePlacement`, com **espaçamento em grade** (`spacing`, `separation`,
`FrequencyReductionMethod`) — a posição sai de uma função do chunk, não de
uma busca.

`[INFERÊNCIA]` Isso não é copiável direto: o Vanilla decide **na geração do
chunk**, com o terreno ainda em ruído, e o mod trabalha em mundo carregado —
é a mesma conclusão que a pesquisa de `terraplanagem-da-vila.md` já
registrou e que fez aquela frente ser descartada.

**Mas a ideia aproveitável é a inversão:** em vez de *procurar onde cabe*,
**derivar candidatos de uma estrutura que já existe** — que é o que o índice
de ruas (`findAmongRoads`) já faz e responde **40 de 40** consultas, contra
a varredura em anéis que nunca fecha.

---

## 4. Opções de correção

| # | Opção | Ataca | Custo | Preço |
|---|---|---|---|---|
| **S1** | **Não descartar a varredura por deriva: transladar o cursor** | §2.2 | **alto** ⚠️ | Ver §4.1 — a aritmética mostra que translação não recupera quase nada |
| **S2** | **Aumentar `PlannerTurns.PER_CYCLE` quando há poucas colônias ativas** | §2.1 | baixo | A cota nasceu para tirar um pico de 214 ms; mexer nela devolve o risco |
| **S3** | **Priorizar no rodízio a colônia que o jogador está vendo** | §2.1 | baixo | Já existe (`watched`) — conferir se está sendo alimentado |
| **S4** | **Unificar as réguas de chão** | §2.3 | baixo | Muda onde a estrada pode crescer; precisa de medição antes |
| **S5** | **Incluir `LotRefusals` no `clearAll`** | §2.4 | trivial | Nenhum |
| **S6** | Contador de recusa por ponta de rua | §2.3 | baixo | Instrumentação, não conserto — mas §2.3 não é decidível sem ele |

### Recomendação, em ordem

1. ✅ **S5 — aplicado nesta sessão.** Trivial, e fecha a assimetria que eu
   mesmo criei hoje ao pôr o `ACCEPTED` no `LotRefusals`.
2. ✅ **S3 — verificado, e já funciona.** `coloniesNearPlayers` alimenta o
   `watched` do `PlannerTurns`, e a vila observada fura a fila. **Nada a
   fazer**, e isso derruba metade do §2.1: com 32 `planner runs` em 32
   minutos, a vila do autor teve vez quase todo ciclo.
3. 🔴 **S1 — é o conserto de verdade.** Com a vez quase todo ciclo, 27
   passagens bastariam para 1,6 voltas; o que as consumiu foi o reinício
   por deriva. **Recuperar o cursor vale mais que qualquer ajuste de cota.**
4. 🟠 **S6 → S4** — medir antes de unificar régua.

⚠️ **Não recomendo S2.** A cota de 8 nasceu de um pico de tique medido em
09-15, e o S3 já resolve o que ela custaria à vila observada. Afrouxá-la
devolveria o problema sem ganho.

---

### 4.1 ⚠️ Por que não implementei o S1

Eu ia escrever a translação do cursor, e a aritmética me impediu. Fica
registrado para ninguém tentar de novo sem ver isto.

O cursor é `(anel, coluna)` **relativo ao centro**. Se o centro anda `D`
blocos, o que foi visto até o anel `R` do centro velho só é **garantidamente
visto** até o anel `R − D` do centro novo (Chebyshev):

| Anel visto | Deriva | Anel seguro no centro novo |
|---|---|---|
| 27 | 40 | **0** |
| 30 | 40 | **0** |
| 60 | 40 | 20 |

**Com a deriva de 40 blocos medida no log, transladar não recupera nada.**
O `CENTER_DRIFT = 20` está, na verdade, sendo conservador com razão.

### 4.2 🔴 E a conta revela outra coisa

`[FATO]` A vila varreu **22.116 colunas** em 27 passagens. Uma volta
completa do raio 64 pede **16.641**. **Ela varreu mais que uma volta
inteira e ainda marcou `0 complete rounds`.**

`[INFERÊNCIA]` O reinício não custou "algumas passagens": custou **a volta
toda, mais um terço**. E como a translação não é viável, o conserto tem de
atacar **a deriva**, não o cursor.

⚠️ **Isso aponta para o C3 do E46**, que está aberto desde ontem: a
detecção vê dois aglomerados de camas (`2448,-2942` e `2491,-3043`) e o
centro oscila entre eles. **Estabilizar o centro conserta a varredura, a
obra órfã e o lote — os três de uma vez.**

**É o item de maior alavancagem do subsistema, e é decisão do autor**,
porque mexe na regra de completude que existe para não encolher a colônia
por leitura pobre (E2, 2026-08-07).

---

## 5. O que fica por medir

- [ ] §2.3 — quantas pontas de rua são recusadas por `isNaturalGround`, e
      quantas passariam por `isLotGround`
- [ ] §2.1 — se `coloniesNearPlayers` está alimentando o `watched` do
      `PlannerTurns` em jogo
- [ ] Se as 35 colônias dormentes deveriam entrar no rodízio: o log mostra
      **uma** colônia reportando atividade e 35 com `assigned 0 tasks`
