# STATE — 2026-09-20

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Este arquivo chegou a **2.277 linhas** antes de
> 09-19: ele passou a atrasar a sessão que devia orientar.

---

## Onde a vila está

**A fundação mínima agora é automática.** Toda vila adotada cria a estrutura
exclusiva `BigHouseMOD`, uma cópia editada da big house Vanilla sem móveis ou
decorações, com seis camas e seis baús. Os seis titulares (`MINER`,
`LUMBERJACK`, `MASON`, `SMELTER`, `BREEDER` e `BUILDER`) recebem uma cama
`HOME` e um baú distinto dentro dela. Agricultor e carpinteiro continuam
disponíveis no crescimento normal, mas seus conjuntos foram removidos da
BigHouseMOD. A Vanilla continua intacta. A execução isolada de
`VillageFoundationGameTest` confirmou o contrato sem sobrescrever blocos;
falta conferir a criação no save aberto pelo autor.

**A primeira casa subiu.** Sessão de 23:21:58 de 09-19:
`Builder e1770e02 stopped — the house is up`. A obra desceu de 83 blocos
a zero em menos de três minutos, com o construtor que conseguiu ficar.

O bloqueio de madeira que este arquivo descrevia como "o de agora" está
vencido — a cadeia inteira (índice de ruas → Regra 3 → Regra 22 → baú
cheio → pedreiro sem material → arenito → acavalamento → madeira) foi
percorrida e a casa fechou.

E a sessão entregou duas coisas de graça:

- `TEST BARRIER covered for nothing this session — 481 pieces were laid
  and every one came from the colony's own chests. Rule 28 can go.`
- O rodízio de ofícios, corrigido abaixo.

---

## 🔴 O que está aberto

**1. A segunda obra ainda depende de playtest.** Depois da casa, nenhuma obra nova
abriu em 3,5 minutos — `no building work: still sweeping — the budget ran
out before an answer — looking for a lot`. O resumo de saída:

```
sweep: 9 planner runs, 9 passes over 8674 columns, 0 answered by the index
       — 0 complete rounds
lot columns: 30 survived every check, 528 were turned down
lot refusals: 342 the ground is not at street level   (65% das recusas)
```

**Zero rondas completas em nove passadas** não distingue varredura lenta
de ciclos bloqueados por obra aberta. `SweepLog.busy` agora conta estes últimos.
Em 09-20, o guarda passou a contar 12.000 tiques de expediente, descontando
a noite sem zerar a espera e renovando o prazo quando há progresso.
O abandono cancela tarefas e limpa destinos, preservando a obra e seu lote.
Seis casos de `BuildProgressGameTest` passam; falta validar a segunda casa.
Não há evidência nova para declarar a varredura resolvida.

**2. O mineiro não entrega.** 66 pedras pedidas, **0 entregues**, 103
quebradas. A assinatura é o E44/E45 outra vez: `2 blocks below it and
unable to climb` e `got no closer than 11,7 blocks in 400 ticks`. O
cursor serve pedra sem rota de subida. O commit local `b7ef9e2` recalcula a
aproximação quando a altura muda, mas o log mostrou que a perna efetivamente
enviada à navegação ainda podia ser a boca três blocos acima. A correção atual
limita essa perna ao próximo patamar pisável; a entrega no save ainda não foi
confirmada.

**3. Playtest de 2026-09-20 — acavalamento e frente arenosa.** O último log
mostrou uma obra retomada ignorando blocos já existentes (`cut_sandstone`,
`smooth_sandstone`, baú e cama) e o mineiro desistindo repetidamente da
frente de arenito. O código agora rejeita, na escolha e na retomada, volumes
que intersectam peças de estruturas Vanilla da vila ou blocos físicos já
ocupados. Depois de cada bloco minerado, `MinerWork` mantém a posição como
âncora, espera a areia/gravilha assentar e escolhe a próxima frente; os drops
continuam passando por `MinerHaul` para o baú ou overflow. A proteção tem
GameTest local, mas a ausência de acavalamento e a progressão no deserto ainda
precisam ser confirmadas no save.

**4. Playtest de 2026-09-20 — casas consecutivas.** Depois de
`desert_medium_house_2`, o planejador abriu outra estrutura da família
`desert_small_house_6`. A seleção agora deriva da última construção concluída
e força `casa → tipo não residencial A → casa → tipo não residencial B`, sem
repetir o tipo A. A descoberta do lote continua passando pelo mesmo
`BuildSiteScanner` para todas as famílias; falta confirmar essa ordem no save.

---

## ✅ O rodízio de ofícios — corrigido em 09-19

