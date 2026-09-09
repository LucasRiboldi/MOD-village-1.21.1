# Falhas conhecidas

Falha conhecida é falha **medida e não corrigida**. Nenhuma entra aqui sem
número de execuções e evidência; nenhuma sai daqui sem correção verificada.

---

## ~~KF-001~~ — `MinerGameTest.aFrozenMinerGivesUpLongBeforeTheStallGuard` era instável

**FECHADO em 2026-09-09, à tarde.** A causa está no fim desta entrada. O que
vem antes fica como estava escrito, inclusive a hipótese errada: é o registro
de uma investigação que passou duas vezes ao lado da resposta.

**Classificação:** P4 (comportamento inconsistente) — mas com efeito P0 sobre a
rede de segurança: **a bateria não é confiavelmente verde**, e isso corrói toda
afirmação de "269/269 passaram".

**Medido em 2026-09-09**, commit `3e367d5`, mesma máquina, mesma sessão:

| lote de execuções | resultado |
|---|---|
| 9 execuções | 1 falha |
| 3 execuções | 2 falhas |
| **total 12** | **3 falhas (~25%)** |

Sempre o mesmo teste. Nenhum outro falhou em nenhuma das 12.

**O que o teste promete.** Um mineiro emparedado por seis faces deve ter a tarefa
devolvida pelo guarda de imobilidade (`STILL_LIMIT` = 300 tiques) muito antes do
guarda de travamento (`STALL_LIMIT` = 2400). Ele afirma no tique 360, com
`tickLimit` 400.

**A evidência do mecanismo.** Duas execuções lado a lado, mesmo aldeão, mesmas
paredes — o que muda é a **velocidade com que o contador anda**:

```
bateria que passou:   still 280/300
bateria que falhou:   still  99/300
```

Não é o guarda que oscila. É **quando ele começa a contar**: o contador só anda
quando o mineiro tem alvo, e ganhar alvo custa uma busca. A busca é um
**orçamento global do servidor** — `MinerWork.SEARCHES_PER_TICK` é 1 para todas
as colônias juntas — e a bateria monta 18 cenários de mineiro. Sessenta tiques de
margem contra uma fila global é pouco.

**Hipótese testada e REFUTADA.** Aumentar a folga (asserção 360 → 700,
`tickLimit` 400 → 800) **piorou**: 2 falhas em 6. A razão provável é que 700
passa da fronteira do ciclo da colônia (600 tiques), e o ciclo **re-reserva** a
tarefa que o guarda havia liberado — a asserção então encontra `RESERVED` de
novo. A folga não pode crescer para além do ciclo, e abaixo dele não há espaço
suficiente. Revertido.

**Por que não foi corrigido.** As duas saídas óbvias são ruins: aumentar a folga
esbarra no ciclo (medido acima), e afrouxar a asserção mascararia o defeito, que
é o que a Regra 33 proíbe. A correção real provavelmente exige desacoplar o teste
do orçamento global — dar alvo ao mineiro sem passar pela busca disputada, ou
isolar o cenário num lote sem outros mineiros. Nenhuma das duas foi tentada.

**Segunda investida, 2026-09-09 — não reproduzida.** Depois de instrumentar o
teste, **18 execuções seguidas passaram**:

| tentativa | execuções | falhas |
|---|---|---|
| descoberta | 12 | 3 |
| com sonda incondicional | 14 | 0 |
| com instrumento condicional | 4 | 0 |
| **total** | **30** | **3, todas nas 12 primeiras** |

A 25%, dezoito verdes seguidos têm ~0,6% de chance. Duas leituras possíveis, e
não há dado para escolher entre elas: ou a instrumentação perturbou o cenário
(Heisenbug), ou as três falhas dependeram de **carga da máquina** — elas
aconteceram durante uma sequência de builds encadeados, e não voltaram no laço
estável.

**Estado: aberto, não corrigido.** Nenhuma alteração de produção foi feita, e a
causa segue sem prova. O que mudou é que a próxima falha vai deixar rastro.

**O que ficou no lugar.** A mensagem de uma asserção de gametest **não aparece no
log da bateria** — o console imprime só o nome do teste. Era por isso que as três
falhas não puderam ser diagnosticadas depois. O teste agora escreve um `WARN` com
`task`, expediente e o relatório do mineiro **quando, e só quando**, a asserção
está prestes a falhar. Bateria verde continua silenciosa.

