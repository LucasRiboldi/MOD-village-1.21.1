# STATE — 2026-09-19

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Este arquivo chegou a **2.277 linhas** antes de
> 09-19: ele passou a atrasar a sessão que devia orientar.

---

## Onde a vila está

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

**1. A varredura não fecha uma ronda.** Depois da casa, nenhuma obra nova
abriu em 3,5 minutos — `no building work: still sweeping — the budget ran
out before an answer — looking for a lot`. O resumo de saída:

```
sweep: 9 planner runs, 9 passes over 8674 columns, 0 answered by the index
       — 0 complete rounds
lot columns: 30 survived every check, 528 were turned down
lot refusals: 342 the ground is not at street level   (65% das recusas)
```

**Zero rondas completas em nove passadas**, e o índice de ruas não
respondeu nada em nenhuma delas. É o próximo P0.

**2. O mineiro não entrega.** 66 pedras pedidas, **0 entregues**, 103
quebradas. A assinatura é o E44/E45 outra vez: `2 blocks below it and
unable to climb` e `got no closer than 11,7 blocks in 400 ticks`. O
cursor serve pedra sem rota de subida.

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

⚠️ A vila do save tem uma casa fechada e o planejador sem ronda completa.
Se nenhuma obra nova abrir, é o item 1 de «O que está aberto», e não
regressão do que fechou.

---

## 🔴 Dívida conhecida

**`ColonyDetectionGameTest` falha 2 de 2 rodadas na base limpa.** É
**pré-existente** ao trabalho de 09-19 e foi medido com `git stash`:
`esperava ao menos 30 trabalhadores, achei 24` — sempre **24**, nunca 25 ou
29. Número fixo não é corrida de relógio, é **teto**; a causa ainda não foi
achada.

**Isto custou caro:** eu atribuí essa falha a três mudanças minhas antes de
medir a base. A lição é a regra que já valia — *isolar contra a base antes
de acusar a própria mudança*.

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