**O defeito.** Um trabalhador (`5afa6bca`) largou **seis ofícios** em 41
minutos, voltando três vezes a `COLLECT_STONE` e três a
`BUILD_STRUCTURE`. As obras paravam no meio porque o construtor da vez
era o mineiro de dois minutos atrás.

**A causa, e ela não era o castigo.** A linha de reserva funcionava
exatamente como escrita — cada ofício largado ficava de castigo. Só que
o castigo é **por ofício** e a colônia tem **sete**: largar um é receber
o seguinte da ordem *na mesma passagem*. O trabalhador atravessava os
sete em oito ciclos, queimando um por ciclo, e nenhum castigo chegava a
significar nada porque sempre sobrava ofício virgem.

**Por que a cobertura não pegou.** O `ProfessionShunTest` media o castigo
**em repouso**: o trabalhador larga o ofício e fica parado enquanto os
ciclos passam. O defeito mora no **movimento** — ele é recontratado no
mesmo ciclo. Quatorze testes verdes e o jogo em rodízio.

**O conserto.** `Worker.BETWEEN_TRADES_CYCLES` — quem larga um ofício
espera quatro passagens antes de aceitar **qualquer** outro. A recusa
tem desfecho próprio no `HiringLog` (`just left a trade`), separada de
`SHUNNED`: somadas, escondiam justamente isto.

**Sinal a procurar no próximo jogo:** `hiring — … just left a trade`, e
a ausência do mesmo UUID largando ofício atrás de ofício.

---

## O que espera playtest

| item | sinal a procurar |
|---|---|
| **rodízio de ofícios curado** | `hiring — … just left a trade`, e nenhum UUID largando ofício atrás de ofício |
| obra retomada com acavalamento | `drops the saved … it sits inside something` |
| fornalha com um pouco de cada | dois ou mais `made … out of` por sessão |
| reserva do cru | `SANDSTONE` e `SMOOTH_SANDSTONE` convivendo |
| **segunda casa** | um segundo `the house is up` — o primeiro saiu 23:21:58 |
| obra sem progresso | `work ticks` só durante expediente; lote parcial preservado |
| varredura versus obra aberta | comparar `cycles never asked (a build was open)` com passadas |
| aproximação do mineiro | confirmar no jogo a perna por patamares e pedra entregue no baú |
| **retomada sem acavalamento** | nenhuma obra usa volume de estrutura Vanilla ou blocos já ocupados |
| **mineiro no deserto** | após cada quebra, a areia assenta, a frente é reavaliada e todos os drops chegam ao baú/overflow |

⚠️ A vila do save tem uma casa fechada e o planejador sem ronda completa.
Se nenhuma obra nova abrir, é o item 1 de «O que está aberto», e não
regressão do que fechou.

---

## 🔴 Dívida conhecida

**`ColonyDetectionGameTest`: falha histórica não reproduzida em 09-20.**
Em 09-19, duas rodadas na base limpa encontraram 24 trabalhadores em vez de
30. A causa continua sem diagnóstico; a suíte de 09-20 passou **378/378**.
Não atribuir essa divergência a um teto ou a uma corrida sem reprodução.

**`ChainRootsGameTest`: corrigido em 09-20.** O relatório chamava
`createDirectories(null)` para um arquivo sem pasta. A escrita agora vai
direto ao arquivo; a asserção de materiais voltou a executar e passou.

**O cenário do acavalamento em obra aberta** está no código e coberto por
teste unitário (`OverlapGuardTest`), mas sem gametest: toda versão que
escrevi derrubava o cenário acima.

---

## Como investigar aqui

**Instrumentar antes de consertar.** É o que funciona neste projeto, e o dia
09-19 mediu: três defeitos foram decididos **numa única leitura** depois de
instrumentados — P1.3 (`ProtectionSample`), P1.6 (`VolumeSample`) e o
`cut_sandstone` (`CraftReasons`).

E a instrumentação pega erro de quem a escreve: o `CraftReasons` acusou o
jogo de não ter uma receita que ele tem, e foi a própria linha que mostrou.

**Quando a mesma causa reaparece, desconfie da ferramenta.** Vários itens
caindo juntos com o mesmo sinal é assinatura compartilhada, não regressão
múltipla.

---

## Ferramentas de diagnóstico

| ferramenta | responde |
|---|---|
| `scripts/verdict.py` | o veredito de cada item pendente, lendo o log |
| `ChainRootsGameTest` | onde cada cadeia começa, por bioma — `chain-roots.txt` |
| `StructureCoverageGameTest` | quem fabrica cada peça — `structure-coverage.txt` |
| `CraftReasons` | por que a peça não saiu, no log |
| `VolumeSample` / `ProtectionSample` | de que as recusas de lote são feitas |