**Próxima investida, quando ela reaparecer:** ler o `WARN`. Se `task` vier
`RESERVED`, a hipótese é re-reserva pelo ciclo da colônia; se vier `AVAILABLE`
com expediente `false`, é o relógio; se o relatório mostrar `still` baixo, é a
fila global de buscas. As três levam a correções diferentes, e é essa escolha que
faltou hoje.

**O que NÃO fazer:** marcar o teste como ignorado, aumentar `tickLimit` sem
entender, ou reexecutar a bateria até dar verde e chamar isso de aprovação.

---

### A causa, terceira investida — 2026-09-09, à tarde

**O teste media um estado que não era só dele, e a produção nunca esteve
errada.**

A afirmação lia `task.state()` **no tique 360**. Estado de tarefa é global, e
quem mais o escreve é o ciclo da colônia: `runColonyCycles` → `ColonyCycle.run`
→ `WorkAssignment.assign` → `takeOneTask`. A **2ª passagem** desse método
devolve a tarefa ao trabalhador em descanso quando não há mais nada da profissão
dele — é deliberada, está documentada e tem teste que a afirma
(`WorkAssignmentTest.theRestNeverLeavesTheWorkerIdle`).

Então: o guarda de imobilidade devolvia a pedra por volta do tique 305, como
prometido; se a fronteira do ciclo caísse entre esse instante e o tique 360, a
tarefa era **reservada de novo para o mesmo mineiro** e a afirmação encontrava
`RESERVED`. O teste reprovava a produção por ter feito exatamente o que ele
promete medir.

**Por que a taxa era ~10%.** `VillageDetectionHandler.tickCounter` é do servidor
inteiro e dispara a cada 600 tiques. A fase dele quando este lote começa depende
de quantos tiques a bateria gastou nos lotes anteriores — que muda com a carga
da máquina, com quanto cada teste demorou, com tudo. A janela vulnerável tem
umas cinco dezenas de tiques em 600: **3 falhas em 30**, que é a medida.

**As duas leituras anteriores caem por terra:**

| o que se disse | o que era |
|---|---|
| o orçamento global de buscas (`SEARCHES_PER_TICK` = 1, 18 cenários de mineiro) | `miner_stillness` é lote de **um teste só**, e lote roda um de cada vez — o log diz `Running test batch 'miner_stillness:0' (1 tests)`. A busca única do tique era sempre dele |
| `still 280/300` numa bateria e `still 99/300` noutra provam que o contador anda em velocidades diferentes | as duas linhas são o `MinerReport` que o **ciclo da colônia** imprime, e o ciclo cai em fases diferentes. Eram amostras de instantes diferentes, nunca comparáveis. O `99` é ainda melhor que isso: é o contador de um `Job` **novo**, criado depois da re-reserva |
| a folga de 700 piorou por passar do ciclo | **certo**, e era a resposta inteira olhando para o lado. A 700 a fronteira do ciclo é atravessada sempre; a 360, só às vezes |
| 18 verdes seguidos a 25% dão 0,6%, então é Heisenbug | a taxa é **3/30 = 10%**, não 25% — o 25% saiu das mesmas 12 execuções que produziram as falhas. A 10%, dezoito verdes têm 15% de chance. Nunca houve o que explicar |

**Reproduzido sob demanda**, que é o que faltava nas duas investidas anteriores:
forçado o ciclo dentro da janela (`WorkAssignment.assign` no tique 340), a
falha sai com a assinatura exata da que se via — `task=RESERVED`, contador
zerado, `stall 46/2400, still 45/300`.

**Correção — no teste, porque o defeito era dele.** O que se guarda agora é o
**instante em que a tarefa voltou**, registrado no tique em que acontece
(`runAtEveryTick`), e não o estado num tique escolhido. Devolver é evento, e
nada o desfaz depois. E o ciclo passou a ser forçado de propósito no tique 340:
o que era sorteio virou parte do que o teste afirma.

**Fase vermelha conferida.** Com `WorkStall.LIMIT` em 3.000 a bateria acusa
**exatamente um** teste — `minergametest.afrozenminergivesuplongbeforethestallguard`.

**Fica aberto, e é do autor decidir:** em jogo, o descanso de quatro ciclos que
o `giveUp` marca é anulado no ciclo seguinte pela 2ª passagem, sempre que a
colônia não tem outro trabalho da profissão. É o cenário real — mineiro que
larga a pedra e recebe a mesma tarefa de volta —, e é o problema que a ADR-010
existia para tratar. Não foi mexido: a 2ª passagem é decisão de projeto, com
teste que a afirma.
