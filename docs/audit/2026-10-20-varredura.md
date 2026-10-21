# Varredura — 2026-10-20 (pós-0.3.66)

Leitura estática do código (o terminal estava bloqueado pelo classificador de permissões: sem rodar
testes, PMD ou build nesta varredura). Parte das auditorias de 14-10
([varredura](2026-10-14-varredura-engenharia.md), [reavaliação](2026-10-14-reavaliacao-pos-0359.md)) e
marca só o que é novo ou persiste. Último estado verificado rodando: test 1530/0, runGametest 769/769
(15-10, commit da 0.3.66).

**Achado principal:** os três defeitos graves estão no código das versões **0.3.64 e 0.3.66, que nunca
foram jogadas**. Os testes passaram porque testam a regra pura, e o defeito está no **chamador** que a
usa — o padrão de "teste vazio" já registrado no projeto.

## 🔴 Grave — corrigir antes do próximo playtest

> **✅ G1, G2 e G3 corrigidos na 0.3.67 (20-10)**, com as opções escolhidas pelo autor: G1 — desistiu,
> fabrica no lugar até o fim da tarefa; G2 — a bancada é procurada uma vez, ao pegar a tarefa
> (`CraftStation.Approach`); G3 — o não-mineiro sem degrau natural volta à vila pelo teleporte da cama
> (`ClimbPolicy.giveUp` → `BedTeleport.toBed`), e sem cama é liberado de volta à fila. Verificado: test
> 1529/0; runGametest 769/769; mutação que reintroduz o G1 derruba
> `onceTheBudgetRunsOutItKeepsCraftingHereUntilTheTaskEnds`. **Não jogado.** O G3 não tem teste próprio
> (depende de mundo); a rede é o `ClimbOutGameTest`, que só cobre o mineiro e a escada natural.

### G1 — A salvaguarda da bancada não segura: bancada inalcançável derruba a produção (0.3.66)
- **Onde:** `CraftingWork.atBench` (`CraftingWork.java:327-328`).
- **Defeito:** quando o prazo de ida (`APPROACH_BUDGET`, 200 tiques) estoura, o código zera
  `job.toBench = 0` e fabrica um tique. No tique seguinte o contador volta a 1 e o artesão **volta a
  caminhar** por mais 200. Com a bancada inalcançável, ele fabrica 1 tique a cada ~201; uma peça pede
  `TICKS_PER_PIECE = 20` → **produção cai ~200×** para carpinteiro/pedreiro nesse caso.
- **É exatamente o cenário que a salvaguarda devia proteger** (bancada do outro lado de um desnível).
- **Correção:** estourado o prazo, o artesão fabrica no lugar **até o fim da tarefa** (não zerar o
  contador; zerar só ao chegar ou ao pegar tarefa nova). Teste: o mesmo `Job` em ticks sucessivos.

### G2 — Varredura de ~9.800 blocos por tique por carpinteiro (0.3.66)
- **Onde:** `CraftStation.benchFor` → `bench` (`CraftStation.java:89-125`), chamado por `atBench`
  **todo tique** (`VillageDetectionHandler.tickActiveServer` → `CraftingWork.tick`).
- **Defeito:** sem bancada do mod perto, o carpinteiro cai no `BlockPos.iterate` de 33×9×33 ≈ **9.801
  leituras de bloco** procurando crafting table, mais uma consulta de POI de raio 64 — a cada tique, por
  carpinteiro. O `spotFor` antigo tinha cache (`CHOSEN`, `REFRESH_TICKS = 6000`); o `benchFor` novo
  **pulou o cache**.
- **Risco:** regressão de MSPT proporcional ao número de carpinteiros/pedreiros.
- **Correção:** `benchFor` passar pelo cache do `CHOSEN` (ou cache próprio por trabalhador com o mesmo
  `REFRESH_TICKS`). Medir com o `cost_ledger` no playtest seguinte.

### G3 — Não-mineiro encalhado fica preso para sempre e loga a cada passagem (0.3.64)
- **Onde:** `ClimbPolicy.leaveToNavigation` (chamado de `ClimbOut.stairs`).
- **Defeito:** ao desistir, remove o estado de escalada (`CLIMBS`) mas **não** tira o aldeão de
  `StrandedWorkers` — e só `StrandedEscape` (quando `isOut`) o libera. Ele continua **fora da fila de
  trabalho**; na passagem seguinte `ClimbOut.pass` recria o estado, refaz `naturalStepAround` (169
  colunas, alguns milhares de leituras) e desiste de novo, logando "found no natural step" — **todas as
  passagens, indefinidamente**.
- **Efeito:** o pastor do laço de 0.3.63 trocaria um laço por outro, agora parado e fora da fila; mais
  custo e spam de log.
- **Correção (pede decisão do autor):** ao desistir, liberar do `StrandedWorkers` e voltar à fila, com um
  intervalo antes de poder ser marcado encalhado de novo (senão o detector o remarca em minutos). Alternativa:
  manter encalhado mas pular as passagens por N segundos.

## 🟠 Importante

- **I1 — Pedido das 6 faces atendido pela metade (0.3.65).** O **pedreiro** segue com 3 texturas
  (`cube_bottom_top`: os 4 lados iguais); o **fazendeiro** tem 6 arquivos mas são cópias de 3 (norte =
  oeste, sul = leste, topo = fundo). O pedido era "cada lado com uma imagem diferente".
- **I2 — Pedreiro: modelo de cubo cheio sobre colisão de stonecutter.** `MASON_CUTTER` é
  `StonecutterBlock` (colisão de ~9/16 de altura) com modelo de cubo inteiro: o jogador vê um bloco
  cheio e afunda na metade de cima.
- **I3 — JAR instalado não conferido.** Há um `village-colony-0.3.66.jar` na pasta de mods 1.21.1, mas o
  hash contra o build não foi conferido (terminal bloqueado). Rodar `bash scripts/install-mod.sh`.
- **I4 — Ociosidade na vila montanhosa** (roteiro em memória): 6 dos 7 ofícios ociosos têm raiz na
  altura → **terraplanagem** (plano guardado). Só o fundidor (C4, espera argila inexistente) é tratável
  sem ela.
- **I5 — 35% das recusas de lote são "reserved road area"** (443 mil de 1,27 mi): o índice de ruas
  reserva faixa demais. Sistema separado do relevo, ainda não investigado.
- **I6 — Mina na parede de montanha:** o A3 (0.3.61) é parcial; TOO_HIGH/NO_STANDING seguem.

## 🟡 Melhoria / dívida consciente

- **M1 — Texturas órfãs da 0.3.65:** as `_top/_front/_side` antigas dos 6 blocos que viraram 6 faces
  não são mais referenciadas, e as 6 `mason_cutter_up/down/…` + `mason_cutter_front/top` também não.
- **M2 — `CraftStation.spotFor` é código morto em produção** (só o `CraftStationGameTest` chama). Ao
  corrigir G2, decidir: o `benchFor` reaproveita o cache dele, ou ele sai.
- **M3 — Complexidade restante (F3 parcial):** `MineCuts.nextCut` 46, `BuildSiteScanner.findForFootprints`
  42, `ChestScanner.findFreeChest` 37 — atacar quando um deles for tocado por outro motivo.
- **M4 — Leitores pequenos do save sem `try/catch`** (`WorkMarksSavedData`, `WorkMemorySavedData`):
  risco baixo (getters de NBT devolvem padrão em vez de lançar), mas fora do padrão do AUD-02.

## ✅ Conferido e OK

- **Segredos:** nenhum segredo real rastreado (o único casamento é um placeholder em documentação de
  skill); nenhum `.env`, `.pem` ou `*.key`.
- **Save principal:** protegido (AUD-02): `ColonySaveReader.readEntry/readSection` descartam entrada
  ruim em vez de derrubar o save.
- **Processo:** sem branch acumulada — `main` contém tudo até a 0.3.66 (PR #29).

## Ordem de ataque

1. **G1 + G2 juntos** (mesmo trecho: `CraftingWork.atBench` + `CraftStation.benchFor`) — corrigir,
   testar o `Job` em ticks sucessivos, build, runGametest. Barato.
2. **G3** — decidir o comportamento ao desistir (liberar com intervalo vs. pausar passagens); corrigir.
3. **I3** — instalar com hash conferido; **jogar** 0.3.67 na vila montanhosa (≥30 min, de dia) e medir:
   pastor fora do laço, artesão sem queda de produção, MSPT estável.
4. **I1 + I2 + M1** — fechar o pedido das 6 faces (pedreiro e fazendeiro) e limpar texturas órfãs.
5. **I4** — fundidor (C4), depois a **terraplanagem** (spec própria), depois **I6** e **I5**.
6. **M2–M4** — oportunistas.
